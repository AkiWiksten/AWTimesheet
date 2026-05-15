package com.akiwiksten.awtimesheet.feature.projects.single

import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.model.isProjectNameOnlyPlaceholder
import org.junit.Assert
import org.junit.Test

class SingleProjectStateTest {

    @Test
    fun isProjectNameOnlyPlaceholder_returnsTrueForUnrecordedProjectNameItem() {
        val state = SingleProjectState(projectName = "Alpha")

        Assert.assertTrue(state.isProjectNameOnlyPlaceholder())
    }

    @Test
    fun isProjectNameOnlyPlaceholder_returnsFalseForRecordedProjectEvenWithDefaultValues() {
        val state = SingleProjectState(
            date = "2026-04-26",
            projectName = "Alpha"
        )

        Assert.assertFalse(state.isProjectNameOnlyPlaceholder())
    }
}
