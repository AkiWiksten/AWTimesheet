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
import com.akiwiksten.worktime30.domain.usecase.GetWorkdayScreenDataUseCase
import com.akiwiksten.worktime30.domain.usecase.SaveWorkdayUseCase
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsArgs
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsUiState
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SingleProjectUiState {
    object Loading : SingleProjectUiState()

    data class Success(
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

    fun setDate(date: String) {
        selectedDate.value = date
        _uiState.update { currentState ->
            when (currentState) {
                is SingleProjectUiState.Success -> {
                    currentState.copy(
                        data = currentState.data.copy(date = date)
                    )
                }

                else -> {
                    SingleProjectUiState.Success(
                        SingleProjectState(date = date)
                    )
                }
            }
        }
    }

    fun setProjectName(projectName: String) {
        selectedProjectName.value = projectName
        _uiState.update { currentState ->
            when (currentState) {
                is SingleProjectUiState.Success -> {
                    currentState.copy(
                        data = currentState.data.copy(projectName = projectName)
                    )
                }

                else -> {
                    SingleProjectUiState.Success(
                        SingleProjectState(projectName = projectName)
                    )
                }
            }
        }
    }

    fun initializeState() {
        var project: SingleProjectState? = null
        viewModelScope.launch {
            project = projectRepository.getProject(
                date = selectedDate.value,
                projectName = selectedProjectName.value
            )
        }
        _uiState.update { currentState ->
            when (currentState) {
                is SingleProjectUiState.Success -> {
                    currentState.copy(
                        data = currentState.data.copy(
                            projectName = project?.projectName ?: "",
                            projectTime = project?.projectTime ?: "",
                            index = project?.index ?: -1,
                            kilometres = project?.kilometres ?: "",
                            allowance = project?.allowance ?: "",
                            workType = project?.workType ?: "",
                            date = project?.date ?: ""
                        )
                    )
                }

                else -> {
                    SingleProjectUiState.Success(
                        SingleProjectState(
                            projectName = project?.projectName ?: "",
                            projectTime = project?.projectTime ?: "",
                            index = project?.index ?: -1,
                            kilometres = project?.kilometres ?: "",
                            allowance = project?.allowance ?: "",
                            workType = project?.workType ?: "",
                            date = project?.date ?: ""
                        )
                    )
                }
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
