package com.akiwiksten.awtimesheet.feature.projects.single

import com.akiwiksten.awtimesheet.test.projectDetailsState
import com.akiwiksten.awtimesheet.test.projectState
import com.akiwiksten.awtimesheet.test.settingsState
import org.junit.Assert.assertEquals
import org.junit.Test

class SingleProjectScreenStateResolverTest {

    @Test
    fun addMode_withFilledProjectName_andBlankKilometres_enablesConfirm() {
        val initialState = projectState(index = -1)
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
        val initialState = projectState(
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
        val initialState = projectState(index = 0, projectName = "Alpha", kilometres = "12")
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
        val state = projectState(index = -1, projectName = "Alpha", projectTime = "01:00")

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
        val existingAlpha = projectState(index = 0, projectName = "Alpha")
        val existingBeta = projectState(index = 1, projectName = "Beta")

        assertEquals(true, isDuplicateProjectName("alpha", currentIndex = -1, singleProjectState = existingAlpha))
        assertEquals(true, isDuplicateProjectName("BETA", currentIndex = -1, singleProjectState = existingBeta))
        assertEquals(false, isDuplicateProjectName("Gamma", currentIndex = -1, singleProjectState = existingAlpha))
    }

    @Test
    fun isDuplicate_editMode_ignoresOwnIndex() {
        val existing = projectState(index = 0, projectName = "Alpha")

        assertEquals(false, isDuplicateProjectName("Alpha", currentIndex = 0, singleProjectState = existing))
        assertEquals(false, isDuplicateProjectName("Beta", currentIndex = 0, singleProjectState = existing))
    }

    @Test
    fun editMode_withNavigationProjectTime_prefersNavigationState() {
        val initialState = projectState(index = 0, projectName = "Alpha", projectTime = "01:45")
        val singleProjectUiState = SingleProjectUiState.Success(
            data = projectState(index = 0, projectName = "Alpha", projectTime = "00:00"),
            workTimeByDate = "00:00",
            workTypes = emptyList()
        )

        val resolved = resolveInitialSingleProjectState(
            initialSingleProjectState = initialState,
            initialProjectDetails = projectDetailsState(projectTime = "01:45"),
            initialSettings = null,
            singleProjectUiState = singleProjectUiState
        )

        assertEquals("01:45", resolved.projectTime)
    }

    @Test
    fun editMode_withoutNavigationPayload_usesCurrentProjectsState() {
        val initialState = projectState(index = 0)
        val singleProjectUiState = SingleProjectUiState.Success(
            data = projectState(index = 0, projectName = "Alpha", projectTime = "02:10"),
            workTimeByDate = "02:10",
            workTypes = emptyList()
        )

        val resolved = resolveInitialSingleProjectState(initialState, null, null, singleProjectUiState)

        assertEquals("Alpha", resolved.projectName)
        assertEquals("02:10", resolved.projectTime)
    }

    @Test
    fun addMode_returnsInitialState() {
        val initialState = projectState(index = -1, projectName = "New", projectTime = "00:30")

        val resolved = resolveInitialSingleProjectState(initialState, null, null, SingleProjectUiState.Loading)

        assertEquals(initialState, resolved)
    }

    @Test
    fun editMode_withNavigationSettings_prefersNavigationState() {
        val initialState = projectState(index = 0)
        val singleProjectUiState = SingleProjectUiState.Success(
            data = projectState(index = 0, projectName = "Alpha", projectTime = "02:10"),
            workTimeByDate = "02:10",
            workTypes = emptyList()
        )

        val resolved = resolveInitialSingleProjectState(
            initialSingleProjectState = initialState,
            initialProjectDetails = null,
            initialSettings = settingsState(dailyWorkTimeEstimate = "07:30"),
            singleProjectUiState = singleProjectUiState
        )

        assertEquals(initialState, resolved)
    }
}
