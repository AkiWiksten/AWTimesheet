package com.akiwiksten.awtimesheet.feature.singleproject

import com.akiwiksten.awtimesheet.feature.singleproject.model.isDuplicateProjectName
import com.akiwiksten.awtimesheet.feature.singleproject.model.isSingleProjectConfirmEnabled
import com.akiwiksten.awtimesheet.feature.singleproject.model.resolveInitialSingleProjectState
import com.akiwiksten.awtimesheet.test.projectState
import org.junit.Assert
import org.junit.Test

class SingleProjectScreenStateResolverTest {

    @Test
    fun addMode_withFilledProjectName_andBlankKilometres_enablesConfirm() {
        val initialState = projectState(listIndex = -1)
        val editedState = initialState.copy(projectName = "Alpha", kilometres = "")

        val isEnabled = isSingleProjectConfirmEnabled(
            state = editedState,
            hasUnsavedChanges = true,
            isDuplicateProjectName = false,
            isAddMode = true
        )

        Assert.assertEquals(true, isEnabled)
    }

    @Test
    fun editMode_withProjectNameAndTime_withoutChanges_enablesConfirm() {
        val initialState = projectState(
            listIndex = 0,
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

        Assert.assertEquals(true, isEnabled)
    }

    @Test
    fun editMode_withChanges_andValidFields_enablesConfirm() {
        val initialState = projectState(listIndex = 0, projectName = "Alpha", kilometres = "12")
        val editedState = initialState.copy(projectTime = "01:00")

        val isEnabled = isSingleProjectConfirmEnabled(
            state = editedState,
            hasUnsavedChanges = true,
            isDuplicateProjectName = false,
            isAddMode = false
        )

        Assert.assertEquals(true, isEnabled)
    }

    @Test
    fun addMode_withDuplicateProjectName_disablesConfirm() {
        val state = projectState(listIndex = -1, projectName = "Alpha", projectTime = "01:00")

        val isEnabled = isSingleProjectConfirmEnabled(
            state = state,
            hasUnsavedChanges = true,
            isDuplicateProjectName = true,
            isAddMode = true
        )

        Assert.assertEquals(false, isEnabled)
    }

    @Test
    fun isDuplicate_addMode_matchesCaseInsensitive() {
        val existingAlpha = projectState(listIndex = 0, projectName = "Alpha")
        val existingBeta = projectState(listIndex = 1, projectName = "Beta")

        Assert.assertEquals(
            true,
            isDuplicateProjectName("alpha", currentIndex = -1, singleProjectState = existingAlpha)
        )
        Assert.assertEquals(
            true,
            isDuplicateProjectName("BETA", currentIndex = -1, singleProjectState = existingBeta)
        )
        Assert.assertEquals(
            false,
            isDuplicateProjectName("Gamma", currentIndex = -1, singleProjectState = existingAlpha)
        )
    }

    @Test
    fun isDuplicate_editMode_ignoresOwnIndex() {
        val existing = projectState(listIndex = 0, projectName = "Alpha")

        Assert.assertEquals(
            false,
            isDuplicateProjectName("Alpha", currentIndex = 0, singleProjectState = existing)
        )
        Assert.assertEquals(
            false,
            isDuplicateProjectName("Beta", currentIndex = 0, singleProjectState = existing)
        )
    }

    @Test
    fun editMode_withNavigationProjectTime_prefersNavigationState() {
        val initialState = projectState(listIndex = 0, projectName = "Alpha", projectTime = "01:45")
        val singleProjectUiState = SingleProjectUiState.Success(
            data = projectState(listIndex = 0, projectName = "Alpha", projectTime = "00:00"),
            workTimeByDate = "00:00",
            workTypes = emptyList(),
            settings = null
        )

        val resolved = resolveInitialSingleProjectState(
            initialSingleProjectState = initialState,
            singleProjectUiState = singleProjectUiState
        )

        Assert.assertEquals("01:45", resolved.projectTime)
    }

    @Test
    fun editMode_withoutNavigationPayload_usesCurrentProjectsState() {
        val initialState = projectState(listIndex = 0).copy(isAddMode = false)
        val singleProjectUiState = SingleProjectUiState.Success(
            data = projectState(listIndex = 0, projectName = "Alpha", projectTime = "02:10"),
            workTimeByDate = "02:10",
            workTypes = emptyList(),
            settings = null
        )

        val resolved =
            resolveInitialSingleProjectState(initialState, singleProjectUiState)

        Assert.assertEquals("Alpha", resolved.projectName)
        Assert.assertEquals("02:10", resolved.projectTime)
    }

    @Test
    fun addMode_returnsInitialState() {
        val initialState = projectState(listIndex = -1, projectName = "New", projectTime = "00:30")

        val resolved =
            resolveInitialSingleProjectState(initialState, SingleProjectUiState.Loading)

        Assert.assertEquals(initialState, resolved)
    }

    @Test
    fun editMode_withNavigationSettings_prefersNavigationState() {
        val initialState = projectState(listIndex = 0)
        val singleProjectUiState = SingleProjectUiState.Success(
            data = projectState(listIndex = 0, projectName = "Alpha", projectTime = "02:10"),
            workTimeByDate = "02:10",
            workTypes = emptyList(),
            settings = null
        )

        val resolved = resolveInitialSingleProjectState(
            initialSingleProjectState = initialState,
            singleProjectUiState = singleProjectUiState
        )

        Assert.assertEquals(initialState, resolved)
    }
}
