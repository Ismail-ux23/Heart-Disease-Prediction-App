package com.example.heartdiseaseapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class MainScreenViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Idle)
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<PredictionHistoryItem>>(emptyList())
    val history: StateFlow<List<PredictionHistoryItem>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    fun predict(age: String, gender: String, bloodPressure: String, cholesterol: String, heartRate: String) {
        val input = try {
            PredictionInput.parse(age, gender, bloodPressure, cholesterol, heartRate)
        } catch (error: IllegalArgumentException) {
            _uiState.value = MainScreenUiState.Error(error.message ?: "Check your input values.")
            return
        }
        _uiState.value = MainScreenUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonParam = JSONObject()
                jsonParam.put("age", input.age)
                jsonParam.put("gender", input.gender)
                jsonParam.put("blood_pressure", input.bloodPressure)
                jsonParam.put("cholesterol", input.cholesterol)
                jsonParam.put("heart_rate", input.heartRate)

                val url = URL("http://10.0.2.2:5000/predict")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true
                conn.doInput = true

                OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                    os.write(jsonParam.toString())
                    os.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                    val responseStr = reader.use { it.readText() }
                    val jsonResponse = JSONObject(responseStr)
                    val prediction = jsonResponse.optString("prediction", "Unknown")
                    val recommendations = jsonResponse.optJSONArray("recommendations")
                        ?.let { array -> List(array.length()) { index -> array.getString(index) } }
                        ?: emptyList()
                    val confidence = (jsonResponse.optDouble("risk_probability", 0.0) * 100).roundToInt()
                    _uiState.value = MainScreenUiState.Success(prediction, confidence, recommendations)
                    loadHistory()
                } else {
                    val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    val message = errorText?.let { JSONObject(it).optString("error") }
                    _uiState.value = MainScreenUiState.Error(message?.takeIf { it.isNotBlank() } ?: "Server returned code: $responseCode")
                }
                conn.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = MainScreenUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val connection = URL("http://10.0.2.2:5000/history?user_id=default_user&limit=20")
                    .openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                        .use { it.readText() }
                    val records = JSONObject(response).optJSONArray("history")
                    _history.value = records?.let { array ->
                        List(array.length()) { index ->
                            val item = array.getJSONObject(index)
                            PredictionHistoryItem(
                                prediction = item.optString("prediction", "Unknown"),
                                age = item.optDouble("age"),
                                bloodPressure = item.optDouble("blood_pressure"),
                                cholesterol = item.optDouble("cholesterol"),
                                createdAt = item.optString("created_at")
                            )
                        }
                    } ?: emptyList()
                }
                connection.disconnect()
            } catch (_: Exception) {
                // History is supplemental; prediction remains usable if it is unavailable.
            }
        }
    }
}

sealed interface MainScreenUiState {
    object Idle : MainScreenUiState
    object Loading : MainScreenUiState
    data class Error(val message: String) : MainScreenUiState
    data class Success(
        val prediction: String,
        val confidencePercent: Int,
        val recommendations: List<String>
    ) : MainScreenUiState
}

data class PredictionHistoryItem(
    val prediction: String,
    val age: Double,
    val bloodPressure: Double,
    val cholesterol: Double,
    val createdAt: String
)
