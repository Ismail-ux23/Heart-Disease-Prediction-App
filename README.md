# Heart Disease Prediction Demo

Flask + SQLite backend and a Kotlin/Compose Android client for an educational
five-feature Random Forest classifier. The `High Risk` / `Low Risk` strings are
legacy model class labels, not diagnoses. `risk_probability` is the model's
class-1 score; this project has not established calibrated personal risk or
clinical validity. It should not guide medical decisions.

## Backend setup

Python 3.12 is used in CI:

```bash
python -m venv .venv
# Activate .venv for your operating system, then:
python -m pip install -r requirements-dev.txt
python train_model.py
python api.py
```

Training reads the repository's `heart_uci_raw.csv` and writes a model beside
`train_model.py`. Use the same scikit-learn environment to train and serve. Only
load trusted local joblib/pickle files: they can execute Python code. The bundled
model may require retraining with your installed scikit-learn version.

`GET /health` returns 200 when a model is loaded and 503 otherwise. Predictions
return 503 without a model, 400 for invalid input, and a generic 500 for inference
or storage failures. The server runs on port 5000 with Flask debugging disabled.

## Inputs and history

`POST /predict` accepts all five required finite numeric fields:

```json
{"age":55,"gender":1,"blood_pressure":130,"cholesterol":220,"heart_rate":160,"user_id":"demo"}
```

- `age`: age in years, greater than zero.
- `gender`: legacy API name for the dataset's binary sex encoding, 0 or 1.
- `blood_pressure`: resting blood pressure in mm Hg, greater than zero.
- `cholesterol`: serum cholesterol in mg/dl, greater than zero.
- `heart_rate`: **maximum heart rate achieved**, in bpm, greater than zero.
  It is the training column `thalach`, not a resting pulse measurement.

Feature definitions: [UCI Heart Disease dataset](https://archive.ics.uci.edu/dataset/45/heart+disease).
Validation bounds are technical checks, not healthy/unsafe clinical thresholds.
The API preserves its response keys for Android compatibility. Recommendations
now explain the demo's limitations rather than interpreting maximum achieved
heart rate using resting-pulse thresholds.

`GET /history?user_id=demo&limit=20` returns recent records; limit must be an
integer from 1 through 100. History defaults to `default_user`. This is a local
prototype: `user_id` is a client-supplied grouping label, not authentication.
Anyone who can access the API can request another label's history. Use synthetic
inputs for demonstrations and do not expose this service with personal records.

SQLite defaults to `predictions.db` beside `database.py`, independently of the
working directory. Set `PREDICTIONS_DB_PATH` to use an existing database or a
separate test database. No schema migration is required. If an older database was
created elsewhere, point this environment variable at it or move it explicitly.
Connections close on query failures and transactions roll back failed writes.

## Android

Open `HeartDiseaseApp` in Android Studio. The client uses the Android emulator's
`http://10.0.2.2:5000` address; start the backend separately. A physical device
needs a reachable backend address. Requests have connection/read timeouts.
Client validation rejects missing/nonfinite values and unknown sex encodings.
The screen labels maximum achieved heart rate and displays the score as a model
class score. The local HTTP/cleartext configuration is for this demo.

```bash
cd HeartDiseaseApp
./gradlew testDebugUnitTest assembleDebug
./gradlew connectedDebugAndroidTest
```

The original placeholder tests referred to missing classes and an obsolete screen
signature; they are replaced with input validation and current screen tests.
Android compilation/device tests remain unverified in this environment because
the Gradle distribution download is unreachable. Backend CI does not verify the
Android build.

## Backend tests

```bash
python -m pytest -q
```

Tests use temporary SQLite databases and deterministic classifiers. They verify
validation, class-probability ordering, history grouping and limits, unavailable
models, invalid model outputs and generic error handling. They do not establish
clinical validity or model accuracy. GitHub Actions runs the backend suite.
