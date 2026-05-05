package com.akiwiksten.worktime30.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.usecase.GetProjectsByMonthUseCase
import com.akiwiksten.worktime30.domain.usecase.GetSettingsUseCase
import com.akiwiksten.worktime30.domain.usecase.ProjectsByMonthResult
import com.akiwiksten.worktime30.domain.usecase.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsUiState {
    object Loading : SettingsUiState()

    data class Success(
        val data: SettingsState,
        val selectedDate: String = "",
        val endMonthDate: String = "",
        val projectsByMonth: List<SingleProjectState> = emptyList()
    ) : SettingsUiState()

    data class Error(val message: String) : SettingsUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val getProjectsByMonthUseCase: GetProjectsByMonthUseCase,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dateRepository.selectedDate.collect { date ->
                if (date.isNotEmpty()) {
                    _uiState.updateSelectedDate(date)
                    refreshProjectsByMonth(date)
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

    fun setLunchTimeEstimate(lunchTimeEstimate: String) {
        val currentState = _uiState.value
        if (currentState is SettingsUiState.Success) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(dailyLunchTimeEstimate = lunchTimeEstimate)
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

    fun refreshProjectsByMonth(date: String) {
        viewModelScope.launch {
            try {
                val monthlyResult = getProjectsByMonthUseCase.getMonthlyProjects(date)
                val currentState = _uiState.value
                if (currentState is SettingsUiState.Success) {
                    _uiState.value = currentState.copy(
                        endMonthDate = monthlyResult.endOfMonth,
                        projectsByMonth = monthlyResult.projects
                    )
                }
            } catch (e: IllegalArgumentException) {
                _uiState.handleException(e, "Failed to load projects")
            } catch (e: IllegalStateException) {
                _uiState.handleException(e, "Failed to load projects")
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                val currentDate = dateRepository.selectedDate.value
                val loadedData = getSettingsUseCase()
                val monthlyResult = getProjectsByMonthUseCase.getMonthlyProjects(currentDate)

                _uiState.value = SettingsUiState.Success(
                    data = loadedData,
                    selectedDate = currentDate,
                    endMonthDate = monthlyResult.endOfMonth,
                    projectsByMonth = monthlyResult.projects
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
