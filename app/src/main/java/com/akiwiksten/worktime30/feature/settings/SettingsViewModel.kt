package com.akiwiksten.worktime30.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.GetWorkdayByMonthUseCase
import com.akiwiksten.worktime30.domain.GetSettingsUseCase
import com.akiwiksten.worktime30.domain.SaveSettingsUseCase
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed class SettingsUiState {
    object Loading : SettingsUiState()

    data class Success(
        val data: SettingsState
    ) : SettingsUiState()

    data class Error(val message: String) : SettingsUiState()
}

data class SettingsState(
    val name: String = "",
    val employer: String = "",
    val selectedDate: String = "",
    val endMonthDate: String = "",
    val workTypes: List<String> = emptyList(),
    val projectsByMonth: List<SingleProjectState> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val getWorkdayByMonthUseCase: GetWorkdayByMonthUseCase,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeDate()
    }

    private fun observeDate() {
        viewModelScope.launch {
            dateRepository.selectedDate.collect { date ->
                if (date.isNotEmpty()) {
                    val currentState = _uiState.value
                    if (currentState is SettingsUiState.Success) {
                        _uiState.value = currentState.copy(
                            data = currentState.data.copy(selectedDate = date)
                        )
                    }
                    loadProjectsByMonth(date)
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

    fun loadProjectsByMonth(date: String) {
        viewModelScope.launch {
            try {
                val parsedDate = LocalDate.parse(date)
                val endOfMonth = parsedDate
                    .withDayOfMonth(parsedDate.month.length(parsedDate.isLeapYear))
                    .toString()
                val projects = getWorkdayByMonthUseCase(date)
                val currentState = _uiState.value
                if (currentState is SettingsUiState.Success) {
                    _uiState.value = currentState.copy(
                        data = currentState.data.copy(
                            endMonthDate = endOfMonth,
                            projectsByMonth = projects
                        )
                    )
                }
            } catch (e: IllegalArgumentException) {
                handleException(e, "Failed to load projects")
            } catch (e: IllegalStateException) {
                handleException(e, "Failed to load projects")
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                val loadedData = getSettingsUseCase()
                val currentDate = dateRepository.selectedDate.value
                val parsedDate = LocalDate.parse(currentDate)
                val endOfMonth = parsedDate
                    .withDayOfMonth(parsedDate.month.length(parsedDate.isLeapYear))
                    .toString()
                val projects = getWorkdayByMonthUseCase(currentDate)

                _uiState.value = SettingsUiState.Success(
                    data = SettingsState(
                        name = loadedData.name,
                        employer = loadedData.employer,
                        selectedDate = currentDate,
                        endMonthDate = endOfMonth,
                        workTypes = loadedData.workTypes,
                        projectsByMonth = projects
                    )
                )
            } catch (e: IllegalArgumentException) {
                handleException(e, "Failed to load settings")
            } catch (e: IllegalStateException) {
                handleException(e, "Failed to load settings")
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is SettingsUiState.Success) {
                    saveSettingsUseCase(
                        name = currentState.data.name,
                        employer = currentState.data.employer,
                        workTypes = currentState.data.workTypes
                    )
                }
            } catch (e: IllegalArgumentException) {
                handleException(e, "Failed to save settings")
            } catch (e: IllegalStateException) {
                handleException(e, "Failed to save settings")
            }
        }
    }

    private fun handleException(exception: Exception, message: String) {
        _uiState.value = SettingsUiState.Error("$message: ${exception.message}")
    }
}
