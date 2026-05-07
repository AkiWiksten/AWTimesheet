package com.akiwiksten.worktime30.feature.projects.details

import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.isNewDayForProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectDetailsUiMapperTest {

    @Test
    fun applyEntitiesToState_withNoProjectDetails_usesSettingsLunchTimeEstimate() {
        val result = ProjectDetailsUiMapper.applyEntitiesToState(
            baseState = ProjectDetailsUiState.Success(
                details = ProjectDetailsState(date = "2026-05-05", projectName = "Alpha")
            ),
            projectDetails = null,
            settings = SettingsState(dailyLunchTimeEstimate = "00:45")
        )

        assertTrue(result.details.isNewDayForProject())
        assertEquals("00:45", result.details.lunchTimeEstimate)
    }

    @Test
    fun applyEntitiesToState_withNewDayProjectDetails_usesSettingsLunchTimeEstimate() {
        val result = ProjectDetailsUiMapper.applyEntitiesToState(
            baseState = ProjectDetailsUiState.Success(details = ProjectDetailsState()),
            projectDetails = ProjectDetailsState(
                date = "2026-05-05",
                projectName = "Alpha"
            ),
            settings = SettingsState(dailyLunchTimeEstimate = "00:45")
        )

        assertTrue(result.details.isNewDayForProject())
        assertEquals("00:45", result.details.lunchTimeEstimate)
    }

    @Test
    fun applyEntitiesToState_withExistingDayProjectDetails_preservesProjectLunchTimeEstimate() {
        val result = ProjectDetailsUiMapper.applyEntitiesToState(
            baseState = ProjectDetailsUiState.Success(details = ProjectDetailsState()),
            projectDetails = ProjectDetailsState(
                date = "2026-05-05",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
                lunchTimeEstimate = "00:15"
            ),
            settings = SettingsState(dailyLunchTimeEstimate = "00:45")
        )

        assertFalse(result.details.isNewDayForProject())
        assertEquals("00:15", result.details.lunchTimeEstimate)
    }
}
