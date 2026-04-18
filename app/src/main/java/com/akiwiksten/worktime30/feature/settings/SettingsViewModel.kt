package com.akiwiksten.worktime30.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.GetProjectsByMonthUseCase
import com.akiwiksten.worktime30.domain.GetSettingsUseCase
import com.akiwiksten.worktime30.domain.SaveSettingsUseCase
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
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
        val selectedDate: String = "",
        val endMonthDate: String = "",
        val workTypes: List<String> = emptyList(),
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

    private val name = MutableStateFlow("")
    private val employer = MutableStateFlow("")
    private val currentSelectedDate = MutableStateFlow("")
    private val endMonthDate = MutableStateFlow("")
    private val workTypes = MutableStateFlow<List<String>>(emptyList())
    private val projectsByMonth = MutableStateFlow<List<SingleProjectState>>(emptyList())
    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)

    init {
        observeUiState()
        observeDate()
    }

    private fun observeDate() {
        viewModelScope.launch {
            dateRepository.selectedDate.collect { date ->
                if (date.isNotEmpty()) {
                    currentSelectedDate.value = date
                    loadProjectsByMonth(date)
                }
            }
        }
    }

    private fun observeUiState() {
        viewModelScope.launch {
            val baseFlow = combine(name, employer, endMonthDate, workTypes, projectsByMonth) {
                    n, e, em, wt, pm ->
                SettingsUiState.Success(
                    name = n,
                    employer = e,
                    endMonthDate = em,
                    workTypes = wt,
                    projectsByMonth = pm
                )
            }
            val successStateFlow = combine(baseFlow, currentSelectedDate) { base, date ->
                base.copy(selectedDate = date)
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
