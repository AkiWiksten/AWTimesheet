package com.akiwiksten.worktime30.feature.projects.single

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.usecase.SaveWorkdayUseCase
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
    //private val workTimeByDate = MutableStateFlow("")
    private val _uiState = MutableStateFlow<SingleProjectUiState>(SingleProjectUiState.Loading)
    val uiState: StateFlow<SingleProjectUiState> = _uiState.asStateFlow()

    fun initializeState(singleProjectState: SingleProjectState) {
        viewModelScope.launch {
            val effectiveDate = selectedDate.value.ifBlank { dateRepository.selectedDate.first() }
            selectedDate.value = effectiveDate
            selectedProjectName.value = singleProjectState.projectName
            //workTimeByDate.value = singleProjectState.projectTime

            val project = projectRepository.getProject(
                date = effectiveDate,
                projectName = selectedProjectName.value
            )
            val workTypes = settingsRepository.getWorkTypes()
            val workTimeByDate = projectRepository.getWorkTimeByDate(effectiveDate)

            _uiState.update { currentState ->
                val currentSuccess = currentState as? SingleProjectUiState.Success
                val currentData = currentSuccess?.data ?: SingleProjectState()
                val projectDate = project?.date?.ifBlank { effectiveDate } ?: effectiveDate

                SingleProjectUiState.Success(
                    workTimeByDate = workTimeByDate,
                    workTypes = workTypes,
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
                val projectToSave = state.copy(date = date)

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
                    projectsToSave = listOf(projectToSave),
                    projectDetailsToSave = projectDetailsToSave
                )

                settings?.let { settingsRepository.insertSettings(it) }
            } catch (e: IllegalArgumentException) {
                Log.e("SingleProjectViewModel", "saveProject: ", e)
            } catch (e: IllegalStateException) {
                Log.e("SingleProjectViewModel", "saveProject: ", e)
            }
        }
    }
}
