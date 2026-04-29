package com.akiwiksten.worktime30.feature.workday.components

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkdayStateSectionsTest {

    @Test
    fun calculateDisplayedCalculatedFlexTimeTotal_usesPersistedInitialFlexTimeTotal() {
        val result = calculateDisplayedCalculatedFlexTimeTotal(
            persistedInitialFlexTimeTotal = "+01:00",
            persistedCalculatedFlexTimeTotal = "+03:30",
            persistedFlexTimeToday = "+00:30",
            editedFlexTimeToday = "+00:30"
        )

        assertEquals("03:30", result)
    }

    @Test
    fun calculateDisplayedCalculatedFlexTimeTotal_keepsZeroTimeOnCleanStart() {
        val result = calculateDisplayedCalculatedFlexTimeTotal(
            persistedInitialFlexTimeTotal = "00:00",
            persistedCalculatedFlexTimeTotal = "00:00",
            persistedFlexTimeToday = "00:00",
            editedFlexTimeToday = "00:00"
        )

        assertEquals("00:00", result)
    }

    @Test
    fun calculateDisplayedCalculatedFlexTimeTotal_updatesWhenEstimateChangesFlexTimeToday() {
        val result = calculateDisplayedCalculatedFlexTimeTotal(
            persistedInitialFlexTimeTotal = "+01:00",
            persistedCalculatedFlexTimeTotal = "+03:30",
            persistedFlexTimeToday = "+00:30",
            editedFlexTimeToday = "-00:30"
        )

        assertEquals("02:30", result)
    }
}
