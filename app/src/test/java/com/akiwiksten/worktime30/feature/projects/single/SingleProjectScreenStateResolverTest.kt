package com.akiwiksten.worktime30.feature.projects.single

import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class SingleProjectScreenStateResolverTest {

    @Test
    fun addMode_withFilledProjectName_andBlankKilometres_enablesConfirm() {
        val initialState = SingleProjectState(index = -1)
        val editedState = initialState.copy(projectName = "Alpha", kilometres = "")

        val isEnabled = isSingleProjectConfirmEnabled(
            state = editedState,
            hasUnsavedChanges = true,
            isDuplicateProjectName = false,
            isAddMode = true
        )

        assertEquals(true, isEnabled)
    }

    @Test
    fun editMode_withProjectNameAndTime_withoutChanges_enablesConfirm() {
        val initialState = SingleProjectState(
            index = 0,
            projectName = "Alpha",
            projectTime = "01:00",
            kilometres = "12"
        )

        val isEnabled = isSingleProjectConfirmEnabled(
            state = initialState,
            hasUnsavedChanges = false,
            isDuplicateProjectName = false,
            isAddMode = false
        )

        assertEquals(true, isEnabled)
    }

    @Test
    fun editMode_withChanges_andValidFields_enablesConfirm() {
        val initialState = SingleProjectState(index = 0, projectName = "Alpha", kilometres = "12")
        val editedState = initialState.copy(projectTime = "01:00")

        val isEnabled = isSingleProjectConfirmEnabled(
            state = editedState,
            hasUnsavedChanges = true,
            isDuplicateProjectName = false,
            isAddMode = false
        )

        assertEquals(true, isEnabled)
    }

    @Test
    fun addMode_withDuplicateProjectName_disablesConfirm() {
        val state = SingleProjectState(index = -1, projectName = "Alpha", projectTime = "01:00")

        val isEnabled = isSingleProjectConfirmEnabled(
            state = state,
            hasUnsavedChanges = true,
            isDuplicateProjectName = true,
            isAddMode = true
        )

        assertEquals(false, isEnabled)
    }

    @Test
    fun isDuplicate_matchesCaseInsensitive() {
        val projects = listOf(
            SingleProjectState(index = 0, projectName = "Alpha"),
            SingleProjectState(index = 1, projectName = "Beta")
        )

        assertEquals(true, isDuplicateProjectName("alpha", currentIndex = -1, projects = projects))
        assertEquals(true, isDuplicateProjectName("BETA", currentIndex = -1, projects = projects))
        assertEquals(false, isDuplicateProjectName("Gamma", currentIndex = -1, projects = projects))
    }

    @Test
    fun isDuplicate_editMode_ignoresOwnIndex() {
        val projects = listOf(
            SingleProjectState(index = 0, projectName = "Alpha"),
            SingleProjectState(index = 1, projectName = "Beta")
        )

        // Editing index 0 with its own name → not a duplicate
        assertEquals(false, isDuplicateProjectName("Alpha", currentIndex = 0, projects = projects))
        // Editing index 0 but using index 1's name → duplicate
        assertEquals(true, isDuplicateProjectName("Beta", currentIndex = 0, projects = projects))
    }

    @Test
    fun editMode_withNavigationProjectTime_prefersNavigationState() {
        val initialState = SingleProjectState(
            index = 0,
            projectName = "Alpha",
            projectTime = "01:45"
        )
        val projectsUiState = WorkdayUiState.Success(
            projects = listOf(
                SingleProjectState(index = 0, projectName = "Alpha", projectTime = "00:00")
            )
        )

        val resolved = resolveInitialSingleProjectState(
            initialSingleProjectState = initialState,
            initialProjectDetails = ProjectDetailsState(projectTime = "01:45"),
            initialSettings = null,
            projectsUiState = projectsUiState
        )

        assertEquals("01:45", resolved.projectTime)
    }

    @Test
    fun editMode_withoutNavigationPayload_usesCurrentProjectsState() {
        val initialState = SingleProjectState(index = 0)
        val projectsUiState = WorkdayUiState.Success(
            projects = listOf(
                SingleProjectState(index = 0, projectName = "Alpha", projectTime = "02:10")
            )
        )

        val resolved = resolveInitialSingleProjectState(initialState, null, null, projectsUiState)

        assertEquals("Alpha", resolved.projectName)
        assertEquals("02:10", resolved.projectTime)
    }

    @Test
    fun addMode_returnsInitialState() {
        val initialState = SingleProjectState(index = -1, projectName = "New", projectTime = "00:30")

        val resolved = resolveInitialSingleProjectState(initialState, null, null, WorkdayUiState.Loading)

        assertEquals(initialState, resolved)
    }

    @Test
    fun editMode_withNavigationSettings_prefersNavigationState() {
        val initialState = SingleProjectState(index = 0)
        val projectsUiState = WorkdayUiState.Success(
            projects = listOf(
                SingleProjectState(index = 0, projectName = "Alpha", projectTime = "02:10")
            )
        )

        val resolved = resolveInitialSingleProjectState(
            initialSingleProjectState = initialState,
            initialProjectDetails = null,
            initialSettings = SettingsState(dailyWorkTimeEstimate = "07:30"),
            projectsUiState = projectsUiState
        )

        assertEquals(initialState, resolved)
    }
}
