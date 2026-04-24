package com.akiwiksten.worktime30.feature.projects.single

import com.akiwiksten.worktime30.feature.workday.WorkdayUiState
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import org.junit.Assert.assertEquals
import org.junit.Test

class SingleProjectScreenStateResolverTest {

    @Test
    fun addMode_withFilledProjectName_andBlankKilometres_enablesConfirm() {
        val initialState = SingleProjectState(index = -1)
        val editedState = initialState.copy(projectName = "Alpha", kilometres = "")

        val isEnabled = isSingleProjectConfirmEnabled(
            state = editedState,
            initialUiState = initialState,
            isAddMode = true
        )

        assertEquals(true, isEnabled)
    }

    @Test
    fun editMode_withoutChanges_disablesConfirm() {
        val initialState = SingleProjectState(index = 0, projectName = "Alpha", kilometres = "12")

        val isEnabled = isSingleProjectConfirmEnabled(
            state = initialState,
            initialUiState = initialState,
            isAddMode = false
        )

        assertEquals(false, isEnabled)
    }

    @Test
    fun editMode_withChanges_andValidFields_enablesConfirm() {
        val initialState = SingleProjectState(index = 0, projectName = "Alpha", kilometres = "12")
        val editedState = initialState.copy(projectTime = "01:00")

        val isEnabled = isSingleProjectConfirmEnabled(
            state = editedState,
            initialUiState = initialState,
            isAddMode = false
        )

        assertEquals(true, isEnabled)
    }

    @Test
    fun editMode_withNavigationProjectTime_prefersNavigationState() {
        val initialState = SingleProjectState(
            index = 0,
            projectName = "Alpha",
            projectTime = "01:45",
            projectDetails = ProjectDetailsState(projectTime = "01:45")
        )
        val projectsUiState = WorkdayUiState.Success(
            projects = listOf(
                SingleProjectState(index = 0, projectName = "Alpha", projectTime = "00:00")
            )
        )

        val resolved = resolveInitialSingleProjectState(initialState, projectsUiState)

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

        val resolved = resolveInitialSingleProjectState(initialState, projectsUiState)

        assertEquals("Alpha", resolved.projectName)
        assertEquals("02:10", resolved.projectTime)
    }

    @Test
    fun addMode_returnsInitialState() {
        val initialState = SingleProjectState(index = -1, projectName = "New", projectTime = "00:30")

        val resolved = resolveInitialSingleProjectState(initialState, WorkdayUiState.Loading)

        assertEquals(initialState, resolved)
    }
}
