package com.akiwiksten.awtimesheet.test

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.data.database.entity.ProjectEntity
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayStatsRow
import kotlinx.coroutines.delay

class FakeProjectRepository : ProjectRepository {
    private val storedProjects = linkedMapOf<String, SingleProjectState>()

    var projectsByDateRange: List<SingleProjectState> = emptyList()
    var projects: List<SingleProjectState> = emptyList()
    var projectsResult: List<SingleProjectState> = emptyList()
    val projectsByRange = mutableMapOf<String, List<SingleProjectState>>()
    val dataByRange = mutableMapOf<String, List<ProjectEntity>>()
    val requestedRanges = mutableListOf<String>()
    var lastStart: String? = null
    var lastEnd: String? = null
    var readDelayMillis: Long = 0
    var projectNames: List<String> = emptyList()
    val insertedProjects = mutableListOf<SingleProjectState>()
    val deletedProjects = mutableListOf<SingleProjectState>()
    val insertedProjectNames = mutableListOf<String>()
    val deletedProjectNames = mutableListOf<String>()
    val isProjectNameUsedByName = mutableMapOf<String, Boolean>()

    override suspend fun anyRecords(): Boolean =
        storedProjects.isNotEmpty() ||
            projectsByRange.isNotEmpty() ||
            dataByRange.isNotEmpty() ||
            projectsByDateRange.isNotEmpty() ||
            projects.isNotEmpty() ||
            projectsResult.isNotEmpty()

    override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
        if (readDelayMillis > 0) {
            delay(readDelayMillis)
        }

        val key = "$start|$end"
        requestedRanges += key
        lastStart = start
        lastEnd = end

        return when {
            projectsByRange[key] != null -> projectsByRange.getValue(key)
            dataByRange[key] != null -> dataByRange.getValue(key).map(::mapEntityToState)
            storedProjects.isNotEmpty() -> storedProjects.values.filter { it.date in start..end }
            projectsResult.isNotEmpty() -> projectsResult.filter { it.date in start..end }
            projects.isNotEmpty() -> projects.filter { it.date in start..end }
            else -> projectsByDateRange.filter { it.date in start..end }
        }
    }

    override suspend fun getProject(date: String, projectName: String): SingleProjectState? {
        return storedProjects["$date|$projectName"]
    }

    override suspend fun insertProject(project: SingleProjectState) {
        insertedProjects += project
        storedProjects["${project.date}|${project.projectName}"] = project
        projectsByDateRange = upsert(projectsByDateRange, project)
        projects = upsert(projects, project)
        projectsResult = upsert(projectsResult, project)
        projectNames = (projectNames + project.projectName).distinct()
    }

    override suspend fun deleteProject(project: SingleProjectState) {
        deletedProjects += project
        storedProjects.remove("${project.date}|${project.projectName}")
        projectsByDateRange = projectsByDateRange.filterNot {
            it.date == project.date && it.projectName == project.projectName
        }
        projects = projects.filterNot { it.date == project.date && it.projectName == project.projectName }
        projectsResult = projectsResult.filterNot { it.date == project.date && it.projectName == project.projectName }
    }

    override suspend fun getProjectNames(): List<String> =
        if (projectNames.isNotEmpty()) {
            projectNames
        } else {
            storedProjects.values.map { it.projectName }.distinct()
        }

    override suspend fun insertProjectName(projectName: String) {
        insertedProjectNames += projectName
        projectNames = (projectNames + projectName).distinct()
    }

    override suspend fun deleteProjectName(projectName: String) {
        deletedProjectNames += projectName
        projectNames = projectNames.filterNot { it == projectName }
    }

    override suspend fun isProjectNameUsed(projectName: String): Boolean {
        return when (val explicit = isProjectNameUsedByName[projectName]) {
            null -> storedProjects.values.any { it.projectName == projectName } ||
                projectsByDateRange.any { it.projectName == projectName } ||
                projects.any { it.projectName == projectName } ||
                projectsResult.any { it.projectName == projectName }
            else -> explicit
        }
    }

    override suspend fun getWorkTimeByDate(date: String): String {
        return currentProjects().filter { it.date == date }.fold(ZERO_TIME) { total, project ->
            WorkTimeCalculator.calculateFlexTime(total, project.projectTime)
        }
    }

    private fun currentProjects(): List<SingleProjectState> = when {
        storedProjects.isNotEmpty() -> storedProjects.values.toList()
        projectsByDateRange.isNotEmpty() -> projectsByDateRange
        projects.isNotEmpty() -> projects
        projectsResult.isNotEmpty() -> projectsResult
        else -> emptyList()
    }

    private fun upsert(
        source: List<SingleProjectState>,
        project: SingleProjectState,
    ): List<SingleProjectState> {
        return source.filterNot { it.date == project.date && it.projectName == project.projectName } + project
    }

    private fun mapEntityToState(entity: ProjectEntity): SingleProjectState {
        return SingleProjectState(
            date = entity.date,
            projectName = entity.projectName,
            projectTime = entity.projectTime,
            kilometres = entity.kilometres.toString(),
            allowance = entity.allowance,
            workType = entity.workType
        )
    }
}

