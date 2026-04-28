package com.akiwiksten.worktime30.feature.projects.single

import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.model.isProjectNameOnlyPlaceholder
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