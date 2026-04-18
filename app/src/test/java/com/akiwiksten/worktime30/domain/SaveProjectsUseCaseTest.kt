package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.WorkdayRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveProjectsUseCaseTest {

    @Test
    fun invoke_savesProjectsAndWorkday() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val workdayRepository = FakeWorkdayRepository()
        val useCase = SaveProjectsUseCase(projectRepository, workdayRepository)

        useCase(
            projectsToSave = listOf(ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "02:00")),
            workdayToSave = WorkdayEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "07:00")
        )

        assertEquals(listOf(ProjectNameEntity(name = "Alpha")), projectRepository.insertedProjectNames)
        assertEquals(1, projectRepository.insertedProjects.size)
        assertEquals(1, workdayRepository.insertedWorkdays.size)
    }

    @Test
    fun invoke_doesNotInsertWorkday_whenWorkdayIsNull() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val workdayRepository = FakeWorkdayRepository()
        val useCase = SaveProjectsUseCase(projectRepository, workdayRepository)

        useCase(
            projectsToSave = listOf(ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "01:00")),
            workdayToSave = null
        )

        assertEquals(emptyList<WorkdayEntity>(), workdayRepository.insertedWorkdays)
    }

    private class FakeProjectRepository : ProjectRepository {
        val insertedProjects = mutableListOf<ProjectEntity>()
        val insertedProjectNames = mutableListOf<ProjectNameEntity>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<ProjectEntity> = emptyList()

        override suspend fun insertProject(project: ProjectEntity) {
            insertedProjects += project
        }

        override suspend fun deleteProject(project: ProjectEntity) = Unit

        override suspend fun getProjectNames(): List<ProjectNameEntity> = emptyList()

        override suspend fun insertProjectName(projectName: ProjectNameEntity) {
            insertedProjectNames += projectName
        }

        override suspend fun deleteProjectName(projectName: ProjectNameEntity) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean =
            false
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        val insertedWorkdays = mutableListOf<WorkdayEntity>()

        override suspend fun getWorkday(date: String, projectName: String): WorkdayEntity? = null

        override suspend fun insertWorkday(workday: WorkdayEntity) {
            insertedWorkdays += workday
        }

        override suspend fun deleteWorkday(workday: WorkdayEntity) = Unit

        override suspend fun getWorkStats(): WorkStatsEntity? = null

        override suspend fun insertWorkStats(workStats: WorkStatsEntity) = Unit

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayEntity> = emptyList()
    }
}
