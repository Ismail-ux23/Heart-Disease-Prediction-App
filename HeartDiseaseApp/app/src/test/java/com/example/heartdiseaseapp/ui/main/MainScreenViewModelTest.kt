package com.example.heartdiseaseapp.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun validInputPreservesNumbersAndSexEncoding() {
        val input = PredictionInput.parse(" 55 ", "Female", "130", "220", "160")
        assertEquals(55.0, input.age, 0.0)
        assertEquals(0.0, input.gender, 0.0)
        assertEquals(160.0, input.heartRate, 0.0)
        assertEquals(1.0, PredictionInput.parse("55", "1", "130", "220", "160").gender, 0.0)
    }

    @Test
    fun malformedNumbersCannotBecomeZeroMeasurements() {
        for (value in listOf("", "abc", "NaN", "Infinity", "0", "-1")) {
            assertThrows(IllegalArgumentException::class.java) {
                PredictionInput.parse(value, "Male", "130", "220", "160")
            }
            assertThrows(IllegalArgumentException::class.java) {
                PredictionInput.parse("55", "Male", "130", "220", value)
            }
        }
    }

    @Test
    fun unknownGenderCannotSilentlyBecomeFemale() {
        for (value in listOf("", "unknown", "2")) {
            assertThrows(IllegalArgumentException::class.java) {
                PredictionInput.parse("55", value, "130", "220", "160")
            }
        }
    }
}