class FakeSettingsRepository : SettingsRepository {
    var settings: SettingsState? = null
    var globalSettings: SettingsState? = null
    var settingsByDate: SettingsState? = null
    var effectiveSettings: SettingsState? = null
    var workTypes: List<String> = emptyList()
    val operations = mutableListOf<String>()
    val insertedWorkTypes = mutableListOf<String>()
    val deletedWorkTypes = mutableListOf<String>()
    val insertedWorkTypeCalls = mutableListOf<String>()
    var savedSettings: SettingsState? = null
    var insertedSettings: SettingsState? = null
    var insertCalls: Int = 0
    var calculatedFlexTimeTotal: String = ZERO_TIME

    override suspend fun getSettings(): SettingsState? = settings ?: globalSettings

    override suspend fun insertSettings(settings: SettingsState) {
        operations += "insertSettings"
        insertCalls += 1
        savedSettings = settings
        insertedSettings = settings
        this.settings = settings
        this.globalSettings = settings
    }

    override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? {
        return settingsByDate ?: effectiveSettings ?: settings ?: globalSettings
    }

    override suspend fun getWorkTypes(): List<String> = workTypes

    override suspend fun insertWorkType(workType: String) {
        operations += "insertWorkType:$workType"
        insertedWorkTypes += workType
        insertedWorkTypeCalls += workType
        workTypes = (workTypes + workType).distinct()
    }

    override suspend fun deleteWorkType(workType: String) {
        operations += "deleteWorkType:$workType"
        deletedWorkTypes += workType
        workTypes = workTypes.filterNot { it == workType }
    }

    override suspend fun deleteAllWorkTypes() {
        operations += "clearWorkTypes"
        workTypes = emptyList()
    }

    override suspend fun getCalculatedFlextimeTotal(): String = calculatedFlexTimeTotal

    override suspend fun insertCalculatedFlextimeTotal(flexTime: String) {
        calculatedFlexTimeTotal = flexTime
    }
}

class FakeProjectDetailsRepository : ProjectDetailsRepository {
    var settings: SettingsState? = null
    var projectDetails: ProjectDetailsState? = null
    var projectDetailsByDateRange: List<ProjectDetailsState> = emptyList()
    var workdayStatsRows: List<WorkdayStatsRow> = emptyList()
    val insertedProjectDetails = mutableListOf<ProjectDetailsState>()
    val deletedProjectDetails = mutableListOf<ProjectDetailsState>()
    val delayMsByProjectName = mutableMapOf<String, Long>()
    val delayMsByDate = mutableMapOf<String, Long>()
    var getProjectDetailsCallCount: Int = 0
    var lastRequestedDate: String? = null
    var lastRequestedProjectName: String? = null

    override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? {
        getProjectDetailsCallCount += 1
        lastRequestedDate = date
        lastRequestedProjectName = projectName
        delay(delayMsByDate[date] ?: 0L)
        delay(delayMsByProjectName[projectName] ?: 0L)
        return projectDetails ?: insertedProjectDetails.firstOrNull {
            it.date == date && (projectName.isEmpty() || it.projectName == projectName)
        }
    }

    override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) {
        this.projectDetails = projectDetails
        insertedProjectDetails += projectDetails
    }

    override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) {
        deletedProjectDetails += projectDetails
        if (this.projectDetails?.date == projectDetails.date &&
            this.projectDetails?.projectName == projectDetails.projectName
        ) {
            this.projectDetails = null
        }
        insertedProjectDetails.removeIf {
            it.date == projectDetails.date && it.projectName == projectDetails.projectName
        }
    }

    override suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsState> {
        if (projectDetailsByDateRange.isNotEmpty()) return projectDetailsByDateRange
        return insertedProjectDetails.filter { it.date in start..end }
    }
}

class FakeWorkdayRepository(
    private val linkedProjectDetailsRepository: FakeProjectDetailsRepository? = null
) : WorkdayRepository {
    var loadWorkdayResult: String? = null
    var lastDate: String? = null
    var lastSaved: String? = null
    var upsertedWorkdayDate: String? = null
    var upsertedWorkTimeByDateEstimate: String? = null
    var workdayStatsRows: List<WorkdayStatsRow> = emptyList()

    override suspend fun loadWorkday(date: String): String? = loadWorkdayResult

    override suspend fun upsertWorkdayStats(date: String, workTimeByDateEstimate: String) {
        lastDate = date
        lastSaved = workTimeByDateEstimate
        upsertedWorkdayDate = date
        upsertedWorkTimeByDateEstimate = workTimeByDateEstimate
        workdayStatsRows = workdayStatsRows.filterNot { it.date == date } + WorkdayStatsRow(
            date = date,
            workTimeByDateEstimate = workTimeByDateEstimate
        )
        linkedProjectDetailsRepository?.let { repository ->
            repository.workdayStatsRows = workdayStatsRows
            repository.settings = (repository.settings ?: SettingsState()).copy(
                dailyWorkTimeEstimate = workTimeByDateEstimate
            )
        }
    }

    override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> {
        return workdayStatsRows.filter { it.date in start..end }
    }
}
