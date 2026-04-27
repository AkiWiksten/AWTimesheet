package com.akiwiksten.worktime30.feature.projects

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akiwiksten.worktime30.core.LOADING_INDICATOR_DELAY_MS
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.single.SingleProjectScreenContent
import com.akiwiksten.worktime30.feature.projects.single.SingleProjectScreenContentParams
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class SingleProjectScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_beforeDelay_screenshot() {
        composeTestRule.mainClock.autoAdvance = false
        setSingleProjectContent(
            params = SingleProjectScreenContentParams(
                date = "2026-04-10",
                state = SingleProjectState(),
                isAddMode = true,
                projectsUiState = WorkdayUiState.Loading,
                isConfirmEnabled = false,
                onStateChange = {},
                onNavigateBack = {},
                onOpenProjectDetails = {},
                onConfirm = {}
            )
        )
        composeTestRule.waitForIdle()
        saveRootScreenshot(fileName = "single_project_loading_before_delay")
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun loadingState_afterDelay_screenshot() {
        composeTestRule.mainClock.autoAdvance = false
        setSingleProjectContent(
            params = SingleProjectScreenContentParams(
                date = "2026-04-10",
                state = SingleProjectState(),
                isAddMode = true,
                projectsUiState = WorkdayUiState.Loading,
                isConfirmEnabled = false,
                onStateChange = {},
                onNavigateBack = {},
                onOpenProjectDetails = {},
                onConfirm = {}
            )
        )
        composeTestRule.mainClock.advanceTimeBy(LOADING_INDICATOR_DELAY_MS + 50L)
        composeTestRule.waitForIdle()
        saveRootScreenshot(fileName = "single_project_loading_after_delay")
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun successState_screenshot() {
        setSingleProjectContent(
            params = SingleProjectScreenContentParams(
                date = "2026-04-10",
                state = SingleProjectState(
                    projectName = "Beta Support",
                    projectTime = "03:30",
                    kilometres = "18",
                    allowance = "Full allowance",
                    workType = "Maintenance",
                ),
                isAddMode = false,
                projectsUiState = WorkdayUiState.Success(
                    date = "2026-04-10",
                    workTimeToday = "07:45",
                    workTypes = listOf("Installation", "Maintenance", "Meeting")
                ),
                isConfirmEnabled = true,
                onStateChange = {},
                onNavigateBack = {},
                onOpenProjectDetails = {},
                onConfirm = {}
            )
        )
        saveRootScreenshot(fileName = "single_project_success")
    }

    @Test
    fun errorState_screenshot() {
        setSingleProjectContent(
            params = SingleProjectScreenContentParams(
                date = "2026-04-10",
                state = SingleProjectState(),
                isAddMode = true,
                projectsUiState = WorkdayUiState.Error(message = "Failed to load project"),
                isConfirmEnabled = false,
                onStateChange = {},
                onNavigateBack = {},
                onOpenProjectDetails = {},
                onConfirm = {}
            )
        )
        saveRootScreenshot(fileName = "single_project_error")
    }

    private fun setSingleProjectContent(params: SingleProjectScreenContentParams) {
        composeTestRule.setContent {
            WorkTime30Theme(dynamicColor = false) {
                SingleProjectScreenContent(params = params)
            }
        }
    }

    private fun saveRootScreenshot(fileName: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        // Use getExternalFilesDir so the files can be pulled via adb
        val outputDir = File(composeTestRule.activity.getExternalFilesDir(null), "screenshots/projects").apply {
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

