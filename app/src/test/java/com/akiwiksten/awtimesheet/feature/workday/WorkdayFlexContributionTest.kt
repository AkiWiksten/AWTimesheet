package com.akiwiksten.awtimesheet.feature.workday

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkdayFlexContributionTest {

    @Test
    fun resolveFlexContribution_whenWorkTimeIsZero_returnsZeroContribution() {
        val contribution = resolveFlexContribution(
            flexTimeByDate = "-07:30",
            workTimeByDate = ZERO_TIME
        )

        assertEquals(ZERO_TIME, contribution)
    }

    @Test
    fun resolveFlexContribution_whenWorkTimeExists_returnsFlexTimeByDate() {
        val contribution = resolveFlexContribution(
            flexTimeByDate = "-06:00",
            workTimeByDate = "01:30"
        )

        assertEquals("-06:00", contribution)
    }
}

