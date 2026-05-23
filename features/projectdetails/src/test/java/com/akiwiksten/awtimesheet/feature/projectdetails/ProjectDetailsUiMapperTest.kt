package com.akiwiksten.awtimesheet.feature.projectdetails

import com.akiwiksten.awtimesheet.domain.model.isNewDayForProject
import com.akiwiksten.awtimesheet.test.projectDetailsState
import com.akiwiksten.awtimesheet.test.settingsState
import org.junit.Assert
import org.junit.Test

class ProjectDetailsUiMapperTest {

    @Test
    fun mapEntitiesToUiState_withNoProjectDetails_usesSettingsLunchTimeEstimate() {
        val result = ProjectDetailsUiMapper.mapEntitiesToUiState(
            baseState = ProjectDetailsUiState.Success(
                details = projectDetailsState(date = "2026-05-05", projectName = "Alpha")
            ),
            projectDetails = null,
            settings = settingsState(dailyLunchTimeEstimate = "00:45")
        )

        Assert.assertTrue(result.details.isNewDayForProject())
        Assert.assertEquals("00:45", result.details.lunchTimeEstimate)
    }

    @Test
    fun mapEntitiesToUiState_withNewDayProjectDetails_usesSettingsLunchTimeEstimate() {
        val result = ProjectDetailsUiMapper.mapEntitiesToUiState(
            baseState = ProjectDetailsUiState.Success(details = projectDetailsState()),
            projectDetails = projectDetailsState(
                date = "2026-05-05",
                projectName = "Alpha"
            ),
            settings = settingsState(dailyLunchTimeEstimate = "00:45")
        )

        Assert.assertTrue(result.details.isNewDayForProject())
        Assert.assertEquals("00:45", result.details.lunchTimeEstimate)
    }

    @Test
    fun mapEntitiesToUiState_withExistingDayProjectDetails_preservesProjectLunchTimeEstimate() {
        val result = ProjectDetailsUiMapper.mapEntitiesToUiState(
            baseState = ProjectDetailsUiState.Success(details = projectDetailsState()),
            projectDetails = projectDetailsState(
                date = "2026-05-05",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
                lunchTimeEstimate = "00:15"
            ),
            settings = settingsState(dailyLunchTimeEstimate = "00:45")
        )

        Assert.assertFalse(result.details.isNewDayForProject())
        Assert.assertEquals("00:15", result.details.lunchTimeEstimate)
    }
}