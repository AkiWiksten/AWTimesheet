package com.akiwiksten.awtimesheet.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationCardStateSaveableTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun locationCardState_persistsAfterActivityRecreate() {
        val initialPoint = Screen.LocationPoint(
            latitude = 60.1708,
            longitude = 24.9375,
            address = "Initial Address"
        )
        val updatedPoint = Screen.LocationPoint(
            latitude = 60.2055,
            longitude = 24.6559,
            address = "Updated Address"
        )

        composeRule.setContent {
            MaterialTheme {
                val cardState = rememberLocationCardState(
                    screen = Screen.Location(startPoint = initialPoint),
                    selectedRoute = null
                )

                Button(
                    onClick = {
                        cardState.value = cardState.value.copy(
                            startPoint = updatedPoint,
                            lastScreenStartPoint = updatedPoint
                        )
                    }
                ) {
                    Text(text = "Update")
                }

                Text(text = "Address: ${cardState.value.startAddress ?: "None"}")
            }
        }

        composeRule.onNodeWithText("Address: Initial Address").assertIsDisplayed()
        composeRule.onNodeWithText("Update").performClick()
        composeRule.onNodeWithText("Address: Updated Address").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Address: Updated Address").assertIsDisplayed()
    }
}

