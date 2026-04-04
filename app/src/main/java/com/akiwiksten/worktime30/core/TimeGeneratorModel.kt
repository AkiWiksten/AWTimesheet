package com.akiwiksten.worktime30.core

import com.akiwiksten.worktime30.feature.editworkday.EditWorkDayViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Suppress("TooManyFunctions")
class TimeGeneratorModel(private val editWorkDayViewModel: EditWorkDayViewModel) {

    companion object {
        fun parseDate(workDay: String): String {
            val formattedDate = LocalDate.parse(workDay)
            val date = DateTimeFormatter.ofPattern("dd").format(formattedDate)
            return date
        }

        fun stringToLocalTime(time: String): LocalTime {
            val outputFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
            val localTime = LocalTime.parse(time, outputFormatter)
            return localTime
        }

        private fun LocalTime.subtract(time: LocalTime): LocalTime {
            return minusHours(time.hour.toLong()).minusMinutes(time.minute.toLong())
        }

        private fun LocalTime.add(time: LocalTime): LocalTime {
            return plusHours(time.hour.toLong()).plusMinutes(time.minute.toLong())
        }

        fun calculateWorkTimeBalance(initialTime: String, addedTime: String): String {
            return if (initialTime.substring(0, 1) == "-") {
                val positiveInitialTime = initialTime.substring(1)
                if (addedTime.substring(0, 1) == "-") {
                    val positiveAddedTime = addedTime.substring(1)
                    calculateTotalMinutes(
                        initialTime = positiveInitialTime,
                        addedTime = positiveAddedTime,
                        isInitialTimeNegative = true,
                        isAddedTimeNegative = true
                    )
                } else {
                    calculateTotalMinutes(
                        initialTime = positiveInitialTime,
                        addedTime = addedTime,
                        isInitialTimeNegative = true,
                        isAddedTimeNegative = false
                    )
                }
            } else {
                if (addedTime.substring(0, 1) == "-") {
                    val positiveAddedTime = addedTime.substring(1)
                    calculateTotalMinutes(
                        initialTime = initialTime,
                        addedTime = positiveAddedTime,
                        isInitialTimeNegative = false,
                        isAddedTimeNegative = true
                    )
                } else {
                    calculateTotalMinutes(
                        initialTime = initialTime,
                        addedTime = addedTime,
                        isInitialTimeNegative = false,
                        isAddedTimeNegative = false
                    )
                }
            }
        }

        fun calculateTotalMinutes(
            initialTime: String,
            addedTime: String,
            isInitialTimeNegative: Boolean,
            isAddedTimeNegative: Boolean
        ): String {
            val (hour0, minute0) = initialTime.split(':')
            val initialTotalMinutes = hour0.toInt() * MINUTES_60 + minute0.toInt()

            val (hour1, minute1) = addedTime.split(':')
            val addedTotalMinutes = hour1.toInt() * MINUTES_60 + minute1.toInt()
            val totalMinutes = if (isInitialTimeNegative) {
                if (isAddedTimeNegative) {
                    -initialTotalMinutes - addedTotalMinutes
                } else {
                    -initialTotalMinutes + addedTotalMinutes
                }
            } else {
                if (isAddedTimeNegative) {
                    initialTotalMinutes - addedTotalMinutes
                } else {
                    initialTotalMinutes + addedTotalMinutes
                }
            }
            val hours = abs(totalMinutes) / MINUTES_60
            val minutes = abs(totalMinutes) % MINUTES_60
            return if (totalMinutes < 0) {
                "-" + convertDate(hours) + ":" + convertDate(minutes)
            } else {
                convertDate(hours) + ":" + convertDate(minutes)
            }
        }

        @Suppress("MagicNumber")
        private fun convertDate(input: Int): String {
            return if (input >= 10) {
                input.toString()
            } else {
                "0$input"
            }
        }

        private fun calculateHalfTime(start: LocalTime, dailyWorkTime: LocalTime): LocalTime {
            val totalMinutes = (dailyWorkTime.hour * MINUTES_60 + dailyWorkTime.minute) / 2
            val hours = totalMinutes / MINUTES_60
            val minutes = totalMinutes % MINUTES_60
            return start.plusHours(hours.toLong()).plusMinutes(minutes.toLong())
        }

        fun checkIfDoubleMinus(value: String): String {
            if (value.substring(0, 2) == "--") {
                return value.substring(2)
            }
            return value
        }
    }

