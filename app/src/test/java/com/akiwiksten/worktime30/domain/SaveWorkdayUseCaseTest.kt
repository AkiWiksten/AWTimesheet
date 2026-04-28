package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayStatsRow
import com.akiwiksten.worktime30.domain.usecase.SaveWorkdayUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveWorkdayUseCaseTest {

    @Test
    fun invoke_savesProjectsAndProjectDetails() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val settingsRepository = FakeSettingsRepository()
        val workdayRepository = FakeWorkdayRepository()
        val useCase = SaveWorkdayUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository
        )

        useCase(
            projectsToSave = listOf(
                SingleProjectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "02:00"
                )
            ),
            projectDetailsToSave = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "07:00"
            )
        )

        assertEquals(listOf("Alpha"), projectRepository.insertedProjectNames)
        assertEquals(1, projectRepository.insertedProjects.size)
        assertEquals(1, projectDetailsRepository.insertedProjectDetails.size)
        assertEquals("2026-04-10", workdayRepository.upsertedWorkdayDate)
    }

    @Test
    fun invoke_doesNotInsertProjectDetails_whenProjectDetailsIsNull() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val settingsRepository = FakeSettingsRepository()
        val workdayRepository = FakeWorkdayRepository()
        val useCase = SaveWorkdayUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository
        )

        useCase(
            projectsToSave = listOf(
                SingleProjectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "01:00"
                )
            ),
            projectDetailsToSave = null
        )

        assertEquals(emptyList<ProjectDetailsState>(), projectDetailsRepository.insertedProjectDetails)
        assertEquals("2026-04-10", workdayRepository.upsertedWorkdayDate)
    }

    private class FakeProjectRepository : ProjectRepository {
        val insertedProjects = mutableListOf<SingleProjectState>()
        val insertedProjectNames = mutableListOf<String>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            return insertedProjects.filter { it.date in start..end }
        }

        override suspend fun insertProject(project: SingleProjectState) {
            insertedProjects += project
        }

        override suspend fun deleteProject(project: SingleProjectState) = Unit

        override suspend fun getProjectNames(): List<String> = emptyList()

        override suspend fun insertProjectName(projectName: String) {
            insertedProjectNames += projectName
        }

        override suspend fun deleteProjectName(projectName: String) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean =
            false

        override suspend fun getProjectTimeSumByDate(date: String): String =
            insertedProjects.filter { it.date == date }.fold(ZERO_TIME) { acc, p ->
                WorkTimeCalculator.calculateFlexTime(acc, p.projectTime)
            }
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        val insertedProjectDetails = mutableListOf<ProjectDetailsState>()

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) {
            insertedProjectDetails += projectDetails
        }

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = emptyList()
    }

    private class FakeSettingsRepository : SettingsRepository {
        override suspend fun getSettings(): SettingsState? = null

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getWorkStats(): WorkStatsState? =
            WorkStatsState(dailyWorkTimeEstimate = "07:30", initialFlexTimeTotal = ZERO_TIME)

        override suspend fun insertWorkStats(workStats: WorkStatsState) = Unit

        override suspend fun getWorkStatsByDate(date: String): WorkStatsState? = null

        override suspend fun getWorkTypes(): List<String> = emptyList()

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun clearWorkTypes() = Unit
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        var upsertedWorkdayDate: String? = null
        var upsertedWorkStats: WorkStatsState? = null

        override suspend fun loadWorkday(date: String): WorkStatsState? = null

        override suspend fun upsertWorkdayStats(date: String, workStats: WorkStatsState) {
            upsertedWorkdayDate = date
            upsertedWorkStats = workStats
        }

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> = emptyList()
    }
}
