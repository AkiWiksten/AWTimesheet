package com.akiwiksten.awtimesheet.feature.projectdetails.model

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.test.projectDetailsState
import com.akiwiksten.awtimesheet.test.settingsState
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectDetailsStateUpdatesTest {

    @Test
    fun `updateTimeField START_TIME for new day updates dependent fields`() {
        val initial = projectDetailsState()
        val settings = settingsState(dailyWorkTimeEstimate = "07:30", dailyLunchTimeEstimate = "00:30")
        
        val result = initial.updateTimeField(ProjectDetailsField.START_TIME, "08:00", settings)
        
        assertEquals("08:00", result.startTime)
        assertEquals("16:00", result.endTime)
        assertEquals("11:45", result.lunchStart)
        assertEquals("12:15", result.lunchEnd)
        assertEquals("08:00", result.breakStart)
        assertEquals("08:00", result.breakEnd)
    }

    @Test
    fun `updateTimeField PROJECT_TIME when only project time normalization works`() {
        val initial = projectDetailsState(projectName = "Alpha")
        val settings = settingsState(dailyWorkTimeEstimate = "07:30", dailyLunchTimeEstimate = "00:30")
        
        val result = initial.updateTimeField(ProjectDetailsField.PROJECT_TIME, "02:00", settings)
        
        assertEquals("02:00", result.projectTime)
        assertEquals("02:30", result.endTime)
        assertEquals(ZERO_TIME, result.startTime)
        assertEquals(ZERO_TIME, result.lunchStart)
        assertEquals(ZERO_TIME, result.lunchEnd)
        assertEquals(ZERO_TIME, result.breakStart)
        assertEquals(ZERO_TIME, result.breakEnd)
    }
}
