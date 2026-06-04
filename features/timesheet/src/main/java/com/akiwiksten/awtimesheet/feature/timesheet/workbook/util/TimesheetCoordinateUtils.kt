package com.akiwiksten.awtimesheet.feature.timesheet.workbook.util

private const val DAYS_IN_ALPHABET = 26

internal fun projectSummaryTotalColumnIndex(projectCount: Int): Int {
    return PROJECT_SUMMARY_START_COLUMN_INDEX + projectCount
}

internal fun allowanceLabelColumnIndex(projectCount: Int): Int {
    return projectSummaryTotalColumnIndex(projectCount) + 2
}

internal fun allowanceStartColumnIndex(projectCount: Int): Int {
    return allowanceLabelColumnIndex(projectCount) + 1
}

internal fun allowanceTotalColumnIndex(projectCount: Int): Int {
    return allowanceStartColumnIndex(projectCount) + projectCount
}

internal fun workTypeLabelColumnIndex(projectCount: Int): Int {
    return allowanceTotalColumnIndex(projectCount) + 2
}

internal fun buildCellReference(columnIndex: Int, rowNumber: Int): String {
    return "${columnIndexToLetters(columnIndex)}$rowNumber"
}

internal fun dayToColumn(day: Int): String {
    return columnIndexToLetters(day + 1)
}

internal fun columnIndexToLetters(columnIndex: Int): String {
    var value = columnIndex
    val builder = StringBuilder()
    while (value > 0) {
        val remainder = (value - 1) % DAYS_IN_ALPHABET
        builder.insert(0, ('A'.code + remainder).toChar())
        value = (value - 1) / DAYS_IN_ALPHABET
    }
    return builder.toString()
}
