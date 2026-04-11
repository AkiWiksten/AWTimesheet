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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val name = MutableStateFlow("")
    private val employer = MutableStateFlow("")
    private val endMonthDate = MutableStateFlow("")
    private val workTypes = MutableStateFlow<List<String>>(emptyList())
    private val projectsByMonth = MutableStateFlow<List<ProjectEntity>>(emptyList())
    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)

    init {
        observeUiState()
    }

    private fun observeUiState() {
        viewModelScope.launch {
            val successStateFlow = combine(name, employer, endMonthDate, workTypes, projectsByMonth) {
                    currentName,
                    currentEmployer,
                    currentEndMonthDate,
                    currentWorkTypes,
                    currentProjects ->
                SettingsUiState.Success(
                    name = currentName,
                    employer = currentEmployer,
                    endMonthDate = currentEndMonthDate,
                    workTypes = currentWorkTypes,
                    projectsByMonth = currentProjects
                )
            }

            combine(isLoading, errorMessage, successStateFlow) { loading, error, successState ->
                when {
                    !error.isNullOrEmpty() -> SettingsUiState.Error(error)
                    loading -> SettingsUiState.Loading
                    else -> successState
                }
            }
                .distinctUntilChanged()
                .collect { _uiState.value = it }
        }
    }

    fun setEndMonthDate(selectedDate: String) {
        val initial = LocalDate.parse(selectedDate)
        endMonthDate.value = initial.withDayOfMonth(initial.month.length(initial.isLeapYear)).toString()
    }

    fun setName(name0: String) {
        name.value = name0
    }

    fun setEmployer(employer0: String) {
        employer.value = employer0
    }

    fun addWorkType(workType: String) {
        workTypes.update { currentWorkTypes ->
            if (workType in currentWorkTypes) {
                currentWorkTypes
            } else {
                (currentWorkTypes + workType).sorted()
            }
        }
    }

    fun removeWorkType(workType: String) {
        workTypes.update { currentWorkTypes ->
            currentWorkTypes.filter { it != workType }
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
                errorMessage.value = null
                endMonthDate.value = endOfMonth
                projectsByMonth.value = projects
            } catch (e: IllegalArgumentException) {
                errorMessage.value = "Failed to load projects: ${e.message}"
            } catch (e: IllegalStateException) {
                errorMessage.value = "Failed to load projects: ${e.message}"
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val data = getSettingsUseCase()
                errorMessage.value = null
                name.value = data.name
                employer.value = data.employer
                workTypes.value = data.workTypes
                isLoading.value = false
            } catch (e: IllegalArgumentException) {
                isLoading.value = false
                errorMessage.value = "Failed to load settings: ${e.message}"
            } catch (e: IllegalStateException) {
                isLoading.value = false
                errorMessage.value = "Failed to load settings: ${e.message}"
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                saveSettingsUseCase(
                    name = name.value,
                    employer = employer.value,
                    workTypes = workTypes.value
                )
                errorMessage.value = null
            } catch (e: IllegalArgumentException) {
                errorMessage.value = "Failed to save settings: ${e.message}"
            } catch (e: IllegalStateException) {
                errorMessage.value = "Failed to save settings: ${e.message}"
            }
        }
    }
}
