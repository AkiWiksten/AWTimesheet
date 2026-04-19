package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import com.akiwiksten.worktime30.feature.settings.SettingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetProjectsScreenDataUseCaseTest {

    @Test
    fun invoke_returnsAllDataWithCalculatedProjectTime() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projects = listOf(
                SingleProjectState(date = "2026-04-10", projectName = "Alpha", projectTime = "03:00"),
                SingleProjectState(date = "2026-04-10", projectName = "Beta", projectTime = "04:30")
            )
            projectNames = listOf("Alpha", "Beta")
        }
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf(WorkTypeEntity(workType = "Office"), WorkTypeEntity(workType = "Remote"))
        }
        val useCase = GetProjectsScreenDataUseCase(projectRepository, settingsRepository)

        val result = useCase("2026-04-10")

        assertEquals("07:30", result.projectTime)
        assertEquals(2, result.projects.size)
        assertEquals(2, result.projectNames.size)
        assertEquals(listOf("Office", "Remote"), result.workTypes)
    }

    @Test
    fun invoke_usesZeroTimeWhenNoProjects() = runBlocking {
        val useCase = GetProjectsScreenDataUseCase(
            projectRepository = FakeProjectRepository(),
            settingsRepository = FakeSettingsRepository()
        )

        val result = useCase("2026-04-10")

        assertEquals(ZERO_TIME, result.projectTime)
    }

    private class FakeProjectRepository : ProjectRepository {
        var projects: List<SingleProjectState> = emptyList()
        var projectNames: List<String> = emptyList()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> = projects

        override suspend fun insertProject(project: SingleProjectState) = Unit

        override suspend fun deleteProject(project: SingleProjectState) = Unit

        override suspend fun getProjectNames(): List<String> = projectNames

        override suspend fun insertProjectName(projectName: String) = Unit

        override suspend fun deleteProjectName(projectName: String) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false
    }

    private class FakeSettingsRepository : SettingsRepository {
        var workTypes: List<WorkTypeEntity> = emptyList()

        override suspend fun getSettings(): SettingsState? = null

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getWorkTypes(): List<WorkTypeEntity> = workTypes

        override suspend fun insertWorkType(workType: WorkTypeEntity) = Unit

        override suspend fun deleteWorkType(workType: WorkTypeEntity) = Unit

        override suspend fun clearWorkTypes() = Unit
    }
}
