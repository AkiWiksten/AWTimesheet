package com.akiwiksten.worktime30.core.calculator

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class WorkTimeCalculatorTest {

    @Test
    fun `extractDayOfMonth returns day for iso date`() {
        val result = WorkTimeCalculator.extractDayOfMonth("2026-04-10")

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
    fun `calculateFlexTime handles negative initial time`() {
        val result = WorkTimeCalculator.calculateFlexTime(
            initialTime = "-01:30",
            addedTime = "00:45"
        )

        assertEquals("-00:45", result)
    }

    @Test
    fun `calculateFlexTime handles negative added time`() {
        val result = WorkTimeCalculator.calculateFlexTime(
            initialTime = "01:30",
            addedTime = "-00:45"
        )

        assertEquals("00:45", result)
    }

    @Test
    fun `calculateFlexTime handles two negative values`() {
        val result = WorkTimeCalculator.calculateFlexTime(
            initialTime = "-01:30",
            addedTime = "-00:45"
        )

        assertEquals("-02:15", result)
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

        val result = ProjectDetailsTimeUpdateCalculator.calculateStartTimeUpdate(
            StartTimeUpdateParams(
                start = LocalTime.of(8, 0),
                dailyWorkTime = LocalTime.of(7, 30),
                lunchTime = LocalTime.of(0, 30),
                projectTime = LocalTime.MIDNIGHT,
                oldStartTime = LocalTime.of(8, 0),
                isNewDay = true
            )
        )

        assertEquals("16:00", result.end)
        assertEquals("11:45", result.lunchStart)
        assertEquals("12:15", result.lunchEnd)
        assertEquals("08:00", result.breakStart)
        assertEquals("08:00", result.breakEnd)
        assertNull(result.projectTime)
        assertFalse(result.shouldRecalculateFlexTime)
    }

    @Test
    fun `calculateStartTimeUpdate for existing day recalculates work time`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateStartTimeUpdate(
            StartTimeUpdateParams(
                start = LocalTime.of(9, 0),
                dailyWorkTime = LocalTime.of(7, 30),
                lunchTime = LocalTime.of(0, 30),
                projectTime = LocalTime.of(8, 0),
                oldStartTime = LocalTime.of(8, 0),
                isNewDay = false
            )
        )

        assertEquals("07:00", result.projectTime)
        assertNull(result.end)
    }

    @Test
    fun `calculateStartTimeUpdate returns empty result when no recalculation is needed`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateStartTimeUpdate(
            StartTimeUpdateParams(
                start = LocalTime.of(8, 0),
                dailyWorkTime = LocalTime.of(7, 30),
                lunchTime = LocalTime.of(0, 30),
                projectTime = LocalTime.MIDNIGHT,
                oldStartTime = LocalTime.of(8, 0),
                isNewDay = false
            )
        )

        assertEquals(WorkTimeCalculator.TimeUpdateResult(), result)
    }

    @Test
    fun `calculateEndTimeUpdate calculates work time from day structure when current work time is zero`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateEndTimeUpdate(
            EndTimeUpdateParams(
                start = LocalTime.of(8, 0),
                end = LocalTime.of(16, 30),
                lunchStart = LocalTime.of(11, 30),
                lunchEnd = LocalTime.of(12, 0),
                breakStart = LocalTime.of(14, 0),
                breakEnd = LocalTime.of(14, 15),
                projectTime = LocalTime.MIDNIGHT,
                oldEndTime = LocalTime.of(16, 0)
            )
        )

        assertEquals("07:45", result.projectTime)
    }

    @Test
    fun `calculateEndTimeUpdate updates existing work time`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateEndTimeUpdate(
            EndTimeUpdateParams(
                start = LocalTime.of(8, 0),
                end = LocalTime.of(17, 0),
                lunchStart = LocalTime.of(0, 0),
                lunchEnd = LocalTime.of(0, 0),
                breakStart = LocalTime.of(0, 0),
                breakEnd = LocalTime.of(0, 0),
                projectTime = LocalTime.of(7, 30),
                oldEndTime = LocalTime.of(16, 0)
            )
        )

        assertEquals("08:30", result.projectTime)
    }

    @Test
    fun `calculateDailyWorkTimeUpdate updates end time only for existing day without explicit work time`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateDailyWorkTimeUpdate(
            end = LocalTime.of(16, 0),
            dailyWorkTime = LocalTime.of(8, 0),
            projectTime = LocalTime.MIDNIGHT,
            oldDailyWorkTime = LocalTime.of(7, 30),
            isNewDay = false
        )

        assertEquals("16:30", result.end)
    }

    @Test
    fun `calculateDailyWorkTimeUpdate returns empty result for new day`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateDailyWorkTimeUpdate(
            end = LocalTime.of(16, 0),
            dailyWorkTime = LocalTime.of(8, 0),
            projectTime = LocalTime.MIDNIGHT,
            oldDailyWorkTime = LocalTime.of(7, 30),
            isNewDay = true
        )

        assertEquals(WorkTimeCalculator.TimeUpdateResult(), result)
    }

    @Test
    fun `calculateLunchStartUpdate updates lunch end from lunch length when work time is zero`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateLunchStartUpdate(
            lunchStart = LocalTime.of(11, 45),
            lunchTime = LocalTime.of(0, 30),
            projectTime = LocalTime.MIDNIGHT,
            oldLunchStart = LocalTime.of(11, 30),
            currentLunchEnd = LocalTime.of(12, 0)
        )

        assertEquals("12:15", result.lunchEnd)
    }

    @Test
    fun `calculateLunchStartUpdate shifts lunch end when work time already exists`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateLunchStartUpdate(
            lunchStart = LocalTime.of(11, 0),
            lunchTime = LocalTime.of(0, 30),
            projectTime = LocalTime.of(7, 30),
            oldLunchStart = LocalTime.of(11, 30),
            currentLunchEnd = LocalTime.of(12, 0)
        )

        assertEquals("11:30", result.lunchEnd)
    }

    @Test
    fun `calculateLunchEndUpdate updates end time when work time is zero`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateLunchEndUpdate(
            end = LocalTime.of(16, 0),
            lunchEnd = LocalTime.of(12, 30),
            projectTime = LocalTime.MIDNIGHT,
            oldLunchEnd = LocalTime.of(12, 0)
        )

        assertEquals("16:30", result.end)
        assertFalse(result.shouldRecalculateFlexTime)
    }

    @Test
    fun `calculateLunchEndUpdate updates work time and flex time when work time exists`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateLunchEndUpdate(
            end = LocalTime.of(16, 0),
            lunchEnd = LocalTime.of(12, 30),
            projectTime = LocalTime.of(7, 30),
            oldLunchEnd = LocalTime.of(12, 0)
        )

        assertEquals("07:00", result.projectTime)
        assertTrue(result.shouldRecalculateFlexTime)
    }

    @Test
    fun `calculateLunchTimeUpdate updates both end time and lunch end when work time is zero`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateLunchTimeUpdate(
            end = LocalTime.of(16, 0),
            lunchStart = LocalTime.of(11, 30),
            lunchTime = LocalTime.of(1, 0),
            projectTime = LocalTime.MIDNIGHT,
            oldLunchTime = LocalTime.of(0, 30)
        )

        assertEquals("16:30", result.end)
        assertEquals("12:30", result.lunchEnd)
    }

    @Test
    fun `calculateLunchTimeUpdate returns empty result when work time exists`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateLunchTimeUpdate(
            end = LocalTime.of(16, 0),
            lunchStart = LocalTime.of(11, 30),
            lunchTime = LocalTime.of(1, 0),
            projectTime = LocalTime.of(7, 30),
            oldLunchTime = LocalTime.of(0, 30)
        )

        assertEquals(WorkTimeCalculator.TimeUpdateResult(), result)
    }

    @Test
    fun `calculateBreakStartUpdate syncs break end when old break start matches current break end`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateBreakStartUpdate(
            end = LocalTime.of(16, 0),
            breakStart = LocalTime.of(14, 15),
            breakEnd = LocalTime.of(14, 0),
            projectTime = LocalTime.MIDNIGHT,
            oldBreakStart = LocalTime.of(14, 0)
        )

        assertEquals("14:15", result.breakEnd)
    }

    @Test
    fun `calculateBreakStartUpdate updates end time when work time is zero`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateBreakStartUpdate(
            end = LocalTime.of(16, 0),
            breakStart = LocalTime.of(14, 10),
            breakEnd = LocalTime.of(14, 15),
            projectTime = LocalTime.MIDNIGHT,
            oldBreakStart = LocalTime.of(14, 0)
        )

        assertEquals("16:15", result.end)
    }

    @Test
    fun `calculateBreakStartUpdate updates work time and flex time when work time exists`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateBreakStartUpdate(
            end = LocalTime.of(16, 0),
            breakStart = LocalTime.of(14, 15),
            breakEnd = LocalTime.of(14, 30),
            projectTime = LocalTime.of(7, 30),
            oldBreakStart = LocalTime.of(14, 0)
        )

        assertEquals("07:45", result.projectTime)
        assertTrue(result.shouldRecalculateFlexTime)
    }

    @Test
    fun `calculateBreakEndUpdate updates end time when work time is zero`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateBreakEndUpdate(
            end = LocalTime.of(16, 0),
            projectTime = LocalTime.MIDNIGHT,
            breakEnd = LocalTime.of(14, 30),
            oldBreakEnd = LocalTime.of(14, 15)
        )

        assertEquals("16:15", result.end)
    }

    @Test
    fun `calculateBreakEndUpdate updates work time and flex time when work time exists`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateBreakEndUpdate(
            end = LocalTime.of(16, 0),
            projectTime = LocalTime.of(7, 30),
            breakEnd = LocalTime.of(14, 30),
            oldBreakEnd = LocalTime.of(14, 15)
        )

        assertEquals("07:15", result.projectTime)
        assertTrue(result.shouldRecalculateFlexTime)
    }

    @Test
    fun `calculateProjectTimeUpdate uses daily work time when previous work time was zero`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateProjectTimeUpdate(
            end = LocalTime.of(16, 0),
            dailyWorkTime = LocalTime.of(7, 30),
            projectTime = LocalTime.of(8, 0),
            oldProjectTime = LocalTime.MIDNIGHT
        )

        assertEquals("16:30", result.end)
    }

    @Test
    fun `calculateProjectTimeUpdate uses old work time when previous work time exists`() {
        val result = ProjectDetailsTimeUpdateCalculator.calculateProjectTimeUpdate(
            end = LocalTime.of(16, 30),
            dailyWorkTime = LocalTime.of(7, 30),
            projectTime = LocalTime.of(8, 0),
            oldProjectTime = LocalTime.of(7, 45)
        )

        assertEquals("16:45", result.end)
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
