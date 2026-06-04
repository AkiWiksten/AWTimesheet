package com.akiwiksten.awtimesheet.core

import org.junit.Assert
import org.junit.Test

class FormChangeRulesTest {

    @Test
    fun hasChanges_returnsFalse_forEqualValues() {
        Assert.assertFalse(hasChanges(current = "A", baseline = "A"))
    }

    @Test
    fun hasChanges_returnsTrue_forDifferentValues() {
        Assert.assertTrue(hasChanges(current = "A", baseline = "B"))
    }

    @Test
    fun isActionEnabled_returnsTrue_whenRequiredFieldsValid_andHasChanges() {
        Assert.assertTrue(
            isActionEnabled(
                hasRequiredFields = true,
                hasUnsavedChanges = true
            )
        )
    }

    @Test
    fun isActionEnabled_returnsTrue_whenAddModeAllowsUnchanged() {
        Assert.assertTrue(
            isActionEnabled(
                hasRequiredFields = true,
                hasUnsavedChanges = false,
                allowWithoutChanges = true
            )
        )
    }

    @Test
    fun isActionEnabled_returnsFalse_whenRequiredFieldsInvalid() {
        Assert.assertFalse(
            isActionEnabled(
                hasRequiredFields = false,
                hasUnsavedChanges = true,
                allowWithoutChanges = true
            )
        )
    }
}
