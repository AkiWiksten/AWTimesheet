package com.akiwiksten.worktime30.feature.workday

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

sealed class WorkdayUiState {
    object Loading : WorkdayUiState()

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
        val workTimeToday: String = ZERO_TIME,
        val workTimeTotal: String = ZERO_TIME,
        val balanceToday: String = ZERO_TIME,
        val oldBalanceToday: String = ZERO_TIME,
        val balanceTotal: String = ZERO_TIME,
        val isNewDay: Boolean = true
    ) : WorkdayUiState()

    data class Error(val message: String) : WorkdayUiState()
}

/**
 * ViewModel for managing the workday screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class WorkdayViewModel @Inject constructor(
    private val workdayRepository: WorkdayRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkdayUiState>(WorkdayUiState.Loading)
    val uiState: StateFlow<WorkdayUiState> = _uiState.asStateFlow()
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
                is WorkdayUiState.Success -> currentState.copy(date = date)
                else -> WorkdayUiState.Success(date = date)
            }
        }
    }

    fun setProjectName(projectName: String) {
        selectedProjectName.value = projectName
        _uiState.update { currentState ->
            when (currentState) {
                is WorkdayUiState.Success -> currentState.copy(projectName = projectName)
                else -> WorkdayUiState.Success(projectName = projectName)
            }
        }
    }

    private fun observeSelectionChanges() {
        viewModelScope.launch {
            combine(selectedDate, selectedProjectName) { date, projectName -> date to projectName }
                .distinctUntilChanged()
                .collect { (date, projectName) ->
                    if (date.isNotEmpty()) {
                        loadWorkdayInternal(
                            baseState = (
                                (uiState.value as? WorkdayUiState.Success)
                                    ?: WorkdayUiState.Success()
                                ).copy(
                                date = date,
                                projectName = projectName
                            ),
                            showLoading = false
                        )
                    }
                }
        }
    }

    fun setStartTime(startTime: String) {
        _uiState.update { currentState ->
            val oldStart = WorkTimeCalculator.stringToLocalTime((currentState as WorkdayUiState.Success).startTime)
            val update = WorkTimeCalculator.calculateStartTimeUpdate(
                StartTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(startTime),
                    dailyWorkTime = WorkTimeCalculator.stringToLocalTime(currentState.dailyWorkTime),
                    lunchTime = WorkTimeCalculator.stringToLocalTime(currentState.lunchTime),
                    workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
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
            val oldEnd = WorkTimeCalculator.stringToLocalTime((currentState as WorkdayUiState.Success).endTime)
            val update = WorkTimeCalculator.calculateEndTimeUpdate(
                EndTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(currentState.startTime),
                    end = WorkTimeCalculator.stringToLocalTime(endTime),
                    lunchStart = WorkTimeCalculator.stringToLocalTime(currentState.lunchStart),
                    lunchEnd = WorkTimeCalculator.stringToLocalTime(currentState.lunchEnd),
                    breakStart = WorkTimeCalculator.stringToLocalTime(currentState.breakStart),
                    breakEnd = WorkTimeCalculator.stringToLocalTime(currentState.breakEnd),
                    workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
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
            val oldDaily = WorkTimeCalculator.stringToLocalTime((currentState as WorkdayUiState.Success).dailyWorkTime)
            val update = WorkTimeCalculator.calculateDailyWorkTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                dailyWorkTime = WorkTimeCalculator.stringToLocalTime(dailyWorkTime),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
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
                (currentState as WorkdayUiState.Success).lunchStart
            )
            val update = WorkTimeCalculator.calculateLunchStartUpdate(
                lunchStart = WorkTimeCalculator.stringToLocalTime(lunchStart0),
                lunchTime = WorkTimeCalculator.stringToLocalTime(currentState.lunchTime),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
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
            val oldLunchEnd = WorkTimeCalculator.stringToLocalTime((currentState as WorkdayUiState.Success).lunchEnd)
            val update = WorkTimeCalculator.calculateLunchEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                lunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
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
            val oldLunchTime = WorkTimeCalculator.stringToLocalTime((currentState as WorkdayUiState.Success).lunchTime)
            val update = WorkTimeCalculator.calculateLunchTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                lunchStart = WorkTimeCalculator.stringToLocalTime(currentState.lunchStart),
                lunchTime = WorkTimeCalculator.stringToLocalTime(lunchTime),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
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
                (currentState as WorkdayUiState.Success).breakStart
            )
            val update = WorkTimeCalculator.calculateBreakStartUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                breakStart = WorkTimeCalculator.stringToLocalTime(breakStart0),
                breakEnd = WorkTimeCalculator.stringToLocalTime(currentState.breakEnd),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
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
            val oldBreakEnd = WorkTimeCalculator.stringToLocalTime((currentState as WorkdayUiState.Success).breakEnd)
            val update = WorkTimeCalculator.calculateBreakEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
                breakEnd = WorkTimeCalculator.stringToLocalTime(breakEnd0),
                oldBreakEnd = oldBreakEnd
            )
            applyUpdateToState(currentState.copy(breakEnd = breakEnd0), update)
        }
    }

    fun currentBreakEnd() {
        setBreakEnd(LocalTime.now().format(timeFormatter))
    }

    fun setWorkTimeToday(workTimeToday0: String) {
        _uiState.update { currentState ->
            val oldWorkTimeToday = WorkTimeCalculator.stringToLocalTime(
                (currentState as WorkdayUiState.Success).workTimeToday
            )
            val update = WorkTimeCalculator.calculateWorkTimeTodayUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                dailyWorkTime = WorkTimeCalculator.stringToLocalTime(currentState.dailyWorkTime),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(workTimeToday0),
                oldWorkTimeToday = oldWorkTimeToday
            )
            applyUpdateToState(currentState.copy(workTimeToday = workTimeToday0), update)
        }
    }

    fun currentWorkTimeToday() {
        setWorkTimeToday(LocalTime.now().format(timeFormatter))
    }

    fun setBalanceToday(balanceToday0: String, isValid: Boolean) {
        if (!isValid) {
            _uiState.update { currentState ->
                when (currentState) {
                    is WorkdayUiState.Success -> currentState.copy(balanceToday = balanceToday0)
                    else -> currentState
                }
            }
            return
        }
        _uiState.update { currentState ->
            val successState = currentState as WorkdayUiState.Success
            val oldWorkTimeToday = WorkTimeCalculator.stringToLocalTime(successState.workTimeToday)
            val oldBalance = successState.oldBalanceToday
            val nextState = successState.copy(balanceToday = balanceToday0)
            val updatedState = calculateBalanceUpdatesInState(nextState, oldWorkTimeToday, oldBalance, false)
            updatedState.copy(oldBalanceToday = balanceToday0)
        }
    }

    private fun applyUpdateToState(
        state: WorkdayUiState.Success,
        result: WorkTimeCalculator.TimeUpdateResult
    ): WorkdayUiState.Success {
        var nextState = state.copy(
            endTime = result.endTime ?: state.endTime,
            lunchStart = result.lunchStart ?: state.lunchStart,
            lunchEnd = result.lunchEnd ?: state.lunchEnd,
            breakStart = result.breakStart ?: state.breakStart,
            breakEnd = result.breakEnd ?: state.breakEnd,
        )

        result.workTimeToday?.let {
            val oldWorkTimeToday = WorkTimeCalculator.stringToLocalTime(nextState.workTimeToday)
            nextState = nextState.copy(workTimeToday = it)
            nextState = calculateBalanceUpdatesInState(nextState, oldWorkTimeToday, null, result.calculateBalance)
        }
        return nextState
    }

    private fun calculateBalanceUpdatesInState(
        state: WorkdayUiState.Success,
        oldWorkTimeToday: LocalTime,
        oldBalanceToday: String?,
        calculateToday: Boolean
    ): WorkdayUiState.Success {
        var nextState = state
        var balanceToRevert = oldBalanceToday
        if (calculateToday) {
            balanceToRevert = nextState.balanceToday
            nextState = nextState.copy(
                balanceToday = WorkTimeCalculator.calculateWorkTimeBalance(
                    initialTime = nextState.workTimeToday,
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
                addedTime = "-$oldWorkTimeToday"
            )
        )
        nextState = nextState.copy(
            workTimeTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                initialTime = nextState.workTimeTotal,
                addedTime = nextState.workTimeToday
            )
        )
        return nextState
    }

    fun getWorkdayEntity(): WorkdayEntity {
        val state = (uiState.value as WorkdayUiState.Success)
        return WorkdayEntity(
            date = state.date,
            projectName = state.projectName,
            startTime = state.startTime,
            endTime = state.endTime,
            lunchStart = state.lunchStart,
            lunchEnd = state.lunchEnd,
            breakStart = state.breakStart,
            breakEnd = state.breakEnd,
            workTimeToday = state.workTimeToday,
            balanceToday = state.balanceToday,
        )
    }

    fun getWorkStatsEntity(): WorkStatsEntity {
        val state = (uiState.value as WorkdayUiState.Success)
        return WorkStatsEntity(
            dailyWorkTime = state.dailyWorkTime,
            lunchTime = state.lunchTime,
            workTimeTotal = state.workTimeTotal,
            balanceTotal = state.balanceTotal
        )
    }

    fun loadWorkday(workdayArg: WorkdayEntity? = null, workStatsArg: WorkStatsEntity? = null) {
        val baseState = (_uiState.value as? WorkdayUiState.Success) ?: WorkdayUiState.Success()
        viewModelScope.launch {
            loadWorkdayInternal(
                baseState = baseState,
                workdayArg = workdayArg,
                workStatsArg = workStatsArg,
                showLoading = true
            )
        }
    }

    private suspend fun loadWorkdayInternal(
        baseState: WorkdayUiState.Success,
        workdayArg: WorkdayEntity? = null,
        workStatsArg: WorkStatsEntity? = null,
        showLoading: Boolean
    ) {
        if (showLoading) {
            _uiState.value = WorkdayUiState.Loading
        }

        try {
            val date = baseState.date
            val projectName = baseState.projectName
            val workday = workdayArg ?: workdayRepository.getWorkday(date, projectName)
            val workStats = workStatsArg ?: workdayRepository.getWorkStats()

            var nextState = baseState.copy(date = date, projectName = projectName)
            if (workStats != null) {
                nextState = nextState.copy(
                    dailyWorkTime = workStats.dailyWorkTime,
                    lunchTime = workStats.lunchTime,
                    workTimeTotal = workStats.workTimeTotal,
                    balanceTotal = workStats.balanceTotal
                )
            } else {
                nextState = nextState.copy(
                    dailyWorkTime = "07:30",
                    lunchTime = ZERO_TIME,
                    workTimeTotal = ZERO_TIME,
                    balanceTotal = ZERO_TIME
                )
            }

            nextState = if (workday != null) {
                nextState.copy(
                    startTime = workday.startTime,
                    endTime = workday.endTime,
                    lunchStart = workday.lunchStart,
                    lunchEnd = workday.lunchEnd,
                    breakStart = workday.breakStart,
                    breakEnd = workday.breakEnd,
                    workTimeToday = workday.workTimeToday,
                    balanceToday = workday.balanceToday,
                    oldBalanceToday = workday.balanceToday,
                    isNewDay = isNewDay(workday)
                )
            } else {
                nextState.copy(
                    isNewDay = true,
                    startTime = ZERO_TIME,
                    endTime = ZERO_TIME,
                    lunchStart = ZERO_TIME,
                    lunchEnd = ZERO_TIME,
                    breakStart = ZERO_TIME,
                    breakEnd = ZERO_TIME,
                    workTimeToday = ZERO_TIME,
                    balanceToday = ZERO_TIME,
                    oldBalanceToday = ZERO_TIME
                )
            }

            _uiState.value = nextState
        } catch (e: IllegalArgumentException) {
            _uiState.value = WorkdayUiState.Error(e.message ?: "Invalid argument")
        } catch (e: IllegalStateException) {
            _uiState.value = WorkdayUiState.Error(e.message ?: "Invalid state")
        }
    }

    fun clearDay() {
        _uiState.update { currentState ->
            val successState = currentState as WorkdayUiState.Success
            var nextState = successState.copy(isNewDay = true)
            if (!(successState.workTimeToday == ZERO_TIME && successState.balanceToday == ZERO_TIME)) {
                nextState = nextState.copy(
                    balanceTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                        initialTime = successState.balanceTotal,
                        addedTime = WorkTimeCalculator.checkIfDoubleMinus(value = "-" + successState.balanceToday)
                    )
                )
                val wTTotal = WorkTimeCalculator.stringToLocalTime(successState.workTimeTotal)
                val wTToday = WorkTimeCalculator.stringToLocalTime(successState.workTimeToday)
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
                workTimeToday = ZERO_TIME,
                balanceToday = ZERO_TIME,
                oldBalanceToday = ZERO_TIME
            )
        }
    }

    private fun isNewDay(workday: WorkdayEntity): Boolean {
        return workday.startTime == ZERO_TIME &&
            workday.endTime == ZERO_TIME &&
            workday.lunchEnd == ZERO_TIME &&
            workday.lunchStart == ZERO_TIME &&
            workday.workTimeToday == ZERO_TIME &&
            workday.breakStart == ZERO_TIME &&
            workday.breakEnd == ZERO_TIME
    }

    @Suppress("UNUSED_PARAMETER")
    fun setBalanceTotal(balanceTotal0: String, isValid: Boolean) {
        _uiState.update { currentState ->
            when (currentState) {
                is WorkdayUiState.Success -> currentState.copy(balanceTotal = balanceTotal0)
                else -> currentState
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun setWorkTimeTotal(workTimeTotal0: String, isValid: Boolean) {
        _uiState.update { currentState ->
            when (currentState) {
                is WorkdayUiState.Success -> currentState.copy(workTimeTotal = workTimeTotal0)
                else -> currentState
            }
        }
    }
}
