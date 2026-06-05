package com.akiwiksten.awtimesheet.feature.singleproject

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.usecase.SaveWorkdayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SingleProjectUiState {
    object Loading : SingleProjectUiState()

    data class Success(
        val workTimeByDate: String = ZERO_TIME,
        val workTypes: List<String> = emptyList(),
        val settings: SettingsState? = null,
        val data: SingleProjectState
    ) : SingleProjectUiState()

    data class Error(val message: String) : SingleProjectUiState()
}

@HiltViewModel
class SingleProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val saveWorkdayUseCase: SaveWorkdayUseCase,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {
    private val selectedProjectName = MutableStateFlow("")
    private val selectedDate = MutableStateFlow("")
    private val _uiState = MutableStateFlow<SingleProjectUiState>(SingleProjectUiState.Loading)
    val uiState: StateFlow<SingleProjectUiState> = _uiState.asStateFlow()

    fun initializeState(singleProjectState: SingleProjectState) {
        viewModelScope.launch {
            val effectiveDate = selectedDate.value.ifBlank { dateRepository.selectedDate.first() }
            selectedDate.value = effectiveDate
            selectedProjectName.value = singleProjectState.projectName

            val project = projectRepository.getProject(
                date = effectiveDate,
                projectName = selectedProjectName.value
            )
            val workTypes = settingsRepository.getWorkTypes()
            val settings = settingsRepository.getSettings()
            val workTimeByDate = projectRepository.getWorkTimeByDate(effectiveDate)

            _uiState.update { currentState ->
                val currentSuccess = currentState as? SingleProjectUiState.Success
                val currentData = currentSuccess?.data ?: SingleProjectState()
                val projectDate = project?.date?.ifBlank { effectiveDate } ?: effectiveDate

                SingleProjectUiState.Success(
                    workTimeByDate = workTimeByDate,
                    workTypes = workTypes,
                    settings = settings,
                    data = currentData.copy(
                        projectName = project?.projectName ?: selectedProjectName.value,
                        projectTime = project?.projectTime ?: currentData.projectTime,
                        index = project?.index ?: currentData.index,
                        kilometres = project?.kilometres ?: currentData.kilometres,
                        allowance = project?.allowance ?: currentData.allowance,
                        workType = project?.workType ?: currentData.workType,
                        date = projectDate
                    )
                )
            }
        }
    }

    fun saveProject(
        state: SingleProjectState,
        projectDetails: ProjectDetailsState? = null,
        settings: SettingsState? = null
    ) {
        viewModelScope.launch {
            try {
                val date = dateRepository.selectedDate.first()
                val existingProject = selectedProjectName.value
                    .takeIf { it.isNotBlank() }
                    ?.let { projectRepository.getProject(date = date, projectName = it) }
                val projectToSave = state.copy(date = date)
                val workTimeByDateChange = WorkTimeCalculator.calculateWorkTimeByDateChange(
                    previousProjectTime = existingProject?.projectTime ?: ZERO_TIME,
                    newProjectTime = projectToSave.projectTime
                )

                val projectDetailsToSave = projectDetails?.copy(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime
                ) ?: ProjectDetailsState(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime
                )

                saveWorkdayUseCase(
                    projectToSave = projectToSave,
                    projectDetailsToSave = projectDetailsToSave
                )

                if (workTimeByDateChange != ZERO_TIME) {
                    dateRepository.addWorkTimeByDateChange(workTimeByDateChange)
                }
                selectedProjectName.value = projectToSave.projectName

                settings?.let { settingsRepository.insertSettings(it) }
            } catch (e: IllegalArgumentException) {
                Log.e("SingleProjectViewModel", "saveProject: ", e)
            } catch (e: IllegalStateException) {
                Log.e("SingleProjectViewModel", "saveProject: ", e)
            }
        }
    }
}
