package com.example.heartdiseaseapp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.heartdiseaseapp.theme.*

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var bloodPressure by remember { mutableStateOf("") }
    var cholesterol by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }

    val glassShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Ink, Color(0xFF123E4A), Color(0xFF10213C))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = 220.dp, y = (-70).dp)
                .background(Brush.radialGradient(listOf(Color(0x6656D9C4), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-150).dp, y = 480.dp)
                .background(Brush.radialGradient(listOf(Color(0x5593BFFF), Color.Transparent)))
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 32.dp)
        ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("HEART HEALTH", style = MaterialTheme.typography.labelMedium, color = Teal, fontWeight = FontWeight.Bold)
                Text("Know your risk.", style = MaterialTheme.typography.displaySmall, color = Cloud, fontWeight = FontWeight.Bold)
                Text("A calm snapshot from five everyday health measurements.", color = MutedCloud)
            }
        }

        item {
            GlassPanel(glassShape) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your snapshot", style = MaterialTheme.typography.titleLarge, color = Cloud, fontWeight = FontWeight.SemiBold)
                    Text("Use your latest resting measurements for a more useful estimate.", style = MaterialTheme.typography.bodySmall, color = MutedCloud)
                    GlassField(age, { age = it }, "Age", KeyboardType.Number)
                    GlassField(gender, { gender = it }, "Gender  •  Male / Female or 1 / 0")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassField(bloodPressure, { bloodPressure = it }, "Blood pressure", KeyboardType.Number, Modifier.weight(1f))
                        GlassField(heartRate, { heartRate = it }, "Heart rate", KeyboardType.Number, Modifier.weight(1f))
                    }
                    GlassField(cholesterol, { cholesterol = it }, "Cholesterol", KeyboardType.Number)
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.predict(age, gender, bloodPressure, cholesterol, heartRate)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink)
            ) {
                Text("Check my risk", fontWeight = FontWeight.Bold)
            }
        }

        item {
            when (state) {
                is MainScreenUiState.Idle -> GlassHint("Ready when you are", "Your result will appear here after you submit the snapshot.")
                is MainScreenUiState.Loading -> GlassHint("Reading your snapshot…", "Crunching the numbers", true)
                is MainScreenUiState.Success -> {
                    val result = state as MainScreenUiState.Success
                    GlassPanel(glassShape, accent = if (result.prediction == "High Risk") Coral else Teal) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("YOUR RESULT", style = MaterialTheme.typography.labelMedium, color = Teal, fontWeight = FontWeight.Bold)
                            Text(result.prediction, style = MaterialTheme.typography.headlineSmall, color = Cloud, fontWeight = FontWeight.Bold)
                            Text("Estimated high-risk probability: ${result.confidencePercent}%", color = MutedCloud)
                            HorizontalDivider(color = GlassBorder)
                            Text("Recommendations", style = MaterialTheme.typography.titleMedium, color = Cloud, fontWeight = FontWeight.SemiBold)
                            result.recommendations.forEach { recommendation -> Text("• $recommendation", color = MutedCloud) }
                        }
                    }
                }
                is MainScreenUiState.Error -> GlassHint("Could not reach the predictor", (state as MainScreenUiState.Error).message, accent = Coral)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Recent checks", style = MaterialTheme.typography.titleLarge, color = Cloud, fontWeight = FontWeight.SemiBold)
                    Text("Your latest snapshots", style = MaterialTheme.typography.bodySmall, color = MutedCloud)
                }
                Text("${history.size} total", style = MaterialTheme.typography.labelMedium, color = Teal)
            }
        }

        if (history.isEmpty()) {
            item { Text("No predictions recorded yet.", color = MutedCloud) }
        } else {
            items(history) { item ->
                GlassPanel(glassShape, accent = if (item.prediction == "High Risk") Coral else Teal) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.prediction, style = MaterialTheme.typography.titleMedium, color = Cloud, fontWeight = FontWeight.SemiBold)
                        Text("Age ${item.age.toInt()}  •  BP ${item.bloodPressure.toInt()}  •  Cholesterol ${item.cholesterol.toInt()}", color = MutedCloud)
                        Text(item.createdAt, style = MaterialTheme.typography.bodySmall, color = MutedCloud.copy(alpha = .75f))
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun GlassPanel(shape: RoundedCornerShape, accent: Color = Teal, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GlassWhite)
            .border(1.dp, GlassBorder, shape)
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Teal,
            unfocusedBorderColor = GlassBorder,
            focusedLabelColor = Teal,
            unfocusedLabelColor = MutedCloud,
            focusedTextColor = Cloud,
            unfocusedTextColor = Cloud,
            cursorColor = Teal,
            focusedContainerColor = Color.White.copy(alpha = .06f),
            unfocusedContainerColor = Color.White.copy(alpha = .03f)
        )
    )
}

@Composable
private fun GlassHint(title: String, message: String, showProgress: Boolean = false, accent: Color = Teal) {
    GlassPanel(RoundedCornerShape(20.dp), accent) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (showProgress) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accent, strokeWidth = 2.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Cloud, fontWeight = FontWeight.SemiBold)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MutedCloud)
            }
        }
    }
}
