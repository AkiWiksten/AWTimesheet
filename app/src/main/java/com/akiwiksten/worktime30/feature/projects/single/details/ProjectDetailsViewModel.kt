package com.akiwiksten.worktime30.feature.projects.single.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.TIME_FORMAT
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.worktime30.core.WorkTimeCalculator.StartTimeUpdateParams
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.WorkdayRepository
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

sealed class ProjectDetailsUiState {
    object Loading : ProjectDetailsUiState()

    data class Success(
        val date: String = "",
        val projectName: String = "",
        val startTime: String = ZERO_TIME,
        val endTime: String = ZERO_TIME,
        val dailyWorkTime: String = ZERO_TIME,
        val lunchStart: String = ZERO_TIME,
        val lunchEnd: String = ZERO_TIME,
        val lunchTime: String = ZERO_TIME,
        val breakStart: String = ZERO_TIME,
        val breakEnd: String = ZERO_TIME,
        val projectTime: String = ZERO_TIME,
        val workTimeTotal: String = ZERO_TIME,
        val balanceToday: String = ZERO_TIME,
        val oldBalanceToday: String = ZERO_TIME,
        val balanceTotal: String = ZERO_TIME,
        val isNewDay: Boolean = true
    ) : ProjectDetailsUiState()

    data class Error(val message: String) : ProjectDetailsUiState()
}

