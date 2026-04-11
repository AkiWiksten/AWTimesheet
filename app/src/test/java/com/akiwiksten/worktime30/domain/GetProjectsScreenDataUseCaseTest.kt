package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.data.repository.WorkdayRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetProjectsScreenDataUseCaseTest {

    @Test
    fun invoke_returnsAllDataWithWorkdayTime() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projects = listOf(ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "03:00"))
            projectNames = listOf(ProjectNameEntity(name = "Alpha"))
        }
        val workdayRepository = FakeWorkdayRepository().apply {
            workday = WorkdayEntity(date = "2026-04-10", workTimeToday = "07:30")
        }
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf(WorkTypeEntity(workType = "Office"), WorkTypeEntity(workType = "Remote"))
        }
        val useCase = GetProjectsScreenDataUseCase(projectRepository, workdayRepository, settingsRepository)

        val result = useCase("2026-04-10")

        assertEquals("07:30", result.workTimeToday)
        assertEquals(1, result.projects.size)
        assertEquals(1, result.projectNames.size)
        assertEquals(listOf("Office", "Remote"), result.workTypes)
    }

    @Test
    fun invoke_usesZeroTimeWhenWorkdayMissing() = runBlocking {
        val useCase = GetProjectsScreenDataUseCase(
            projectRepository = FakeProjectRepository(),
            workdayRepository = FakeWorkdayRepository().apply { workday = null },
            settingsRepository = FakeSettingsRepository()
        )

        val result = useCase("2026-04-10")

        assertEquals(ZERO_TIME, result.workTimeToday)
    }

    private class FakeProjectRepository : ProjectRepository {
        var projects: List<ProjectEntity> = emptyList()
        var projectNames: List<ProjectNameEntity> = emptyList()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<ProjectEntity> = projects

        override suspend fun insertProject(project: ProjectEntity) = Unit

        override suspend fun deleteProject(project: ProjectEntity) = Unit

        override suspend fun getProjectNames(): List<ProjectNameEntity> = projectNames

        override suspend fun insertProjectName(projectName: ProjectNameEntity) = Unit

        override suspend fun deleteProjectName(projectName: ProjectNameEntity) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        var workday: WorkdayEntity? = null

        override suspend fun getWorkday(date: String, projectName: String): WorkdayEntity? = workday

        override suspend fun insertWorkday(workday: WorkdayEntity) = Unit

        override suspend fun deleteWorkday(workday: WorkdayEntity) = Unit

        override suspend fun getWorkStats(): WorkStatsEntity? = null

        override suspend fun insertWorkStats(workStats: WorkStatsEntity) = Unit

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayEntity> = emptyList()
    }

    private class FakeSettingsRepository : SettingsRepository {
        var workTypes: List<WorkTypeEntity> = emptyList()

        override suspend fun getSettings(): SettingsEntity? = null

        override suspend fun insertSettings(settings: SettingsEntity) = Unit

        override suspend fun getWorkTypes(): List<WorkTypeEntity> = workTypes

        override suspend fun insertWorkType(workType: WorkTypeEntity) = Unit

        override suspend fun deleteWorkType(workType: WorkTypeEntity) = Unit

        override suspend fun clearWorkTypes() = Unit
    }
}

