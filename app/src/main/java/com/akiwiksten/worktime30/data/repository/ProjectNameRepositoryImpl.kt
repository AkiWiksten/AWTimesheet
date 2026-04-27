package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.ProjectNameDao
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toProjectNameEntity
import com.akiwiksten.worktime30.domain.repository.ProjectNameRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectNameRepositoryImpl @Inject constructor(
    private val projectNameDao: ProjectNameDao
) : ProjectNameRepository {
    override suspend fun anyRecords(): Boolean = projectNameDao.anyRecords()

    override suspend fun insertProjectName(projectName: String) =
        projectNameDao.insertProjectName(projectName.toProjectNameEntity())

    override suspend fun loadProjectNames(): List<String> =
        projectNameDao.loadProjectNames().map { it.toDomain() }

    override suspend fun deleteProjectName(projectName: String) =
        projectNameDao.delete(projectName.toProjectNameEntity())
}
