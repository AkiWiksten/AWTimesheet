package com.akiwiksten.awtimesheet.feature.projectdetails

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akiwiksten.awtimesheet.feature.projectdetails.R
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsHeaderSection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDetailsHeaderGroupTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun helperText_showsAddNewProjectDetails_whenRequested() {
        val helperText = composeRule.activity.getString(R.string.add_new_project_details)

        composeRule.setContent {
            ProjectDetailsHeaderSection(
                date = "2026-05-05",
                projectName = "Alpha",
                helperTextResId = R.string.add_new_project_details,
                onClearDetails = {}
            )
        }

        composeRule.onNodeWithText(helperText).assertIsDisplayed()
    }

    @Test
    fun helperText_showsSelectEndTime_whenRequested() {
        val helperText = composeRule.activity.getString(R.string.select_end_time)

        composeRule.setContent {
            ProjectDetailsHeaderSection(
                date = "2026-05-05",
                projectName = "Alpha",
                helperTextResId = R.string.select_end_time,
                onClearDetails = {}
            )
        }

        composeRule.onNodeWithText(helperText).assertIsDisplayed()
    }

    @Test
    fun helperText_isHidden_whenNotRequested() {
        val addNewProjectText = composeRule.activity.getString(R.string.add_new_project_details)
        val selectEndTimeText = composeRule.activity.getString(R.string.select_end_time)

        composeRule.setContent {
            ProjectDetailsHeaderSection(
                date = "2026-05-05",
                projectName = "Alpha",
                helperTextResId = null,
                onClearDetails = {}
            )
        }

        composeRule.onAllNodesWithText(addNewProjectText).assertCountEquals(0)
        composeRule.onAllNodesWithText(selectEndTimeText).assertCountEquals(0)
    }
}

