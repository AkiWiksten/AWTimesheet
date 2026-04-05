package com.akiwiksten.worktime30.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
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
    val workType: String
) {
    constructor(uiState: ProjectListItemUiState) : this(
        projectName = uiState.projectName,
        projectTime = uiState.projectTime,
        kilometres = uiState.kilometres.toString(),
        allowance = uiState.allowance.ifEmpty { "No Allowance" },
        workType = uiState.workType
    )

    fun toUiState() = ProjectListItemUiState(
        projectName = projectName,
        projectTime = projectTime,
        kilometres = kilometres.toIntOrNull() ?: 0,
        allowance = allowance,
        workType = workType
    )
}

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val getProjectsScreenDataUseCase: GetProjectsScreenDataUseCase,
    private val saveProjectsUseCase: SaveProjectsUseCase,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    fun loadData(date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, date = date) }
            val data = getProjectsScreenDataUseCase(date)
            _uiState.update { currentState ->
                currentState.copy(
                    workTimeToday = data.workTimeToday,
                    projects = data.projects.mapIndexed { index, entity ->
                        ProjectListItemUiState(
                            index = index,
                            projectName = entity.projectName,
                            projectTime = entity.projectTime,
                            projectStartTime = entity.projectStartTime,
                            projectEndTime = entity.projectEndTime,
                            kilometres = entity.kilometres,
                            allowance = entity.allowance,
                            workType = entity.workType,
                        )
                    },
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
            loadData(date)
        }
    }

    fun deleteProject(uiState: ProjectListItemUiState) {
        viewModelScope.launch {
            val entity = ProjectEntity(
                date = _uiState.value.date,
                projectName = uiState.projectName,
                projectTime = uiState.projectTime,
                projectStartTime = uiState.projectStartTime,
                projectEndTime = uiState.projectEndTime,
                kilometres = uiState.kilometres,
                allowance = uiState.allowance,
                workType = uiState.workType
            )
            projectRepository.deleteProject(entity)
            loadData(_uiState.value.date)
        }
    }
}
