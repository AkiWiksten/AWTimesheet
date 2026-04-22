package com.akiwiksten.worktime30.feature.projects.daily

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.DeleteProjectsUseCase
import com.akiwiksten.worktime30.domain.GetProjectsScreenDataUseCase
import com.akiwiksten.worktime30.domain.SaveProjectsUseCase
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
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
    val index: Int = -1,
    val projectName: String = "",
    val projectTime: String = ZERO_TIME,
    val kilometres: String = "0",
    val allowance: String = "",
    val workType: String = "",
    val projectDetails: ProjectDetailsState? = null,
    val workStats: WorkStatsState? = null,
    val date: String = ""
)

sealed class ProjectsUiState {
    object Loading : ProjectsUiState()

    data class Success(
        val date: String = "",
        val workTimeToday: String = ZERO_TIME,
        val dailyWorkTime: String = ZERO_TIME,
        val balanceToday: String = ZERO_TIME,
        val balanceTotal: String = ZERO_TIME,
        val projects: List<SingleProjectState> = emptyList(),
        val projectNames: List<String> = emptyList(),
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
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(value = 0)

    val uiState: StateFlow<ProjectsUiState> = refreshTrigger
        .flatMapLatest { dateRepository.selectedDate }
        .map { date ->
            val data = getProjectsScreenDataUseCase(date)
            val recordedNames = data.projects.map { it.projectName }.toSet()

            val unrecordedProjects = data.projectNames
                .filter { it !in recordedNames }
                .map { name ->
                    SingleProjectState(projectName = name)
                }

            val allProjects = (data.projects + unrecordedProjects)
                .sortedBy { it.projectName }
                .mapIndexed { index, project -> project.copy(index = index) }

            ProjectsUiState.Success(
                date = date,
                workTimeToday = data.projectTime,
                dailyWorkTime = data.dailyWorkTime,
                balanceToday = WorkTimeCalculator.calculateWorkTimeBalance(
                    initialTime = data.projectTime,
                    addedTime = "-${data.dailyWorkTime}"
                ),
                balanceTotal = data.balanceTotal,
                projects = allProjects,
                projectNames = data.projectNames,
                workTypes = data.workTypes
            ) as ProjectsUiState
        }
        .onStart { emit(value = ProjectsUiState.Loading) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
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

                val projectToSave = state.copy(date = date)

                val projectDetailsToSave = state.projectDetails?.copy(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime
                ) ?: ProjectDetailsState(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime,
                    balanceToday = ZERO_TIME
                )

                saveProjectsUseCase(
                    projectsToSave = listOf(projectToSave),
                    projectDetailsToSave = projectDetailsToSave
                )

                state.workStats?.let { projectDetailsRepository.insertWorkStats(it) }

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

    fun updateWorkStats(dailyWorkTime: String, balanceTotal: String) {
        if (!isValidDailyWorkTimeInput(dailyWorkTime) || !isValidBalanceTotalInput(balanceTotal)) {
            return
        }

        viewModelScope.launch {
            try {
                val currentWorkStats = projectDetailsRepository.getWorkStats()
                projectDetailsRepository.insertWorkStats(
                    WorkStatsState(
                        dailyWorkTime = dailyWorkTime,
                        lunchTime = currentWorkStats?.lunchTime ?: ZERO_TIME,
                        balanceTotal = balanceTotal
                    )
                )
                requestReload()
            } catch (e: IllegalArgumentException) {
                Log.e("ProjectsViewModel", "updateWorkStats: ", e)
            } catch (e: IllegalStateException) {
                Log.e("ProjectsViewModel", "updateWorkStats: ", e)
            }
        }
    }
}

private fun isValidDailyWorkTimeInput(value: String): Boolean {
    return value.matches(regex = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]"))
}

private fun isValidBalanceTotalInput(value: String): Boolean {
    return value.matches(regex = Regex(pattern = "[+-]?(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]"))
}
