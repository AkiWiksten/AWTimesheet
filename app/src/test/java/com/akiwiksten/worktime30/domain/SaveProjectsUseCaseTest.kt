package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveProjectsUseCaseTest {

    @Test
    fun invoke_savesProjectsAndProjectDetails() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = SaveProjectsUseCase(projectRepository, projectDetailsRepository)

        useCase(
            projectsToSave = listOf(ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "02:00")),
            projectDetailsToSave = ProjectDetailsEntity(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "07:00"
            )
        )

        assertEquals(listOf(ProjectNameEntity(name = "Alpha")), projectRepository.insertedProjectNames)
        assertEquals(1, projectRepository.insertedProjects.size)
        assertEquals(1, projectDetailsRepository.insertedProjectDetails.size)
    }

    @Test
    fun invoke_doesNotInsertProjectDetails_whenProjectDetailsIsNull() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = SaveProjectsUseCase(projectRepository, projectDetailsRepository)

        useCase(
            projectsToSave = listOf(ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "01:00")),
            projectDetailsToSave = null
        )

        assertEquals(emptyList<ProjectDetailsEntity>(), projectDetailsRepository.insertedProjectDetails)
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

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        val insertedProjectDetails = mutableListOf<ProjectDetailsEntity>()

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsEntity? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsEntity) {
            insertedProjectDetails += projectDetails
        }

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsEntity) = Unit

        override suspend fun getWorkStats(): WorkStatsEntity? = null

        override suspend fun insertWorkStats(workStats: WorkStatsEntity) = Unit

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsEntity> = emptyList()
    }
}
