package com.akiwiksten.worktime30.feature.projects.daily

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.DeleteProjectsUseCase
import com.akiwiksten.worktime30.domain.GetProjectsScreenDataUseCase
import com.akiwiksten.worktime30.domain.SaveProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SingleProjectState(
    val index: Int = 0,
    val projectName: String = "",
    val projectTime: String = ZERO_TIME,
    val kilometres: String = "0",
    val allowance: String = "No Allowance",
    val workType: String = "",
    val workday: WorkdayEntity? = null,
    val workStats: WorkStatsEntity? = null
)

sealed class ProjectsUiState {
    object Loading : ProjectsUiState()

    data class Success(
        val date: String = "",
        val workTimeToday: String = ZERO_TIME,
        val projects: List<SingleProjectState> = emptyList(),
        val projectNames: List<ProjectNameEntity> = emptyList(),
        val workTypes: List<String> = emptyList()
    ) : ProjectsUiState()

    data class Error(val message: String) : ProjectsUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val getProjectsScreenDataUseCase: GetProjectsScreenDataUseCase,
    private val saveProjectsUseCase: SaveProjectsUseCase,
    private val deleteProjectsUseCase: DeleteProjectsUseCase,
    private val workdayRepository: WorkdayRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(value = 0)

    val uiState: StateFlow<ProjectsUiState> = refreshTrigger
        .flatMapLatest { dateRepository.selectedDate }
        .map { date ->
            val data = getProjectsScreenDataUseCase(date)
            val recordedProjects = data.projects.map { entity ->
                SingleProjectState(
                    projectName = entity.projectName,
                    projectTime = entity.projectTime,
                    kilometres = entity.kilometres.toString(),
                    allowance = entity.allowance,
                    workType = entity.workType,
                )
            }

            val recordedNames = data.projects.map { it.projectName }.toSet()

            val unrecordedProjects = data.projectNames
                .filter { it.name !in recordedNames }
                .map { entity ->
                    SingleProjectState(projectName = entity.name)
                }

            val allProjects = (recordedProjects + unrecordedProjects)
                .mapIndexed { index, project -> project.copy(index = index) }

            ProjectsUiState.Success(
                date = date,
                workTimeToday = data.projectTime,
                projects = allProjects,
                projectNames = data.projectNames,
                workTypes = data.workTypes
            ) as ProjectsUiState
        }
        .onStart { emit(value = ProjectsUiState.Loading) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = ProjectsUiState.Loading
        )

    fun retryLoad() {
        requestReload()
    }

    private fun requestReload() {
        refreshTrigger.value += 1
    }

    fun saveProject(state: SingleProjectState) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                val date = (currentState as? ProjectsUiState.Success)?.date ?: return@launch

                val entity = ProjectEntity(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime,
                    kilometres = state.kilometres.toIntOrNull() ?: 0,
                    allowance = state.allowance,
                    workType = state.workType
                )

                val workdayToSave = state.workday?.copy(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime
                ) ?: WorkdayEntity(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime,
                    balanceToday = ZERO_TIME
                )

                saveProjectsUseCase(
                    projectsToSave = listOf(entity),
                    workdayToSave = workdayToSave
                )

                state.workStats?.let { workdayRepository.insertWorkStats(it) }

                requestReload()
            } catch (e: IllegalArgumentException) {
                Log.e("ProjectsViewModel", "saveProject: ", e)
            } catch (e: IllegalStateException) {
                Log.e("ProjectsViewModel", "saveProject: ", e)
            }
        }
    }

    fun deleteProject(state: SingleProjectState) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                val date = (currentState as? ProjectsUiState.Success)?.date ?: return@launch

                deleteProjectsUseCase(date = date, projectName = state.projectName)
                requestReload()
            } catch (e: IllegalArgumentException) {
                Log.e("ProjectsViewModel", "deleteProject: ", e)
            } catch (e: IllegalStateException) {
                Log.e("ProjectsViewModel", "deleteProject: ", e)
            }
        }
    }
}
