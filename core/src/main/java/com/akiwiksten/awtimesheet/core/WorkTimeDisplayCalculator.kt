package com.akiwiksten.awtimesheet.core

/**
 * Pure logic for calculating time values that are specifically used for display purposes,
 * such as combining persisted values with in-progress edits.
 */
object WorkTimeDisplayCalculator {

    /**
     * Calculates the flex time to be displayed on the UI, taking into account current edits.
     * If the current edit is invalid, it falls back to the persisted value.
     */
    fun calculateDisplayedFlexTimeByDate(
        persistedWorkTimeByDate: String,
        persistedFlexTimeByDate: String,
        editedWorkTimeByDateEstimate: String,
        isEditedWorkTimeByDateEstimateValid: Boolean
    ): String {
        return when {
            !isEditedWorkTimeByDateEstimateValid -> persistedFlexTimeByDate
            persistedWorkTimeByDate == ZERO_TIME -> ZERO_TIME
            else -> WorkTimeCalculator.calculateFlexTime(
                initialTime = persistedWorkTimeByDate,
                addedTime = "-$editedWorkTimeByDateEstimate"
            )
        }
    }

    /**
     * Recalculates the total flex balance to be shown on the UI when the daily estimate is being edited.
     */
    fun calculateDisplayedCalculatedFlexTimeTotal(
        persistedInitialFlexTimeTotal: String,
        persistedDisplayedFlexTimeTotal: String,
        persistedFlexTimeByDate: String,
        editedFlexTimeByDate: String
    ): String {
        val persistedCalculatedFlexDeltaTotal = WorkTimeCalculator.calculateFlexTime(
            initialTime = persistedDisplayedFlexTimeTotal,
            addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$persistedInitialFlexTimeTotal")
        )

        val flexTimeByDateDelta = WorkTimeCalculator.calculateFlexTime(
            initialTime = editedFlexTimeByDate,
            addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$persistedFlexTimeByDate")
        )

        val recalculatedFlexDeltaTotal = WorkTimeCalculator.calculateFlexTime(
            initialTime = persistedCalculatedFlexDeltaTotal,
            addedTime = flexTimeByDateDelta
        )

        return WorkTimeCalculator.calculateFlexTime(
            initialTime = persistedInitialFlexTimeTotal,
            addedTime = recalculatedFlexDeltaTotal
        )
    }
}
