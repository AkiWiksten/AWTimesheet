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
import com.akiwiksten.awtimesheet.domain.usecase.SaveWorkdayUseCase
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectRouteArgs
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
        val data: SingleProjectState,
        val projectDetails: ProjectDetailsState? = null,
        val otherProjectNames: List<String> = emptyList(),
        val baseline: SingleProjectState? = null
    ) : SingleProjectUiState()

    data class Error(val message: String) : SingleProjectUiState()
}

@HiltViewModel
class SingleProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val saveWorkdayUseCase: SaveWorkdayUseCase,
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

    fun initializeState(
        args: SingleProjectRouteArgs,
    ) {
        viewModelScope.launch {
            val effectiveDate = selectedDate.value.ifBlank { dateRepository.selectedDate.first() }
            selectedDate.value = effectiveDate
            selectedProjectName.value = args.originalProjectName.ifBlank { args.projectName }

            val workTypes = settingsRepository.getWorkTypes()
            val settings = settingsRepository.getSettings()
            val workTimeByDate = projectRepository.getWorkTimeByDate(effectiveDate)
            val projectDetails = args.projectDetails ?: projectDetailsRepository.getProjectDetails(
                date = effectiveDate,
                projectName = selectedProjectName.value
            )
            val project = projectRepository.getProject(effectiveDate, selectedProjectName.value)
            val otherProjectNames = projectRepository.getProjectNames()
                .filter { !it.equals(selectedProjectName.value, ignoreCase = true) }

            val dbProjectState = project?.let {
                SingleProjectState(
                    projectName = it.projectName,
                    projectTime = it.projectTime,
                    kilometres = it.kilometres,
                    allowance = it.allowance,
                    workType = it.workType,
                    date = it.date,
                    comment = it.comment,
                    isAddMode = false,
                    listIndex = args.listIndex
                )
            } ?: SingleProjectState(isAddMode = true, date = effectiveDate, listIndex = args.listIndex)

            _uiState.update { currentState ->
                val currentSuccess = currentState as? SingleProjectUiState.Success
                val currentData = currentSuccess?.data ?: SingleProjectState()
                val projectDate = project?.date?.ifBlank { effectiveDate } ?: effectiveDate
                SingleProjectUiState.Success(
                    workTimeByDate = workTimeByDate,
                    workTypes = workTypes,
                    settings = settings,
                    projectDetails = projectDetails,
                    otherProjectNames = otherProjectNames,
                    baseline = currentSuccess?.baseline ?: dbProjectState,
                    data = currentData.copy(
                        projectName = args.projectName.ifEmpty { project?.projectName ?: selectedProjectName.value },
                        projectTime = args.projectTime.ifEmpty { project?.projectTime ?: currentData.projectTime },
                        kilometres = args.kilometres ?: project?.kilometres ?: currentData.kilometres,
                        allowance = args.allowance ?: project?.allowance ?: currentData.allowance,
                        workType = args.workType ?: project?.workType ?: currentData.workType,
                        date = projectDate,
                        isAddMode = args.isAddMode,
                        listIndex = args.listIndex,
                        comment = args.comment ?: project?.comment ?: currentData.comment
                    )
                )
            }
        }
    }

    fun saveProject(
        singleProject: SingleProjectState,
        details: ProjectDetailsState? = null,
        settings: SettingsState? = null
    ) {
        viewModelScope.launch {
            try {
                val date = dateRepository.selectedDate.first()
                val projectToSave = singleProject.copy(date = date)
                val oldWorkTimeByDate = projectRepository.getWorkTimeByDate(date)

                saveWorkdayUseCase(
                    projectToSave = projectToSave,
                    projectDetailsToSave = details?.copy(projectName = projectToSave.projectName),
                    localizedFlexDayWorkType = localizedFlexDayWorkType,
                    originalProjectName = selectedProjectName.value
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
}
