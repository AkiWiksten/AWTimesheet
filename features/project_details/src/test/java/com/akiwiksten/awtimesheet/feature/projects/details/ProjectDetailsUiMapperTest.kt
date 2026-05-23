package com.akiwiksten.awtimesheet.feature.projects.details

import com.akiwiksten.awtimesheet.domain.model.isNewDayForProject
import com.akiwiksten.awtimesheet.feature.project_details.ProjectDetailsUiMapper
import com.akiwiksten.awtimesheet.feature.project_details.ProjectDetailsUiState
import com.akiwiksten.awtimesheet.test.projectDetailsState
import com.akiwiksten.awtimesheet.test.settingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

        assertTrue(result.details.isNewDayForProject())
        assertEquals("00:45", result.details.lunchTimeEstimate)
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

        assertTrue(result.details.isNewDayForProject())
        assertEquals("00:45", result.details.lunchTimeEstimate)
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

        assertFalse(result.details.isNewDayForProject())
        assertEquals("00:15", result.details.lunchTimeEstimate)
    }
}
