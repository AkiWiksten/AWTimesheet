package com.akiwiksten.worktime30.feature.projects.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.TIME_FORMAT
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.ProjectDetailsTimeUpdateCalculator
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator.StartTimeUpdateParams
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
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
            !state.data.isNewDay
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
                    dailyWorkTime = WorkTimeCalculator.stringToLocalTime(successState.data.workStats.dailyWorkTime),
                    lunchTime = WorkTimeCalculator.stringToLocalTime(successState.data.workStats.lunchTime),
                    projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                    oldStartTime = oldStart,
                    isNewDay = successState.data.isNewDay
                )
            )
            val nextState = successState.copy(
                data = successState.data.copy(
                    startTime = startTime,
                    isNewDay = false
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
                lunchTime = WorkTimeCalculator.stringToLocalTime(successState.data.workStats.lunchTime),
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

    val setLunchTime: (String) -> Unit = { lunchTime ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldLunchTime = WorkTimeCalculator.stringToLocalTime(successState.data.workStats.lunchTime)
            val update = ProjectDetailsTimeUpdateCalculator.calculateLunchTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                lunchStart = WorkTimeCalculator.stringToLocalTime(successState.data.lunchStart),
                lunchTime = WorkTimeCalculator.stringToLocalTime(lunchTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                oldLunchTime = oldLunchTime
            )
            applyUpdateToState(
                successState.copy(
                    data = successState.data.copy(
                        workStats = successState.data.workStats.copy(lunchTime = lunchTime)
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
            val oldProjectTime = WorkTimeCalculator.stringToLocalTime(nextState.data.projectTime)
            nextState = nextState.copy(data = nextState.data.copy(projectTime = it))
            nextState = calculateFlexTimeUpdatesInState(
                nextState,
                oldProjectTime,
                result.shouldRecalculateFlexTime
            )
        }
        return nextState
    }

    private fun calculateFlexTimeUpdatesInState(
        state: ProjectDetailsUiState.Success,
        oldProjectTime: LocalTime,
        calculateToday: Boolean
    ): ProjectDetailsUiState.Success {
        var nextState = state
        if (calculateToday) {
            nextState = updateFlexTimeTodayIfNeeded(nextState)
        }

        val flexTimeAdjustment = calculateFlexTimeAdjustment(
            state = state,
            nextState = nextState,
            oldProjectTime = oldProjectTime
        )

        nextState = nextState.copy(
            data = nextState.data.copy(
                workStats = nextState.data.workStats.copy(
                    initialFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
                        initialTime = nextState.data.workStats.initialFlexTimeTotal,
                        addedTime = flexTimeAdjustment
                    )
                )
            )
        )

        return nextState
    }

    private fun updateFlexTimeTodayIfNeeded(
        state: ProjectDetailsUiState.Success
    ): ProjectDetailsUiState.Success {
        val totalProjectTimeForDay = WorkTimeCalculator.calculateFlexTime(
            state.data.otherProjectsTotalTime,
            state.data.projectTime
        )
        return state.copy(
            data = state.data.copy(
                flexTimeToday = WorkTimeCalculator.calculateFlexTime(
                    initialTime = totalProjectTimeForDay,
                    addedTime = "-" + state.data.workStats.dailyWorkTime
                )
            )
        )
    }

    private fun calculateFlexTimeAdjustment(
        state: ProjectDetailsUiState.Success,
        nextState: ProjectDetailsUiState.Success,
        oldProjectTime: LocalTime
    ): String {
        var flexTimeAdjustment = WorkTimeCalculator.calculateFlexTime(
            nextState.data.projectTime,
            WorkTimeCalculator.checkIfDoubleMinus("-$oldProjectTime")
        )

        if (!state.data.hasOtherProjects &&
            oldProjectTime == LocalTime.MIDNIGHT &&
            nextState.data.projectTime != ZERO_TIME
        ) {
            flexTimeAdjustment = WorkTimeCalculator.calculateFlexTime(
                flexTimeAdjustment,
                "-" + nextState.data.workStats.dailyWorkTime
            )
        }

        return flexTimeAdjustment
    }

    val getProjectDetailsState: () -> ProjectDetailsState = {
        (uiState.value as ProjectDetailsUiState.Success).data
    }

    val getWorkStatsState: () -> WorkStatsState = {
        (uiState.value as ProjectDetailsUiState.Success).data.workStats
    }

    fun loadProjectDetails(projectDetailsArg: ProjectDetailsState? = null, workStatsArg: WorkStatsState? = null) {
        _isInitialLoadComplete.value = false
        val currentState = _uiState.value
        val showLoading = currentState !is ProjectDetailsUiState.Success && projectDetailsArg == null
        val baseState = (currentState as? ProjectDetailsUiState.Success)
            ?: ProjectDetailsUiState.Success(data = ProjectDetailsState())
        viewModelScope.launch {
            loadProjectDetailsInternal(
                baseState = baseState,
                projectDetailsArg = projectDetailsArg,
                workStatsArg = workStatsArg,
                showLoading = showLoading
            )
        }
    }

    private suspend fun loadProjectDetailsInternal(
        baseState: ProjectDetailsUiState.Success,
        projectDetailsArg: ProjectDetailsState? = null,
        workStatsArg: WorkStatsState? = null,
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
            val workStats = workStatsArg ?: projectDetailsRepository.getWorkStats()

            // Fetch other projects for this date to calculate daily flex time correctly.
            val allProjectsForDay = projectDetailsRepository.getProjectDetailsByDateRange(date, date)
            val otherProjects = allProjectsForDay.filter { it.projectName != projectName }
            val otherProjectsTotal = otherProjects.fold(ZERO_TIME) { acc, p ->
                WorkTimeCalculator.calculateFlexTime(acc, p.projectTime)
            }

            val nextState = ProjectDetailsUiMapper.applyEntitiesToState(
                baseState.copy(
                    data = baseState.data.copy(
                        date = date,
                        projectName = projectName,
                        otherProjectsTotalTime = otherProjectsTotal,
                        hasOtherProjects = otherProjects.isNotEmpty()
                    )
                ),
                projectDetails,
                workStats
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
            val oldProjectTime = successState.data.projectTime
            var nextInitialFlexTimeTotal = successState.data.workStats.initialFlexTimeTotal

            if (oldProjectTime != ZERO_TIME) {
                nextInitialFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
                    nextInitialFlexTimeTotal,
                    WorkTimeCalculator.checkIfDoubleMinus("-$oldProjectTime")
                )

                if (!successState.data.hasOtherProjects) {
                    nextInitialFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
                        nextInitialFlexTimeTotal,
                        successState.data.workStats.dailyWorkTime
                    )
                }
            }

            successState.copy(
                data = successState.data.copy(
                    isNewDay = true,
                    startTime = ZERO_TIME,
                    endTime = ZERO_TIME,
                    lunchStart = ZERO_TIME,
                    lunchEnd = ZERO_TIME,
                    breakStart = ZERO_TIME,
                    breakEnd = ZERO_TIME,
                    projectTime = ZERO_TIME,
                    flexTimeToday = ZERO_TIME,
                    oldFlexTimeToday = ZERO_TIME,
                    workStats = successState.data.workStats.copy(initialFlexTimeTotal = nextInitialFlexTimeTotal)
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
                dailyWorkTime = WorkTimeCalculator.stringToLocalTime(successState.data.workStats.dailyWorkTime),
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
