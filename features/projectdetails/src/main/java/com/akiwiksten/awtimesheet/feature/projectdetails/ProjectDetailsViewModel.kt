package com.akiwiksten.awtimesheet.feature.projectdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.TIME_FORMAT
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator.StartTimeUpdateParams
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.model.hasOnlyProjectTime
import com.akiwiksten.awtimesheet.domain.model.isNewDayForProject
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.feature.projectdetails.calculator.ProjectDetailsTimeUpdateCalculator
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsField
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class ProjectDetailsUiState {
    object Loading : ProjectDetailsUiState()

    data class Success(
        val details: ProjectDetailsState,
        val settings: SettingsState = SettingsState()
    ) : ProjectDetailsUiState()

    data class Error(val message: String) : ProjectDetailsUiState()
}

/**
 * ViewModel for managing the project details screen.
 */
@HiltViewModel
class ProjectDetailsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectDetailsUiState>(ProjectDetailsUiState.Loading)
    val uiState: StateFlow<ProjectDetailsUiState> = _uiState.asStateFlow()
    private val _isInitialLoadComplete = MutableStateFlow(false)
    val isInitialLoadComplete: StateFlow<Boolean> = _isInitialLoadComplete.asStateFlow()
    private var dateObserverJob: Job? = null
    private var loadProjectDetailsJob: Job? = null
    private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)

    fun observeDateRepository(projectName: String, projectTime: String) {
        dateObserverJob?.cancel()
        dateObserverJob = viewModelScope.launch {
            dateRepository.selectedDate.collectLatest { date ->
                val currentDetails = (uiState.value as? ProjectDetailsUiState.Success)?.details
                    ?: ProjectDetailsState(
                        date = date,
                        projectName = projectName,
                        projectTime = projectTime
                    )
                loadProjectDetails(
                    date = date,
                    projectDetailsArg = currentDetails.copy(date = date)
                )
            }
        }
    }

    fun setDate(date: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is ProjectDetailsUiState.Success -> {
                    currentState.copy(
                        details = currentState.details.copy(date = date)
                    )
                }

                else -> {
                    ProjectDetailsUiState.Success(
                        ProjectDetailsState(date = date)
                    )
                }
            }
        }
    }

    fun setProjectName(projectName: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is ProjectDetailsUiState.Success -> {
                    currentState.copy(
                        details = currentState.details.copy(projectName = projectName)
                    )
                }

                else -> {
                    ProjectDetailsUiState.Success(
                        details = ProjectDetailsState(projectName = projectName)
                    )
                }
            }
        }
    }

    fun setProjectTime(projectTime: String) {
        updateTime(ProjectDetailsField.PROJECT_TIME, projectTime)
    }

    val updateTime: (ProjectDetailsField, String) -> Unit = { field, time ->
        _uiState.update { currentState ->
            val successState = currentState as? ProjectDetailsUiState.Success
                ?: return@update currentState

            val update = when (field) {
                ProjectDetailsField.START_TIME -> {
                    ProjectDetailsTimeUpdateCalculator.calculateStartTimeUpdate(
                        StartTimeUpdateParams(
                            start = WorkTimeCalculator.stringToLocalTime(time),
                            dailyWorkTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                                successState.settings.dailyWorkTimeEstimate
                            ),
                            dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                                successState.details.lunchTimeEstimate
                            ),
                            projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                            oldStartTime = WorkTimeCalculator.stringToLocalTime(successState.details.startTime),
                            isNewDayForProject = successState.details.isNewDayForProject()
                        )
                    )
                }

                ProjectDetailsField.END_TIME -> {
                    ProjectDetailsTimeUpdateCalculator.calculateEndTimeUpdate(
                        EndTimeUpdateParams(
                            start = WorkTimeCalculator.stringToLocalTime(successState.details.startTime),
                            end = WorkTimeCalculator.stringToLocalTime(time),
                            lunchStart = WorkTimeCalculator.stringToLocalTime(successState.details.lunchStart),
                            lunchEnd = WorkTimeCalculator.stringToLocalTime(successState.details.lunchEnd),
                            breakStart = WorkTimeCalculator.stringToLocalTime(successState.details.breakStart),
                            breakEnd = WorkTimeCalculator.stringToLocalTime(successState.details.breakEnd),
                            projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                            oldEndTime = WorkTimeCalculator.stringToLocalTime(successState.details.endTime)
                        )
                    )
                }

                ProjectDetailsField.LUNCH_START -> {
                    ProjectDetailsTimeUpdateCalculator.calculateLunchStartUpdate(
                        lunchStart = WorkTimeCalculator.stringToLocalTime(time),
                        dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                            successState.details.lunchTimeEstimate
                        ),
                        projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                        oldLunchStart = WorkTimeCalculator.stringToLocalTime(successState.details.lunchStart),
                        currentLunchEnd = WorkTimeCalculator.stringToLocalTime(successState.details.lunchEnd)
                    )
                }

                ProjectDetailsField.LUNCH_END -> {
                    ProjectDetailsTimeUpdateCalculator.calculateLunchEndUpdate(
                        end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                        lunchEnd = WorkTimeCalculator.stringToLocalTime(time),
                        projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                        oldLunchEnd = WorkTimeCalculator.stringToLocalTime(successState.details.lunchEnd)
                    )
                }

                ProjectDetailsField.LUNCH_TIME -> {
                    ProjectDetailsTimeUpdateCalculator.calculateLunchTimeUpdate(
                        end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                        lunchStart = WorkTimeCalculator.stringToLocalTime(successState.details.lunchStart),
                        dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(time),
                        projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                        oldDailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                            successState.details.lunchTimeEstimate
                        )
                    )
                }

                ProjectDetailsField.BREAK_START -> {
                    ProjectDetailsTimeUpdateCalculator.calculateBreakStartUpdate(
                        end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                        breakStart = WorkTimeCalculator.stringToLocalTime(time),
                        breakEnd = WorkTimeCalculator.stringToLocalTime(successState.details.breakEnd),
                        projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                        oldBreakStart = WorkTimeCalculator.stringToLocalTime(successState.details.breakStart)
                    )
                }

                ProjectDetailsField.BREAK_END -> {
                    ProjectDetailsTimeUpdateCalculator.calculateBreakEndUpdate(
                        end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                        projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                        breakEnd = WorkTimeCalculator.stringToLocalTime(time),
                        oldBreakEnd = WorkTimeCalculator.stringToLocalTime(successState.details.breakEnd)
                    )
                }

                ProjectDetailsField.PROJECT_TIME -> {
                    val nextDetails = successState.details.copy(projectTime = time)
                    if (nextDetails.hasOnlyProjectTime()) {
                        return@update successState.copy(
                            details = ProjectDetailsUiMapper.normalizeProjectDetails(
                                nextDetails,
                                successState.settings
                            ) ?: nextDetails
                        )
                    }

                    ProjectDetailsTimeUpdateCalculator.calculateProjectTimeUpdate(
                        end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                        dailyWorkTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                            successState.settings.dailyWorkTimeEstimate
                        ),
                        projectTime = WorkTimeCalculator.stringToLocalTime(time),
                        oldProjectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime)
                    )
                }
            }

            val nextDetails = when (field) {
                ProjectDetailsField.START_TIME -> successState.details.copy(startTime = time)
                ProjectDetailsField.END_TIME -> successState.details.copy(endTime = time)
                ProjectDetailsField.LUNCH_START -> successState.details.copy(lunchStart = time)
                ProjectDetailsField.LUNCH_END -> successState.details.copy(lunchEnd = time)
                ProjectDetailsField.LUNCH_TIME -> successState.details.copy(lunchTimeEstimate = time)
                ProjectDetailsField.BREAK_START -> successState.details.copy(breakStart = time)
                ProjectDetailsField.BREAK_END -> successState.details.copy(breakEnd = time)
                ProjectDetailsField.PROJECT_TIME -> successState.details.copy(projectTime = time)
            }

            applyUpdateToState(successState.copy(details = nextDetails), update)
        }
    }

    val currentTime: (ProjectDetailsField) -> Unit = { field ->
        updateTime(field, LocalTime.now().format(timeFormatter))
    }

    private fun applyUpdateToState(
        state: ProjectDetailsUiState.Success,
        result: WorkTimeCalculator.TimeUpdateResult
    ): ProjectDetailsUiState.Success {
        var nextState = state.copy(
            details = state.details.copy(
                endTime = result.end ?: state.details.endTime,
                lunchStart = result.lunchStart ?: state.details.lunchStart,
                lunchEnd = result.lunchEnd ?: state.details.lunchEnd,
                breakStart = result.breakStart ?: state.details.breakStart,
                breakEnd = result.breakEnd ?: state.details.breakEnd,
            )
        )

        result.projectTime?.let {
            nextState = nextState.copy(details = nextState.details.copy(projectTime = it))
        }
        return nextState
    }

    val getProjectDetailsState: () -> ProjectDetailsState = {
        val successState = uiState.value as? ProjectDetailsUiState.Success
            ?: error("Project details are unavailable before successful load.")
        successState.details
    }

    val getSettingsEstimatesState: () -> SettingsState = {
        val successState = uiState.value as? ProjectDetailsUiState.Success
            ?: error("Settings are unavailable before successful load.")
        successState.settings
    }

    fun loadProjectDetails(
        date: String,
        projectDetailsArg: ProjectDetailsState? = null
    ) {
        if (date.isBlank()) {
            _isInitialLoadComplete.value = true
            return
        }

        val currentState = _uiState.value
        val showLoading = currentState !is ProjectDetailsUiState.Success && projectDetailsArg == null
        if (showLoading) {
            _isInitialLoadComplete.value = false
        }
        val baseState = (currentState as? ProjectDetailsUiState.Success)
            ?.let { successState ->
                successState.copy(details = successState.details.copy(date = date, projectName = projectDetailsArg?.projectName ?: ""))
            }
            ?: ProjectDetailsUiState.Success(
                details = ProjectDetailsState(date = date, projectName = projectDetailsArg?.projectName ?: "")
            )
        // Cancel any in-flight load so rapid date changes always keep the latest result.
        loadProjectDetailsJob?.cancel()
        loadProjectDetailsJob = viewModelScope.launch {
            loadProjectDetailsInternal(
                baseState = baseState,
                date = date,
                projectName = projectDetailsArg?.projectName ?: "",
                projectDetailsArg = projectDetailsArg,
                showLoading = showLoading
            )
        }
    }

    private suspend fun loadProjectDetailsInternal(
        baseState: ProjectDetailsUiState.Success,
        date: String,
        projectName: String,
        projectDetailsArg: ProjectDetailsState? = null,
        showLoading: Boolean
    ) {
        if (showLoading && _uiState.value !is ProjectDetailsUiState.Success) {
            _uiState.value = ProjectDetailsUiState.Loading
        }

        try {
            val projectDetails = projectDetailsRepository.getProjectDetails(
                date,
                projectName
            ) ?: projectDetailsArg
            val settings = resolveSettings(projectDetails, date)
            val normalizedProjectDetails = ProjectDetailsUiMapper.normalizeProjectDetails(
                projectDetails,
                settings
            )

            _uiState.value = createNextState(
                baseState = baseState,
                date = date,
                projectName = projectName,
                projectDetails = normalizedProjectDetails,
                settings = settings
            )
        } catch (e: IllegalArgumentException) {
            _uiState.value = ProjectDetailsUiState.Error(e.message ?: "Invalid argument")
        } catch (e: IllegalStateException) {
            _uiState.value = ProjectDetailsUiState.Error(e.message ?: "Invalid state")
        } finally {
            _isInitialLoadComplete.value = true
        }
    }

    private suspend fun resolveSettings(
        projectDetails: ProjectDetailsState?,
        date: String
    ): SettingsState? {
        return when {
            projectDetails == null -> settingsRepository.getEffectiveSettingsForDate(date)
            else -> settingsRepository.getSettings()
        }
    }

    private fun createNextState(
        baseState: ProjectDetailsUiState.Success,
        date: String,
        projectName: String,
        projectDetails: ProjectDetailsState?,
        settings: SettingsState?
    ): ProjectDetailsUiState.Success {
        return ProjectDetailsUiMapper.mapEntitiesToUiState(
            baseState.copy(
                details = baseState.details.copy(
                    date = date,
                    projectName = projectName,
                )
            ),
            projectDetails,
            settings
        )
    }

    val clearDetails: () -> Unit = {
        _uiState.update { currentState ->
            val successState = currentState as? ProjectDetailsUiState.Success
                ?: return@update currentState

            successState.copy(
                details = successState.details.copy(
                    startTime = ZERO_TIME,
                    endTime = ZERO_TIME,
                    lunchStart = ZERO_TIME,
                    lunchEnd = ZERO_TIME,
                    breakStart = ZERO_TIME,
                    breakEnd = ZERO_TIME,
                    projectTime = ZERO_TIME,
                    lunchTimeEstimate = successState.settings.dailyLunchTimeEstimate
                )
            )
        }
    }

    fun saveProjectDetails(projectToSave: ProjectDetailsState) {
        viewModelScope.launch {
            projectDetailsRepository.insertProjectDetails(projectToSave)
            val project = projectRepository.getProject(
                date = projectToSave.date,
                projectName = projectToSave.projectName
            )
            projectRepository.insertProject(
                project?.copy(
                    projectTime = projectToSave.projectTime
                ) ?: SingleProjectState()
            )
        }
    }
}
