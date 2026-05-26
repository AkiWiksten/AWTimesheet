package com.akiwiksten.awtimesheet.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.usecase.GetProjectsByMonthUseCase
import com.akiwiksten.awtimesheet.domain.usecase.GetSettingsUseCase
import com.akiwiksten.awtimesheet.domain.usecase.GenerateWorkdaysUseCase
import com.akiwiksten.awtimesheet.domain.usecase.GeneratedAllowanceLabels
import com.akiwiksten.awtimesheet.domain.usecase.ProjectsByMonthResult
import com.akiwiksten.awtimesheet.domain.usecase.SaveSettingsUseCase
import com.akiwiksten.awtimesheet.domain.usecase.WorkdayGenerationMode
import com.akiwiksten.awtimesheet.domain.usecase.WorkdayGenerationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsUiState {
    object Loading : SettingsUiState()

    data class Success(
        val data: SettingsState,
        val selectedDate: String = ""
    ) : SettingsUiState()

    data class Error(val message: String) : SettingsUiState()
}

sealed class SettingsEvent {
    data class TimesheetReportReady(
        val projectsByMonth: List<SingleProjectState>,
        val endOfMonthDate: String,
        val name: String,
        val employer: String,
        val totalFlexTimeTotal: String = "00:00"
    ) : SettingsEvent()

    object NoProjectsForMonth : SettingsEvent()

    data class MonthlyReportError(val message: String) : SettingsEvent()

    data class WorkdayGenerationSuccess(
        val mode: WorkdayGenerationMode,
        val insertedCount: Int,
        val updatedCount: Int,
        val weekdayCandidates: Int,
        val startDate: String,
        val endDate: String
    ) : SettingsEvent()

