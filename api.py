"""Local educational classifier API; outputs are model scores, not diagnoses."""
import math
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from flask import Flask, request, jsonify
from werkzeug.exceptions import BadRequest, UnsupportedMediaType

from database import init_db, save_prediction, get_history

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024
BASE_DIR = Path(__file__).resolve().parent
FEATURES = ['age', 'gender', 'blood_pressure', 'cholesterol', 'heart_rate']
DISCLAIMER = 'Educational model output only; not a diagnosis or a calibrated personal risk estimate.'
try:
    # Never load model artifacts from untrusted sources.
    model = joblib.load(BASE_DIR / 'heart_disease_model.pkl')
except Exception:
    app.logger.warning('Model unavailable. Retrain using train_model.py.')
    model = None
init_db()


def parse_user_id(value):
    if not isinstance(value, str) or not value.strip() or len(value.strip()) > 100:
        raise ValueError('user_id must be a nonempty string of at most 100 characters.')
    return value.strip()


def parse_features(data):
    if not isinstance(data, dict):
        raise ValueError('Send a JSON object with all five feature fields.')
    features = {}
    for key in FEATURES:
        value = data.get(key)
        if value is None or isinstance(value, bool):
            raise ValueError(f'{key} is required and must be a finite number.')
        try:
            number = float(value)
        except (TypeError, ValueError, OverflowError):
            raise ValueError(f'{key} must be a finite number.') from None
        if not math.isfinite(number):
            raise ValueError(f'{key} must be a finite number.')
        if key == 'gender':
            if number not in (0, 1):
                raise ValueError('gender must use the dataset encoding 0 or 1.')
        elif number <= 0:
            raise ValueError(f'{key} must be greater than zero.')
        features[key] = number
    return features


@app.get('/health')
def health():
    return jsonify(status='ready' if model is not None else 'model_unavailable'), 200 if model is not None else 503


@app.post('/predict')
def predict():
    try:
        data = request.get_json()
        values = parse_features(data)
        user_id = parse_user_id(data.get('user_id', 'default_user'))
    except (BadRequest, UnsupportedMediaType, ValueError) as exc:
        message = str(exc) if isinstance(exc, ValueError) else 'Send a valid JSON object.'
        return jsonify(error=message), 400
    if model is None:
        return jsonify(error='Model unavailable. Retrain using train_model.py.'), 503
    try:
        frame = pd.DataFrame([values], columns=FEATURES)
        classes = list(model.classes_)
        if len(classes) != 2 or set(classes) != {0, 1}:
            raise ValueError('Expected binary model classes 0 and 1')
        prediction = model.predict(frame)[0]
        probabilities = np.asarray(model.predict_proba(frame), dtype=float)
        if prediction not in (0, 1) or probabilities.shape != (1, 2):
            raise ValueError('Unexpected model output')
        if not np.isfinite(probabilities).all() or (probabilities < 0).any() or (probabilities > 1).any():
            raise ValueError('Invalid model scores')
        if not np.isclose(probabilities[0].sum(), 1):
            raise ValueError('Invalid model scores')
        risk_probability = float(probabilities[0][classes.index(1)])
    except Exception:
        app.logger.exception('Prediction failed')
        return jsonify(error='Prediction failed. Check the server logs.'), 500
    result = 'High Risk' if prediction == 1 else 'Low Risk'
    try:
        record_id = save_prediction(user_id, *(values[key] for key in FEATURES), result, risk_probability)
    except Exception:
        app.logger.exception('Saving prediction failed')
        return jsonify(error='Could not save prediction history.'), 500
    return jsonify(id=record_id, prediction=result, risk_probability=round(risk_probability, 3),
                   recommendations=[DISCLAIMER], disclaimer=DISCLAIMER,
                   probability_interpretation='Model score for class 1; not calibrated individual risk.')


@app.get('/history')
def history():
    try:
        user_id = parse_user_id(request.args.get('user_id', 'default_user'))
        limit = int(request.args.get('limit', '50'))
        if not 1 <= limit <= 100:
            raise ValueError('limit must be between 1 and 100.')
    except (ValueError, TypeError):
        return jsonify(error='Use a nonempty user_id (at most 100 characters) and a whole-number limit between 1 and 100.'), 400
    try:
        records = get_history(user_id=user_id, limit=limit)
    except Exception:
        app.logger.exception('Reading history failed')
        return jsonify(error='Could not read prediction history.'), 500
    return jsonify(user_id=user_id, count=len(records), history=records)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
