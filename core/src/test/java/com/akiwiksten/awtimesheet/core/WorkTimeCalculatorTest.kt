package com.akiwiksten.awtimesheet.core

import org.junit.Assert
import org.junit.Test
import java.time.LocalTime

class WorkTimeCalculatorTest {

    @Test
    fun `extractDayOfMonth returns day for iso date`() {
        val result = WorkTimeCalculator.extractDayOfMonth("2026-04-10")

        Assert.assertEquals("10", result)
    }

    @Test
    fun `stringToLocalTime parses hours and minutes`() {
        val result = WorkTimeCalculator.stringToLocalTime("08:30")

        Assert.assertEquals(LocalTime.of(8, 30), result)
    }

    @Test
    fun `calculateTotalMinutes adds positive times`() {
        val result = WorkTimeCalculator.calculateTotalMinutes(
            initialTime = "08:30",
            addedTime = "01:45",
            isInitialTimeNegative = false,
            isAddedTimeNegative = false
        )

        Assert.assertEquals("10:15", result)
    }

    @Test
    fun `calculateTotalMinutes handles result below zero`() {
        val result = WorkTimeCalculator.calculateTotalMinutes(
            initialTime = "01:15",
            addedTime = "02:00",
            isInitialTimeNegative = false,
            isAddedTimeNegative = true
        )

        Assert.assertEquals("-00:45", result)
    }

    @Test
    fun `calculateFlexTime handles negative initial time`() {
        val result = WorkTimeCalculator.calculateFlexTime(
            initialTime = "-01:30",
            addedTime = "00:45"
        )

        Assert.assertEquals("-00:45", result)
    }

    @Test
    fun `calculateFlexTime handles negative added time`() {
        val result = WorkTimeCalculator.calculateFlexTime(
            initialTime = "01:30",
            addedTime = "-00:45"
        )

        Assert.assertEquals("00:45", result)
    }

    @Test
    fun `calculateFlexTime handles two negative values`() {
        val result = WorkTimeCalculator.calculateFlexTime(
            initialTime = "-01:30",
            addedTime = "-00:45"
        )

        Assert.assertEquals("-02:15", result)
    }

    @Test
    fun `normalizeDuplicateMinus removes duplicate minus sign`() {
        val result = WorkTimeCalculator.normalizeDuplicateMinus("--08:00")

        Assert.assertEquals("08:00", result)
    }

    @Test
    fun `normalizeDuplicateMinus leaves normal values unchanged`() {
        val result = WorkTimeCalculator.normalizeDuplicateMinus("-08:00")

        Assert.assertEquals("-08:00", result)
    }
}