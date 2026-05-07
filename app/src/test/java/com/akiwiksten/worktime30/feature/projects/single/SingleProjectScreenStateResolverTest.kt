package com.akiwiksten.worktime30.feature.projects.single

import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
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
    fun isDuplicate_addMode_matchesCaseInsensitive() {
        val existingAlpha = SingleProjectState(index = 0, projectName = "Alpha")
        val existingBeta = SingleProjectState(index = 1, projectName = "Beta")

        assertEquals(true, isDuplicateProjectName("alpha", currentIndex = -1, singleProjectState = existingAlpha))
        assertEquals(true, isDuplicateProjectName("BETA", currentIndex = -1, singleProjectState = existingBeta))
        assertEquals(false, isDuplicateProjectName("Gamma", currentIndex = -1, singleProjectState = existingAlpha))
    }

    @Test
    fun isDuplicate_editMode_ignoresOwnIndex() {
        val existing = SingleProjectState(index = 0, projectName = "Alpha")

        // Editing index 0 with its own name → not a duplicate (edit mode bypasses check)
        assertEquals(false, isDuplicateProjectName("Alpha", currentIndex = 0, singleProjectState = existing))
        // Editing index 0 but checking against different project → still not duplicate (edit mode ignores)
        assertEquals(false, isDuplicateProjectName("Beta", currentIndex = 0, singleProjectState = existing))
    }

    @Test
    fun editMode_withNavigationProjectTime_prefersNavigationState() {
        val initialState = SingleProjectState(
            index = 0,
            projectName = "Alpha",
            projectTime = "01:45"
        )
        val singleProjectUiState = SingleProjectUiState.Success(
            data = SingleProjectState(index = 0, projectName = "Alpha", projectTime = "00:00"),
            workTimeByDate = "00:00",
            workTypes = emptyList()
        )

        val resolved = resolveInitialSingleProjectState(
            initialSingleProjectState = initialState,
            initialProjectDetails = ProjectDetailsState(projectTime = "01:45"),
            initialSettings = null,
            singleProjectUiState = singleProjectUiState
        )

        assertEquals("01:45", resolved.projectTime)
    }

    @Test
    fun editMode_withoutNavigationPayload_usesCurrentProjectsState() {
        val initialState = SingleProjectState(index = 0)
        val singleProjectUiState = SingleProjectUiState.Success(
            data = SingleProjectState(index = 0, projectName = "Alpha", projectTime = "02:10"),
            workTimeByDate = "02:10",
            workTypes = emptyList()
        )

        val resolved = resolveInitialSingleProjectState(initialState, null, null, singleProjectUiState)

        assertEquals("Alpha", resolved.projectName)
        assertEquals("02:10", resolved.projectTime)
    }

    @Test
    fun addMode_returnsInitialState() {
        val initialState = SingleProjectState(index = -1, projectName = "New", projectTime = "00:30")

        val resolved = resolveInitialSingleProjectState(initialState, null, null, SingleProjectUiState.Loading)

        assertEquals(initialState, resolved)
    }

    @Test
    fun editMode_withNavigationSettings_prefersNavigationState() {
        val initialState = SingleProjectState(index = 0)
        val singleProjectUiState = SingleProjectUiState.Success(
            data = SingleProjectState(index = 0, projectName = "Alpha", projectTime = "02:10"),
            workTimeByDate = "02:10",
            workTypes = emptyList()
        )

        val resolved = resolveInitialSingleProjectState(
            initialSingleProjectState = initialState,
            initialProjectDetails = null,
            initialSettings = SettingsState(dailyWorkTimeEstimate = "07:30"),
            singleProjectUiState = singleProjectUiState
        )

        assertEquals(initialState, resolved)
    }
}
