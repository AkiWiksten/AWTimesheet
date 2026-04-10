package com.akiwiksten.worktime30.feature.workday

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class WorkdayScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_screenshot() {
        setWorkdayContent(uiState = WorkdayUiState.Loading)
        saveRootScreenshot(fileName = "workday_loading")
    }

    @Test
    fun successState_screenshot() {
        setWorkdayContent(
            uiState = WorkdayUiState.Success(
                date = "2026-04-10",
                projectName = "Beta Support",
                startTime = "08:00",
                endTime = "16:30",
                lunchStart = "11:30",
                lunchEnd = "12:00",
                breakStart = "14:15",
                breakEnd = "14:30",
                workTimeToday = "08:00",
                dailyWorkTime = "07:30",
                lunchTime = "00:30",
                balanceToday = "+00:30",
                workTimeTotal = "140:00",
                balanceTotal = "+04:10",
                isNewDay = false
            ),
            projectName = "Beta Support"
        )
        saveRootScreenshot(fileName = "workday_success")
    }

    @Test
    fun errorState_screenshot() {
        setWorkdayContent(uiState = WorkdayUiState.Error(message = "Failed to load workday"))
        saveRootScreenshot(fileName = "workday_error")
    }

    private fun setWorkdayContent(uiState: WorkdayUiState, projectName: String? = null) {
        composeTestRule.setContent {
            WorkTime30Theme(dynamicColor = false) {
                WorkdayStateContent(
                    padding = PaddingValues(0.dp),
                    uiState = uiState,
                    projectName = projectName,
                    actions = WorkdayScreenActions()
                )
            }
        }
    }

    private fun saveRootScreenshot(fileName: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val outputDir = File(composeTestRule.activity.filesDir, "test-screenshots/workday").apply {
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

