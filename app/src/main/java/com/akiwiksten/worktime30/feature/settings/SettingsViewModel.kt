package com.akiwiksten.worktime30.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.GetProjectsByMonthUseCase
import com.akiwiksten.worktime30.domain.GetSettingsUseCase
import com.akiwiksten.worktime30.domain.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed class SettingsUiState {
    object Loading : SettingsUiState()

    data class Success(
        val name: String = "",
        val employer: String = "",
        val endMonthDate: String = "",
        val workTypes: List<String> = emptyList(),
        val projectsByMonth: List<ProjectEntity> = emptyList()
    ) : SettingsUiState()

    data class Error(val message: String) : SettingsUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val getProjectsByMonthUseCase: GetProjectsByMonthUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setEndMonthDate(selectedDate: String) {
        val initial = LocalDate.parse(selectedDate)
        val endOfMonth = initial.withDayOfMonth(initial.month.length(initial.isLeapYear)).toString()
        _uiState.update { currentState ->
            (currentState as? SettingsUiState.Success)?.copy(endMonthDate = endOfMonth)
                ?: currentState
        }
    }

    fun setName(name0: String) {
        _uiState.update { currentState ->
            (currentState as? SettingsUiState.Success)?.copy(name = name0)
                ?: currentState
        }
    }

    fun setEmployer(employer0: String) {
        _uiState.update { currentState ->
            (currentState as? SettingsUiState.Success)?.copy(employer = employer0)
                ?: currentState
        }
    }

    fun addWorkType(workType: String) {
        _uiState.update { currentState ->
            if (currentState is SettingsUiState.Success && !currentState.workTypes.contains(workType)) {
                currentState.copy(workTypes = (currentState.workTypes + workType).sorted())
            } else {
                currentState
            }
        }
    }

    fun removeWorkType(workType: String) {
        _uiState.update { currentState ->
            if (currentState is SettingsUiState.Success) {
                currentState.copy(workTypes = currentState.workTypes.filter { it != workType })
            } else {
                currentState
            }
        }
        viewModelScope.launch {
            settingsRepository.deleteWorkType(WorkTypeEntity(workType = workType))
        }
    }

    fun loadProjectsByMonth(date: String) {
        viewModelScope.launch {
            try {
                val parsedDate = LocalDate.parse(date)
                val endOfMonth = parsedDate
                    .withDayOfMonth(parsedDate.month.length(parsedDate.isLeapYear))
                    .toString()
                val projects = getProjectsByMonthUseCase(date)
                _uiState.update { currentState ->
                    (currentState as? SettingsUiState.Success)?.copy(
                        endMonthDate = endOfMonth,
                        projectsByMonth = projects
                    ) ?: currentState
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update { SettingsUiState.Error("Failed to load projects: ${e.message}") }
            } catch (e: IllegalStateException) {
                _uiState.update { SettingsUiState.Error("Failed to load projects: ${e.message}") }
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            try {
                val data = getSettingsUseCase()
                _uiState.update { currentState ->
                    if (currentState is SettingsUiState.Success) {
                        currentState.copy(
                            name = data.name,
                            employer = data.employer,
                            workTypes = data.workTypes
                        )
                    } else {
                        SettingsUiState.Success(
                            name = data.name,
                            employer = data.employer,
                            workTypes = data.workTypes
                        )
                    }
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update { SettingsUiState.Error("Failed to load settings: ${e.message}") }
            } catch (e: IllegalStateException) {
                _uiState.update { SettingsUiState.Error("Failed to load settings: ${e.message}") }
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                if (state is SettingsUiState.Success) {
                    saveSettingsUseCase(
                        name = state.name,
                        employer = state.employer,
                        workTypes = state.workTypes
                    )
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update { SettingsUiState.Error("Failed to save settings: ${e.message}") }
            } catch (e: IllegalStateException) {
                _uiState.update { SettingsUiState.Error("Failed to save settings: ${e.message}") }
            }
        }
    }
}
