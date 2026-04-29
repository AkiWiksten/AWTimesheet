package com.akiwiksten.worktime30.feature.projects.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.TIME_FORMAT
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.ProjectDetailsTimeUpdateCalculator
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator.StartTimeUpdateParams
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.isNewDayForProject
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

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
    private val selectedDate = MutableStateFlow("")
    private val selectedProjectName = MutableStateFlow("")

    private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)

    init {
        observeSelectionChanges()
        observeDateRepository()
    }

    private fun observeDateRepository() {
        viewModelScope.launch {
            dateRepository.selectedDate.collect { date ->
                setDate(date = date)
            }
        }
    }

    fun setDate(date: String) {
        selectedDate.value = date
        _uiState.update { currentState ->
            when (currentState) {
                is ProjectDetailsUiState.Success -> {
                    currentState.copy(
                        data = currentState.data.copy(date = date)
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
        selectedProjectName.value = projectName
        _uiState.update { currentState ->
            when (currentState) {
                is ProjectDetailsUiState.Success -> {
                    currentState.copy(
                        data = currentState.data.copy(projectName = projectName)
                    )
                }

                else -> {
                    ProjectDetailsUiState.Success(
                        ProjectDetailsState(projectName = projectName)
                    )
                }
            }
        }
    }

    private fun observeSelectionChanges() {
        viewModelScope.launch {
            combine(selectedDate, selectedProjectName) { date, projectName -> date to projectName }
                .distinctUntilChanged()
                .collect { (date, projectName) ->
                    if (date.isNotEmpty()) {
                        val currentState = _uiState.value
                        if (isSameSelection(currentState, date, projectName)) {
                            return@collect
                        }

                        loadProjectDetailsInternal(
                            baseState = getBaseState(date, projectName),
                            showLoading = false
                        )
                    }
                }
        }
    }

    private fun isSameSelection(state: ProjectDetailsUiState, date: String, projectName: String): Boolean {
        return state is ProjectDetailsUiState.Success &&
            state.data.date == date &&
            state.data.projectName == projectName &&
            !state.data.isNewDayForProject()
    }

    private fun getBaseState(date: String, projectName: String): ProjectDetailsUiState.Success {
        val currentSuccess = uiState.value as? ProjectDetailsUiState.Success
        return ProjectDetailsUiState.Success(
            data = (currentSuccess?.data ?: ProjectDetailsState()).copy(
                date = date,
                projectName = projectName
            )
        )
    }

    val setStartTime: (String) -> Unit = { startTime ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldStart = WorkTimeCalculator.stringToLocalTime(successState.data.startTime)
            val update = ProjectDetailsTimeUpdateCalculator.calculateStartTimeUpdate(
                StartTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(startTime),
                    dailyWorkTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                        successState.settings.dailyWorkTimeEstimate
                    ),
                    dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                        successState.data.lunchTimeEstimate
                    ),
                    projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                    oldStartTime = oldStart,
                    isNewDay = successState.data.isNewDayForProject()
                )
            )
            val nextState = successState.copy(
                data = successState.data.copy(
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
            val successState = currentState as ProjectDetailsUiState.Success
            val oldEnd = WorkTimeCalculator.stringToLocalTime(successState.data.endTime)
            val update = ProjectDetailsTimeUpdateCalculator.calculateEndTimeUpdate(
                EndTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(successState.data.startTime),
                    end = WorkTimeCalculator.stringToLocalTime(endTime),
                    lunchStart = WorkTimeCalculator.stringToLocalTime(successState.data.lunchStart),
                    lunchEnd = WorkTimeCalculator.stringToLocalTime(successState.data.lunchEnd),
                    breakStart = WorkTimeCalculator.stringToLocalTime(successState.data.breakStart),
                    breakEnd = WorkTimeCalculator.stringToLocalTime(successState.data.breakEnd),
                    projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                    oldEndTime = oldEnd
                )
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(endTime = endTime)), update)
        }
    }

    val currentEndTime: () -> Unit = {
        setEndTime(LocalTime.now().format(timeFormatter))
    }

    val setLunchStart: (String) -> Unit = { lunchStart0 ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldLunchStart = WorkTimeCalculator.stringToLocalTime(successState.data.lunchStart)
            val update = ProjectDetailsTimeUpdateCalculator.calculateLunchStartUpdate(
                lunchStart = WorkTimeCalculator.stringToLocalTime(lunchStart0),
                dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                    successState.data.lunchTimeEstimate
                ),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                oldLunchStart = oldLunchStart,
                currentLunchEnd = WorkTimeCalculator.stringToLocalTime(successState.data.lunchEnd)
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(lunchStart = lunchStart0)), update)
        }
    }

    val currentLunchStart: () -> Unit = {
        setLunchStart(LocalTime.now().format(timeFormatter))
    }

    val setLunchEnd: (String) -> Unit = { lunchEnd ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldLunchEnd = WorkTimeCalculator.stringToLocalTime(successState.data.lunchEnd)
            val update = ProjectDetailsTimeUpdateCalculator.calculateLunchEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                lunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                oldLunchEnd = oldLunchEnd
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(lunchEnd = lunchEnd)), update)
        }
    }

    val currentLunchEnd: () -> Unit = {
        setLunchEnd(LocalTime.now().format(timeFormatter))
    }

    val setLunchTime: (String) -> Unit = { dailyLunchTimeEstimate ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldDailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                successState.data.lunchTimeEstimate
            )
            val update = ProjectDetailsTimeUpdateCalculator.calculateLunchTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                lunchStart = WorkTimeCalculator.stringToLocalTime(successState.data.lunchStart),
                dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(dailyLunchTimeEstimate),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                oldDailyLunchTimeEstimate = oldDailyLunchTimeEstimate
            )
            applyUpdateToState(
                successState.copy(
                    data = successState.data.copy(
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

    val setDailyWorkTime: (String) -> Unit = { dailyWorkTimeEstimate ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val updatedState = successState.copy(
                settings = successState.settings.copy(dailyWorkTimeEstimate = dailyWorkTimeEstimate)
            )
            // flexTimeByDate is now calculated on-the-fly in the UI, not stored in state
            updatedState
        }
    }

    val setBreakStart: (String) -> Unit = { breakStart0 ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldBreakStart = WorkTimeCalculator.stringToLocalTime(successState.data.breakStart)
            val update = ProjectDetailsTimeUpdateCalculator.calculateBreakStartUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                breakStart = WorkTimeCalculator.stringToLocalTime(breakStart0),
                breakEnd = WorkTimeCalculator.stringToLocalTime(successState.data.breakEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                oldBreakStart = oldBreakStart
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(breakStart = breakStart0)), update)
        }
    }

    val currentBreakStart: () -> Unit = {
        setBreakStart(LocalTime.now().format(timeFormatter))
    }

    val setBreakEnd: (String) -> Unit = { breakEnd0 ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldBreakEnd = WorkTimeCalculator.stringToLocalTime(successState.data.breakEnd)
            val update = ProjectDetailsTimeUpdateCalculator.calculateBreakEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                breakEnd = WorkTimeCalculator.stringToLocalTime(breakEnd0),
                oldBreakEnd = oldBreakEnd
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(breakEnd = breakEnd0)), update)
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
            data = state.data.copy(
                endTime = result.end ?: state.data.endTime,
                lunchStart = result.lunchStart ?: state.data.lunchStart,
                lunchEnd = result.lunchEnd ?: state.data.lunchEnd,
                breakStart = result.breakStart ?: state.data.breakStart,
                breakEnd = result.breakEnd ?: state.data.breakEnd,
            )
        )

        result.projectTime?.let {
            nextState = nextState.copy(data = nextState.data.copy(projectTime = it))
        }
        return nextState
    }

    val getProjectDetailsState: () -> ProjectDetailsState = {
        (uiState.value as ProjectDetailsUiState.Success).data
    }

    val getSettingsEstimatesState: () -> SettingsState = {
        (uiState.value as ProjectDetailsUiState.Success).settings
    }

    fun loadProjectDetails(projectDetailsArg: ProjectDetailsState? = null, settingsArg: SettingsState? = null) {
        _isInitialLoadComplete.value = false
        val currentState = _uiState.value
        val showLoading = currentState !is ProjectDetailsUiState.Success && projectDetailsArg == null
        val baseState = (currentState as? ProjectDetailsUiState.Success)
            ?: ProjectDetailsUiState.Success(data = ProjectDetailsState())
        viewModelScope.launch {
            loadProjectDetailsInternal(
                baseState = baseState,
                projectDetailsArg = projectDetailsArg,
                settingsArg = settingsArg,
                showLoading = showLoading
            )
        }
    }

    private suspend fun loadProjectDetailsInternal(
        baseState: ProjectDetailsUiState.Success,
        projectDetailsArg: ProjectDetailsState? = null,
        settingsArg: SettingsState? = null,
        showLoading: Boolean
    ) {
        if (showLoading && _uiState.value !is ProjectDetailsUiState.Success) {
            _uiState.value = ProjectDetailsUiState.Loading
        }

        try {
            val date = baseState.data.date.ifEmpty { projectDetailsArg?.date ?: selectedDate.value }
            val projectName = baseState.data.projectName.ifEmpty {
                projectDetailsArg?.projectName ?: selectedProjectName.value
            }

            if (date.isEmpty()) return

            val projectDetails = projectDetailsArg ?: projectDetailsRepository.getProjectDetails(date, projectName)
            val settings = when {
                settingsArg != null -> settingsArg
                projectDetails == null -> settingsRepository.getEffectiveSettingsForDate(date)
                else -> settingsRepository.getSettings()
            }

            val nextState = ProjectDetailsUiMapper.applyEntitiesToState(
                baseState.copy(
                    data = baseState.data.copy(
                        date = date,
                        projectName = projectName,
                    )
                ),
                projectDetails,
                settings
            )
            _uiState.value = nextState
        } catch (e: IllegalArgumentException) {
            _uiState.value = ProjectDetailsUiState.Error(e.message ?: "Invalid argument")
        } catch (e: IllegalStateException) {
            _uiState.value = ProjectDetailsUiState.Error(e.message ?: "Invalid state")
        } finally {
            _isInitialLoadComplete.value = true
        }
    }

    val clearDay: () -> Unit = {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success

            successState.copy(
                data = successState.data.copy(
                    startTime = ZERO_TIME,
                    endTime = ZERO_TIME,
                    lunchStart = ZERO_TIME,
                    lunchEnd = ZERO_TIME,
                    breakStart = ZERO_TIME,
                    breakEnd = ZERO_TIME,
                    projectTime = ZERO_TIME
                )
            )
        }
    }

    val setProjectTime: (String) -> Unit = { projectTime ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldProjectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime)
            val update = ProjectDetailsTimeUpdateCalculator.calculateProjectTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                dailyWorkTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                    successState.settings.dailyWorkTimeEstimate
                ),
                projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                oldProjectTime = oldProjectTime
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(projectTime = projectTime)), update)
        }
    }

    val currentProjectTime: () -> Unit = {
        setProjectTime(LocalTime.now().format(timeFormatter))
    }
}
