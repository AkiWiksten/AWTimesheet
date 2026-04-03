package com.akiwiksten.worktime30.feature.editworkday

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.TIME_FORMAT
import com.akiwiksten.worktime30.core.TimeGeneratorModel
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.WorkDay
import com.akiwiksten.worktime30.data.database.WorkDayOneRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class EditWorkDayViewModel @Inject constructor() : ViewModel() {
    private val _ctx = MutableStateFlow<Context?>(null)

    private var _date = MutableStateFlow("")
    var date = _date.asStateFlow()

    private var _startTime = MutableStateFlow(ZERO_TIME)
    var startTime = _startTime.asStateFlow()

    private var _endTime = MutableStateFlow(ZERO_TIME)
    var endTime = _endTime.asStateFlow()

    private var _dailyWorkTime = MutableStateFlow(ZERO_TIME)
    var dailyWorkTime = _dailyWorkTime.asStateFlow()

    private var _lunchStart = MutableStateFlow(ZERO_TIME)
    var lunchStart = _lunchStart.asStateFlow()

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
    var balanceToday = _balanceToday.asStateFlow()

    private var _oldBalanceToday = MutableStateFlow(ZERO_TIME)
    var oldBalanceToday = _oldBalanceToday.asStateFlow()

    private var _balanceTotal = MutableStateFlow(ZERO_TIME)
    var balanceTotal = _balanceTotal.asStateFlow()

    private var _isNewDay = MutableStateFlow(true)
    var isNewDay = _isNewDay.asStateFlow()

    private val tgm = TimeGeneratorModel(this)

    fun setCtx(ctx: Context) {
        _ctx.value = ctx
    }

    fun setDate(date0: String) {
        _date.value = date0
    }

    fun setStartTime(startTime0: String) {
        val oldStartTime = _startTime.value
        _startTime.value = startTime0
        tgm.calculateFieldsFromStartTime(oldStartTime = oldStartTime)
        _isNewDay.value = false
    }

    fun updateStartTime(value: String) {
        _startTime.value = value
    }

    fun currentStartTime() {
        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
        val current = LocalDateTime.now().format(formatter)
        val oldValue = _startTime.value
        _startTime.value = current
        tgm.calculateFieldsFromStartTime(oldStartTime = oldValue)
        _isNewDay.value = false
    }

    fun setEndTime(endTime0: String) {
        val oldEndTime = _endTime.value
        _endTime.value = endTime0
        tgm.calculateFieldsFromEndTime(oldEndTime = oldEndTime)
    }

    fun updateEndTime(value: String) {
        _endTime.value = value
    }

    fun currentEndTime() {
        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
        val current = LocalDateTime.now().format(formatter)
        val oldValue = _endTime.value
        _endTime.value = current
        tgm.calculateFieldsFromEndTime(oldEndTime = oldValue)
    }

    fun setDailyWorkTime(dailyWorkTime0: String) {
        val oldDailyWorkTime = _dailyWorkTime.value
        _dailyWorkTime.value = dailyWorkTime0
        tgm.calculateFieldsFromDailyWorkTime(oldDailyWorkTime = oldDailyWorkTime)
    }

    fun updateDailyWorkTime(value: String) {
        _dailyWorkTime.value = value
    }

    fun currentDailyWorkTime() {
        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
        val current = LocalDateTime.now().format(formatter)
        val oldValue = _dailyWorkTime.value
        _dailyWorkTime.value = current
        tgm.calculateFieldsFromDailyWorkTime(oldDailyWorkTime = oldValue)
    }

    fun setLunchStart(lunchStart0: String) {
        val oldLunchStart = _lunchStart.value
        _lunchStart.value = lunchStart0
        tgm.calculateFieldsFromLunchStart(oldLunchStart = oldLunchStart)
    }

    fun updateLunchStart(value: String) {
        _lunchStart.value = value
    }

    fun currentLunchStart() {
        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
        val current = LocalDateTime.now().format(formatter)
        val oldValue = _lunchStart.value
        _lunchStart.value = current
        tgm.calculateFieldsFromLunchStart(oldLunchStart = oldValue)
    }

    fun setLunchEnd(lunchEnd0: String) {
        val oldLunchEnd = _lunchEnd.value
        _lunchEnd.value = lunchEnd0
        tgm.calculateFieldsFromLunchEnd(oldLunchEnd = oldLunchEnd)
    }

    fun updateLunchEnd(value: String) {
        _lunchEnd.value = value
    }

    fun currentLunchEnd() {
        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
        val current = LocalDateTime.now().format(formatter)
        val oldValue = _lunchEnd.value
        _lunchEnd.value = current
        tgm.calculateFieldsFromLunchEnd(oldLunchEnd = oldValue)
    }

    fun setLunchTime(lunchTime0: String) {
        val oldLunchTime = _lunchTime.value
        _lunchTime.value = lunchTime0
        tgm.calculateFieldsFromLunchTime(oldLunchTime = oldLunchTime)
    }

    fun updateLunchTime(value: String) {
        _lunchTime.value = value
    }

    fun currentLunchTime() {
        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
        val current = LocalDateTime.now().format(formatter)
        val oldValue = _lunchTime.value
        _lunchTime.value = current
        tgm.calculateFieldsFromLunchTime(oldLunchTime = oldValue)
    }

    fun setBreakStart(breakStart0: String) {
        val oldBreakStart = _breakStart.value
        _breakStart.value = breakStart0
        tgm.calculateFieldsFromBreakStart(oldBreakStart = oldBreakStart)
    }

    fun updateBreakStart(value: String) {
        _breakStart.value = value
    }

    fun currentBreakStart() {
        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
        val current = LocalDateTime.now().format(formatter)
        val oldValue = _breakStart.value
        _breakStart.value = current
        tgm.calculateFieldsFromBreakStart(oldBreakStart = oldValue)
    }

    fun setBreakEnd(breakEnd0: String) {
        val oldBreakEnd = _breakEnd.value
        _breakEnd.value = breakEnd0
        tgm.calculateFieldsFromBreakEnd(oldBreakEnd = oldBreakEnd)
    }

    fun updateBreakEnd(value: String) {
        _breakEnd.value = value
    }

    fun currentBreakEnd() {
        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
        val current = LocalDateTime.now().format(formatter)
        val oldValue = _breakEnd.value
        _breakEnd.value = current
        tgm.calculateFieldsFromBreakEnd(oldBreakEnd = oldValue)
    }

    fun setWorkTimeToday(workTimeToday0: String) {
        val oldWorkTimeToday = _workTimeToday.value
        _workTimeToday.value = workTimeToday0
        tgm.calculateFieldsFromWorkTimeToday(oldWorkTimeToday = oldWorkTimeToday)
    }

    fun updateWorkTimeToday(value: String) {
        _workTimeToday.value = value
    }

    fun getWorkTimeToday() : String {
        return _workTimeToday.value
    }
    fun currentWorkTimeToday() {
        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
        val current = LocalDateTime.now().format(formatter)
        val oldValue = _workTimeToday.value
        _workTimeToday.value = current
        tgm.calculateFieldsFromWorkTimeToday(oldWorkTimeToday = oldValue)
    }

    @Suppress("UNUSED_PARAMETER")
    fun setWorkTimeTotal(workTimeTotal0: String, isValid: Boolean) {
        _workTimeTotal.value = workTimeTotal0
    }

    fun updateWorkTimeTotal(value: String) {
        _workTimeTotal.value = value
    }

    fun setBalanceToday(balanceToday0: String, isValid: Boolean) {
        _balanceToday.value = balanceToday0
        if (isValid) {
            tgm.calculateFieldsFromBalanceToday(oldBalanceToday = _oldBalanceToday.value)
        }
    }

    fun updateBalanceToday(value: String) {
        _balanceToday.value = value
    }

    @Suppress("UNUSED_PARAMETER")
    fun setBalanceTotal(balanceTotal0: String, isValid: Boolean) {
        _balanceTotal.value = balanceTotal0
    }

    fun updateBalanceTotal(value: String) {
        _balanceTotal.value = value
    }

    fun updateOldBalanceToday(value: String) {
        _oldBalanceToday.value = value
    }

    fun insertWorkDay() {
        viewModelScope.launch {
            if(_date.value.isNotEmpty()) {
                val workDayOneRow = WorkDayOneRow(
                    dailyWorkTime = _dailyWorkTime.value,
                    lunchTime = _lunchTime.value,
                    workTimeTotal = _workTimeTotal.value,
                    balanceTotal = _balanceTotal.value
                )
                AppDatabase.getInstance(_ctx.value!!).workDayOneRowDao()
                    .insertWorkDayOneRow(workDayOneRow)
                val workDay = WorkDay(
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
                AppDatabase.getInstance(_ctx.value!!).workDayDao().insertWorkDay(workDay)
            }
        }
    }

    fun loadWorkDay() {
        viewModelScope.launch {
            val workDay = AppDatabase.getInstance(_ctx.value!!).workDayDao().loadWorkDay(_date.value)
            val workDayOneRow = AppDatabase
                .getInstance(_ctx.value!!)
                .workDayOneRowDao()
                .loadWorkDayOneRow()

            if(workDayOneRow != null) {
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
            }
            else {
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
        if(!(_workTimeToday.value == ZERO_TIME && _balanceToday.value == ZERO_TIME)) {
            _balanceTotal.value = TimeGeneratorModel.calculateWorkTimeBalance(
                initialTime = _balanceTotal.value,
                addedTime = TimeGeneratorModel.checkIfDoubleMinus(value = "-" + _balanceToday.value)
            )
            val wTTotal = TimeGeneratorModel.stringToLocalTime(_workTimeTotal.value)
            val wTToday = TimeGeneratorModel.stringToLocalTime(_workTimeToday.value)
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
}
