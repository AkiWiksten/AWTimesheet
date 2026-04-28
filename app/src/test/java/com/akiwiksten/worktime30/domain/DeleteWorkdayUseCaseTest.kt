package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.WorkStatsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayStatsRow
import com.akiwiksten.worktime30.domain.usecase.DeleteWorkdayUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DeleteWorkdayUseCaseTest {

    @Test
    fun invoke_nonZeroTime_deletesProjectAndProjectDetails_andDeletesUnusedProjectName() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = false
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val workStatsRepository = FakeWorkStatsRepository()
        val workdayRepository = FakeWorkdayRepository()

        val useCase = DeleteWorkdayUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            workStatsRepository = workStatsRepository,
            workdayRepository = workdayRepository
        )

        useCase(date = "2026-04-10", projectName = "Beta", projectTime = "01:00")

        assertEquals(
            listOf(SingleProjectState(date = "2026-04-10", projectName = "Beta", projectTime = "01:00")),
            projectRepository.deletedProjects
        )
        assertEquals(listOf("Beta"), projectRepository.deletedProjectNames)
        assertEquals(
            listOf(ProjectDetailsState(date = "2026-04-10", projectName = "Beta")),
            projectDetailsRepository.deletedProjectDetails
        )
        assertEquals("2026-04-10", workdayRepository.upsertedWorkdayDate)
        assertNotNull(workdayRepository.upsertedWorkStats)
    }

    @Test
    fun invoke_zeroTime_deletesOnlyProjectName_evenWhenStillUsed() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = true
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val workStatsRepository = FakeWorkStatsRepository()
        val workdayRepository = FakeWorkdayRepository()

        val useCase = DeleteWorkdayUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            workStatsRepository = workStatsRepository,
            workdayRepository = workdayRepository
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
        val workStatsRepository = FakeWorkStatsRepository()
        val workdayRepository = FakeWorkdayRepository()

        val useCase = DeleteWorkdayUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            workStatsRepository = workStatsRepository,
            workdayRepository = workdayRepository
        )

        useCase(date = "2026-04-10", projectName = "Beta", projectTime = "01:00")

        assertEquals(emptyList<String>(), projectRepository.deletedProjectNames)
    }

    private class FakeProjectRepository : ProjectRepository {
        val deletedProjects = mutableListOf<SingleProjectState>()
        val deletedProjectNames = mutableListOf<String>()
        val isProjectNameUsedByName = mutableMapOf<String, Boolean>()
        val projectsByDateRange = mutableMapOf<String, List<SingleProjectState>>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            return projectsByDateRange[start] ?: emptyList()
        }

        override suspend fun insertProject(project: SingleProjectState) = Unit

        override suspend fun deleteProject(project: SingleProjectState) {
            deletedProjects += project
        }

        override suspend fun getProjectNames(): List<String> = emptyList()

        override suspend fun insertProjectName(projectName: String) = Unit

        override suspend fun deleteProjectName(projectName: String) {
            deletedProjectNames += projectName
        }

        override suspend fun isProjectNameUsed(projectName: String): Boolean =
            isProjectNameUsedByName[projectName] ?: false

        override suspend fun getProjectTimeSumByDate(date: String): String =
            (projectsByDateRange[date] ?: emptyList()).fold(ZERO_TIME) { acc, p ->
                WorkTimeCalculator.calculateFlexTime(acc, p.projectTime)
            }
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        val deletedProjectDetails = mutableListOf<ProjectDetailsState>()

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) {
            deletedProjectDetails += projectDetails
        }

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = emptyList()
    }

    private class FakeWorkStatsRepository : WorkStatsRepository {
        override suspend fun getWorkStats(): WorkStatsState? = null

        override suspend fun insertWorkStats(workStats: WorkStatsState) = Unit

        override suspend fun getWorkStatsByDate(date: String): WorkStatsState? =
            WorkStatsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        var upsertedWorkdayDate: String? = null
        var upsertedWorkStats: WorkStatsState? = null

        override suspend fun loadWorkday(date: String): WorkStatsState? = null

        override suspend fun upsertWorkdayStats(date: String, workTimeToday: String, workStats: WorkStatsState) {
            upsertedWorkdayDate = date
            upsertedWorkStats = workStats
        }

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> = emptyList()
    }
}
