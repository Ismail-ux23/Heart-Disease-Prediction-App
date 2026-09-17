"""
Trains the heart disease risk model on the REAL UCI Heart Disease dataset
(303 patient records from the Cleveland Clinic Foundation), instead of the
old 15-row synthetic dataset.

We keep the same 5 input features the Flask API (api.py) already expects
-- age, gender, blood_pressure, cholesterol, heart_rate -- by mapping them
from the UCI dataset's original column names:

    UCI column   -> API feature name
    ----------------------------------
    age          -> age
    sex          -> gender          (1 = male, 0 = female)
    trestbps     -> blood_pressure  (resting blood pressure, mm Hg)
    chol         -> cholesterol     (serum cholesterol, mg/dl)
    thalach      -> heart_rate      (max heart rate achieved)
    target       -> target          (1 = disease present, 0 = no disease)

This means api.py does NOT need to change -- it keeps sending the same
5 fields, they are just backed by a real, trained-on-real-data model now.
"""

import pandas as pd
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
import joblib

SOURCE_URL = "https://raw.githubusercontent.com/sharmaroshan/Heart-UCI-Dataset/master/heart.csv"

# ---------------------------------------------------------------------------
# 1. Load the real UCI dataset
# ---------------------------------------------------------------------------
# If you already downloaded it, this reads the local copy. Otherwise, you can
# grab it once with:
#   curl -o heart_uci_raw.csv https://raw.githubusercontent.com/sharmaroshan/Heart-UCI-Dataset/master/heart.csv
raw = pd.read_csv("heart_uci_raw.csv")

# ---------------------------------------------------------------------------
# 2. Select and rename the 5 columns our API uses
# ---------------------------------------------------------------------------
df = raw[["age", "sex", "trestbps", "chol", "thalach", "target"]].copy()
df.columns = ["age", "gender", "blood_pressure", "cholesterol", "heart_rate", "target"]

# Save the cleaned dataset (this replaces the old synthetic CSV)
df.to_csv("heart_disease_dataset.csv", index=False)
print(f"Dataset saved: {len(df)} real patient records -> heart_disease_dataset.csv")

# ---------------------------------------------------------------------------
# 3. Train / test split
# ---------------------------------------------------------------------------
X = df.drop("target", axis=1)
y = df["target"]

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

# ---------------------------------------------------------------------------
# 4. Train the model
# ---------------------------------------------------------------------------
model = RandomForestClassifier(n_estimators=200, max_depth=6, random_state=42)
model.fit(X_train, y_train)

# ---------------------------------------------------------------------------
# 5. Evaluate
# ---------------------------------------------------------------------------
y_pred = model.predict(X_test)
accuracy = accuracy_score(y_test, y_pred)
cv_scores = cross_val_score(model, X, y, cv=5)

print(f"\nTest set accuracy: {accuracy * 100:.2f}%")
print(f"5-fold cross-validation accuracy: {cv_scores.mean() * 100:.2f}% (+/- {cv_scores.std() * 100:.2f}%)")
print("\nClassification report:")
print(classification_report(y_test, y_pred, target_names=["Low Risk", "High Risk"]))

# ---------------------------------------------------------------------------
# 6. Save the trained model
# ---------------------------------------------------------------------------
joblib.dump(model, "heart_disease_model.pkl")
print("\nModel saved as 'heart_disease_model.pkl'")
