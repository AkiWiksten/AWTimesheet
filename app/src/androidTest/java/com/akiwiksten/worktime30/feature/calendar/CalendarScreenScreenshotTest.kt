package com.akiwiksten.worktime30.feature.calendar

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class CalendarScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_screenshot() {
        setCalendarContent(CalendarUiState.Loading)
        saveRootScreenshot(fileName = "calendar_loading")
    }

    @Test
    fun successState_screenshot() {
        setCalendarContent(
            CalendarUiState.Success(
                date = "2026-04-10",
                timePerDay = "08:30 h",
                timePerWeek = "42:30 h",
                timePerMonth = "170:00 h",
                workDaysMonth = listOf(
                    WorkdayEntity(date = "2026-04-09", workTimeToday = "08:00"),
                    WorkdayEntity(date = "2026-04-10", workTimeToday = "08:30")
                )
            )
        )
        saveRootScreenshot(fileName = "calendar_success")
    }

    @Test
    fun errorState_screenshot() {
        setCalendarContent(CalendarUiState.Error(message = "Failed to load calendar data"))
        saveRootScreenshot(fileName = "calendar_error")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setCalendarContent(uiState: CalendarUiState) {
        composeTestRule.setContent {
            WorkTime30Theme(dynamicColor = false) {
                CalendarContent(
                    uiState = uiState,
                    datePickerState = rememberDatePickerState()
                )
            }
        }
    }

    private fun saveRootScreenshot(fileName: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val outputDir = File(composeTestRule.activity.filesDir, "test-screenshots/calendar").apply {
            mkdirs()
        }
        val outputFile = File(outputDir, "$fileName.png")

        FileOutputStream(outputFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        check(bitmap.width > 0 && bitmap.height > 0) {
            "Captured screenshot is empty for $fileName"
        }
    }
}

