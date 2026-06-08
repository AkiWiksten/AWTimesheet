package com.akiwiksten.awtimesheet.feature.workday.components

import com.akiwiksten.awtimesheet.core.WorkTimeDisplayCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkdayStateSectionsTest {

    @Test
    fun calculateDisplayedFlexTimeByDate_whenWorkTimeIsZero_returnsZeroTime() {
        val result = WorkTimeDisplayCalculator.calculateDisplayedFlexTimeByDate(
            persistedWorkTimeByDate = ZERO_TIME,
            persistedFlexTimeByDate = ZERO_TIME,
            editedWorkTimeByDateEstimate = "07:30",
            isEditedWorkTimeByDateEstimateValid = true
        )

        assertEquals(ZERO_TIME, result)
    }

    @Test
    fun calculateDisplayedFlexTimeByDate_whenEstimateInvalid_returnsPersistedFlexTime() {
        val result = WorkTimeDisplayCalculator.calculateDisplayedFlexTimeByDate(
            persistedWorkTimeByDate = "02:00",
            persistedFlexTimeByDate = "-05:30",
            editedWorkTimeByDateEstimate = "bad",
            isEditedWorkTimeByDateEstimateValid = false
        )

        assertEquals("-05:30", result)
    }

    @Test
    fun calculateDisplayedFlexTimeByDate_whenSpecialRuleApplied_returnsPersistedFlexTime() {
        val result = WorkTimeDisplayCalculator.calculateDisplayedFlexTimeByDate(
            persistedWorkTimeByDate = "07:30",
            persistedFlexTimeByDate = "-07:30",
            editedWorkTimeByDateEstimate = "07:30",
            isEditedWorkTimeByDateEstimateValid = true,
            usePersistedFlexTimeByDate = true
        )

        assertEquals("-07:30", result)
    }

    @Test
    fun calculateDisplayedCalculatedFlexTimeTotal_usesPersistedInitialFlexTimeTotal() {
        val result = WorkTimeDisplayCalculator.calculateDisplayedCalculatedFlexTimeTotal(
            persistedInitialFlexTimeTotal = "+01:00",
            persistedDisplayedFlexTimeTotal = "+03:30",
            persistedFlexTimeByDate = "+00:30",
            editedFlexTimeByDate = "+00:30"
        )

        assertEquals("03:30", result)
    }

    @Test
    fun calculateDisplayedCalculatedFlexTimeTotal_keepsZeroTimeOnCleanStart() {
        val result = WorkTimeDisplayCalculator.calculateDisplayedCalculatedFlexTimeTotal(
            persistedInitialFlexTimeTotal = "00:00",
            persistedDisplayedFlexTimeTotal = "00:00",
            persistedFlexTimeByDate = "00:00",
            editedFlexTimeByDate = "00:00"
        )

        assertEquals("00:00", result)
    }

    @Test
    fun calculateDisplayedCalculatedFlexTimeTotal_updatesWhenEstimateChangesflexTimeByDate() {
        val result = WorkTimeDisplayCalculator.calculateDisplayedCalculatedFlexTimeTotal(
            persistedInitialFlexTimeTotal = "+01:00",
            persistedDisplayedFlexTimeTotal = "+03:30",
            persistedFlexTimeByDate = "+00:30",
            editedFlexTimeByDate = "-00:30"
        )

        assertEquals("02:30", result)
    }
}
