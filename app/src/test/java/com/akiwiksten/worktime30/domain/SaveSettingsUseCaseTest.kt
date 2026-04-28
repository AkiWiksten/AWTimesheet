package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayStatsRow
import com.akiwiksten.worktime30.domain.usecase.SaveSettingsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SaveSettingsUseCaseTest {

    @Test
    fun invoke_clearsWorkTypes_insertsNewTypes_andSavesSettings() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val workdayRepository = FakeWorkdayRepository()
        val projectRepository = FakeProjectRepository()
        val dateRepository = DateRepository()
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        useCase(
            name = "Aki",
            employer = "WorkTime",
            workTypes = listOf("Office", "Remote")
        )

        assertEquals(
            listOf("clearWorkTypes", "insertWorkType:Office", "insertWorkType:Remote", "insertSettings"),
            settingsRepository.operations
        )
        assertEquals(SettingsState(name = "Aki", employer = "WorkTime"), settingsRepository.savedSettings)
    }

    @Test
    fun invoke_withEmptyWorkTypes_stillSavesSettings() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = FakeWorkdayRepository(),
            projectRepository = FakeProjectRepository(),
            dateRepository = DateRepository()
        )

        useCase(name = "Aki", employer = "WorkTime", workTypes = emptyList())

        assertEquals(listOf("clearWorkTypes", "insertSettings"), settingsRepository.operations)
        assertEquals(SettingsState(name = "Aki", employer = "WorkTime"), settingsRepository.savedSettings)
    }

    @Test
    fun invoke_withDailyWorkTimeEstimate_currentDayAndZeroWorkTime_savesDailyWorkTime() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = FakeWorkdayRepository(),
            projectRepository = FakeProjectRepository(),
            dateRepository = DateRepository().apply { updateDate(LocalDate.now().toString()) }
        )

        useCase(name = "Aki", employer = "WorkTime", workTypes = emptyList(), dailyWorkTimeEstimate = "07:30")

        assertEquals("07:30", settingsRepository.insertedWorkStats?.dailyWorkTimeEstimate)
        assertEquals("00:00", settingsRepository.insertedWorkStats?.dailyLunchTimeEstimate)
    }

    @Test
    fun invoke_withLunchTimeEstimate_currentDayAndZeroWorkTime_savesLunchTime() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = FakeWorkdayRepository(),
            projectRepository = FakeProjectRepository(),
            dateRepository = DateRepository().apply { updateDate(LocalDate.now().toString()) }
        )

        useCase(
            name = "Aki",
            employer = "WorkTime",
            workTypes = emptyList(),
            dailyWorkTimeEstimate = "07:30",
            dailyLunchTimeEstimate = "00:30"
        )

        assertEquals("00:30", settingsRepository.insertedWorkStats?.dailyLunchTimeEstimate)
    }

    @Test
    fun invoke_withDailyWorkTimeEstimate_nonCurrentDay_updatesGlobalStatsButNotWorkday() = runBlocking {
        val settingsRepository = FakeSettingsRepository().apply {
            workStats = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
        }
        val workdayRepository = FakeWorkdayRepository()
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            projectRepository = FakeProjectRepository(),
            dateRepository = DateRepository().apply { updateDate("2000-01-01") }
        )

        useCase(name = "Aki", employer = "WorkTime", workTypes = emptyList(), dailyWorkTimeEstimate = "08:00")

        assertEquals("08:00", settingsRepository.insertedWorkStats?.dailyWorkTimeEstimate)
        assertNull(workdayRepository.upsertedWorkdayDate)
    }

    @Test
    fun invoke_withDailyWorkTimeEstimate_currentDayAndNonZeroWorkTime_updatesGlobalStatsButNotWorkday() = runBlocking {
        val today = LocalDate.now().toString()
        val settingsRepository = FakeSettingsRepository().apply {
            workStats = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
        }
        val workdayRepository = FakeWorkdayRepository()
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(SingleProjectState(date = today, projectName = "Alpha", projectTime = "01:00"))
        }
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            projectRepository = projectRepository,
            dateRepository = DateRepository().apply { updateDate(today) }
        )

        useCase(name = "Aki", employer = "WorkTime", workTypes = emptyList(), dailyWorkTimeEstimate = "08:00")

        assertEquals("08:00", settingsRepository.insertedWorkStats?.dailyWorkTimeEstimate)
        assertNull(workdayRepository.upsertedWorkdayDate)
    }

    private class FakeSettingsRepository : SettingsRepository {
        val operations = mutableListOf<String>()
        val insertedWorkTypes = mutableListOf<WorkTypeEntity>()
        var savedSettings: SettingsState? = null
        var workStats: SettingsState? = null
        var insertedWorkStats: SettingsState? = null

        override suspend fun getSettings(): SettingsState? = null

        override suspend fun insertSettings(settings: SettingsState) {
            operations += "insertSettings"
            savedSettings = settings
        }

        override suspend fun getWorkStats(): SettingsState? = workStats

        override suspend fun insertWorkStats(workStats: SettingsState) {
            insertedWorkStats = workStats
            this.workStats = workStats
        }

        override suspend fun getWorkStatsByDate(date: String): SettingsState? = workStats

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

        override suspend fun getProjectTimeSumByDate(date: String): String =
            projectsByDateRange.filter { it.date == date }.fold(ZERO_TIME) { acc, project ->
                WorkTimeCalculator.calculateFlexTime(acc, project.projectTime)
            }
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        var upsertedWorkdayDate: String? = null

        override suspend fun loadWorkday(date: String): SettingsState? = null

        override suspend fun upsertWorkdayStats(date: String, workStats: SettingsState) {
            upsertedWorkdayDate = date
        }

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> = emptyList()
    }
}
