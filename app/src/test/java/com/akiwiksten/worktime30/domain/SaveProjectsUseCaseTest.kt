package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
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
    fun invoke_savesProjectsAndWorkday_andDeletesUnusedProjectName() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = false
        }
        val workdayRepository = FakeWorkdayRepository()
        val useCase = SaveProjectsUseCase(projectRepository, workdayRepository)

        useCase(
            date = "2026-04-10",
            projectsToSave = listOf(ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "02:00")),
            projectNamesToDelete = listOf("Beta"),
            workdayToSave = WorkdayEntity(date = "2026-04-10", projectName = "Alpha", workTimeToday = "07:00")
        )

        assertEquals(listOf(ProjectNameEntity(name = "Alpha")), projectRepository.insertedProjectNames)
        assertEquals(1, projectRepository.insertedProjects.size)
        assertEquals(
            listOf(ProjectEntity(date = "2026-04-10", projectName = "Beta", projectTime = ZERO_TIME)),
            projectRepository.deletedProjects
        )
        assertEquals(listOf(ProjectNameEntity(name = "Beta")), projectRepository.deletedProjectNames)
        assertEquals(1, workdayRepository.insertedWorkdays.size)
        assertEquals(
            listOf(
                WorkdayEntity(
                    date = "2026-04-10",
                    projectName = "Beta"
                )
            ),
            workdayRepository.deletedWorkdays
        )
    }

    @Test
    fun invoke_doesNotDeleteProjectName_whenStillUsed() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = true
        }
        val workdayRepository = FakeWorkdayRepository()
        val useCase = SaveProjectsUseCase(projectRepository, workdayRepository)

        useCase(
            date = "2026-04-10",
            projectsToSave = emptyList(),
            projectNamesToDelete = listOf("Beta"),
            workdayToSave = null
        )

        assertEquals(emptyList<ProjectNameEntity>(), projectRepository.deletedProjectNames)
    }

    private class FakeProjectRepository : ProjectRepository {
        val insertedProjects = mutableListOf<ProjectEntity>()
        val deletedProjects = mutableListOf<ProjectEntity>()
        val insertedProjectNames = mutableListOf<ProjectNameEntity>()
        val deletedProjectNames = mutableListOf<ProjectNameEntity>()
        val isProjectNameUsedByName = mutableMapOf<String, Boolean>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<ProjectEntity> = emptyList()

        override suspend fun insertProject(project: ProjectEntity) {
            insertedProjects += project
        }

        override suspend fun deleteProject(project: ProjectEntity) {
            deletedProjects += project
        }

        override suspend fun getProjectNames(): List<ProjectNameEntity> = emptyList()

        override suspend fun insertProjectName(projectName: ProjectNameEntity) {
            insertedProjectNames += projectName
        }

        override suspend fun deleteProjectName(projectName: ProjectNameEntity) {
            deletedProjectNames += projectName
        }

        override suspend fun isProjectNameUsed(projectName: String): Boolean =
            isProjectNameUsedByName[projectName] ?: false
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        val insertedWorkdays = mutableListOf<WorkdayEntity>()
        val deletedWorkdays = mutableListOf<WorkdayEntity>()

        override suspend fun getWorkday(date: String, projectName: String): WorkdayEntity? = null

        override suspend fun insertWorkday(workday: WorkdayEntity) {
            insertedWorkdays += workday
        }

        override suspend fun deleteWorkday(workday: WorkdayEntity) {
            deletedWorkdays += workday
        }

        override suspend fun getWorkStats(): WorkStatsEntity? = null

        override suspend fun insertWorkStats(workStats: WorkStatsEntity) = Unit

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayEntity> = emptyList()
    }
}
