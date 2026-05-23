package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.usecase.DeleteProjectUseCase
import com.akiwiksten.awtimesheet.test.FakeProjectDetailsRepository
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.projectDetailsState
import com.akiwiksten.awtimesheet.test.projectState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteProjectUseCaseTest {

    @Test
    fun invoke_nonZeroTime_deletesProjectAndProjectDetails_andKeepsProjectName() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = false
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()

        val useCase = DeleteProjectUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository
        )

        useCase(date = "2026-04-10", projectName = "Beta", projectTime = "01:00")

        assertEquals(
            listOf(projectState(date = "2026-04-10", projectName = "Beta", projectTime = "01:00")),
            projectRepository.deletedProjects
        )
        assertEquals(emptyList<String>(), projectRepository.deletedProjectNames)
        assertEquals(
            listOf(projectDetailsState(date = "2026-04-10", projectName = "Beta")),
            projectDetailsRepository.deletedProjectDetails
        )
    }

    @Test
    fun invoke_zeroTime_deletesOnlyProjectName_evenWhenStillUsed() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = true
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()

        val useCase = DeleteProjectUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository
        )

        useCase(date = "2026-04-10", projectName = "Beta")

        assertEquals(listOf("Beta"), projectRepository.deletedProjectNames)
        assertEquals(emptyList<SingleProjectState>(), projectRepository.deletedProjects)
        assertEquals(emptyList<ProjectDetailsState>(), projectDetailsRepository.deletedProjectDetails)
    }

    @Test
    fun invoke_nonZeroTime_doesNotDeleteProjectName_whenStillUsed() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = true
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()

        val useCase = DeleteProjectUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository
        )

        useCase(date = "2026-04-10", projectName = "Beta", projectTime = "01:00")

        assertEquals(emptyList<String>(), projectRepository.deletedProjectNames)
    }
}
