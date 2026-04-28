package com.akiwiksten.worktime30.feature.workday

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.usecase.DeleteWorkdayUseCase
import com.akiwiksten.worktime30.domain.usecase.GetWorkdayScreenDataUseCase
import com.akiwiksten.worktime30.domain.usecase.SaveWorkdayUseCase
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
import java.time.LocalDate
import javax.inject.Inject

sealed class WorkdayUiState {
    object Loading : WorkdayUiState()

    data class Success(
        val date: String = "",
        val workTimeToday: String = ZERO_TIME,
        val workTimeTodayEstimate: String = ZERO_TIME,
        val flexTimeToday: String = ZERO_TIME,
        val initialFlexTimeTotal: String = ZERO_TIME,
        val calculatedFlexTimeTotal: String = ZERO_TIME,
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
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository,
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
                workTimeTodayEstimate = data.workTimeTodayEstimate,
                flexTimeToday = WorkTimeCalculator.calculateFlexTime(
                    initialTime = data.projectTime,
                    addedTime = "-${data.workTimeTodayEstimate}"
                ),
                initialFlexTimeTotal = data.initialFlexTimeTotal,
                calculatedFlexTimeTotal = data.calculatedFlexTimeTotal,
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
                    projectTime = state.projectTime
                )

                saveWorkdayUseCase(
                    projectsToSave = listOf(projectToSave),
                    projectDetailsToSave = projectDetailsToSave
                )

                state.workStats?.let {
                    settingsRepository.insertWorkStats(
                        SettingsState(
                            dailyWorkTimeEstimate = it.dailyWorkTimeEstimate,
                            dailyLunchTimeEstimate = it.dailyLunchTimeEstimate,
                            initialFlexTimeTotal = it.initialFlexTimeTotal
                        )
                    )
                }

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

    fun updateWorkStats(workTimeTodayEstimate: String, initialFlexTimeTotal: String) {
        if (
            !isValidWorkTimeTodayEstimateInput(workTimeTodayEstimate) ||
            !isValidInitialFlexTimeTotalInput(initialFlexTimeTotal)
        ) {
            return
        }

        viewModelScope.launch {
            try {
                val currentUiState = uiState.value as? WorkdayUiState.Success ?: return@launch
                val isCurrentDay = currentUiState.date == LocalDate.now().toString()
                val canUpdateWorkTimeTodayEstimate = isCurrentDay && currentUiState.workTimeToday == ZERO_TIME
                val currentWorkStats = settingsRepository.getWorkStatsByDate(currentUiState.date)
                val existingWorkTimeTodayEstimate = currentWorkStats?.dailyWorkTimeEstimate
                    ?.ifEmpty { currentUiState.workTimeTodayEstimate }
                    ?: currentUiState.workTimeTodayEstimate

                workdayRepository.upsertWorkdayStats(
                    date = currentUiState.date,
                    workStats = WorkStatsState(
                        dailyWorkTimeEstimate = if (canUpdateWorkTimeTodayEstimate) {
                            workTimeTodayEstimate
                        } else {
                            existingWorkTimeTodayEstimate
                        },
                        dailyLunchTimeEstimate = currentWorkStats?.dailyLunchTimeEstimate ?: ZERO_TIME,
                        initialFlexTimeTotal = initialFlexTimeTotal
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

private fun isValidWorkTimeTodayEstimateInput(value: String): Boolean {
    return value.matches(regex = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]"))
}

private fun isValidInitialFlexTimeTotalInput(value: String): Boolean {
    return value.matches(regex = Regex(pattern = "[+-]?(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]"))
}