    private fun calculateCustomTimeValues(
        oldWorkTimeToday: LocalTime,
        oldBalanceToday: String?,
        calculateBalanceToday: Boolean = true
    ) {
        var oldBalanceD = oldBalanceToday
        if (calculateBalanceToday) {
            oldBalanceD = editWorkDayViewModel.balanceToday.value
            editWorkDayViewModel.updateBalanceToday(
                calculateWorkTimeBalance(
                    initialTime = editWorkDayViewModel.workTimeToday.value,
                    addedTime = "-" + editWorkDayViewModel.dailyWorkTime.value
                )
            )
        }
        editWorkDayViewModel.updateBalanceTotal(
            calculateWorkTimeBalance(
                initialTime = editWorkDayViewModel.balanceTotal.value,
                addedTime = checkIfDoubleMinus("-$oldBalanceD")
            )
        )
        editWorkDayViewModel.updateBalanceTotal(
            calculateWorkTimeBalance(
                initialTime = editWorkDayViewModel.balanceTotal.value,
                addedTime = editWorkDayViewModel.balanceToday.value
            )
        )
        editWorkDayViewModel.updateWorkTimeTotal(
            calculateWorkTimeBalance(
                initialTime = editWorkDayViewModel.workTimeTotal.value,
                addedTime = "-$oldWorkTimeToday"
            )
        )
        editWorkDayViewModel.updateWorkTimeTotal(
            calculateWorkTimeBalance(
                initialTime = editWorkDayViewModel.workTimeTotal.value,
                addedTime = editWorkDayViewModel.workTimeToday.value
            )
        )
    }

    fun calculateFieldsFromStartTime(oldStartTime: String) {
        val start = stringToLocalTime(editWorkDayViewModel.startTime.value)
        val dailyWorkTime = stringToLocalTime(editWorkDayViewModel.dailyWorkTime.value)
        var workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)
        val lunchTime = stringToLocalTime(editWorkDayViewModel.lunchTime.value)

