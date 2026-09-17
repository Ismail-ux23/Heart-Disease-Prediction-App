from flask import Flask, request, jsonify
import joblib
import numpy as np

from database import init_db, save_prediction, get_history

app = Flask(__name__)

# Load the trained model (now trained on the real UCI dataset)
try:
    model = joblib.load('heart_disease_model.pkl')
except Exception as e:
    print("Error loading model. Make sure you have trained it first.")
    model = None

# Create the predictions table if it doesn't exist yet (FR6, FR8)
init_db()


def build_recommendations(prediction_label, age, blood_pressure, cholesterol, heart_rate):
    """
    Simple rule-based health tips (FR5). This is NOT medical advice --
    it's meant to nudge the user toward seeing a doctor and give basic
    context on which numbers look out of range.
    """
    tips = []

    if prediction_label == "High Risk":
        tips.append("Your inputs suggest an elevated risk. Please consult a doctor or cardiologist soon.")
    else:
        tips.append("Your inputs suggest a lower risk, but regular checkups are still recommended.")

    if blood_pressure >= 140:
        tips.append("Your blood pressure is in a high range (>=140 mm Hg). Consider reducing salt intake and monitoring it regularly.")
    elif blood_pressure >= 130:
        tips.append("Your blood pressure is slightly elevated (130-139 mm Hg).")

    if cholesterol >= 240:
        tips.append("Your cholesterol level is high (>=240 mg/dl). A heart-healthy diet and exercise can help lower it.")
    elif cholesterol >= 200:
        tips.append("Your cholesterol is borderline high (200-239 mg/dl).")

    if heart_rate < 60:
        tips.append("Your heart rate is on the lower side (<60 bpm); mention this to your doctor if you feel unusually tired or dizzy.")
    elif heart_rate > 100:
        tips.append("Your heart rate is on the higher side (>100 bpm) for a resting measurement.")

    if age >= 55:
        tips.append("Heart disease risk naturally increases with age; annual screening is a good idea.")

    return tips


@app.route('/predict', methods=['POST'])
def predict():
    if model is None:
        return jsonify({'error': 'Model not loaded on server'}), 500

    try:
        data = request.get_json()

        # Extract features
        age = float(data.get('age', 0))
        gender = float(data.get('gender', 0))
        blood_pressure = float(data.get('blood_pressure', 0))
        cholesterol = float(data.get('cholesterol', 0))
        heart_rate = float(data.get('heart_rate', 0))

        # Optional: tag predictions per user once the Android app has login.
        # Until then, everything is grouped under 'default_user'.
        user_id = str(data.get('user_id', 'default_user'))

        # Create input array
        features = np.array([[age, gender, blood_pressure, cholesterol, heart_rate]])

        # Predict
        prediction = model.predict(features)[0]
        # predict_proba gives us a confidence score to show in the app
        risk_probability = float(model.predict_proba(features)[0][1])  # P(High Risk)

        result = "High Risk" if prediction == 1 else "Low Risk"

        # FR5: basic recommendations
        recommendations = build_recommendations(result, age, blood_pressure, cholesterol, heart_rate)

        # FR6 / FR8: store this prediction so it shows up in history
        record_id = save_prediction(
            user_id, age, gender, blood_pressure, cholesterol, heart_rate,
            result, risk_probability
        )

        # FR4: return a clean, easy-to-render response for the app
        return jsonify({
            'id': record_id,
            'prediction': result,
            'risk_probability': round(risk_probability, 3),
            'recommendations': recommendations
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 400


@app.route('/history', methods=['GET'])
def history():
    """FR6: lets the app fetch a user's previous predictions.
    Usage: GET /history?user_id=default_user&limit=20
    """
    user_id = request.args.get('user_id', 'default_user')
    limit = int(request.args.get('limit', 50))

    records = get_history(user_id=user_id, limit=limit)
    return jsonify({'user_id': user_id, 'count': len(records), 'history': records})


if __name__ == '__main__':
    # Run on all interfaces so the Android emulator (10.0.2.2) can access it
    app.run(host='0.0.0.0', port=5000, debug=True)
