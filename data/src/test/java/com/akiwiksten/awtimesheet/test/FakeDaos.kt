@file:Suppress("unused", "ImportOrdering")

package com.akiwiksten.awtimesheet.test

import com.akiwiksten.awtimesheet.data.database.dao.CalculatedFlexTimeTotalDao
import com.akiwiksten.awtimesheet.data.database.dao.ProjectDao
import com.akiwiksten.awtimesheet.data.database.dao.ProjectDetailsDao
import com.akiwiksten.awtimesheet.data.database.dao.ProjectNameDao
import com.akiwiksten.awtimesheet.data.database.dao.SettingsDao
import com.akiwiksten.awtimesheet.data.database.dao.WorkdayDao
import com.akiwiksten.awtimesheet.data.database.dao.WorkTypeDao
import com.akiwiksten.awtimesheet.data.database.entity.CalculatedFlextimeTotalEntity
import com.akiwiksten.awtimesheet.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.awtimesheet.data.database.entity.ProjectEntity
import com.akiwiksten.awtimesheet.data.database.entity.ProjectNameEntity
import com.akiwiksten.awtimesheet.data.database.entity.SettingsEntity
import com.akiwiksten.awtimesheet.data.database.entity.WorkdayEntity
import com.akiwiksten.awtimesheet.data.database.entity.WorkTypeEntity
import com.akiwiksten.awtimesheet.data.mapper.toDomain
import com.akiwiksten.awtimesheet.data.mapper.toEntity
import com.akiwiksten.awtimesheet.data.mapper.toProjectNameEntity

class FakeProjectDao : ProjectDao {
    var projectsByDateRangeResult: List<ProjectEntity> = emptyList()
    var insertedProject: ProjectEntity? = null
    var deletedProject: ProjectEntity? = null
    var lastDateStart: String? = null
    var lastDateEnd: String? = null
    var projectNameUsed: Boolean = false

    override suspend fun anyRecords(): Boolean = false

    override suspend fun getAll(): List<ProjectEntity> = emptyList()

    override suspend fun insertProject(project: ProjectEntity) {
        insertedProject = project
    }

    override suspend fun loadProjectsByDate(date: String): List<ProjectEntity> = emptyList()

    override suspend fun loadProject(date: String, projectName: String): ProjectEntity? = null

    override suspend fun delete(project: ProjectEntity) {
        deletedProject = project
    }

    override suspend fun getProjectsByDateRange(dateStart: String, dateEnd: String): List<ProjectEntity> {
        lastDateStart = dateStart
        lastDateEnd = dateEnd
        return projectsByDateRangeResult
    }

    override suspend fun isProjectNameUsed(projectName: String): Boolean = projectNameUsed

    override suspend fun getProjectTimesByDate(date: String): List<String> = emptyList()
}

class FakeProjectNameDao : ProjectNameDao {
    var projectNamesResult: List<String> = emptyList()
    var insertedProjectName: String? = null
    var deletedProjectName: String? = null

    override suspend fun anyRecords(): Boolean = false

    override suspend fun insertProjectName(project: ProjectNameEntity) {
        insertedProjectName = project.name
    }

    override suspend fun loadProjectNames(): List<ProjectNameEntity> =
        projectNamesResult.map { it.toProjectNameEntity() }

    override suspend fun delete(project: ProjectNameEntity) {
        deletedProjectName = project.name
    }
}

class FakeProjectDetailsDao : ProjectDetailsDao {
    var projectDetailsResult: ProjectDetailsEntity? = null
    var projectDetailsByDateRangeResult: List<ProjectDetailsEntity> = emptyList()
    var insertedProjectDetails: ProjectDetailsEntity? = null
    var deletedProjectDetails: ProjectDetailsEntity? = null
    var lastDate: String? = null
    var lastProjectName: String? = null
    var lastDateStart: String? = null
    var lastDateEnd: String? = null

    override suspend fun anyRecords(): Boolean = false

    override suspend fun getAll(): List<ProjectDetailsEntity> = emptyList()

    override suspend fun loadProjectDetails(date: String, projectName: String): ProjectDetailsEntity? {
        lastDate = date
        lastProjectName = projectName
        return projectDetailsResult
    }

    override suspend fun insertProjectDetails(projectDetails: ProjectDetailsEntity) {
        insertedProjectDetails = projectDetails
    }

    override suspend fun delete(projectDetails: ProjectDetailsEntity) {
        deletedProjectDetails = projectDetails
    }

    override suspend fun getProjectDetailsByDateRange(dateStart: String, dateEnd: String): List<ProjectDetailsEntity> {
        lastDateStart = dateStart
        lastDateEnd = dateEnd
        return projectDetailsByDateRangeResult
    }
}

class FakeSettingsDao : SettingsDao {
    var settingsResult: com.akiwiksten.awtimesheet.domain.model.SettingsState? = null
    var insertedSettings: com.akiwiksten.awtimesheet.domain.model.SettingsState? = null

    override suspend fun anyRecord(): Boolean = false

    override suspend fun insertSettings(settings: SettingsEntity) {
        insertedSettings = settings.toDomain()
    }

    override suspend fun loadSettings(): SettingsEntity? = settingsResult?.toEntity()
}

class FakeWorkTypeDao : WorkTypeDao {
    var workTypesResult: List<String> = emptyList()
    var insertedWorkType: String? = null
    var deletedWorkType: String? = null
    var deleteAllCallCount: Int = 0

    override suspend fun anyRecords(): Boolean = false

    override suspend fun insertWorkType(workType: WorkTypeEntity) {
        insertedWorkType = workType.workType
    }

    override suspend fun loadWorkTypes(): List<WorkTypeEntity> =
        workTypesResult.map { WorkTypeEntity(workType = it) }

    override suspend fun delete(workType: WorkTypeEntity) {
        deletedWorkType = workType.workType
    }

    override suspend fun deleteAllWorkTypes() {
        deleteAllCallCount += 1
    }
}

class FakeWorkdayDao : WorkdayDao {
    var workdayResult: WorkdayEntity? = null

    override suspend fun loadWorkday(date: String): WorkdayEntity? = workdayResult

    override suspend fun insertWorkday(workday: WorkdayEntity) = Unit

    override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayEntity> = emptyList()
}

class FakeCalculatedFlexTimeTotalDao : CalculatedFlexTimeTotalDao {
    var storedEntity: CalculatedFlextimeTotalEntity? = null
    var insertedFlexTime: String? = null

    override suspend fun insertCalculatedFlextimeTotal(flexTime: CalculatedFlextimeTotalEntity) {
        insertedFlexTime = flexTime.calculatedFlexTimeTotal
        storedEntity = flexTime
    }

    override suspend fun loadCalculatedFlextimeTotal(): CalculatedFlextimeTotalEntity? = storedEntity
}

