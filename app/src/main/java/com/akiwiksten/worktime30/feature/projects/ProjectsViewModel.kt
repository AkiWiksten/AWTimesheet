package com.akiwiksten.worktime30.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.WorkDayRepository
import com.akiwiksten.worktime30.domain.GetProjectsScreenDataUseCase
import com.akiwiksten.worktime30.domain.SaveProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectListItemUiState(
    val index: Int = 0,
    val projectName: String = "",
    val projectTime: String = ZERO_TIME,
    val projectStartTime: String = ZERO_TIME,
    val projectEndTime: String = ZERO_TIME,
    val kilometres: Int = 0,
    val allowance: String = "",
    val workType: String = "",
    val titleId: Int = -1,
    val leftOvers: String = "",
    val initBalance: String = "",
    val workDay: WorkDayEntity? = null,
    val workStats: WorkStatsEntity? = null
)

data class ProjectsUiState(
    val date: String = "",
    val workTimeToday: String = ZERO_TIME,
    val projects: List<ProjectListItemUiState> = emptyList(),
    val projectNames: List<ProjectNameEntity> = emptyList(),
    val workTypes: List<String> = emptyList(),
    val isLoading: Boolean = false
)

data class ProjectDialogState(
    val projectName: String,
    val projectTime: String,
    val kilometres: String,
    val allowance: String,
    val workType: String,
    val workDay: WorkDayEntity? = null,
    val workStats: WorkStatsEntity? = null
) {
    constructor(uiState: ProjectListItemUiState) : this(
        projectName = uiState.projectName,
        projectTime = uiState.projectTime,
        kilometres = uiState.kilometres.toString(),
        allowance = uiState.allowance.ifEmpty { "No Allowance" },
        workType = uiState.workType,
        workDay = uiState.workDay,
        workStats = uiState.workStats
    )

    fun toUiState() = ProjectListItemUiState(
        projectName = projectName,
        projectTime = projectTime,
        kilometres = kilometres.toIntOrNull() ?: 0,
        allowance = allowance,
        workType = workType,
        workDay = workDay,
        workStats = workStats
    )
}

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val getProjectsScreenDataUseCase: GetProjectsScreenDataUseCase,
    private val saveProjectsUseCase: SaveProjectsUseCase,
    private val projectRepository: ProjectRepository,
    private val workDayRepository: WorkDayRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dateRepository.selectedDate.collect { date ->
                loadData(date)
            }
        }
    }

    fun loadData(date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, date = date) }
            val data = getProjectsScreenDataUseCase(date)
            _uiState.update { currentState ->
                val recordedProjects = data.projects.map { entity ->
                    ProjectListItemUiState(
                        projectName = entity.projectName,
                        projectTime = entity.projectTime,
                        projectStartTime = entity.projectStartTime,
                        projectEndTime = entity.projectEndTime,
                        kilometres = entity.kilometres,
                        allowance = entity.allowance,
                        workType = entity.workType,
                    )
                }

                val recordedNames = data.projects.map { it.projectName }.toSet()

                val unrecordedProjects = data.projectNames
                    .filter { it.name !in recordedNames }
                    .map { entity ->
                        ProjectListItemUiState(projectName = entity.name)
                    }

                val combinedList = (recordedProjects + unrecordedProjects)
                    .sortedBy { it.projectName }
                    .mapIndexed { index, item -> item.copy(index = index) }

                val totalProjectTime = combinedList.fold(ZERO_TIME) { acc, project ->
                    WorkTimeCalculator.calculateWorkTimeBalance(acc, project.projectTime)
                }

                currentState.copy(
                    workTimeToday = totalProjectTime,
                    projects = combinedList,
                    projectNames = data.projectNames,
                    workTypes = data.workTypes,
                    isLoading = false
                )
            }
        }
    }

    fun saveProject(uiState: ProjectListItemUiState) {
        viewModelScope.launch {
            val date = _uiState.value.date
            val entity = ProjectEntity(
                date = date,
                projectName = uiState.projectName,
                projectTime = uiState.projectTime,
                projectStartTime = uiState.projectStartTime,
                projectEndTime = uiState.projectEndTime,
                kilometres = uiState.kilometres,
                allowance = uiState.allowance,
                workType = uiState.workType
            )
            saveProjectsUseCase(date, listOf(entity), emptyList())
            
            val workDayToSave = uiState.workDay ?: WorkDayEntity(
                date = date,
                projectName = uiState.projectName,
                workTimeToday = uiState.projectTime,
                balanceToday = ZERO_TIME 
            )
            workDayRepository.insertWorkDay(workDayToSave)

            uiState.workStats?.let { workDayRepository.insertWorkStats(it) }

            loadData(date)
        }
    }

    fun deleteProject(uiState: ProjectListItemUiState) {
        viewModelScope.launch {
            val date = _uiState.value.date
            val entity = ProjectEntity(
                date = date,
                projectName = uiState.projectName,
                projectTime = uiState.projectTime,
                projectStartTime = uiState.projectStartTime,
                projectEndTime = uiState.projectEndTime,
                kilometres = uiState.kilometres,
                allowance = uiState.allowance,
                workType = uiState.workType
            )
            projectRepository.deleteProject(entity)
            workDayRepository.deleteWorkDay(WorkDayEntity(date = date, projectName = uiState.projectName))

            if (uiState.projectTime == ZERO_TIME) {
                projectRepository.deleteProjectName(ProjectNameEntity(uiState.projectName))
            }
            loadData(date)
        }
    }
}
