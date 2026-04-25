package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import com.akiwiksten.worktime30.feature.settings.SettingsState
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SaveSettingsUseCaseTest {

    @Test
    fun invoke_clearsWorkTypes_insertsNewTypes_andSavesSettings() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val projectRepository = FakeProjectRepository()
        val dateRepository = DateRepository()
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            projectDetailsRepository = projectDetailsRepository,
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        useCase(
            name = "Aki",
            employer = "WorkTime",
            workTypes = listOf("Office", "Remote")
        )

        assertEquals(
            listOf(
                "clearWorkTypes",
                "insertWorkType:Office",
                "insertWorkType:Remote",
                "insertSettings"
            ),
            settingsRepository.operations
        )
        assertEquals(
            SettingsState(name = "Aki", employer = "WorkTime"),
            settingsRepository.savedSettings
        )
    }

    @Test
    fun invoke_withEmptyWorkTypes_stillSavesSettings() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val projectRepository = FakeProjectRepository()
        val dateRepository = DateRepository()
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            projectDetailsRepository = projectDetailsRepository,
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        useCase(name = "Aki", employer = "WorkTime", workTypes = emptyList())

        assertEquals(listOf("clearWorkTypes", "insertSettings"), settingsRepository.operations)
        assertEquals(SettingsState(name = "Aki", employer = "WorkTime"), settingsRepository.savedSettings)
    }

    @Test
    fun invoke_withDailyWorkTimeEstimate_currentDayAndZeroWorkTime_savesDailyWorkTime() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val projectRepository = FakeProjectRepository()
        val dateRepository = DateRepository().apply {
            updateDate(LocalDate.now().toString())
        }
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            projectDetailsRepository = projectDetailsRepository,
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        useCase(
            name = "Aki",
            employer = "WorkTime",
            workTypes = emptyList(),
            dailyWorkTimeEstimate = "07:30"
        )

        assertEquals("07:30", projectDetailsRepository.insertedWorkStats?.dailyWorkTimeEstimate)
        assertEquals("00:00", projectDetailsRepository.insertedWorkStats?.dailyLunchTimeEstimate)
    }

    @Test
    fun invoke_withLunchTimeEstimate_currentDayAndZeroWorkTime_savesLunchTime() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val projectRepository = FakeProjectRepository()
        val dateRepository = DateRepository().apply {
            updateDate(LocalDate.now().toString())
        }
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            projectDetailsRepository = projectDetailsRepository,
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        useCase(
            name = "Aki",
            employer = "WorkTime",
            workTypes = emptyList(),
            dailyWorkTimeEstimate = "07:30",
            lunchTimeEstimate = "00:30"
        )

        assertEquals("00:30", projectDetailsRepository.insertedWorkStats?.dailyLunchTimeEstimate)
    }

    @Test
    fun invoke_withDailyWorkTimeEstimate_nonCurrentDay_updatesGlobalStatsButNotWorkday() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = WorkStatsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
        }
        val projectRepository = FakeProjectRepository()
        val dateRepository = DateRepository().apply {
            updateDate("2000-01-01")
        }
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            projectDetailsRepository = projectDetailsRepository,
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        useCase(
            name = "Aki",
            employer = "WorkTime",
            workTypes = emptyList(),
            dailyWorkTimeEstimate = "08:00"
        )

        // Global WorkStatsEntity always updated
        assertEquals("08:00", projectDetailsRepository.insertedWorkStats?.dailyWorkTimeEstimate)
        // Workday-per-day entry not updated (non-current day)
        assertNull(projectDetailsRepository.upsertedWorkdayDate)
    }

    @Test
    fun invoke_withDailyWorkTimeEstimate_currentDayAndNonZeroWorkTime_updatesGlobalStatsButNotWorkday() = runBlocking {
        val today = LocalDate.now().toString()
        val settingsRepository = FakeSettingsRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = WorkStatsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
        }
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                SingleProjectState(date = today, projectName = "Alpha", projectTime = "01:00")
            )
        }
        val dateRepository = DateRepository().apply {
            updateDate(today)
        }
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            projectDetailsRepository = projectDetailsRepository,
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        useCase(
            name = "Aki",
            employer = "WorkTime",
            workTypes = emptyList(),
            dailyWorkTimeEstimate = "08:00"
        )

        // Global WorkStatsEntity always updated
        assertEquals("08:00", projectDetailsRepository.insertedWorkStats?.dailyWorkTimeEstimate)
        // Workday-per-day entry not updated (non-zero work time)
        assertNull(projectDetailsRepository.upsertedWorkdayDate)
    }

    private class FakeSettingsRepository : SettingsRepository {
        val operations = mutableListOf<String>()
        val insertedWorkTypes = mutableListOf<WorkTypeEntity>()
        var savedSettings: SettingsState? = null

        override suspend fun getSettings(): SettingsState? = null

        override suspend fun insertSettings(settings: SettingsState) {
            operations += "insertSettings"
            savedSettings = settings
        }

        override suspend fun getWorkTypes(): List<String> = emptyList()

        override suspend fun insertWorkType(workType: String) {
            operations += "insertWorkType:$workType"
            insertedWorkTypes += WorkTypeEntity(workType = workType)
        }

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun clearWorkTypes() {
            operations += "clearWorkTypes"
        }
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        var workStats: WorkStatsState? = null
        var insertedWorkStats: WorkStatsState? = null
        var upsertedWorkdayDate: String? = null

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun getWorkStats(): WorkStatsState? = workStats

        override suspend fun insertWorkStats(workStats: WorkStatsState) {
            insertedWorkStats = workStats
            this.workStats = workStats
        }

        override suspend fun getWorkStatsByDate(date: String): WorkStatsState? = workStats

        override suspend fun upsertWorkdayStats(date: String, workTimeToday: String, workStats: WorkStatsState) {
            upsertedWorkdayDate = date
        }

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = emptyList()
    }

    private class FakeProjectRepository : ProjectRepository {
        var projectsByDateRange: List<SingleProjectState> = emptyList()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            return projectsByDateRange.filter { it.date in start..end }
        }

        override suspend fun insertProject(project: SingleProjectState) = Unit

        override suspend fun deleteProject(project: SingleProjectState) = Unit

        override suspend fun getProjectNames(): List<String> = emptyList()

        override suspend fun insertProjectName(projectName: String) = Unit

        override suspend fun deleteProjectName(projectName: String) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false
    }
}
