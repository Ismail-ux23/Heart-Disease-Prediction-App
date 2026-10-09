import sqlite3
import numpy as np
import pytest

import api
import database

VALUES = dict(age=55, gender=1, blood_pressure=130, cholesterol=220, heart_rate=160)


class FakeModel:
    classes_ = np.array([0, 1])
    def predict(self, frame):
        assert list(frame.columns) == api.FEATURES
        return np.array([1])
    def predict_proba(self, frame):
        return np.array([[0.25, 0.75]])


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setattr(database, 'DB_PATH', str(tmp_path / 'history.db'))
    database.init_db()
    monkeypatch.setattr(api, 'model', FakeModel())
    api.app.config['TESTING'] = True
    return api.app.test_client()


@pytest.mark.parametrize('data', [None, [], 5, 'text', {}, {**VALUES, 'age': None},
    {**VALUES, 'age': True}, {**VALUES, 'age': 'abc'}, {**VALUES, 'age': 'nan'},
    {**VALUES, 'cholesterol': 'inf'}, {**VALUES, 'blood_pressure': 0},
    {**VALUES, 'heart_rate': -1}, {**VALUES, 'gender': 2}, {**VALUES, 'gender': 0.5},
    {**VALUES, 'user_id': []}, {**VALUES, 'user_id': ''}])
def test_invalid_input_never_saves(client, data):
    assert client.post('/predict', json=data).status_code == 400
    assert database.get_history() == []


def test_malformed_json_is_client_error(client):
    assert client.post('/predict', data='{bad', content_type='application/json').status_code == 400


def test_prediction_and_user_history(client):
    response = client.post('/predict', json=dict(VALUES, user_id='alice'))
    assert response.status_code == 200
    assert response.json['risk_probability'] == 0.75
    assert 'not a diagnosis' in response.json['disclaimer']
    assert client.get('/history?user_id=alice').json['count'] == 1
    assert client.get('/history?user_id=bob').json['count'] == 0


def test_probability_uses_class_label_not_column_position(client, monkeypatch):
    class ReversedModel(FakeModel):
        classes_ = np.array([1, 0])
        def predict_proba(self, frame):
            return np.array([[0.75, 0.25]])
    monkeypatch.setattr(api, 'model', ReversedModel())
    assert client.post('/predict', json=VALUES).json['risk_probability'] == 0.75


@pytest.mark.parametrize('limit', ['abc', '0', '-1', '101', '1.5', ''])
def test_invalid_history_limit(client, limit):
    assert client.get('/history?limit=' + limit).status_code == 400


def test_missing_model_is_unavailable(client, monkeypatch):
    monkeypatch.setattr(api, 'model', None)
    assert client.get('/health').status_code == 503
    assert client.post('/predict', json=VALUES).status_code == 503
    assert database.get_history() == []


def test_server_failures_are_not_client_errors_or_leaked(client, monkeypatch):
    class BrokenModel(FakeModel):
        def predict(self, frame):
            raise RuntimeError('private-path')
    monkeypatch.setattr(api, 'model', BrokenModel())
    response = client.post('/predict', json=VALUES)
    assert response.status_code == 500
    assert b'private-path' not in response.data
    assert database.get_history() == []


def test_failed_database_write_is_server_error(client, monkeypatch):
    def fail(*args):
        raise sqlite3.OperationalError('private-database-path')
    monkeypatch.setattr(api, 'save_prediction', fail)
    response = client.post('/predict', json=VALUES)
    assert response.status_code == 500
    assert b'private-database-path' not in response.data


@pytest.mark.parametrize('probabilities', [[[float('nan'), 0.5]], [[-0.5, 1.5]], [[0.4, 0.4]]])
def test_invalid_model_probability_is_not_saved(client, monkeypatch, probabilities):
    model = FakeModel()
    model.predict_proba = lambda frame: np.array(probabilities)
    monkeypatch.setattr(api, 'model', model)
    assert client.post('/predict', json=VALUES).status_code == 500
    assert database.get_history() == []


def test_failed_real_sql_write_closes_connection_and_saves_nothing(client, monkeypatch):
    connection = database.get_connection()
    monkeypatch.setattr(database, 'get_connection', lambda: connection)
    with pytest.raises(sqlite3.IntegrityError):
        database.save_prediction('demo', float('nan'), 1, 130, 220, 160, 'High Risk', 0.75)
    with pytest.raises(sqlite3.ProgrammingError):
        connection.execute('SELECT 1')
    with sqlite3.connect(database.DB_PATH) as check:
        assert check.execute('SELECT COUNT(*) FROM predictions').fetchone()[0] == 0
