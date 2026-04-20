package com.akiwiksten.worktime30.feature.projects.single

import com.akiwiksten.worktime30.feature.projects.daily.ProjectsUiState
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.single.details.ProjectDetailsState
import org.junit.Assert.assertEquals
import org.junit.Test

class SingleProjectScreenStateResolverTest {

    @Test
    fun editMode_withNavigationProjectTime_prefersNavigationState() {
        val initialState = SingleProjectState(
            index = 0,
            projectName = "Alpha",
            projectTime = "01:45",
            projectDetails = ProjectDetailsState(projectTime = "01:45")
        )
        val projectsUiState = ProjectsUiState.Success(
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
        val projectsUiState = ProjectsUiState.Success(
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

        val resolved = resolveInitialSingleProjectState(initialState, ProjectsUiState.Loading)

        assertEquals(initialState, resolved)
    }
}
