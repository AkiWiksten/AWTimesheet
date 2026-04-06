package com.akiwiksten.worktime30.feature.editworkday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.TIME_FORMAT
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.worktime30.core.WorkTimeCalculator.StartTimeUpdateParams
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.repository.WorkDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EditWorkDayUiState(
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
)

/**
 * ViewModel for managing the edit/add work day screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class EditWorkDayViewModel @Inject constructor(
    private val workDayRepository: WorkDayRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditWorkDayUiState())
    val uiState: StateFlow<EditWorkDayUiState> = _uiState.asStateFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)

    fun setDate(date0: String) {
        _uiState.update { it.copy(date = date0) }
    }

    fun setProjectName(projectName: String) {
        _uiState.update { it.copy(projectName = projectName) }
    }

    fun setStartTime(startTime0: String) {
        _uiState.update { currentState ->
            val oldStart = WorkTimeCalculator.stringToLocalTime(currentState.startTime)
            val update = WorkTimeCalculator.calculateStartTimeUpdate(
                StartTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(startTime0),
                    dailyWorkTime = WorkTimeCalculator.stringToLocalTime(currentState.dailyWorkTime),
                    lunchTime = WorkTimeCalculator.stringToLocalTime(currentState.lunchTime),
                    workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
                    oldStartTime = oldStart,
                    isNewDay = currentState.isNewDay
                )
            )
            val nextState = currentState.copy(
                startTime = startTime0,
                isNewDay = false
            )
            applyUpdateToState(nextState, update)
        }
    }

    fun currentStartTime() {
        setStartTime(LocalTime.now().format(timeFormatter))
    }

    fun setEndTime(endTime0: String) {
        _uiState.update { currentState ->
            val oldEnd = WorkTimeCalculator.stringToLocalTime(currentState.endTime)
            val update = WorkTimeCalculator.calculateEndTimeUpdate(
                EndTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(currentState.startTime),
                    end = WorkTimeCalculator.stringToLocalTime(endTime0),
                    lunchStart = WorkTimeCalculator.stringToLocalTime(currentState.lunchStart),
                    lunchEnd = WorkTimeCalculator.stringToLocalTime(currentState.lunchEnd),
                    breakStart = WorkTimeCalculator.stringToLocalTime(currentState.breakStart),
                    breakEnd = WorkTimeCalculator.stringToLocalTime(currentState.breakEnd),
                    workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
                    oldEndTime = oldEnd
                )
            )
            applyUpdateToState(currentState.copy(endTime = endTime0), update)
        }
    }

    fun currentEndTime() {
        setEndTime(LocalTime.now().format(timeFormatter))
    }

    fun setDailyWorkTime(dailyWorkTime0: String) {
        _uiState.update { currentState ->
            val oldDaily = WorkTimeCalculator.stringToLocalTime(currentState.dailyWorkTime)
            val update = WorkTimeCalculator.calculateDailyWorkTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                dailyWorkTime = WorkTimeCalculator.stringToLocalTime(dailyWorkTime0),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
                oldDailyWorkTime = oldDaily,
                isNewDay = currentState.isNewDay
            )
            applyUpdateToState(currentState.copy(dailyWorkTime = dailyWorkTime0), update)
        }
    }

    fun currentDailyWorkTime() {
        setDailyWorkTime(LocalTime.now().format(timeFormatter))
    }

    fun setLunchStart(lunchStart0: String) {
        _uiState.update { currentState ->
            val oldLunchStart = WorkTimeCalculator.stringToLocalTime(currentState.lunchStart)
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

    fun setLunchEnd(lunchEnd0: String) {
        _uiState.update { currentState ->
            val oldLunchEnd = WorkTimeCalculator.stringToLocalTime(currentState.lunchEnd)
            val update = WorkTimeCalculator.calculateLunchEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                lunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd0),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
                oldLunchEnd = oldLunchEnd
            )
            applyUpdateToState(currentState.copy(lunchEnd = lunchEnd0), update)
        }
    }

    fun currentLunchEnd() {
        setLunchEnd(LocalTime.now().format(timeFormatter))
    }

    fun setLunchTime(lunchTime0: String) {
        _uiState.update { currentState ->
            val oldLunchTime = WorkTimeCalculator.stringToLocalTime(currentState.lunchTime)
            val update = WorkTimeCalculator.calculateLunchTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(currentState.endTime),
                lunchStart = WorkTimeCalculator.stringToLocalTime(currentState.lunchStart),
                lunchTime = WorkTimeCalculator.stringToLocalTime(lunchTime0),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday),
                oldLunchTime = oldLunchTime
            )
            applyUpdateToState(currentState.copy(lunchTime = lunchTime0), update)
        }
    }

    fun currentLunchTime() {
        setLunchTime(LocalTime.now().format(timeFormatter))
    }

    fun setBreakStart(breakStart0: String) {
        _uiState.update { currentState ->
            val oldBreakStart = WorkTimeCalculator.stringToLocalTime(currentState.breakStart)
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
            val oldBreakEnd = WorkTimeCalculator.stringToLocalTime(currentState.breakEnd)
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
            val oldWorkTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday)
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
            _uiState.update { it.copy(balanceToday = balanceToday0) }
            return
        }
        _uiState.update { currentState ->
            val oldWorkTimeToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday)
            val oldBalance = currentState.oldBalanceToday
            val nextState = currentState.copy(balanceToday = balanceToday0)
            val updatedState = calculateBalanceUpdatesInState(nextState, oldWorkTimeToday, oldBalance, false)
            updatedState.copy(oldBalanceToday = balanceToday0)
        }
    }

    private fun applyUpdateToState(
        state: EditWorkDayUiState,
        result: WorkTimeCalculator.TimeUpdateResult
    ): EditWorkDayUiState {
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
        state: EditWorkDayUiState,
        oldWorkTimeToday: LocalTime,
        oldBalanceToday: String?,
        calculateToday: Boolean
    ): EditWorkDayUiState {
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

    fun getWorkDayEntity(): WorkDayEntity {
        val state = _uiState.value
        return WorkDayEntity(
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
        val state = _uiState.value
        return WorkStatsEntity(
            dailyWorkTime = state.dailyWorkTime,
            lunchTime = state.lunchTime,
            workTimeTotal = state.workTimeTotal,
            balanceTotal = state.balanceTotal
        )
    }

    fun loadWorkDay(workDayArg: WorkDayEntity? = null, workStatsArg: WorkStatsEntity? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            val workDay = workDayArg ?: workDayRepository.getWorkDay(state.date, state.projectName)
            val workStats = workStatsArg ?: workDayRepository.getWorkStats()

            _uiState.update { currentState ->
                var nextState = currentState
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
                        lunchTime = "00:30",
                        workTimeTotal = ZERO_TIME,
                        balanceTotal = ZERO_TIME
                    )
                }

                if (workDay != null) {
                    nextState = nextState.copy(
                        startTime = workDay.startTime,
                        endTime = workDay.endTime,
                        lunchStart = workDay.lunchStart,
                        lunchEnd = workDay.lunchEnd,
                        breakStart = workDay.breakStart,
                        breakEnd = workDay.breakEnd,
                        workTimeToday = workDay.workTimeToday,
                        balanceToday = workDay.balanceToday,
                        oldBalanceToday = workDay.balanceToday,
                        isNewDay = isNewDay(workDay)
                    )
                } else {
                    nextState = nextState.copy(
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
                nextState
            }
        }
    }

    fun clearDay() {
        _uiState.update { currentState ->
            var nextState = currentState.copy(isNewDay = true)
            if (!(currentState.workTimeToday == ZERO_TIME && currentState.balanceToday == ZERO_TIME)) {
                nextState = nextState.copy(
                    balanceTotal = WorkTimeCalculator.calculateWorkTimeBalance(
                        initialTime = currentState.balanceTotal,
                        addedTime = WorkTimeCalculator.checkIfDoubleMinus(value = "-" + currentState.balanceToday)
                    )
                )
                val wTTotal = WorkTimeCalculator.stringToLocalTime(currentState.workTimeTotal)
                val wTToday = WorkTimeCalculator.stringToLocalTime(currentState.workTimeToday)
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

    private fun isNewDay(workDay: WorkDayEntity): Boolean {
        return workDay.startTime == ZERO_TIME &&
            workDay.endTime == ZERO_TIME &&
            workDay.lunchEnd == ZERO_TIME &&
            workDay.lunchStart == ZERO_TIME &&
            workDay.workTimeToday == ZERO_TIME &&
            workDay.breakStart == ZERO_TIME &&
            workDay.breakEnd == ZERO_TIME
    }

    @Suppress("UNUSED_PARAMETER")
    fun setBalanceTotal(balanceTotal0: String, isValid: Boolean) {
        _uiState.update { it.copy(balanceTotal = balanceTotal0) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun setWorkTimeTotal(workTimeTotal0: String, isValid: Boolean) {
        _uiState.update { it.copy(workTimeTotal = workTimeTotal0) }
    }
}
