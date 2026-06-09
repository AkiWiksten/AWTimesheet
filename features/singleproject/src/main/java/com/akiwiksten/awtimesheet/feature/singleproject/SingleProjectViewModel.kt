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
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.usecase.DeleteDraftProjectUseCase
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
    private val deleteDraftProjectUseCase: DeleteDraftProjectUseCase,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {
    private var localizedFlexDayWorkType: String = ""

    private val selectedProjectName = MutableStateFlow("")
    private val selectedDate = MutableStateFlow("")
    private val _uiState = MutableStateFlow<SingleProjectUiState>(SingleProjectUiState.Loading)
    val uiState: StateFlow<SingleProjectUiState> = _uiState.asStateFlow()

    fun setLocalizedFlexDayWorkType(workType: String) {
        localizedFlexDayWorkType = workType
    }

    fun initializeState(projectName: String, projectTime: String, isAddMode: Boolean, listIndex: Int,) {
        viewModelScope.launch {
            val effectiveDate = selectedDate.value.ifBlank { dateRepository.selectedDate.first() }
            selectedDate.value = effectiveDate
            selectedProjectName.value = projectName


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
                        projectTime = projectTime.ifEmpty { project?.projectTime ?: currentData.projectTime },
                        kilometres = project?.kilometres ?: currentData.kilometres,
                        allowance = project?.allowance ?: currentData.allowance,
                        workType = project?.workType ?: currentData.workType,
                        date = projectDate,
                        isAddMode = isAddMode,
                        listIndex = listIndex
                    )
                )
            }
        }
    }

    fun saveProject(
        state: SingleProjectState,
        isDraft: Boolean = false,
        settings: SettingsState? = null
    ) {
        viewModelScope.launch {
            try {
                val date = dateRepository.selectedDate.first()
                val projectToSave = state.copy(date = date, isDraft = isDraft)
                val oldWorkTimeByDate = projectRepository.getWorkTimeByDate(date)

                saveWorkdayUseCase(
                    projectToSave = projectToSave,
                    localizedFlexDayWorkType = localizedFlexDayWorkType
                )

                val newWorkTimeByDate = projectRepository.getWorkTimeByDate(date)
                val workTimeByDateChange = WorkTimeCalculator.calculateWorkTimeByDateChange(
                    previousProjectTime = oldWorkTimeByDate,
                    newProjectTime = newWorkTimeByDate
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

    fun deleteDraftProject() {
        viewModelScope.launch {
            try {
                val date = dateRepository.selectedDate.value
                deleteDraftProjectUseCase(date = date, projectName = selectedProjectName.value)
            } catch (e: IllegalArgumentException) {
                Log.e("SingleProjectViewModel", "deleteDraftProject: ", e)
            } catch (e: IllegalStateException) {
                Log.e("SingleProjectViewModel", "deleteDraftProject: ", e)
            }
        }
    }
}