    data class WorkdayGenerationError(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val getProjectsByMonthUseCase: GetProjectsByMonthUseCase,
    private val generateWorkdaysUseCase: GenerateWorkdaysUseCase,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            dateRepository.selectedDate.collect { date ->
                if (date.isNotEmpty()) {
                    _uiState.updateSelectedDate(date)
                }
            }
        }
    }

    fun setName(name: String) {
        val currentState = _uiState.value
        if (currentState is SettingsUiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(name = name)
            )
        }
    }

    fun setEmployer(employer: String) {
        val currentState = _uiState.value
        if (currentState is SettingsUiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(employer = employer)
            )
        }
    }

    fun setDailyWorkTimeEstimate(dailyWorkTimeEstimate: String) {
        val currentState = _uiState.value
        if (currentState is SettingsUiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(dailyWorkTimeEstimate = dailyWorkTimeEstimate)
            )
        }
    }

    fun setDailyLunchTimeEstimate(dailyLunchTimeEstimate: String) {
        val currentState = _uiState.value
        if (currentState is SettingsUiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(dailyLunchTimeEstimate = dailyLunchTimeEstimate)
            )
        }
    }

    fun setInitialFlexTimeTotal(initialFlexTimeTotal: String) {
        val currentState = _uiState.value
        if (currentState is SettingsUiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(initialFlexTimeTotal = initialFlexTimeTotal)
            )
        }
    }

    fun addWorkType(workType: String) {
        val currentState = _uiState.value
        if (currentState is SettingsUiState.Success) {
            val currentWorkTypes = currentState.data.workTypes
            val updatedWorkTypes = if (workType in currentWorkTypes) {
                currentWorkTypes
            } else {
                (currentWorkTypes + workType).sorted()
            }
            _uiState.value = currentState.copy(
                data = currentState.data.copy(workTypes = updatedWorkTypes)
            )
        }
    }

    fun ensureDefaultWorkType(defaultWorkType: String) {
        val currentState = _uiState.value
        if (
            defaultWorkType.isBlank() ||
            currentState !is SettingsUiState.Success ||
            defaultWorkType in currentState.data.workTypes
        ) {
            return
        }

        val updatedWorkTypes = (currentState.data.workTypes + defaultWorkType).sorted()
        _uiState.value = currentState.copy(
            data = currentState.data.copy(workTypes = updatedWorkTypes)
        )

        viewModelScope.launch {
            settingsRepository.insertWorkType(defaultWorkType)
        }
    }

    fun removeWorkType(workType: String) {
        val currentState = _uiState.value
        if (currentState is SettingsUiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(
                    workTypes = currentState.data.workTypes.filter { it != workType }
                )
            )
        }
        viewModelScope.launch {
            settingsRepository.deleteWorkType(workType)
        }
    }

    fun requestMonthlyReport(
        name: String,
        employer: String
    ) {
        viewModelScope.launch {
            try {
                val selectedDate = (_uiState.value as? SettingsUiState.Success)?.selectedDate
                    ?.takeIf { it.isNotBlank() }
                    ?: dateRepository.selectedDate.value
                val monthlyResult = getProjectsByMonthUseCase.getMonthlyProjects(selectedDate)
                if (monthlyResult.projects.isEmpty()) {
                    _events.emit(SettingsEvent.NoProjectsForMonth)
                } else {
                    _events.emit(
                        SettingsEvent.TimesheetReportReady(
                            projectsByMonth = monthlyResult.projects,
                            endOfMonthDate = monthlyResult.endOfMonth,
                            name = name,
                            employer = employer,
                            totalFlexTimeTotal = monthlyResult.flexTimeTotal
                        )
                    )
                }
            } catch (e: IllegalArgumentException) {
                _events.emit(SettingsEvent.MonthlyReportError("Failed to load projects: ${e.message}"))
            } catch (e: IllegalStateException) {
                _events.emit(SettingsEvent.MonthlyReportError("Failed to load projects: ${e.message}"))
            }
        }
    }

    fun generateWorkdaysForSelectedMonth(
        allowanceLabels: GeneratedAllowanceLabels = defaultGeneratedAllowanceLabels()
    ) {
        generateWorkdays(
            scope = WorkdayGenerationScope.MONTH,
            mode = WorkdayGenerationMode.INSERT_MISSING,
            allowanceLabels = allowanceLabels
        )
    }

    fun generateWorkdaysForSelectedYear(
        allowanceLabels: GeneratedAllowanceLabels = defaultGeneratedAllowanceLabels()
    ) {
        generateWorkdays(
            scope = WorkdayGenerationScope.YEAR,
            mode = WorkdayGenerationMode.INSERT_MISSING,
            allowanceLabels = allowanceLabels
        )
    }

    fun refreshWorkdaysForSelectedMonth(
        allowanceLabels: GeneratedAllowanceLabels = defaultGeneratedAllowanceLabels()
    ) {
        generateWorkdays(
            scope = WorkdayGenerationScope.MONTH,
            mode = WorkdayGenerationMode.UPSERT_ALL_WEEKDAYS,
            allowanceLabels = allowanceLabels
        )
    }

    fun refreshWorkdaysForSelectedYear(
        allowanceLabels: GeneratedAllowanceLabels = defaultGeneratedAllowanceLabels()
    ) {
        generateWorkdays(
            scope = WorkdayGenerationScope.YEAR,
            mode = WorkdayGenerationMode.UPSERT_ALL_WEEKDAYS,
            allowanceLabels = allowanceLabels
        )
    }

    private fun generateWorkdays(
        scope: WorkdayGenerationScope,
        mode: WorkdayGenerationMode,
        allowanceLabels: GeneratedAllowanceLabels
    ) {
        viewModelScope.launch {
            try {
                val selectedDate = (_uiState.value as? SettingsUiState.Success)?.selectedDate
                    ?.takeIf { it.isNotBlank() }
                    ?: dateRepository.selectedDate.value

                val result = generateWorkdaysUseCase(
                    selectedDate = selectedDate,
                    scope = scope,
                    mode = mode,
                    allowanceLabels = allowanceLabels
                )

                _events.emit(
                    SettingsEvent.WorkdayGenerationSuccess(
                        mode = mode,
                        insertedCount = result.insertedWorkdays,
                        updatedCount = result.updatedWorkdays,
                        weekdayCandidates = result.weekdayCandidates,
                        startDate = result.startDate,
                        endDate = result.endDate
                    )
                )
            } catch (e: IllegalArgumentException) {
                _events.emit(SettingsEvent.WorkdayGenerationError(e.message ?: "Unknown error"))
            } catch (e: IllegalStateException) {
                _events.emit(SettingsEvent.WorkdayGenerationError(e.message ?: "Unknown error"))
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                val currentDate = dateRepository.selectedDate.value
                val loadedData = getSettingsUseCase()

                _uiState.value = SettingsUiState.Success(
                    data = loadedData,
                    selectedDate = currentDate
                )
            } catch (e: IllegalArgumentException) {
                _uiState.handleException(e, "Failed to load settings")
            } catch (e: IllegalStateException) {
                _uiState.handleException(e, "Failed to load settings")
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is SettingsUiState.Success) {
                    saveSettingsUseCase(
                        settings = currentState.data
                    )
                }
            } catch (e: IllegalArgumentException) {
                _uiState.handleException(e, "Failed to save settings")
            } catch (e: IllegalStateException) {
                _uiState.handleException(e, "Failed to save settings")
            }
        }
    }
}

private fun defaultGeneratedAllowanceLabels(): GeneratedAllowanceLabels {
    return GeneratedAllowanceLabels(
        noAllowance = "No allowance",
        fullAllowance = "Full allowance",
        halfDayAllowance = "Half-day allowance"
    )
}

private fun MutableStateFlow<SettingsUiState>.updateSelectedDate(date: String) {
    val currentState = value
    if (currentState is SettingsUiState.Success) {
        value = currentState.copy(selectedDate = date)
    }
}

private suspend fun GetProjectsByMonthUseCase.getMonthlyProjects(date: String): ProjectsByMonthResult {
    return invoke(date)
}

private fun MutableStateFlow<SettingsUiState>.handleException(exception: Exception, message: String) {
    value = SettingsUiState.Error("$message: ${exception.message}")
}
