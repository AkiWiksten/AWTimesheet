package com.akiwiksten.awtimesheet.core.ui

/** Shared helpers for save/confirm button enablement decisions. */
fun <T> hasChanges(current: T, baseline: T): Boolean = current != baseline

fun isActionEnabled(
    hasRequiredFields: Boolean,
    hasUnsavedChanges: Boolean,
    allowWithoutChanges: Boolean = false
): Boolean {
    return hasRequiredFields && (allowWithoutChanges || hasUnsavedChanges)
}
