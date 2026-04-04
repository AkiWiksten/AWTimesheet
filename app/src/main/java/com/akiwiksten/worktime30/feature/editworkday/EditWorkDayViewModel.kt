package com.akiwiksten.worktime30.feature.editworkday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.TIME_FORMAT
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.worktime30.core.WorkTimeCalculator.StartTimeUpdateParams
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkDayOneRowEntity
import com.akiwiksten.worktime30.data.repository.WorkDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for managing the edit/add work day screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class EditWorkDayViewModel @Inject constructor(
    private val workDayRepository: WorkDayRepository
) : ViewModel() {

    private var _date = MutableStateFlow("")
    val date = _date.asStateFlow()

    private var _startTime = MutableStateFlow(ZERO_TIME)
    var startTime = _startTime.asStateFlow()

    private var _endTime = MutableStateFlow(ZERO_TIME)
    var endTime = _endTime.asStateFlow()

    private var _dailyWorkTime = MutableStateFlow(ZERO_TIME)
    val dailyWorkTime = _dailyWorkTime.asStateFlow()

    private var _lunchStart = MutableStateFlow(ZERO_TIME)
    val lunchStart = _lunchStart.asStateFlow()

    private var _lunchEnd = MutableStateFlow(ZERO_TIME)
    var lunchEnd = _lunchEnd.asStateFlow()

    private var _lunchTime = MutableStateFlow(ZERO_TIME)
    var lunchTime = _lunchTime.asStateFlow()

    private var _breakStart = MutableStateFlow(ZERO_TIME)
    var breakStart = _breakStart.asStateFlow()

    private var _breakEnd = MutableStateFlow(ZERO_TIME)
    var breakEnd = _breakEnd.asStateFlow()

    private var _workTimeToday = MutableStateFlow(ZERO_TIME)
    var workTimeToday = _workTimeToday.asStateFlow()

    private var _workTimeTotal = MutableStateFlow(ZERO_TIME)
    var workTimeTotal = _workTimeTotal.asStateFlow()

    private var _balanceToday = MutableStateFlow(ZERO_TIME)
    val balanceToday = _balanceToday.asStateFlow()

    private var _oldBalanceToday = MutableStateFlow(ZERO_TIME)
    val oldBalanceToday = _oldBalanceToday.asStateFlow()

    private var _balanceTotal = MutableStateFlow(ZERO_TIME)
    var balanceTotal = _balanceTotal.asStateFlow()

    private var _isNewDay = MutableStateFlow(true)
    val isNewDay = _isNewDay.asStateFlow()

    fun setDate(date0: String) {
        _date.value = date0
    }

    fun setStartTime(startTime0: String) {
        val oldStart = WorkTimeCalculator.stringToLocalTime(_startTime.value)
        _startTime.value = startTime0
        applyUpdate(WorkTimeCalculator.calculateStartTimeUpdate(
            StartTimeUpdateParams(
                start = WorkTimeCalculator.stringToLocalTime(startTime0),
                dailyWorkTime = WorkTimeCalculator.stringToLocalTime(_dailyWorkTime.value),
                lunchTime = WorkTimeCalculator.stringToLocalTime(_lunchTime.value),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value),
                oldStartTime = oldStart,
                isNewDay = _isNewDay.value
            )
        ))
        _isNewDay.value = false
    }

    fun currentStartTime() {
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT))
        setStartTime(current)
    }

    fun setEndTime(endTime0: String) {
        val oldEnd = WorkTimeCalculator.stringToLocalTime(_endTime.value)
        _endTime.value = endTime0
        applyUpdate(WorkTimeCalculator.calculateEndTimeUpdate(
            EndTimeUpdateParams(
                start = WorkTimeCalculator.stringToLocalTime(_startTime.value),
                end = WorkTimeCalculator.stringToLocalTime(endTime0),
                lunchStart = WorkTimeCalculator.stringToLocalTime(_lunchStart.value),
                lunchEnd = WorkTimeCalculator.stringToLocalTime(_lunchEnd.value),
                breakStart = WorkTimeCalculator.stringToLocalTime(_breakStart.value),
                breakEnd = WorkTimeCalculator.stringToLocalTime(_breakEnd.value),
                workTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value),
                oldEndTime = oldEnd
            )
        ))
    }

    fun currentEndTime() {
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT))
        setEndTime(current)
    }

    fun setDailyWorkTime(dailyWorkTime0: String) {
        val oldDaily = WorkTimeCalculator.stringToLocalTime(_dailyWorkTime.value)
        _dailyWorkTime.value = dailyWorkTime0
        applyUpdate(WorkTimeCalculator.calculateDailyWorkTimeUpdate(
            end = WorkTimeCalculator.stringToLocalTime(_endTime.value),
            dailyWorkTime = WorkTimeCalculator.stringToLocalTime(dailyWorkTime0),
            workTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value),
            oldDailyWorkTime = oldDaily,
            isNewDay = _isNewDay.value
        ))
    }

    fun currentDailyWorkTime() {
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT))
        setDailyWorkTime(current)
    }

    fun setLunchStart(lunchStart0: String) {
        val oldLunchStart = WorkTimeCalculator.stringToLocalTime(_lunchStart.value)
        _lunchStart.value = lunchStart0
        applyUpdate(WorkTimeCalculator.calculateLunchStartUpdate(
            lunchStart = WorkTimeCalculator.stringToLocalTime(lunchStart0),
            lunchTime = WorkTimeCalculator.stringToLocalTime(_lunchTime.value),
            workTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value),
            oldLunchStart = oldLunchStart,
            currentLunchEnd = WorkTimeCalculator.stringToLocalTime(_lunchEnd.value)
        ))
    }

    fun currentLunchStart() {
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT))
        setLunchStart(current)
    }

    fun setLunchEnd(lunchEnd0: String) {
        val oldLunchEnd = WorkTimeCalculator.stringToLocalTime(_lunchEnd.value)
        _lunchEnd.value = lunchEnd0
        applyUpdate(WorkTimeCalculator.calculateLunchEndUpdate(
            end = WorkTimeCalculator.stringToLocalTime(_endTime.value),
            lunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd0),
            workTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value),
            oldLunchEnd = oldLunchEnd
        ))
    }

    fun currentLunchEnd() {
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT))
        setLunchEnd(current)
    }

    fun setLunchTime(lunchTime0: String) {
        val oldLunchTime = WorkTimeCalculator.stringToLocalTime(_lunchTime.value)
        _lunchTime.value = lunchTime0
        applyUpdate(WorkTimeCalculator.calculateLunchTimeUpdate(
            end = WorkTimeCalculator.stringToLocalTime(_endTime.value),
            lunchStart = WorkTimeCalculator.stringToLocalTime(_lunchStart.value),
            lunchTime = WorkTimeCalculator.stringToLocalTime(lunchTime0),
            workTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value),
            oldLunchTime = oldLunchTime
        ))
    }

    fun currentLunchTime() {
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT))
        setLunchTime(current)
    }

    fun setBreakStart(breakStart0: String) {
        val oldBreakStart = WorkTimeCalculator.stringToLocalTime(_breakStart.value)
        _breakStart.value = breakStart0
        applyUpdate(WorkTimeCalculator.calculateBreakStartUpdate(
            end = WorkTimeCalculator.stringToLocalTime(_endTime.value),
            breakStart = WorkTimeCalculator.stringToLocalTime(breakStart0),
            breakEnd = WorkTimeCalculator.stringToLocalTime(_breakEnd.value),
            workTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value),
            oldBreakStart = oldBreakStart
        ))
    }

    fun currentBreakStart() {
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT))
        setBreakStart(current)
    }

    fun setBreakEnd(breakEnd0: String) {
        val oldBreakEnd = WorkTimeCalculator.stringToLocalTime(_breakEnd.value)
        _breakEnd.value = breakEnd0
        applyUpdate(WorkTimeCalculator.calculateBreakEndUpdate(
            end = WorkTimeCalculator.stringToLocalTime(_endTime.value),
            workTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value),
            breakEnd = WorkTimeCalculator.stringToLocalTime(breakEnd0),
            oldBreakEnd = oldBreakEnd
        ))
    }

    fun currentBreakEnd() {
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT))
        setBreakEnd(current)
    }

    fun setWorkTimeToday(workTimeToday0: String) {
        val oldWorkTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value)
        _workTimeToday.value = workTimeToday0
        applyUpdate(WorkTimeCalculator.calculateWorkTimeTodayUpdate(
            end = WorkTimeCalculator.stringToLocalTime(_endTime.value),
            dailyWorkTime = WorkTimeCalculator.stringToLocalTime(_dailyWorkTime.value),
            workTimeToday = WorkTimeCalculator.stringToLocalTime(workTimeToday0),
            oldWorkTimeToday = oldWorkTimeToday
        ))
    }

    fun currentWorkTimeToday() {
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT))
        setWorkTimeToday(current)
    }

    fun setBalanceToday(balanceToday0: String, isValid: Boolean) {
        val oldWorkTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value)
        val oldBalance = _oldBalanceToday.value
        _balanceToday.value = balanceToday0
        if (isValid) {
            calculateBalanceUpdates(oldWorkTimeToday, oldBalance, false)
            _oldBalanceToday.value = balanceToday0
        }
    }

    private fun applyUpdate(result: WorkTimeCalculator.TimeUpdateResult) {
        result.endTime?.let { _endTime.value = it }
        result.lunchStart?.let { _lunchStart.value = it }
        result.lunchEnd?.let { _lunchEnd.value = it }
        result.breakStart?.let { _breakStart.value = it }
        result.breakEnd?.let { _breakEnd.value = it }
        result.workTimeToday?.let {
            val oldWorkTimeToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value)
            _workTimeToday.value = it
            calculateBalanceUpdates(oldWorkTimeToday, null, result.calculateBalance)
        }
    }

    private fun calculateBalanceUpdates(
        oldWorkTimeToday: LocalTime,
        oldBalanceToday: String?,
        calculateToday: Boolean
    ) {
        var balanceToRevert = oldBalanceToday
        if (calculateToday) {
            balanceToRevert = _balanceToday.value
            _balanceToday.value = WorkTimeCalculator.calculateWorkTimeBalance(
                initialTime = _workTimeToday.value,
                addedTime = "-" + _dailyWorkTime.value
            )
        }

        // Adjust total balance
        _balanceTotal.value = WorkTimeCalculator.calculateWorkTimeBalance(
            initialTime = _balanceTotal.value,
            addedTime = WorkTimeCalculator.checkIfDoubleMinus("-$balanceToRevert")
        )
        _balanceTotal.value = WorkTimeCalculator.calculateWorkTimeBalance(
            initialTime = _balanceTotal.value,
            addedTime = _balanceToday.value
        )

        // Adjust total work time
        _workTimeTotal.value = WorkTimeCalculator.calculateWorkTimeBalance(
            initialTime = _workTimeTotal.value,
            addedTime = "-$oldWorkTimeToday"
        )
        _workTimeTotal.value = WorkTimeCalculator.calculateWorkTimeBalance(
            initialTime = _workTimeTotal.value,
            addedTime = _workTimeToday.value
        )
    }

    fun insertWorkDay() {
        viewModelScope.launch {
            if (_date.value.isNotEmpty()) {
                val workDayOneRow = WorkDayOneRowEntity(
                    dailyWorkTime = _dailyWorkTime.value,
                    lunchTime = _lunchTime.value,
                    workTimeTotal = _workTimeTotal.value,
                    balanceTotal = _balanceTotal.value
                )
                workDayRepository.insertWorkDayOneRow(workDayOneRow)
                val workDay = WorkDayEntity(
                    date = _date.value,
                    startTime = _startTime.value,
                    endTime = _endTime.value,
                    lunchStart = _lunchStart.value,
                    lunchEnd = _lunchEnd.value,
                    breakStart = _breakStart.value,
                    breakEnd = _breakEnd.value,
                    workTimeToday = _workTimeToday.value,
                    balanceToday = _balanceToday.value,
                )
                workDayRepository.insertWorkDay(workDay)
            }
        }
    }

    fun loadWorkDay() {
        viewModelScope.launch {
            val workDay = workDayRepository.getWorkDay(_date.value)
            val workDayOneRow = workDayRepository.getWorkDayOneRow()

            if (workDayOneRow != null) {
                _dailyWorkTime.value = workDayOneRow.dailyWorkTime
                _lunchTime.value = workDayOneRow.lunchTime
                _workTimeTotal.value = workDayOneRow.workTimeTotal
                _balanceTotal.value = workDayOneRow.balanceTotal
            } else {
                _dailyWorkTime.value = "07:30"
                _lunchTime.value = "00:30"
                _workTimeTotal.value = ZERO_TIME
                _balanceTotal.value = ZERO_TIME
            }

            if (workDay != null) {
                _startTime.value = workDay.startTime
                _endTime.value = workDay.endTime
                _lunchStart.value = workDay.lunchStart
                _lunchEnd.value = workDay.lunchEnd
                _breakStart.value = workDay.breakStart
                _breakEnd.value = workDay.breakEnd
                _workTimeToday.value = workDay.workTimeToday
                _balanceToday.value = workDay.balanceToday
                _oldBalanceToday.value = _balanceToday.value
                _isNewDay.value = isNewDay(_startTime.value)
            } else {
                _isNewDay.value = true
                _startTime.value = ZERO_TIME
                _endTime.value = ZERO_TIME
                _lunchStart.value = ZERO_TIME
                _lunchEnd.value = ZERO_TIME
                _breakStart.value = ZERO_TIME
                _breakEnd.value = ZERO_TIME
                _workTimeToday.value = ZERO_TIME
                _balanceToday.value = ZERO_TIME
                _oldBalanceToday.value = ZERO_TIME
            }
        }
    }

    fun clearDay() {
        _isNewDay.value = true
        if (!(_workTimeToday.value == ZERO_TIME && _balanceToday.value == ZERO_TIME)) {
            _balanceTotal.value = WorkTimeCalculator.calculateWorkTimeBalance(
                initialTime = _balanceTotal.value,
                addedTime = WorkTimeCalculator.checkIfDoubleMinus(value = "-" + _balanceToday.value)
            )
            val wTTotal = WorkTimeCalculator.stringToLocalTime(_workTimeTotal.value)
            val wTToday = WorkTimeCalculator.stringToLocalTime(_workTimeToday.value)
            _workTimeTotal.value = wTTotal
                .minusHours(wTToday.hour.toLong())
                .minusMinutes(wTToday.minute.toLong()).toString()
        }
        _startTime.value = ZERO_TIME
        _endTime.value = ZERO_TIME
        _lunchStart.value = ZERO_TIME
        _lunchEnd.value = ZERO_TIME
        _breakStart.value = ZERO_TIME
        _breakEnd.value = ZERO_TIME
        _workTimeToday.value = ZERO_TIME
        _balanceToday.value = ZERO_TIME
        _oldBalanceToday.value = ZERO_TIME
    }

    fun isNewDay(oldStartTime: String?): Boolean {
        return oldStartTime == ZERO_TIME &&
            _endTime.value == ZERO_TIME &&
            _lunchEnd.value == ZERO_TIME &&
            _lunchStart.value == ZERO_TIME &&
            _workTimeToday.value == ZERO_TIME &&
            _breakStart.value == ZERO_TIME &&
            _breakEnd.value == ZERO_TIME
    }

    @Suppress("UNUSED_PARAMETER")
    fun setBalanceTotal(balanceTotal0: String, isValid: Boolean) {
        _balanceTotal.value = balanceTotal0
    }

    @Suppress("UNUSED_PARAMETER")
    fun setWorkTimeTotal(workTimeTotal0: String, isValid: Boolean) {
        _workTimeTotal.value = workTimeTotal0
    }

    fun getWorkTimeToday() : String {
        return _workTimeToday.value
    }
}