/**
 * ViewModel for managing the project details screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ProjectDetailsViewModel @Inject constructor(
    private val workdayRepository: WorkdayRepository,
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
                is ProjectDetailsUiState.Success -> currentState.copy(date = date)
                else -> ProjectDetailsUiState.Success(date = date)
            }
        }
    }

    fun setProjectName(projectName: String) {
        selectedProjectName.value = projectName
        _uiState.update { currentState ->
            when (currentState) {
                is ProjectDetailsUiState.Success -> currentState.copy(projectName = projectName)
                else -> ProjectDetailsUiState.Success(projectName = projectName)
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
            state.date == date &&
            state.projectName == projectName &&
            !state.isNewDay
    }

    private fun getBaseState(date: String, projectName: String): ProjectDetailsUiState.Success {
        val currentSuccess = uiState.value as? ProjectDetailsUiState.Success
        return (currentSuccess ?: ProjectDetailsUiState.Success()).copy(
            date = date,
            projectName = projectName
        )
    }

    fun setStartTime(startTime: String) {
        _uiState.update { currentState ->
            val oldStart = WorkTimeCalculator
                .stringToLocalTime(
                    (currentState as ProjectDetailsUiState.Success)
                        .startTime
                )
            val update = WorkTimeCalculator.calculateStartTimeUpdate(
                StartTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(startTime),
                    dailyWorkTime = WorkTimeCalculator.stringToLocalTime(currentState.dailyWorkTime),
                    lunchTime = WorkTimeCalculator.stringToLocalTime(currentState.lunchTime),
                    projectTime = WorkTimeCalculator.stringToLocalTime(currentState.projectTime),
                    oldStartTime = oldStart,
                    isNewDay = currentState.isNewDay
                )
            )
            val nextState = currentState.copy(
                startTime = startTime,
                isNewDay = false
            )
            applyUpdateToState(nextState, update)
        }
    }

    fun currentStartTime() {
        setStartTime(LocalTime.now().format(timeFormatter))
    }

    fun setEndTime(endTime: String) {
        _uiState.update { currentState ->
            val oldEnd = WorkTimeCalculator.stringToLocalTime((currentState as ProjectDetailsUiState.Success).endTime)
            val update = WorkTimeCalculator.calculateEndTimeUpdate(
                EndTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(currentState.startTime),
                    end = WorkTimeCalculator.stringToLocalTime(endTime),
                    lunchStart = WorkTimeCalculator.stringToLocalTime(currentState.lunchStart),
                    lunchEnd = WorkTimeCalculator.stringToLocalTime(currentState.lunchEnd),
                    breakStart = WorkTimeCalculator.stringToLocalTime(currentState.breakStart),
                    breakEnd = WorkTimeCalculator.stringToLocalTime(currentState.breakEnd),
                    projectTime = WorkTimeCalculator.stringToLocalTime(currentState.projectTime),
                    oldEndTime = oldEnd
                )
            )
            applyUpdateToState(currentState.copy(endTime = endTime), update)
        }
    }

    fun currentEndTime() {
        setEndTime(LocalTime.now().format(timeFormatter))
    }

    fun setDailyWorkTime(dailyWorkTime: String) {
        _uiState.update { currentState ->
            val oldDaily = WorkTimeCalculator
                .stringToLocalTime(
                    (currentState as ProjectDetailsUiState.Success)
                        .dailyWorkTime
                )
            val update = WorkTimeCalculator.calculateDailyWorkTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                dailyWorkTime = WorkTimeCalculator.stringToLocalTime(dailyWorkTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(currentState.projectTime),
                oldDailyWorkTime = oldDaily,
                isNewDay = currentState.isNewDay
            )
            applyUpdateToState(currentState.copy(dailyWorkTime = dailyWorkTime), update)
        }
    }

    fun currentDailyWorkTime() {
        setDailyWorkTime(LocalTime.now().format(timeFormatter))
    }

    fun setLunchStart(lunchStart0: String) {
        _uiState.update { currentState ->
            val oldLunchStart = WorkTimeCalculator.stringToLocalTime(
                (currentState as ProjectDetailsUiState.Success).lunchStart
            )
            val update = WorkTimeCalculator.calculateLunchStartUpdate(
                lunchStart = WorkTimeCalculator.stringToLocalTime(lunchStart0),
                lunchTime = WorkTimeCalculator.stringToLocalTime(currentState.lunchTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(currentState.projectTime),
                oldLunchStart = oldLunchStart,
                currentLunchEnd = WorkTimeCalculator.stringToLocalTime(currentState.lunchEnd)
            )
            applyUpdateToState(currentState.copy(lunchStart = lunchStart0), update)
        }
    }

    fun currentLunchStart() {
        setLunchStart(LocalTime.now().format(timeFormatter))
    }

    fun setLunchEnd(lunchEnd: String) {
        _uiState.update { currentState ->
            val oldLunchEnd = WorkTimeCalculator
                .stringToLocalTime(
                    (currentState as ProjectDetailsUiState.Success)
                        .lunchEnd
                )
            val update = WorkTimeCalculator.calculateLunchEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                lunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(currentState.projectTime),
                oldLunchEnd = oldLunchEnd
            )
            applyUpdateToState(currentState.copy(lunchEnd = lunchEnd), update)
        }
    }

    fun currentLunchEnd() {
        setLunchEnd(LocalTime.now().format(timeFormatter))
    }

    fun setLunchTime(lunchTime: String) {
        _uiState.update { currentState ->
            val oldLunchTime = WorkTimeCalculator
                .stringToLocalTime(
                    (currentState as ProjectDetailsUiState.Success)
                        .lunchTime
                )
            val update = WorkTimeCalculator.calculateLunchTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                lunchStart = WorkTimeCalculator.stringToLocalTime(currentState.lunchStart),
                lunchTime = WorkTimeCalculator.stringToLocalTime(lunchTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(currentState.projectTime),
                oldLunchTime = oldLunchTime
            )
            applyUpdateToState(currentState.copy(lunchTime = lunchTime), update)
        }
    }

    fun currentLunchTime() {
        setLunchTime(LocalTime.now().format(timeFormatter))
    }

    fun setBreakStart(breakStart0: String) {
        _uiState.update { currentState ->
            val oldBreakStart = WorkTimeCalculator.stringToLocalTime(
                (currentState as ProjectDetailsUiState.Success).breakStart
            )
            val update = WorkTimeCalculator.calculateBreakStartUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                breakStart = WorkTimeCalculator.stringToLocalTime(breakStart0),
                breakEnd = WorkTimeCalculator.stringToLocalTime(currentState.breakEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(currentState.projectTime),
                oldBreakStart = oldBreakStart
            )
            applyUpdateToState(currentState.copy(breakStart = breakStart0), update)
        }
    }

    fun currentBreakStart() {
        setBreakStart(LocalTime.now().format(timeFormatter))
    }

    fun setBreakEnd(breakEnd0: String) {
        _uiState.update { currentState ->
            val oldBreakEnd = WorkTimeCalculator
                .stringToLocalTime(
                    (currentState as ProjectDetailsUiState.Success)
                        .breakEnd
                )
            val update = WorkTimeCalculator.calculateBreakEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(currentState.projectTime),
                breakEnd = WorkTimeCalculator.stringToLocalTime(breakEnd0),
                oldBreakEnd = oldBreakEnd
            )
            applyUpdateToState(currentState.copy(breakEnd = breakEnd0), update)
        }
    }

    fun currentBreakEnd() {
        setBreakEnd(LocalTime.now().format(timeFormatter))
    }

    fun setBalanceToday(balanceToday0: String, isValid: Boolean) {
        if (!isValid) {
            _uiState.update { currentState ->
                when (currentState) {
                    is ProjectDetailsUiState.Success -> currentState.copy(balanceToday = balanceToday0)
                    else -> currentState
                }
            }
            return
        }
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldProjectTime = WorkTimeCalculator.stringToLocalTime(successState.projectTime)
            val oldBalance = successState.oldBalanceToday
            val nextState = successState.copy(balanceToday = balanceToday0)
            val updatedState = calculateBalanceUpdatesInState(nextState, oldProjectTime, oldBalance, false)
            updatedState.copy(oldBalanceToday = balanceToday0)
        }
    }

    private fun applyUpdateToState(
        state: ProjectDetailsUiState.Success,
        result: WorkTimeCalculator.TimeUpdateResult
    ): ProjectDetailsUiState.Success {
        var nextState = state.copy(
            endTime = result.end ?: state.endTime,
            lunchStart = result.lunchStart ?: state.lunchStart,
            lunchEnd = result.lunchEnd ?: state.lunchEnd,
            breakStart = result.breakStart ?: state.breakStart,
            breakEnd = result.breakEnd ?: state.breakEnd,
        )

        result.projectTime?.let {
            val oldProjectTime = WorkTimeCalculator.stringToLocalTime(nextState.projectTime)
            nextState = nextState.copy(projectTime = it)
            nextState = calculateBalanceUpdatesInState(nextState, oldProjectTime, null, result.calculateBalance)
        }
        return nextState
    }

    private fun calculateBalanceUpdatesInState(
        state: ProjectDetailsUiState.Success,
        oldProjectTime: LocalTime,
        oldBalanceToday: String?,
        calculateToday: Boolean
    ): ProjectDetailsUiState.Success {
        var nextState = state
        var balanceToRevert = oldBalanceToday
        if (calculateToday) {
            balanceToRevert = nextState.balanceToday
            nextState = nextState.copy(
                balanceToday = WorkTimeCalculator.calculateWorkTimeBalance(
                    initialTime = nextState.projectTime,
                    addedTime = "-" + nextState.dailyWorkTime
                )
            )
        }

        // Adjust total balance
        nextState = nextState.copy(
            balanceTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                initialTime = nextState.balanceTotal,
                addedTime = WorkTimeCalculator.checkIfDoubleMinus("-$balanceToRevert")
            )
        )
        nextState = nextState.copy(
            balanceTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                initialTime = nextState.balanceTotal,
                addedTime = nextState.balanceToday
            )
        )

        // Adjust total work time
        nextState = nextState.copy(
            workTimeTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                initialTime = nextState.workTimeTotal,
                addedTime = "-$oldProjectTime"
            )
        )
        nextState = nextState.copy(
            workTimeTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                initialTime = nextState.workTimeTotal,
                addedTime = nextState.projectTime
            )
        )
        return nextState
    }

    fun getProjectDetailsEntity(): WorkdayEntity {
        val state = (uiState.value as ProjectDetailsUiState.Success)
        return WorkdayEntity(
            date = state.date,
            projectName = state.projectName,
            startTime = state.startTime,
            endTime = state.endTime,
            lunchStart = state.lunchStart,
            lunchEnd = state.lunchEnd,
            breakStart = state.breakStart,
            breakEnd = state.breakEnd,
            projectTime = state.projectTime,
            balanceToday = state.balanceToday,
        )
    }

    fun getWorkStatsEntity(): WorkStatsEntity {
        val state = (uiState.value as ProjectDetailsUiState.Success)
        return WorkStatsEntity(
            dailyWorkTime = state.dailyWorkTime,
            lunchTime = state.lunchTime,
            workTimeTotal = state.workTimeTotal,
            balanceTotal = state.balanceTotal
        )
    }

    fun loadProjectDetails(projectDetailsArg: WorkdayEntity? = null, workStatsArg: WorkStatsEntity? = null) {
        val currentState = _uiState.value
        val showLoading = currentState !is ProjectDetailsUiState.Success && projectDetailsArg == null
        val baseState = (currentState as? ProjectDetailsUiState.Success) ?: ProjectDetailsUiState.Success()
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
        projectDetailsArg: WorkdayEntity? = null,
        workStatsArg: WorkStatsEntity? = null,
        showLoading: Boolean
    ) {
        if (showLoading && _uiState.value !is ProjectDetailsUiState.Success) {
            _uiState.value = ProjectDetailsUiState.Loading
        }

        try {
            val date = baseState.date.ifEmpty { projectDetailsArg?.date ?: selectedDate.value }
            val projectName = baseState.projectName.ifEmpty {
                projectDetailsArg?.projectName ?: selectedProjectName.value
            }

            if (date.isEmpty()) return

            val projectDetails = projectDetailsArg ?: workdayRepository.getWorkday(date, projectName)
            val workStats = workStatsArg ?: workdayRepository.getWorkStats()

            val nextState = applyEntitiesToState(
                baseState.copy(date = date, projectName = projectName),
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

    private fun applyEntitiesToState(
        baseState: ProjectDetailsUiState.Success,
        projectDetails: WorkdayEntity?,
        workStats: WorkStatsEntity?
    ): ProjectDetailsUiState.Success {
        var state = baseState
        state = if (workStats != null) {
            state.copy(
                dailyWorkTime = workStats.dailyWorkTime.ifEmpty { "07:30" },
                lunchTime = workStats.lunchTime.ifEmpty { ZERO_TIME },
                workTimeTotal = workStats.workTimeTotal.ifEmpty { ZERO_TIME },
                balanceTotal = workStats.balanceTotal.ifEmpty { ZERO_TIME }
            )
        } else {
            state.copy(
                dailyWorkTime = "07:30",
                lunchTime = ZERO_TIME,
                workTimeTotal = ZERO_TIME,
                balanceTotal = ZERO_TIME
            )
        }

        return if (projectDetails != null) {
            state.copy(
                date = projectDetails.date,
                projectName = projectDetails.projectName,
                startTime = projectDetails.startTime.ifEmpty { ZERO_TIME },
                endTime = projectDetails.endTime.ifEmpty { ZERO_TIME },
                lunchStart = projectDetails.lunchStart.ifEmpty { ZERO_TIME },
                lunchEnd = projectDetails.lunchEnd.ifEmpty { ZERO_TIME },
                breakStart = projectDetails.breakStart.ifEmpty { ZERO_TIME },
                breakEnd = projectDetails.breakEnd.ifEmpty { ZERO_TIME },
                projectTime = projectDetails.projectTime.ifEmpty { ZERO_TIME },
                balanceToday = projectDetails.balanceToday.ifEmpty { ZERO_TIME },
                oldBalanceToday = projectDetails.balanceToday.ifEmpty { ZERO_TIME },
                isNewDay = isNewDay(projectDetails)
            )
        } else {
            state.copy(
                isNewDay = true,
                startTime = ZERO_TIME,
                endTime = ZERO_TIME,
                lunchStart = ZERO_TIME,
                lunchEnd = ZERO_TIME,
                breakStart = ZERO_TIME,
                breakEnd = ZERO_TIME,
                projectTime = ZERO_TIME,
                balanceToday = ZERO_TIME,
                oldBalanceToday = ZERO_TIME
            )
        }
    }

    fun clearDay() {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            var nextState = successState.copy(isNewDay = true)
            if (!(successState.projectTime == ZERO_TIME && successState.balanceToday == ZERO_TIME)) {
                nextState = nextState.copy(
                    balanceTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                        initialTime = successState.balanceTotal,
                        addedTime = WorkTimeCalculator.checkIfDoubleMinus(value = "-" + successState.balanceToday)
                    )
                )
                val wTTotal = WorkTimeCalculator.stringToLocalTime(successState.workTimeTotal)
                val wTToday = WorkTimeCalculator.stringToLocalTime(successState.projectTime)
                nextState = nextState.copy(
                    workTimeTotal = wTTotal
                        .minusHours(wTToday.hour.toLong())
                        .minusMinutes(wTToday.minute.toLong()).toString()
                )
            }
            nextState.copy(
                startTime = ZERO_TIME,
                endTime = ZERO_TIME,
                lunchStart = ZERO_TIME,
                lunchEnd = ZERO_TIME,
                breakStart = ZERO_TIME,
                breakEnd = ZERO_TIME,
                projectTime = ZERO_TIME,
                balanceToday = ZERO_TIME,
                oldBalanceToday = ZERO_TIME
            )
        }
    }

    private fun isNewDay(projectDetails: WorkdayEntity): Boolean {
        fun isZero(time: String) = time == ZERO_TIME || time.isEmpty()
        return isZero(projectDetails.startTime) &&
            isZero(projectDetails.endTime) &&
            isZero(projectDetails.lunchEnd) &&
            isZero(projectDetails.lunchStart) &&
            isZero(projectDetails.projectTime) &&
            isZero(projectDetails.breakStart) &&
            isZero(projectDetails.breakEnd)
    }

    @Suppress("UNUSED_PARAMETER")
    fun setBalanceTotal(balanceTotal0: String, isValid: Boolean) {
        _uiState.update { currentState ->
            when (currentState) {
                is ProjectDetailsUiState.Success -> currentState.copy(balanceTotal = balanceTotal0)
                else -> currentState
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun setWorkTimeTotal(workTimeTotal0: String, isValid: Boolean) {
        _uiState.update { currentState ->
            when (currentState) {
                is ProjectDetailsUiState.Success -> currentState.copy(workTimeTotal = workTimeTotal0)
                else -> currentState
            }
        }
    }

    fun setProjectTime(projectTime: String) {
        _uiState.update { currentState ->
            val oldProjectTime = WorkTimeCalculator.stringToLocalTime(
                (currentState as ProjectDetailsUiState.Success).projectTime
            )
            val update = WorkTimeCalculator.calculateProjectTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                dailyWorkTime = WorkTimeCalculator.stringToLocalTime(currentState.dailyWorkTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                oldProjectTime = oldProjectTime
            )
            applyUpdateToState(currentState.copy(projectTime = projectTime), update)
        }
    }

    fun currentProjectTime() {
        setProjectTime(LocalTime.now().format(timeFormatter))
    }
}
