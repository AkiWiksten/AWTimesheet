package com.akiwiksten.worktime30.feature.projects.details

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akiwiksten.worktime30.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDetailsHeaderGroupTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun helperText_isShown_whenNewDayForProjectIsTrue() {
        val helperText = composeRule.activity.getString(R.string.add_new_project_details)

        composeRule.setContent {
            ProjectDetailsHeaderGroup(
                date = "2026-05-05",
                projectName = "Alpha",
                isNewDayForProject = true,
                onClearDetails = {}
            )
        }

        composeRule.onNodeWithText(helperText).assertIsDisplayed()
    }

    @Test
    fun helperText_isHidden_whenNewDayForProjectIsFalse() {
        val helperText = composeRule.activity.getString(R.string.add_new_project_details)

        composeRule.setContent {
            ProjectDetailsHeaderGroup(
                date = "2026-05-05",
                projectName = "Alpha",
                isNewDayForProject = false,
                onClearDetails = {}
            )
        }

        composeRule.onAllNodesWithText(helperText).assertCountEquals(0)
    }
}
