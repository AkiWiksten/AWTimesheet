package com.akiwiksten.worktime30.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.GetProjectsScreenDataUseCase
import com.akiwiksten.worktime30.domain.SaveProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectListItemUiState(
    val index: Int = 0,
    val projectName: String = "",
    val projectTime: String = ZERO_TIME,
    val kilometres: Int = 0,
    val allowance: String = "",
    val workType: String = "",
    val titleId: Int = -1,
    val leftOvers: String = "",
    val initBalance: String = "",
    val workday: WorkdayEntity? = null,
    val workStats: WorkStatsEntity? = null
)

sealed class ProjectsUiState {
    object Loading : ProjectsUiState()

    data class Success(
        val date: String = "",
        val workTimeToday: String = ZERO_TIME,
        val projects: List<ProjectListItemUiState> = emptyList(),
        val projectNames: List<ProjectNameEntity> = emptyList(),
        val workTypes: List<String> = emptyList()
    ) : ProjectsUiState()

    data class Error(val message: String) : ProjectsUiState()
}

data class ProjectDialogState(
    val projectName: String,
    val projectTime: String,
    val kilometres: String,
    val allowance: String,
    val workType: String,
    val workday: WorkdayEntity? = null,
    val workStats: WorkStatsEntity? = null
) {
    constructor(uiState: ProjectListItemUiState) : this(
        projectName = uiState.projectName,
        projectTime = uiState.projectTime,
        kilometres = uiState.kilometres.toString(),
        allowance = uiState.allowance.ifEmpty { "No Allowance" },
        workType = uiState.workType,
        workday = uiState.workday,
        workStats = uiState.workStats
    )

    fun toUiState() = ProjectListItemUiState(
        projectName = projectName,
        projectTime = projectTime,
        kilometres = kilometres.toIntOrNull() ?: 0,
        allowance = allowance,
        workType = workType,
        workday = workday,
        workStats = workStats
    )
}

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val getProjectsScreenDataUseCase: GetProjectsScreenDataUseCase,
    private val saveProjectsUseCase: SaveProjectsUseCase,
    private val workdayRepository: WorkdayRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectsUiState>(ProjectsUiState.Loading)
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
            try {
                _uiState.value = ProjectsUiState.Loading
                val data = getProjectsScreenDataUseCase(date)

                val recordedProjects = data.projects.map { entity ->
                    ProjectListItemUiState(
                        projectName = entity.projectName,
                        projectTime = entity.projectTime,
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

                _uiState.value = ProjectsUiState.Success(
                    date = date,
                    workTimeToday = totalProjectTime,
                    projects = combinedList,
                    projectNames = data.projectNames,
                    workTypes = data.workTypes
                )
            } catch (e: Exception) {
                _uiState.value = ProjectsUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun saveProject(uiState: ProjectListItemUiState) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val date = (currentState as? ProjectsUiState.Success)?.date ?: return@launch

                val entity = ProjectEntity(
                    date = date,
                    projectName = uiState.projectName,
                    projectTime = uiState.projectTime,
                    kilometres = uiState.kilometres,
                    allowance = uiState.allowance,
                    workType = uiState.workType
                )

                val workdayToSave = uiState.workday?.copy(
                    date = date,
                    projectName = uiState.projectName,
                    workTimeToday = uiState.projectTime
                ) ?: WorkdayEntity(
                    date = date,
                    projectName = uiState.projectName,
                    workTimeToday = uiState.projectTime,
                    balanceToday = ZERO_TIME
                )

                saveProjectsUseCase(
                    date = date,
                    projectsToSave = listOf(entity),
                    projectNamesToDelete = emptyList(),
                    workdayToSave = workdayToSave
                )

                uiState.workStats?.let { workdayRepository.insertWorkStats(it) }

                loadData(date)
            } catch (e: Exception) {
                _uiState.value = ProjectsUiState.Error(e.message ?: "Failed to save project")
            }
        }
    }

    fun deleteProject(uiState: ProjectListItemUiState) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val date = (currentState as? ProjectsUiState.Success)?.date ?: return@launch

                saveProjectsUseCase(
                    date = date,
                    projectsToSave = emptyList(),
                    projectNamesToDelete = listOf(uiState.projectName)
                )
                loadData(date)
            } catch (e: Exception) {
                _uiState.value = ProjectsUiState.Error(e.message ?: "Failed to delete project")
            }
        }
    }
}
