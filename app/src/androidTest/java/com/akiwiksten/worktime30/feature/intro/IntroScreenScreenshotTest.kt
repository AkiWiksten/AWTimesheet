package com.akiwiksten.worktime30.feature.intro

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

private const val INTRO_ANIMATION_WAIT_MS = 3200L

@RunWith(AndroidJUnit4::class)
class IntroScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_screenshot() {
        setIntroContent(uiState = IntroUiState.Loading)
        saveRootScreenshot(fileName = "intro_loading")
    }

    @Test
    fun successState_screenshot() {
        setIntroContent(
            uiState = IntroUiState.Success(appName = "WorkTime 3.0"),
            advanceAnimation = true
        )
        saveRootScreenshot(fileName = "intro_success")
    }

    @Test
    fun successStateLongName_screenshot() {
        setIntroContent(
            uiState = IntroUiState.Success(appName = "WorkTime 3.0 Professional Edition"),
            advanceAnimation = true
        )
        saveRootScreenshot(fileName = "intro_success_long_name")
    }

    @Test
    fun errorState_screenshot() {
        setIntroContent(uiState = IntroUiState.Error(message = "Failed to load intro"))
        saveRootScreenshot(fileName = "intro_error")
    }

    private fun setIntroContent(
        uiState: IntroUiState,
        advanceAnimation: Boolean = false
    ) {
        composeTestRule.mainClock.autoAdvance = !advanceAnimation

        composeTestRule.setContent {
            WorkTime30Theme(dynamicColor = false) {
                IntroStateContent(
                    uiState = uiState,
                    onItemClick = {}
                )
            }
        }

        if (advanceAnimation) {
            composeTestRule.mainClock.advanceTimeBy(INTRO_ANIMATION_WAIT_MS)
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    private fun saveRootScreenshot(fileName: String) {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val outputDir = File(composeTestRule.activity.filesDir, "test-screenshots/intro").apply {
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

