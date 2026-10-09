"""
Handles the SQLite database used to store prediction history (FR6, FR8).

A single table, `predictions`, stores every prediction made through the API:
who asked (optionally tagged by user_id, useful once the Android app has
login), what values were submitted, what the model predicted, and when.
"""

import sqlite3
import os
from pathlib import Path
from contextlib import closing
from datetime import datetime, timezone

DB_PATH = os.environ.get("PREDICTIONS_DB_PATH", str(Path(__file__).resolve().parent / "predictions.db"))


def get_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row  # lets us access columns by name
    return conn


def init_db():
    """Creates the predictions table if it doesn't already exist.
    Safe to call every time the app starts."""
    with closing(get_connection()) as conn, conn:
        conn.execute("""
            CREATE TABLE IF NOT EXISTS predictions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT DEFAULT 'default_user',
                age REAL NOT NULL,
                gender REAL NOT NULL,
                blood_pressure REAL NOT NULL,
                cholesterol REAL NOT NULL,
                heart_rate REAL NOT NULL,
                prediction TEXT NOT NULL,
                risk_probability REAL,
                created_at TEXT NOT NULL
            )
        """)


def save_prediction(user_id, age, gender, blood_pressure, cholesterol,
                     heart_rate, prediction, risk_probability):
    """Inserts one prediction record and returns its new id."""
    with closing(get_connection()) as conn, conn:
        cursor = conn.execute("""
            INSERT INTO predictions
                (user_id, age, gender, blood_pressure, cholesterol, heart_rate,
                 prediction, risk_probability, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            user_id, age, gender, blood_pressure, cholesterol, heart_rate,
            prediction, risk_probability, datetime.now(timezone.utc).isoformat()
        ))
        new_id = cursor.lastrowid
        return new_id


def get_history(user_id="default_user", limit=50):
    """Returns the most recent predictions for a user, newest first."""
    with closing(get_connection()) as conn, conn:
        rows = conn.execute("""
            SELECT * FROM predictions
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT ?
        """, (user_id, limit)).fetchall()
        return [dict(row) for row in rows]
