package com.akiwiksten.awtimesheet.feature.singleproject

import com.akiwiksten.awtimesheet.domain.model.isProjectNameOnlyPlaceholder
import com.akiwiksten.awtimesheet.test.projectState
import org.junit.Assert
import org.junit.Test

class SingleProjectStateTest {

    @Test
    fun isProjectNameOnlyPlaceholder_returnsTrueForUnrecordedProjectNameItem() {
        val state = projectState(projectName = "Alpha")

        Assert.assertTrue(state.isProjectNameOnlyPlaceholder())
    }

    @Test
    fun isProjectNameOnlyPlaceholder_returnsFalseForRecordedProjectEvenWithDefaultValues() {
        val state = projectState(
            date = "2026-04-26",
            projectName = "Alpha"
        )

        Assert.assertFalse(state.isProjectNameOnlyPlaceholder())
    }
}