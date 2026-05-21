package com.akiwiksten.awtimesheet.feature.workday

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.calculator.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.usecase.DeleteProjectUseCase
import com.akiwiksten.awtimesheet.domain.usecase.GetWorkdayScreenDataUseCase
import com.akiwiksten.awtimesheet.domain.usecase.UpdateSettingsParams
import com.akiwiksten.awtimesheet.domain.usecase.UpdateSettingsUseCase
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

sealed class WorkdayUiState {
    object Loading : WorkdayUiState()

    data class Success(
        val date: String = "",
        val workTimeByDate: String = ZERO_TIME,
        val workTimeByDateEstimate: String = ZERO_TIME,
        val flexTimeByDate: String = ZERO_TIME,
        val initialFlexTimeTotal: String = ZERO_TIME,
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
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    val workTimeByDateChange: StateFlow<String> = dateRepository.workTimeByDateChange

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

            val workTypes = settingsRepository.getWorkTypes()
            val flexTimeByDate = WorkTimeCalculator.calculateFlexTime(
                initialTime = data.workTimeByDate,
                addedTime = "-${data.workTimeByDateEstimate}"
            )

            WorkdayUiState.Success(
                date = date,
                workTimeByDate = data.workTimeByDate,
                workTimeByDateEstimate = data.workTimeByDateEstimate,
                flexTimeByDate = flexTimeByDate,
                initialFlexTimeTotal = data.initialFlexTimeTotal,
                flexTimeTotal = data.flexTimeTotal,
                projects = allProjects,
                workTypes = workTypes
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

    fun reconcileFlexTimeTotalAfterProjectEditorReturn(oldFlexTimeByDate: String) {
        viewModelScope.launch {
            try {
                reconcileFlexTimeTotal(oldFlexTimeByDate = oldFlexTimeByDate)
            } catch (e: IllegalArgumentException) {
                Log.e("WorkdayViewModel", "reconcileFlexTimeTotalAfterProjectEditorReturn: ", e)
            } catch (e: IllegalStateException) {
                Log.e("WorkdayViewModel", "reconcileFlexTimeTotalAfterProjectEditorReturn: ", e)
            }
        }
    }

    fun deleteProject(state: SingleProjectState) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value as? WorkdayUiState.Success ?: return@launch
                val date = currentState.date
                val oldFlexTimeByDate = currentState.flexTimeByDate

                deleteProjectUseCase(date = date, projectName = state.projectName, projectTime = state.projectTime)
                if (state.projectTime != ZERO_TIME) {
                    dateRepository.addWorkTimeByDateChange("-${state.projectTime}")
                }
                reconcileFlexTimeTotal(oldFlexTimeByDate = oldFlexTimeByDate)
            } catch (e: IllegalArgumentException) {
                Log.e("WorkdayViewModel", "deleteProject: ", e)
            } catch (e: IllegalStateException) {
                Log.e("WorkdayViewModel", "deleteProject: ", e)
            }
        }
    }

    fun updateSettings(workTimeByDateEstimate: String, updateGlobalSettings: Boolean = false) {
        if (!isValidWorkTimeByDateEstimateInput(workTimeByDateEstimate)) {
            return
        }

        viewModelScope.launch {
            try {
                val currentUiState = uiState.value as? WorkdayUiState.Success ?: return@launch
                updateSettingsUseCase(
                    UpdateSettingsParams(
                        date = currentUiState.date,
                        workTimeByDate = currentUiState.workTimeByDate,
                        currentWorkTimeByDateEstimate = currentUiState.workTimeByDateEstimate,
                        newWorkTimeByDateEstimate = workTimeByDateEstimate,
                        newInitialFlexTimeTotal = currentUiState.initialFlexTimeTotal,
                        updateGlobalSettings = updateGlobalSettings
                    )
                )
                requestReload()
            } catch (e: IllegalArgumentException) {
                Log.e("WorkdayViewModel", "updateSettings: ", e)
            } catch (e: IllegalStateException) {
                Log.e("WorkdayViewModel", "updateSettings: ", e)
            }
        }
    }

    private suspend fun reconcileFlexTimeTotal(oldFlexTimeByDate: String) {
        val date = dateRepository.selectedDate.value
        if (date.isBlank()) {
            requestReload()
            return
        }

        val latestData = getWorkdayScreenDataUseCase(date)
        val newFlexTimeByDate = calculateFlexTimeByDate(
            workTimeByDate = latestData.workTimeByDate,
            workTimeByDateEstimate = latestData.workTimeByDateEstimate
        )
        val flexTimeByDateDelta = WorkTimeCalculator.calculateFlexTime(
            initialTime = newFlexTimeByDate,
            addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$oldFlexTimeByDate")
        )

        if (flexTimeByDateDelta != ZERO_TIME) {
            val persistedCalculatedFlexTimeTotal = settingsRepository.getCalculatedFlextimeTotal()
            val updatedCalculatedFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
                initialTime = persistedCalculatedFlexTimeTotal,
                addedTime = flexTimeByDateDelta
            )
            settingsRepository.insertCalculatedFlextimeTotal(updatedCalculatedFlexTimeTotal)
        }

        requestReload()
    }
}

private fun isValidWorkTimeByDateEstimateInput(value: String): Boolean {
    return value.matches(regex = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]"))
}

private fun calculateFlexTimeByDate(workTimeByDate: String, workTimeByDateEstimate: String): String {
    return WorkTimeCalculator.calculateFlexTime(
        initialTime = workTimeByDate,
        addedTime = "-$workTimeByDateEstimate"
    )
}
