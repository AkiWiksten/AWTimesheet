package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import com.akiwiksten.worktime30.feature.settings.SettingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveSettingsUseCaseTest {

    @Test
    fun invoke_clearsWorkTypes_insertsNewTypes_andSavesSettings() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = SaveSettingsUseCase(settingsRepository, projectDetailsRepository)

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
        val useCase = SaveSettingsUseCase(settingsRepository, projectDetailsRepository)

        useCase(name = "Aki", employer = "WorkTime", workTypes = emptyList())

        assertEquals(listOf("clearWorkTypes", "insertSettings"), settingsRepository.operations)
        assertEquals(SettingsState(name = "Aki", employer = "WorkTime"), settingsRepository.savedSettings)
    }

    @Test
    fun invoke_withDailyWorkTimeEstimate_savesDailyWorkTime() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = SaveSettingsUseCase(settingsRepository, projectDetailsRepository)

        useCase(
            name = "Aki",
            employer = "WorkTime",
            workTypes = emptyList(),
            dailyWorkTimeEstimate = "07:30"
        )

        assertEquals("07:30", projectDetailsRepository.insertedWorkStats?.dailyWorkTime)
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
        var insertedWorkStats: WorkStatsState? = null

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun getWorkStats(): WorkStatsState? = null

        override suspend fun insertWorkStats(workStats: WorkStatsState) {
            insertedWorkStats = workStats
        }

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = emptyList()
    }
}
