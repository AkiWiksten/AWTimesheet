package com.akiwiksten.awtimesheet.test

import org.junit.Assert.assertEquals
import org.junit.Test

class TestFixturesSmokeTest {
    @Test
    fun `fixtures create expected defaults`() {
        val settings = settingsState()
        val project = projectState()
        val details = projectDetailsState()
        val row = workdayStatsRow(date = "2026-05-23", workTimeByDateEstimate = "07:30")

        assertEquals("", settings.name)
        assertEquals("", project.projectName)
        assertEquals("00:00", details.projectTime)
        assertEquals("2026-05-23", row.date)
    }
}
