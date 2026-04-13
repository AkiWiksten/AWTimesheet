package com.akiwiksten.worktime30.feature.settings

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akiwiksten.worktime30.core.LOADING_INDICATOR_DELAY_MS
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class SettingsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_beforeDelay_screenshot() {
        composeTestRule.mainClock.autoAdvance = false
        setSettingsContent(uiState = SettingsUiState.Loading)
        composeTestRule.runOnIdle {
            // Ensure composition and layout are complete
        }
        saveRootScreenshot(fileName = "settings_loading_before_delay")
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun loadingState_afterDelay_screenshot() {
        composeTestRule.mainClock.autoAdvance = false
        setSettingsContent(uiState = SettingsUiState.Loading)
        composeTestRule.mainClock.advanceTimeBy(LOADING_INDICATOR_DELAY_MS + 50L)
        composeTestRule.waitForIdle()
        saveRootScreenshot(fileName = "settings_loading_after_delay")
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun successState_screenshot() {
        setSettingsContent(
            uiState = SettingsUiState.Success(
                name = "Aki Wiksten",
                employer = "WorkTime Oy",
                endMonthDate = "2026-04-30",
                workTypes = listOf("Installation", "Maintenance", "Meeting"),
                projectsByMonth = listOf(
                    ProjectEntity(
                        date = "2026-04-10",
                        projectName = "Alpha Site",
                        projectTime = "04:00",
                        kilometres = 20,
                        allowance = "Daily allowance",
                        workType = "Installation"
                    )
                )
            )
        )
        saveRootScreenshot(fileName = "settings_success")
    }

    @Test
    fun errorState_screenshot() {
        setSettingsContent(uiState = SettingsUiState.Error(message = "Failed to load settings"))
        saveRootScreenshot(fileName = "settings_error")
    }

    private fun setSettingsContent(uiState: SettingsUiState) {
        composeTestRule.setContent {
            WorkTime30Theme(dynamicColor = false) {
                SettingsStateContent(
                    uiState = uiState,
                    createActions = { _ ->
                        SettingsActions(
                            onNameChange = {},
                            onEmployerChange = {},
                            onWorkTypeAdded = {},
                            onWorkTypeRemoved = {},
                            onSave = {},
                            onGeneratePdf = {}
                        )
                    }
                )
            }
        }
    }

    private fun saveRootScreenshot(fileName: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val outputDir = File(composeTestRule.activity.filesDir, "test-screenshots/settings").apply {
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

