package com.example.heartdiseaseapp.ui.main

/** Syntax/range validation for the demo API, not clinical interpretation. */
data class PredictionInput(
    val age: Double,
    val gender: Double,
    val bloodPressure: Double,
    val cholesterol: Double,
    val heartRate: Double
) {
    companion object {
        fun parse(age: String, gender: String, bloodPressure: String, cholesterol: String, heartRate: String): PredictionInput {
            fun positive(text: String, label: String): Double {
                val number = text.trim().toDoubleOrNull()
                require(number != null && number.isFinite() && number > 0) {
                    "$label must be a positive, finite number."
                }
                return number
            }
            val encodedGender = when (gender.trim().lowercase()) {
                "male", "1" -> 1.0
                "female", "0" -> 0.0
                else -> throw IllegalArgumentException("Enter Male, Female, 1 or 0 for the dataset sex encoding.")
            }
            return PredictionInput(positive(age, "Age"), encodedGender,
                positive(bloodPressure, "Blood pressure"), positive(cholesterol, "Cholesterol"),
                positive(heartRate, "Maximum achieved heart rate"))
        }
    }
}
