package com.akiwiksten.worktime30.core

import com.akiwiksten.worktime30.core.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.worktime30.core.WorkTimeCalculator.StartTimeUpdateParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class WorkTimeCalculatorTest {

    @Test
    fun `parseDate returns day for iso date`() {
        val result = WorkTimeCalculator.parseDate("2026-04-10")

        assertEquals("10", result)
    }

    @Test
    fun `stringToLocalTime parses hours and minutes`() {
        val result = WorkTimeCalculator.stringToLocalTime("08:30")

        assertEquals(LocalTime.of(8, 30), result)
    }

    @Test
    fun `calculateTotalMinutes adds positive times`() {
        val result = WorkTimeCalculator.calculateTotalMinutes(
            initialTime = "08:30",
            addedTime = "01:45",
            isInitialTimeNegative = false,
            isAddedTimeNegative = false
        )

        assertEquals("10:15", result)
    }

    @Test
    fun `calculateTotalMinutes handles result below zero`() {
        val result = WorkTimeCalculator.calculateTotalMinutes(
            initialTime = "01:15",
            addedTime = "02:00",
            isInitialTimeNegative = false,
            isAddedTimeNegative = true
        )

        assertEquals("-00:45", result)
    }

    @Test
    fun `calculateWorkTimeBalance handles negative initial time`() {
        val result = WorkTimeCalculator.calculateWorkTimeBalance(
            initialTime = "-01:30",
            addedTime = "00:45"
        )

        assertEquals("-00:45", result)
    }

    @Test
    fun `calculateWorkTimeBalance handles negative added time`() {
        val result = WorkTimeCalculator.calculateWorkTimeBalance(
            initialTime = "01:30",
            addedTime = "-00:45"
        )

        assertEquals("00:45", result)
    }

    @Test
    fun `calculateWorkTimeBalance handles two negative values`() {
        val result = WorkTimeCalculator.calculateWorkTimeBalance(
            initialTime = "-01:30",
            addedTime = "-00:45"
        )

        assertEquals("-02:15", result)
    }

    @Test
    fun `calculateStartTimeUpdate for new day updates dependent fields`() {
        val result = WorkTimeCalculator.calculateStartTimeUpdate(
            StartTimeUpdateParams(
                start = LocalTime.of(8, 0),
                dailyWorkTime = LocalTime.of(7, 30),
                lunchTime = LocalTime.of(0, 30),
                workTimeToday = LocalTime.MIDNIGHT,
                oldStartTime = LocalTime.of(8, 0),
                isNewDay = true
            )
        )

        assertEquals("16:00", result.endTime)
        assertEquals("11:45", result.lunchStart)
        assertEquals("12:15", result.lunchEnd)
        assertEquals("08:00", result.breakStart)
        assertEquals("08:00", result.breakEnd)
        assertNull(result.workTimeToday)
        assertFalse(result.calculateBalance)
    }

    @Test
    fun `calculateStartTimeUpdate for existing day recalculates work time`() {
        val result = WorkTimeCalculator.calculateStartTimeUpdate(
            StartTimeUpdateParams(
                start = LocalTime.of(9, 0),
                dailyWorkTime = LocalTime.of(7, 30),
                lunchTime = LocalTime.of(0, 30),
                workTimeToday = LocalTime.of(8, 0),
                oldStartTime = LocalTime.of(8, 0),
                isNewDay = false
            )
        )

        assertEquals("07:00", result.workTimeToday)
        assertNull(result.endTime)
    }

    @Test
    fun `calculateStartTimeUpdate returns empty result when no recalculation is needed`() {
        val result = WorkTimeCalculator.calculateStartTimeUpdate(
            StartTimeUpdateParams(
                start = LocalTime.of(8, 0),
                dailyWorkTime = LocalTime.of(7, 30),
                lunchTime = LocalTime.of(0, 30),
                workTimeToday = LocalTime.MIDNIGHT,
                oldStartTime = LocalTime.of(8, 0),
                isNewDay = false
            )
        )

        assertEquals(WorkTimeCalculator.TimeUpdateResult(), result)
    }

    @Test
    fun `calculateEndTimeUpdate calculates work time from day structure when current work time is zero`() {
        val result = WorkTimeCalculator.calculateEndTimeUpdate(
            EndTimeUpdateParams(
                start = LocalTime.of(8, 0),
                end = LocalTime.of(16, 30),
                lunchStart = LocalTime.of(11, 30),
                lunchEnd = LocalTime.of(12, 0),
                breakStart = LocalTime.of(14, 0),
                breakEnd = LocalTime.of(14, 15),
                workTimeToday = LocalTime.MIDNIGHT,
                oldEndTime = LocalTime.of(16, 0)
            )
        )

        assertEquals("07:45", result.workTimeToday)
    }

    @Test
    fun `calculateEndTimeUpdate updates existing work time`() {
        val result = WorkTimeCalculator.calculateEndTimeUpdate(
            EndTimeUpdateParams(
                start = LocalTime.of(8, 0),
                end = LocalTime.of(17, 0),
                lunchStart = LocalTime.of(11, 30),
                lunchEnd = LocalTime.of(12, 0),
                breakStart = LocalTime.of(14, 0),
                breakEnd = LocalTime.of(14, 15),
                workTimeToday = LocalTime.of(7, 30),
                oldEndTime = LocalTime.of(16, 0)
            )
        )

        assertEquals("08:30", result.workTimeToday)
    }

    @Test
    fun `calculateDailyWorkTimeUpdate updates end time only for existing day without explicit work time`() {
        val result = WorkTimeCalculator.calculateDailyWorkTimeUpdate(
            end = LocalTime.of(16, 0),
            dailyWorkTime = LocalTime.of(8, 0),
            workTimeToday = LocalTime.MIDNIGHT,
            oldDailyWorkTime = LocalTime.of(7, 30),
            isNewDay = false
        )

        assertEquals("16:30", result.endTime)
    }

    @Test
    fun `calculateDailyWorkTimeUpdate returns empty result for new day`() {
        val result = WorkTimeCalculator.calculateDailyWorkTimeUpdate(
            end = LocalTime.of(16, 0),
            dailyWorkTime = LocalTime.of(8, 0),
            workTimeToday = LocalTime.MIDNIGHT,
            oldDailyWorkTime = LocalTime.of(7, 30),
            isNewDay = true
        )

        assertEquals(WorkTimeCalculator.TimeUpdateResult(), result)
    }

    @Test
    fun `calculateLunchStartUpdate updates lunch end from lunch length when work time is zero`() {
        val result = WorkTimeCalculator.calculateLunchStartUpdate(
            lunchStart = LocalTime.of(11, 45),
            lunchTime = LocalTime.of(0, 30),
            workTimeToday = LocalTime.MIDNIGHT,
            oldLunchStart = LocalTime.of(11, 30),
            currentLunchEnd = LocalTime.of(12, 0)
        )

        assertEquals("12:15", result.lunchEnd)
    }

    @Test
    fun `calculateLunchStartUpdate shifts lunch end when work time already exists`() {
        val result = WorkTimeCalculator.calculateLunchStartUpdate(
            lunchStart = LocalTime.of(11, 0),
            lunchTime = LocalTime.of(0, 30),
            workTimeToday = LocalTime.of(7, 30),
            oldLunchStart = LocalTime.of(11, 30),
            currentLunchEnd = LocalTime.of(12, 0)
        )

        assertEquals("11:30", result.lunchEnd)
    }

    @Test
    fun `calculateLunchEndUpdate updates end time when work time is zero`() {
        val result = WorkTimeCalculator.calculateLunchEndUpdate(
            end = LocalTime.of(16, 0),
            lunchEnd = LocalTime.of(12, 30),
            workTimeToday = LocalTime.MIDNIGHT,
            oldLunchEnd = LocalTime.of(12, 0)
        )

        assertEquals("16:30", result.endTime)
        assertFalse(result.calculateBalance)
    }

    @Test
    fun `calculateLunchEndUpdate updates work time and balance when work time exists`() {
        val result = WorkTimeCalculator.calculateLunchEndUpdate(
            end = LocalTime.of(16, 0),
            lunchEnd = LocalTime.of(12, 30),
            workTimeToday = LocalTime.of(7, 30),
            oldLunchEnd = LocalTime.of(12, 0)
        )

        assertEquals("07:00", result.workTimeToday)
        assertTrue(result.calculateBalance)
    }

    @Test
    fun `calculateLunchTimeUpdate updates both end time and lunch end when work time is zero`() {
        val result = WorkTimeCalculator.calculateLunchTimeUpdate(
            end = LocalTime.of(16, 0),
            lunchStart = LocalTime.of(11, 30),
            lunchTime = LocalTime.of(1, 0),
            workTimeToday = LocalTime.MIDNIGHT,
            oldLunchTime = LocalTime.of(0, 30)
        )

        assertEquals("16:30", result.endTime)
        assertEquals("12:30", result.lunchEnd)
    }

    @Test
    fun `calculateLunchTimeUpdate returns empty result when work time exists`() {
        val result = WorkTimeCalculator.calculateLunchTimeUpdate(
            end = LocalTime.of(16, 0),
            lunchStart = LocalTime.of(11, 30),
            lunchTime = LocalTime.of(1, 0),
            workTimeToday = LocalTime.of(7, 30),
            oldLunchTime = LocalTime.of(0, 30)
        )

        assertEquals(WorkTimeCalculator.TimeUpdateResult(), result)
    }

    @Test
    fun `calculateBreakStartUpdate syncs break end when old break start matches current break end`() {
        val result = WorkTimeCalculator.calculateBreakStartUpdate(
            end = LocalTime.of(16, 0),
            breakStart = LocalTime.of(14, 15),
            breakEnd = LocalTime.of(14, 0),
            workTimeToday = LocalTime.MIDNIGHT,
            oldBreakStart = LocalTime.of(14, 0)
        )

        assertEquals("14:15", result.breakEnd)
    }

    @Test
    fun `calculateBreakStartUpdate updates end time when work time is zero`() {
        val result = WorkTimeCalculator.calculateBreakStartUpdate(
            end = LocalTime.of(16, 0),
            breakStart = LocalTime.of(14, 10),
            breakEnd = LocalTime.of(14, 15),
            workTimeToday = LocalTime.MIDNIGHT,
            oldBreakStart = LocalTime.of(14, 0)
        )

        assertEquals("16:15", result.endTime)
    }

    @Test
    fun `calculateBreakStartUpdate updates work time and balance when work time exists`() {
        val result = WorkTimeCalculator.calculateBreakStartUpdate(
            end = LocalTime.of(16, 0),
            breakStart = LocalTime.of(14, 15),
            breakEnd = LocalTime.of(14, 30),
            workTimeToday = LocalTime.of(7, 30),
            oldBreakStart = LocalTime.of(14, 0)
        )

        assertEquals("07:45", result.workTimeToday)
        assertTrue(result.calculateBalance)
    }

    @Test
    fun `calculateBreakEndUpdate updates end time when work time is zero`() {
        val result = WorkTimeCalculator.calculateBreakEndUpdate(
            end = LocalTime.of(16, 0),
            workTimeToday = LocalTime.MIDNIGHT,
            breakEnd = LocalTime.of(14, 30),
            oldBreakEnd = LocalTime.of(14, 15)
        )

        assertEquals("16:15", result.endTime)
    }

    @Test
    fun `calculateBreakEndUpdate updates work time and balance when work time exists`() {
        val result = WorkTimeCalculator.calculateBreakEndUpdate(
            end = LocalTime.of(16, 0),
            workTimeToday = LocalTime.of(7, 30),
            breakEnd = LocalTime.of(14, 30),
            oldBreakEnd = LocalTime.of(14, 15)
        )

        assertEquals("07:15", result.workTimeToday)
        assertTrue(result.calculateBalance)
    }

    @Test
    fun `calculateWorkTimeTodayUpdate uses daily work time when previous work time was zero`() {
        val result = WorkTimeCalculator.calculateWorkTimeTodayUpdate(
            end = LocalTime.of(16, 0),
            dailyWorkTime = LocalTime.of(7, 30),
            workTimeToday = LocalTime.of(8, 0),
            oldWorkTimeToday = LocalTime.MIDNIGHT
        )

        assertEquals("16:30", result.endTime)
    }

    @Test
    fun `calculateWorkTimeTodayUpdate uses old work time when previous work time exists`() {
        val result = WorkTimeCalculator.calculateWorkTimeTodayUpdate(
            end = LocalTime.of(16, 30),
            dailyWorkTime = LocalTime.of(7, 30),
            workTimeToday = LocalTime.of(8, 0),
            oldWorkTimeToday = LocalTime.of(7, 45)
        )

        assertEquals("16:45", result.endTime)
    }

    @Test
    fun `checkIfDoubleMinus removes duplicate minus sign`() {
        val result = WorkTimeCalculator.checkIfDoubleMinus("--08:00")

        assertEquals("08:00", result)
    }

    @Test
    fun `checkIfDoubleMinus leaves normal values unchanged`() {
        val result = WorkTimeCalculator.checkIfDoubleMinus("-08:00")

        assertEquals("-08:00", result)
    }
}
