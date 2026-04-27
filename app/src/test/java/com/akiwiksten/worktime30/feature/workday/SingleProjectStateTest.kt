package com.akiwiksten.worktime30.feature.workday

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleProjectStateTest {

    @Test
    fun isProjectNameOnlyPlaceholder_returnsTrueForUnrecordedProjectNameItem() {
        val state = SingleProjectState(projectName = "Alpha")

        assertTrue(state.isProjectNameOnlyPlaceholder())
    }

    @Test
    fun isProjectNameOnlyPlaceholder_returnsFalseForRecordedProjectEvenWithDefaultValues() {
        val state = SingleProjectState(
            date = "2026-04-26",
            projectName = "Alpha"
        )

        assertFalse(state.isProjectNameOnlyPlaceholder())
    }
}
