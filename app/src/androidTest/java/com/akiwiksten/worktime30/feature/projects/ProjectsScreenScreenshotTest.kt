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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ProjectsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_beforeDelay_screenshot() {
        composeTestRule.mainClock.autoAdvance = false
        setProjectsContent(projectsUiState = ProjectsUiState.Loading, selectedItemIndex = -1)
        composeTestRule.waitForIdle()
        saveRootScreenshot(fileName = "projects_loading_before_delay")
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun loadingState_afterDelay_screenshot() {
        composeTestRule.mainClock.autoAdvance = false
        setProjectsContent(projectsUiState = ProjectsUiState.Loading, selectedItemIndex = -1)
        composeTestRule.mainClock.advanceTimeBy(LOADING_INDICATOR_DELAY_MS + 50L)
        composeTestRule.waitForIdle()
        saveRootScreenshot(fileName = "projects_loading_after_delay")
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun successState_screenshot() {
        setProjectsContent(
            projectsUiState = ProjectsUiState.Success(
                date = "2026-04-10",
                workTimeToday = "07:45",
                projects = listOf(
                    ProjectListItemUiState(
                        index = 0,
                        projectName = "Alpha Site",
                        projectTime = "04:15",
                        kilometres = 24,
                        allowance = "Daily allowance",
                        workType = "Installation"
                    ),
                    ProjectListItemUiState(
                        index = 1,
                        projectName = "Beta Support",
                        projectTime = "03:30",
                        kilometres = 8,
                        allowance = "",
                        workType = "Maintenance"
                    )
                )
            ),
            selectedItemIndex = 0
        )
        saveRootScreenshot(fileName = "projects_success")
    }

    @Test
    fun errorState_screenshot() {
        setProjectsContent(
            projectsUiState = ProjectsUiState.Error(message = "Failed to load projects"),
            selectedItemIndex = -1
        )
        saveRootScreenshot(fileName = "projects_error")
    }

    private fun setProjectsContent(projectsUiState: ProjectsUiState, selectedItemIndex: Int) {
        composeTestRule.setContent {
            WorkTime30Theme(dynamicColor = false) {
                ProjectsContent(
                    projectsUiState = projectsUiState,
                    selectedItemIndex = selectedItemIndex,
                    actions = ProjectsActions(
                        onSelectedItemIndexChange = {},
                        onNavigateToSingleProject = {},
                        onRetry = {},
                        onDeleteProject = {}
                    )
                )
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

