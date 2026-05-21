package com.akiwiksten.awtimesheet.core.ui

import com.akiwiksten.awtimesheet.core.hasChanges
import com.akiwiksten.awtimesheet.core.isActionEnabled
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormChangeRulesTest {

    @Test
    fun hasChanges_returnsFalse_forEqualValues() {
        assertFalse(hasChanges(current = "A", baseline = "A"))
    }

    @Test
    fun hasChanges_returnsTrue_forDifferentValues() {
        assertTrue(hasChanges(current = "A", baseline = "B"))
    }

    @Test
    fun isActionEnabled_returnsTrue_whenRequiredFieldsValid_andHasChanges() {
        assertTrue(
            isActionEnabled(
                hasRequiredFields = true,
                hasUnsavedChanges = true
            )
        )
    }

    @Test
    fun isActionEnabled_returnsTrue_whenAddModeAllowsUnchanged() {
        assertTrue(
            isActionEnabled(
                hasRequiredFields = true,
                hasUnsavedChanges = false,
                allowWithoutChanges = true
            )
        )
    }

    @Test
    fun isActionEnabled_returnsFalse_whenRequiredFieldsInvalid() {
        assertFalse(
            isActionEnabled(
                hasRequiredFields = false,
                hasUnsavedChanges = true,
                allowWithoutChanges = true
            )
        )
    }
}
