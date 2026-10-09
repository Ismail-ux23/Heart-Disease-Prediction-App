package com.example.heartdiseaseapp.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun demoExplainsMaximumHeartRateInput() {
        composeTestRule.setContent { MainScreen(onItemClick = {}) }
        composeTestRule.onNodeWithText("Max achieved HR (bpm)").assertExists()
        composeTestRule.onNodeWithText("Run demo prediction").assertExists()
    }
}