        if (editWorkDayViewModel.isNewDay(oldStartTime)) {
            // Work day hasn't been modified yet
            val end = start.add(dailyWorkTime).add(lunchTime)
            editWorkDayViewModel.updateEndTime(end.toString())
            val lunchStart = calculateHalfTime(start, dailyWorkTime)
            editWorkDayViewModel.updateLunchStart(lunchStart.toString())
            val lunchEnd = lunchStart.add(lunchTime)
            editWorkDayViewModel.updateLunchEnd(lunchEnd.toString())
            editWorkDayViewModel.updateBreakStart(start.toString())
            editWorkDayViewModel.updateBreakEnd(start.toString())
        } else if (!(workTimeToday.hour == 0 && workTimeToday.minute == 0)) {
            // Work day has ended already
            val oldStartTimeLocal = stringToLocalTime(oldStartTime)
            val oldWorkTimeToday = workTimeToday
            workTimeToday = workTimeToday.subtract(start).add(oldStartTimeLocal)
            editWorkDayViewModel.updateWorkTimeToday(workTimeToday.toString())
            calculateCustomTimeValues(oldWorkTimeToday, null, true)
        }
    }

    fun calculateFieldsFromEndTime(oldEndTime: String?) {
        val start = stringToLocalTime(editWorkDayViewModel.startTime.value)
        val end = stringToLocalTime(editWorkDayViewModel.endTime.value)
        val lunchEnd = stringToLocalTime(editWorkDayViewModel.lunchEnd.value)
        val lunchStart = stringToLocalTime(editWorkDayViewModel.lunchStart.value)
        var workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)
        val breakStart = stringToLocalTime(editWorkDayViewModel.breakStart.value)
        val breakEnd = stringToLocalTime(editWorkDayViewModel.breakEnd.value)
        val oldWorkTimeToday = workTimeToday
        if (workTimeToday.hour == 0 && workTimeToday.minute == 0) {
            // The day has not ended
            workTimeToday = end.subtract(start).subtract(lunchEnd).add(lunchStart)
                .subtract(breakEnd).add(breakStart)
        } else {
            val oldEndTimeLocal = stringToLocalTime(oldEndTime!!)
            workTimeToday = workTimeToday.subtract(oldEndTimeLocal).add(end)
        }
        editWorkDayViewModel.updateWorkTimeToday(workTimeToday.toString())
        calculateCustomTimeValues(oldWorkTimeToday, null, true)
    }

    fun calculateFieldsFromDailyWorkTime(oldDailyWorkTime: String?) {
        var end = stringToLocalTime(editWorkDayViewModel.endTime.value)
        val dailyWorkTime = stringToLocalTime(editWorkDayViewModel.dailyWorkTime.value)
        val workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)

        if (!editWorkDayViewModel.isNewDay(editWorkDayViewModel.startTime.value) &&
            workTimeToday.hour == 0 &&
            workTimeToday.minute == 0
        ) {
            val oldDailyWorkTimeLocal = stringToLocalTime(oldDailyWorkTime!!)
            end = end.subtract(oldDailyWorkTimeLocal).add(dailyWorkTime)
            editWorkDayViewModel.updateEndTime(end.toString())
        }
    }

    fun calculateFieldsFromLunchStart(oldLunchStart: String?) {
        var lunchEnd = stringToLocalTime(editWorkDayViewModel.lunchEnd.value)
        val lunchStart = stringToLocalTime(editWorkDayViewModel.lunchStart.value)
        val workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)
        val lunchTime = stringToLocalTime(editWorkDayViewModel.lunchTime.value)

        if (workTimeToday.hour == 0 && workTimeToday.minute == 0) {
            lunchEnd = lunchStart.add(lunchTime)
            editWorkDayViewModel.updateLunchEnd(lunchEnd.toString())
        } else {
            val oldLunchStartLocal = stringToLocalTime(oldLunchStart!!)
            lunchEnd = lunchEnd.subtract(oldLunchStartLocal).add(lunchStart)
            editWorkDayViewModel.updateLunchEnd(lunchEnd.toString())
        }
    }

    fun calculateFieldsFromLunchEnd(oldLunchEnd: String?) {
        var end = stringToLocalTime(editWorkDayViewModel.endTime.value)
        val lunchEnd = stringToLocalTime(editWorkDayViewModel.lunchEnd.value)
        var workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)
        val oldLunchEndLocal = stringToLocalTime(oldLunchEnd!!)

        if (workTimeToday.hour == 0 && workTimeToday.minute == 0) {
            end = end.subtract(oldLunchEndLocal).add(lunchEnd)
            editWorkDayViewModel.updateEndTime(end.toString())
        } else {
            val oldWorkTimeToday = workTimeToday
            workTimeToday = workTimeToday.subtract(lunchEnd).add(oldLunchEndLocal)
            editWorkDayViewModel.updateWorkTimeToday(workTimeToday.toString())
            calculateCustomTimeValues(
                oldWorkTimeToday = oldWorkTimeToday,
                oldBalanceToday = null,
                calculateBalanceToday = true
            )
        }
    }

    fun calculateFieldsFromLunchTime(oldLunchTime: String?) {
        var end = stringToLocalTime(editWorkDayViewModel.endTime.value)
        val lunchStart = stringToLocalTime(editWorkDayViewModel.lunchStart.value)
        val workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)
        val lunchTime = stringToLocalTime(editWorkDayViewModel.lunchTime.value)
        val oldLunchTimeLocal = stringToLocalTime(oldLunchTime!!)

        if (workTimeToday.hour == 0 && workTimeToday.minute == 0) {
            end = end.add(lunchTime).subtract(oldLunchTimeLocal)
            editWorkDayViewModel.updateEndTime(end.toString())
            val lunchEnd = lunchStart.add(lunchTime)
            editWorkDayViewModel.updateLunchEnd(lunchEnd.toString())
        }
    }

    fun calculateFieldsFromBreakStart(oldBreakStart: String?) {
        var end = stringToLocalTime(editWorkDayViewModel.endTime.value)
        var workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)
        val breakStart = stringToLocalTime(editWorkDayViewModel.breakStart.value)
        var breakEnd = stringToLocalTime(editWorkDayViewModel.breakEnd.value)

        val oldBreakStartLocal = stringToLocalTime(oldBreakStart!!)

        if (oldBreakStartLocal == breakEnd) {
            breakEnd = breakStart
            editWorkDayViewModel.updateBreakEnd(breakEnd.toString())
        } else if (workTimeToday.hour == 0 && workTimeToday.minute == 0) {
            end = end.subtract(oldBreakStartLocal).add(breakEnd)
            editWorkDayViewModel.updateEndTime(end.toString())
        } else {
            val oldWorkTimeToday = workTimeToday
            workTimeToday = workTimeToday.subtract(oldBreakStartLocal).add(breakStart)
            editWorkDayViewModel.updateWorkTimeToday(workTimeToday.toString())
            calculateCustomTimeValues(oldWorkTimeToday, null, true)
        }
    }

    fun calculateFieldsFromBreakEnd(oldBreakEnd: String?) {
        var end = stringToLocalTime(editWorkDayViewModel.endTime.value)
        var workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)
        val breakEnd = stringToLocalTime(editWorkDayViewModel.breakEnd.value)
        val oldBreakEndLocal = stringToLocalTime(oldBreakEnd!!)

        if (workTimeToday.hour == 0 && workTimeToday.minute == 0) {
            end = end.subtract(oldBreakEndLocal).add(breakEnd)
            editWorkDayViewModel.updateEndTime(end.toString())
        } else {
            val oldWorkTimeToday = workTimeToday
            workTimeToday = workTimeToday.subtract(breakEnd).add(oldBreakEndLocal)
            editWorkDayViewModel.updateWorkTimeToday(workTimeToday.toString())
            calculateCustomTimeValues(oldWorkTimeToday, null, true)
        }
    }

    fun calculateFieldsFromBalanceToday(oldBalanceToday: String?) {
        val start = stringToLocalTime(editWorkDayViewModel.startTime.value)
        val lunchEnd = stringToLocalTime(editWorkDayViewModel.lunchEnd.value)
        val lunchStart = stringToLocalTime(editWorkDayViewModel.lunchStart.value)
        var workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)
        val breakStart = stringToLocalTime(editWorkDayViewModel.breakStart.value)
        val breakEnd = stringToLocalTime(editWorkDayViewModel.breakEnd.value)
        val oldWorkTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)

        if (workTimeToday.hour == 0 && workTimeToday.minute == 0) {
            val balanceSum = calculateWorkTimeBalance(
                initialTime = checkIfDoubleMinus("-$oldBalanceToday"),
                addedTime = editWorkDayViewModel.balanceToday.value
            )
            editWorkDayViewModel.updateWorkTimeToday(
                calculateWorkTimeBalance(
                    initialTime = balanceSum,
                    addedTime = editWorkDayViewModel.dailyWorkTime.value
                )
            )
            workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)
            val end = start.add(workTimeToday).subtract(lunchStart).add(lunchEnd)
                .subtract(breakStart).add(breakEnd)
            editWorkDayViewModel.updateEndTime(end.toString())
        } else {
            val balanceSum = calculateWorkTimeBalance(
                initialTime = checkIfDoubleMinus("-$oldBalanceToday"),
                addedTime = editWorkDayViewModel.balanceToday.value
            )
            editWorkDayViewModel.updateEndTime(
                calculateWorkTimeBalance(
                    initialTime = balanceSum,
                    addedTime = editWorkDayViewModel.endTime.value
                )
            )
            editWorkDayViewModel.updateWorkTimeToday(
                calculateWorkTimeBalance(
                    initialTime = balanceSum,
                    addedTime = editWorkDayViewModel.workTimeToday.value
                )
            )
        }
        calculateCustomTimeValues(oldWorkTimeToday, oldBalanceToday, false)
        editWorkDayViewModel.updateOldBalanceToday(editWorkDayViewModel.balanceToday.value)
    }

    fun calculateFieldsFromWorkTimeToday(oldWorkTimeToday: String?) {
        var end = stringToLocalTime(editWorkDayViewModel.endTime.value)
        val dailyWorkTime = stringToLocalTime(editWorkDayViewModel.dailyWorkTime.value)
        val workTimeToday = stringToLocalTime(editWorkDayViewModel.workTimeToday.value)
        val oldWorkTimeTodayLocal = stringToLocalTime(oldWorkTimeToday!!)

        end = if (oldWorkTimeTodayLocal.hour == 0 && oldWorkTimeTodayLocal.minute == 0) {
            end.subtract(dailyWorkTime).add(workTimeToday)
        } else {
            end.subtract(oldWorkTimeTodayLocal).add(workTimeToday)
        }
        editWorkDayViewModel.updateEndTime(end.toString())
    }
}
