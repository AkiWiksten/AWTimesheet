package com.akiwiksten.awtimesheet.feature.projectdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.core.TIME_FORMAT
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator.StartTimeUpdateParams
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.hasOnlyProjectTime
import com.akiwiksten.awtimesheet.domain.model.isNewDayForProject
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.feature.projectdetails.calculator.ProjectDetailsTimeUpdateCalculator
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

    fun observeDateRepository(projectDetails: ProjectDetailsState) {
        dateObserverJob?.cancel()
        dateObserverJob = viewModelScope.launch {
            dateRepository.selectedDate.collectLatest { date ->
                val currentDetails = (uiState.value as? ProjectDetailsUiState.Success)?.details
                    ?: projectDetails
                loadProjectDetails(
                    date = date,
                    projectName = currentDetails.projectName,
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

    val setStartTime: (String) -> Unit = { startTime ->
        _uiState.update { currentState ->
            val successState = currentState as? ProjectDetailsUiState.Success
                ?: return@update currentState
            val oldStart = WorkTimeCalculator.stringToLocalTime(successState.details.startTime)
            val update = ProjectDetailsTimeUpdateCalculator.calculateStartTimeUpdate(
                StartTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(startTime),
                    dailyWorkTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                        successState.settings.dailyWorkTimeEstimate
                    ),
                    dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                        successState.details.lunchTimeEstimate
                    ),
                    projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                    oldStartTime = oldStart,
                    isNewDayForProject = successState.details.isNewDayForProject()
                )
            )
            val nextState = successState.copy(
                details = successState.details.copy(
                    startTime = startTime
                )
            )
            applyUpdateToState(nextState, update)
        }
    }

    val currentStartTime: () -> Unit = {
        setStartTime(LocalTime.now().format(timeFormatter))
    }

    val setEndTime: (String) -> Unit = { endTime ->
        _uiState.update { currentState ->
            val successState = currentState as? ProjectDetailsUiState.Success
                ?: return@update currentState
            val oldEnd = WorkTimeCalculator.stringToLocalTime(successState.details.endTime)
            val update = ProjectDetailsTimeUpdateCalculator.calculateEndTimeUpdate(
                EndTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(successState.details.startTime),
                    end = WorkTimeCalculator.stringToLocalTime(endTime),
                    lunchStart = WorkTimeCalculator.stringToLocalTime(successState.details.lunchStart),
                    lunchEnd = WorkTimeCalculator.stringToLocalTime(successState.details.lunchEnd),
                    breakStart = WorkTimeCalculator.stringToLocalTime(successState.details.breakStart),
                    breakEnd = WorkTimeCalculator.stringToLocalTime(successState.details.breakEnd),
                    projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                    oldEndTime = oldEnd
                )
            )
            applyUpdateToState(successState.copy(details = successState.details.copy(endTime = endTime)), update)
        }
    }

    val currentEndTime: () -> Unit = {
        setEndTime(LocalTime.now().format(timeFormatter))
    }

    val setLunchStart: (String) -> Unit = { lunchStart0 ->
        _uiState.update { currentState ->
            val successState = currentState as? ProjectDetailsUiState.Success
                ?: return@update currentState
            val oldLunchStart = WorkTimeCalculator.stringToLocalTime(successState.details.lunchStart)
            val update = ProjectDetailsTimeUpdateCalculator.calculateLunchStartUpdate(
                lunchStart = WorkTimeCalculator.stringToLocalTime(lunchStart0),
                dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                    successState.details.lunchTimeEstimate
                ),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                oldLunchStart = oldLunchStart,
                currentLunchEnd = WorkTimeCalculator.stringToLocalTime(successState.details.lunchEnd)
            )
            applyUpdateToState(successState.copy(details = successState.details.copy(lunchStart = lunchStart0)), update)
        }
    }

    val currentLunchStart: () -> Unit = {
        setLunchStart(LocalTime.now().format(timeFormatter))
    }

    val setLunchEnd: (String) -> Unit = { lunchEnd ->
        _uiState.update { currentState ->
            val successState = currentState as? ProjectDetailsUiState.Success
                ?: return@update currentState
            val oldLunchEnd = WorkTimeCalculator.stringToLocalTime(successState.details.lunchEnd)
            val update = ProjectDetailsTimeUpdateCalculator.calculateLunchEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                lunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                oldLunchEnd = oldLunchEnd
            )
            applyUpdateToState(successState.copy(details = successState.details.copy(lunchEnd = lunchEnd)), update)
        }
    }

    val currentLunchEnd: () -> Unit = {
        setLunchEnd(LocalTime.now().format(timeFormatter))
    }

    val setLunchTime: (String) -> Unit = { dailyLunchTimeEstimate ->
        _uiState.update { currentState ->
            val successState = currentState as? ProjectDetailsUiState.Success
                ?: return@update currentState
            val oldDailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                successState.details.lunchTimeEstimate
            )
            val update = ProjectDetailsTimeUpdateCalculator.calculateLunchTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                lunchStart = WorkTimeCalculator.stringToLocalTime(successState.details.lunchStart),
                dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(dailyLunchTimeEstimate),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                oldDailyLunchTimeEstimate = oldDailyLunchTimeEstimate
            )
            applyUpdateToState(
                successState.copy(
                    details = successState.details.copy(
                        lunchTimeEstimate = dailyLunchTimeEstimate
                    )
                ),
                update
            )
        }
    }

    val currentLunchTime: () -> Unit = {
        setLunchTime(LocalTime.now().format(timeFormatter))
    }

    val setBreakStart: (String) -> Unit = { breakStart0 ->
        _uiState.update { currentState ->
            val successState = currentState as? ProjectDetailsUiState.Success
                ?: return@update currentState
            val oldBreakStart = WorkTimeCalculator.stringToLocalTime(successState.details.breakStart)
            val update = ProjectDetailsTimeUpdateCalculator.calculateBreakStartUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                breakStart = WorkTimeCalculator.stringToLocalTime(breakStart0),
                breakEnd = WorkTimeCalculator.stringToLocalTime(successState.details.breakEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                oldBreakStart = oldBreakStart
            )
            applyUpdateToState(successState.copy(details = successState.details.copy(breakStart = breakStart0)), update)
        }
    }

    val currentBreakStart: () -> Unit = {
        setBreakStart(LocalTime.now().format(timeFormatter))
    }

    val setBreakEnd: (String) -> Unit = { breakEnd0 ->
        _uiState.update { currentState ->
            val successState = currentState as? ProjectDetailsUiState.Success
                ?: return@update currentState
            val oldBreakEnd = WorkTimeCalculator.stringToLocalTime(successState.details.breakEnd)
            val update = ProjectDetailsTimeUpdateCalculator.calculateBreakEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                breakEnd = WorkTimeCalculator.stringToLocalTime(breakEnd0),
                oldBreakEnd = oldBreakEnd
            )
            applyUpdateToState(successState.copy(details = successState.details.copy(breakEnd = breakEnd0)), update)
        }
    }

    val currentBreakEnd: () -> Unit = {
        setBreakEnd(LocalTime.now().format(timeFormatter))
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
        projectName: String,
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
                successState.copy(details = successState.details.copy(date = date, projectName = projectName))
            }
            ?: ProjectDetailsUiState.Success(
                details = ProjectDetailsState(date = date, projectName = projectName)
            )
        // Cancel any in-flight load so rapid date changes always keep the latest result.
        loadProjectDetailsJob?.cancel()
        loadProjectDetailsJob = viewModelScope.launch {
            loadProjectDetailsInternal(
                baseState = baseState,
                date = date,
                projectName = projectName,
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
            val loadedProjectDetails = resolveLoadedProjectDetails(
                date = date,
                projectName = projectName,
                projectDetailsArg = projectDetailsArg
            )
            val settings = resolveSettings(loadedProjectDetails, date)
            val normalizedProjectDetails = normalizeProjectDetails(loadedProjectDetails, settings)

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

    private suspend fun resolveLoadedProjectDetails(
        date: String,
        projectName: String,
        projectDetailsArg: ProjectDetailsState?
    ): ProjectDetailsState? {
        if (projectDetailsArg != null && !projectDetailsArg.hasOnlyProjectTime()) {
            return projectDetailsArg
        }

        return projectDetailsRepository.getProjectDetails(
            date,
            projectName
        ) ?: projectDetailsArg
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

    private fun normalizeProjectDetails(
        projectDetails: ProjectDetailsState?,
        settings: SettingsState?
    ): ProjectDetailsState? {
        if (projectDetails == null || !projectDetails.hasOnlyProjectTime()) {
            return projectDetails
        }

        val update = ProjectDetailsTimeUpdateCalculator.calculateProjectTimeUpdate(
            projectTime = WorkTimeCalculator.stringToLocalTime(projectDetails.projectTime),
            dailyLunchTimeEstimate = WorkTimeCalculator
                .stringToLocalTime(settings?.dailyLunchTimeEstimate ?: ZERO_TIME)
        )
        return projectDetails.copy(
            startTime = ZERO_TIME,
            endTime = update.end?.format(timeFormatter) ?: ZERO_TIME,
            lunchStart = update.lunchStart?.format(timeFormatter) ?: ZERO_TIME,
            lunchEnd = update.lunchEnd?.format(timeFormatter) ?: ZERO_TIME,
            breakStart = update.breakStart?.format(timeFormatter) ?: ZERO_TIME,
            breakEnd = update.breakEnd?.format(timeFormatter) ?: ZERO_TIME
        )
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

    val setProjectTime: (String) -> Unit = { projectTime ->
        _uiState.update { currentState ->
            val successState = currentState as? ProjectDetailsUiState.Success
                ?: return@update currentState
            val nextDetails = successState.details.copy(projectTime = projectTime)

            if (nextDetails.hasOnlyProjectTime()) {
                return@update successState.copy(
                    details = normalizeProjectDetails(nextDetails, successState.settings) ?: nextDetails
                )
            }

            val oldProjectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime)
            val update = ProjectDetailsTimeUpdateCalculator.calculateProjectTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                dailyWorkTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                    successState.settings.dailyWorkTimeEstimate
                ),
                projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                oldProjectTime = oldProjectTime
            )
            applyUpdateToState(
                successState.copy(
                    details = nextDetails
                ),
                update
            )
        }
    }

    val currentProjectTime: () -> Unit = {
        setProjectTime(LocalTime.now().format(timeFormatter))
    }
}
