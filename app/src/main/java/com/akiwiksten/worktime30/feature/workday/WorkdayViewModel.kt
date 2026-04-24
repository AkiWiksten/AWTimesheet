package com.akiwiksten.worktime30.feature.workday

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.DeleteWorkdayUseCase
import com.akiwiksten.worktime30.domain.GetWorkdayScreenDataUseCase
import com.akiwiksten.worktime30.domain.SaveWorkdayUseCase
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

sealed class WorkdayUiState {
    object Loading : WorkdayUiState()

    data class Success(
        val date: String = "",
        val workTimeToday: String = ZERO_TIME,
        val dailyWorkTime: String = ZERO_TIME,
        val flexTimeToday: String = ZERO_TIME,
        val flexTimeTotal: String = ZERO_TIME,
        val projects: List<SingleProjectState> = emptyList(),
        val workTypes: List<String> = emptyList()
    ) : WorkdayUiState()

    data class Error(val message: String) : WorkdayUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkdayViewModel @Inject constructor(
    private val getWorkdayScreenDataUseCase: GetWorkdayScreenDataUseCase,
    private val saveWorkdayUseCase: SaveWorkdayUseCase,
    private val deleteWorkdayUseCase: DeleteWorkdayUseCase,
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(value = 0)

    val uiState: StateFlow<WorkdayUiState> = refreshTrigger
        .flatMapLatest { dateRepository.selectedDate }
        .map { date ->
            val data = getWorkdayScreenDataUseCase(date)
            val recordedNames = data.projects.map { it.projectName }.toSet()

            val unrecordedProjects = data.projectNames
                .filter { it !in recordedNames }
                .map { name ->
                    SingleProjectState(projectName = name)
                }

            val allProjects = (data.projects + unrecordedProjects)
                .sortedBy { it.projectName }
                .mapIndexed { index, project -> project.copy(index = index) }

            WorkdayUiState.Success(
                date = date,
                workTimeToday = data.projectTime,
                dailyWorkTime = data.dailyWorkTime,
                flexTimeToday = WorkTimeCalculator.calculateFlexTime(
                    initialTime = data.projectTime,
                    addedTime = "-${data.dailyWorkTime}"
                ),
                flexTimeTotal = data.flexTimeTotal,
                projects = allProjects,
                workTypes = data.workTypes
            ) as WorkdayUiState
        }
        .onStart { emit(value = WorkdayUiState.Loading) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = WorkdayUiState.Loading
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
                val date = (currentState as? WorkdayUiState.Success)?.date ?: return@launch

                val projectToSave = state.copy(date = date)

                val projectDetailsToSave = state.projectDetails?.copy(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime
                ) ?: ProjectDetailsState(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime,
                    flexTimeToday = ZERO_TIME
                )

                saveWorkdayUseCase(
                    projectsToSave = listOf(projectToSave),
                    projectDetailsToSave = projectDetailsToSave
                )

                state.workStats?.let { projectDetailsRepository.insertWorkStats(it) }

                requestReload()
            } catch (e: IllegalArgumentException) {
                Log.e("WorkdayViewModel", "saveProject: ", e)
            } catch (e: IllegalStateException) {
                Log.e("WorkdayViewModel", "saveProject: ", e)
            }
        }
    }

    fun deleteProject(state: SingleProjectState) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                val date = (currentState as? WorkdayUiState.Success)?.date ?: return@launch

                deleteWorkdayUseCase(date = date, projectName = state.projectName, projectTime = state.projectTime)
                requestReload()
            } catch (e: IllegalArgumentException) {
                Log.e("WorkdayViewModel", "deleteProject: ", e)
            } catch (e: IllegalStateException) {
                Log.e("WorkdayViewModel", "deleteProject: ", e)
            }
        }
    }

    fun updateWorkStats(dailyWorkTime: String, flexTimeTotal: String) {
        if (!isValidDailyWorkTimeInput(dailyWorkTime) || !isValidFlexTimeTotalInput(flexTimeTotal)) {
            return
        }

        viewModelScope.launch {
            try {
                val currentWorkStats = projectDetailsRepository.getWorkStats()
                projectDetailsRepository.insertWorkStats(
                    WorkStatsState(
                        dailyWorkTime = dailyWorkTime,
                        lunchTime = currentWorkStats?.lunchTime ?: ZERO_TIME,
                        flexTimeTotal = flexTimeTotal
                    )
                )
                requestReload()
            } catch (e: IllegalArgumentException) {
                Log.e("WorkdayViewModel", "updateWorkStats: ", e)
            } catch (e: IllegalStateException) {
                Log.e("WorkdayViewModel", "updateWorkStats: ", e)
            }
        }
    }
}

private fun isValidDailyWorkTimeInput(value: String): Boolean {
    return value.matches(regex = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]"))
}

private fun isValidFlexTimeTotalInput(value: String): Boolean {
    return value.matches(regex = Regex(pattern = "[+-]?(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]"))
}


