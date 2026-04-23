package com.akiwiksten.worktime30.feature.projects.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.TIME_FORMAT
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.worktime30.core.WorkTimeCalculator.StartTimeUpdateParams
import com.akiwiksten.worktime30.core.ZERO_TIME
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
@Suppress("TooManyFunctions")
@HiltViewModel
class ProjectDetailsViewModel @Inject constructor(
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectDetailsUiState>(ProjectDetailsUiState.Loading)
    val uiState: StateFlow<ProjectDetailsUiState> = _uiState.asStateFlow()
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

    fun setStartTime(startTime: String) {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldStart = WorkTimeCalculator
                .stringToLocalTime(
                    successState.data.startTime
                )
            val update = WorkTimeCalculator.calculateStartTimeUpdate(
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

    fun currentStartTime() {
        setStartTime(LocalTime.now().format(timeFormatter))
    }

    fun setEndTime(endTime: String) {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldEnd = WorkTimeCalculator.stringToLocalTime(successState.data.endTime)
            val update = WorkTimeCalculator.calculateEndTimeUpdate(
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

    fun currentEndTime() {
        setEndTime(LocalTime.now().format(timeFormatter))
    }

    fun setDailyWorkTime(dailyWorkTime: String) {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldDaily = WorkTimeCalculator
                .stringToLocalTime(
                    successState.data.workStats.dailyWorkTime
                )
            val update = WorkTimeCalculator.calculateDailyWorkTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                dailyWorkTime = WorkTimeCalculator.stringToLocalTime(dailyWorkTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                oldDailyWorkTime = oldDaily,
                isNewDay = successState.data.isNewDay
            )
            applyUpdateToState(
                successState.copy(
                    data = successState.data.copy(
                        workStats = successState.data.workStats.copy(
                            dailyWorkTime = dailyWorkTime
                        )
                    )
                ),
                update
            )
        }
    }

    fun setLunchStart(lunchStart0: String) {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldLunchStart = WorkTimeCalculator.stringToLocalTime(
                successState.data.lunchStart
            )
            val update = WorkTimeCalculator.calculateLunchStartUpdate(
                lunchStart = WorkTimeCalculator.stringToLocalTime(lunchStart0),
                lunchTime = WorkTimeCalculator.stringToLocalTime(successState.data.workStats.lunchTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                oldLunchStart = oldLunchStart,
                currentLunchEnd = WorkTimeCalculator.stringToLocalTime(successState.data.lunchEnd)
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(lunchStart = lunchStart0)), update)
        }
    }

    fun currentLunchStart() {
        setLunchStart(LocalTime.now().format(timeFormatter))
    }

    fun setLunchEnd(lunchEnd: String) {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldLunchEnd = WorkTimeCalculator
                .stringToLocalTime(
                    successState.data.lunchEnd
                )
            val update = WorkTimeCalculator.calculateLunchEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                lunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                oldLunchEnd = oldLunchEnd
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(lunchEnd = lunchEnd)), update)
        }
    }

    fun currentLunchEnd() {
        setLunchEnd(LocalTime.now().format(timeFormatter))
    }

    fun setLunchTime(lunchTime: String) {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldLunchTime = WorkTimeCalculator
                .stringToLocalTime(
                    successState.data.workStats.lunchTime
                )
            val update = WorkTimeCalculator.calculateLunchTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                lunchStart = WorkTimeCalculator.stringToLocalTime(successState.data.lunchStart),
                lunchTime = WorkTimeCalculator.stringToLocalTime(lunchTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                oldLunchTime = oldLunchTime
            )
            applyUpdateToState(
                successState.copy(
                    data = successState.data.copy(
                        workStats = successState.data.workStats.copy(
                            lunchTime = lunchTime
                        )
                    )
                ),
                update
            )
        }
    }

    fun currentLunchTime() {
        setLunchTime(LocalTime.now().format(timeFormatter))
    }

    fun setBreakStart(breakStart0: String) {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldBreakStart = WorkTimeCalculator.stringToLocalTime(
                successState.data.breakStart
            )
            val update = WorkTimeCalculator.calculateBreakStartUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                breakStart = WorkTimeCalculator.stringToLocalTime(breakStart0),
                breakEnd = WorkTimeCalculator.stringToLocalTime(successState.data.breakEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                oldBreakStart = oldBreakStart
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(breakStart = breakStart0)), update)
        }
    }

    fun currentBreakStart() {
        setBreakStart(LocalTime.now().format(timeFormatter))
    }

    fun setBreakEnd(breakEnd0: String) {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldBreakEnd = WorkTimeCalculator
                .stringToLocalTime(
                    successState.data.breakEnd
                )
            val update = WorkTimeCalculator.calculateBreakEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.data.projectTime),
                breakEnd = WorkTimeCalculator.stringToLocalTime(breakEnd0),
                oldBreakEnd = oldBreakEnd
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(breakEnd = breakEnd0)), update)
        }
    }

    fun currentBreakEnd() {
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
            nextState = calculateBalanceUpdatesInState(nextState, oldProjectTime, null, result.calculateBalance)
        }
        return nextState
    }

    private fun calculateBalanceUpdatesInState(
        state: ProjectDetailsUiState.Success,
        oldProjectTime: LocalTime,
        oldFlexTimeToday: String?,
        calculateToday: Boolean
    ): ProjectDetailsUiState.Success {
        var nextState = state
        if (calculateToday) {
            nextState = updateFlexTimeTodayIfNeeded(nextState)
        }

        val balanceAdjustment = calculateBalanceAdjustment(
            state = state,
            nextState = nextState,
            oldProjectTime = oldProjectTime,
            oldFlexTimeToday = oldFlexTimeToday
        )

        nextState = nextState.copy(
            data = nextState.data.copy(
                workStats = nextState.data.workStats.copy(
                    flexTimeTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                        initialTime = nextState.data.workStats.flexTimeTotal,
                        addedTime = balanceAdjustment
                    )
                )
            )
        )

        return nextState
    }

    private fun updateFlexTimeTodayIfNeeded(
        state: ProjectDetailsUiState.Success
    ): ProjectDetailsUiState.Success {
        val totalProjectTimeForDay = WorkTimeCalculator.calculateWorkTimeBalance(
            state.data.otherProjectsTotalTime,
            state.data.projectTime
        )
        return state.copy(
            data = state.data.copy(
                flexTimeToday = WorkTimeCalculator.calculateWorkTimeBalance(
                    initialTime = totalProjectTimeForDay,
                    addedTime = "-" + state.data.workStats.dailyWorkTime
                )
            )
        )
    }

    private fun calculateBalanceAdjustment(
        state: ProjectDetailsUiState.Success,
        nextState: ProjectDetailsUiState.Success,
        oldProjectTime: LocalTime,
        oldFlexTimeToday: String?
    ): String {
        var balanceAdjustment = WorkTimeCalculator.calculateWorkTimeBalance(
            nextState.data.projectTime,
            WorkTimeCalculator.checkIfDoubleMinus("-$oldProjectTime")
        )

        if (!state.data.hasOtherProjects &&
            oldProjectTime == LocalTime.MIDNIGHT &&
            nextState.data.projectTime != ZERO_TIME
        ) {
            balanceAdjustment = WorkTimeCalculator.calculateWorkTimeBalance(
                balanceAdjustment,
                "-" + nextState.data.workStats.dailyWorkTime
            )
        }

        if (oldFlexTimeToday != null) {
            balanceAdjustment = WorkTimeCalculator.calculateWorkTimeBalance(
                nextState.data.flexTimeToday,
                WorkTimeCalculator.checkIfDoubleMinus("-$oldFlexTimeToday")
            )
        }

        return balanceAdjustment
    }

    fun getProjectDetailsState(): ProjectDetailsState {
        return (uiState.value as ProjectDetailsUiState.Success).data
    }

    fun getWorkStatsState(): WorkStatsState {
        return (uiState.value as ProjectDetailsUiState.Success).data.workStats
    }

    fun loadProjectDetails(projectDetailsArg: ProjectDetailsState? = null, workStatsArg: WorkStatsState? = null) {
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

            // Fetch other projects for this date to calculate daily balance correctly
            val allProjectsForDay = projectDetailsRepository.getProjectDetailsByDateRange(date, date)
            val otherProjects = allProjectsForDay.filter { it.projectName != projectName }
            val otherProjectsTotal = otherProjects.fold(ZERO_TIME) { acc, p ->
                WorkTimeCalculator.calculateWorkTimeBalance(acc, p.projectTime)
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
        }
    }

    fun clearDay() {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldProjectTime = successState.data.projectTime
            var nextFlexTimeTotal = successState.data.workStats.flexTimeTotal

            if (oldProjectTime != ZERO_TIME) {
                // Revert project time contribution
                nextFlexTimeTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                    nextFlexTimeTotal,
                    WorkTimeCalculator.checkIfDoubleMinus("-$oldProjectTime")
                )

                // If it was the only project, revert the daily work time subtraction too
                if (!successState.data.hasOtherProjects) {
                    nextFlexTimeTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                        nextFlexTimeTotal,
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
                    workStats = successState.data.workStats.copy(
                        flexTimeTotal = nextFlexTimeTotal
                    )
                )
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun setFlexTimeTotal(flexTimeTotal: String, isValid: Boolean) {
        _uiState.update { currentState ->
            when (currentState) {
                is ProjectDetailsUiState.Success ->
                    currentState.copy(
                        data = currentState.data.copy(
                            workStats = currentState.data.workStats.copy(flexTimeTotal = flexTimeTotal)
                        )
                    )
                else -> currentState
            }
        }
    }

    fun setProjectTime(projectTime: String) {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldProjectTime = WorkTimeCalculator.stringToLocalTime(
                successState.data.projectTime
            )
            val update = WorkTimeCalculator.calculateProjectTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.data.endTime),
                dailyWorkTime = WorkTimeCalculator.stringToLocalTime(successState.data.workStats.dailyWorkTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                oldProjectTime = oldProjectTime
            )
            applyUpdateToState(successState.copy(data = successState.data.copy(projectTime = projectTime)), update)
        }
    }

    fun currentProjectTime() {
        setProjectTime(LocalTime.now().format(timeFormatter))
    }
}

