package com.akiwiksten.awtimesheet.feature.timesheet.workbook.util

// Row and block layout configuration.
internal const val DAILY_ENTRY_ROW_HEIGHT = 7
internal const val DAILY_ENTRIES_START_ROW = 9
internal const val DAILY_ENTRIES_SEPARATOR_ROW = DAILY_ENTRIES_START_ROW - 1
internal const val TEMPLATE_DAILY_ENTRY_BLOCKS = 1

internal const val DAYS_IN_MONTH = 31

// Row offsets within a daily entry block.
internal const val DAILY_ENTRY_NAME_ROW_OFFSET = 2
internal const val DAILY_ENTRY_TIME_ROW_OFFSET = 3
internal const val DAILY_ENTRY_ALLOWANCE_ROW_OFFSET = 4
internal const val DAILY_ENTRY_WORK_TYPE_ROW_OFFSET = 5
internal const val DAILY_ENTRY_COMMENT_ROW_OFFSET = 6
internal const val DAILY_ENTRY_KILOMETRES_ROW_OFFSET = 7

// Row offsets within the summary sections.
internal const val SUMMARY_LABEL_ROW = 1
internal const val SUMMARY_WORK_TIME_ROW = 2
internal const val SUMMARY_KILOMETRES_ROW = 3

// Workbook style ids (match template cellXfs 0-based index).
//   0 = default plain text (no bold, no border)
//   1 = bold, no border
//   2 = bold + all thin borders          (A8 "Day of Month" label)
//   3 = bold + all thin borders + center  (B8-AF8 day-of-month numbers)
//   6 = hh:mm time format
//   7 = [hh]:mm cumulative time format
//   8 = integer number format
internal const val PLAIN_TEXT_STYLE = 0
internal const val BOLD_TEXT_STYLE = 1
internal const val DAY_OF_MONTH_VALUE_STYLE = 3 // bold + border + center; B8-AF8
internal const val PLAIN_TIME_STYLE = 18 // normalizes to 18-11=7 = [hh]:mm
internal const val PLAIN_INTEGER_STYLE = 19 // normalizes to 19-11=8 = integer

internal const val MAX_SUMMARY_PROJECTS = 3
internal const val PROJECT_SUMMARY_START_COLUMN_INDEX = 5 // E
internal const val PROJECT_SUMMARY_HEADER_STYLE = PLAIN_TEXT_STYLE
internal const val PROJECT_SUMMARY_WORK_TIME_STYLE = PLAIN_TIME_STYLE
internal const val PROJECT_SUMMARY_KILOMETRES_STYLE = PLAIN_INTEGER_STYLE
internal const val PROJECT_SUMMARY_TOTAL_HEADER_STYLE = PLAIN_TEXT_STYLE
internal const val PROJECT_SUMMARY_TOTAL_WORK_TIME_STYLE = PLAIN_TIME_STYLE
internal const val PROJECT_SUMMARY_TOTAL_KILOMETRES_STYLE = PLAIN_INTEGER_STYLE
internal const val ALLOWANCE_HEADER_STYLE = PLAIN_TEXT_STYLE
internal const val ALLOWANCE_TOTAL_HEADER_STYLE = PLAIN_TEXT_STYLE
internal val ALLOWANCE_PROJECT_VALUE_STYLES = listOf(PLAIN_INTEGER_STYLE, PLAIN_INTEGER_STYLE, PLAIN_INTEGER_STYLE)
internal val ALLOWANCE_TOTAL_VALUE_STYLES = listOf(PLAIN_INTEGER_STYLE, PLAIN_INTEGER_STYLE, PLAIN_INTEGER_STYLE)
internal const val WORK_TYPE_HEADER_STYLE = PLAIN_TEXT_STYLE
internal const val WORK_TYPE_TOTAL_HEADER_STYLE = PLAIN_TEXT_STYLE

// Fixed template layout cells.
internal val PROJECT_NAME_HEADER_CELLS = listOf("E1", "F1", "G1")
internal val PROJECT_TIME_SUMMARY_CELLS = listOf("E2", "F2", "G2")
internal val PROJECT_KILOMETRES_SUMMARY_CELLS = listOf("E3", "F3", "G3")
internal val ALLOWANCE_HEADER_CELLS = listOf("H1", "I1", "J1", "K1")
internal val ALLOWANCE_LABEL_CELLS = listOf("G2", "G3", "G4")
internal val WORK_TYPE_HEADER_CELLS = listOf("N1", "O1", "P1", "Q1")
internal val WORK_TYPE_LABEL_CELLS = listOf("M2", "M3", "M4")
internal val WORK_TYPE_VALUE_CELLS = listOf(
    listOf("N2", "O2", "P2"),
    listOf("N3", "O3", "P3"),
    listOf("N4", "O4", "P4")
)
internal val WORK_TYPE_TOTAL_CELLS = listOf("Q2", "Q3", "Q4")
internal val WORK_TYPE_VALUE_STYLES = listOf(PLAIN_TIME_STYLE, PLAIN_TIME_STYLE, PLAIN_TIME_STYLE)
internal val WORK_TYPE_TOTAL_STYLES = listOf(PLAIN_TIME_STYLE, PLAIN_TIME_STYLE, PLAIN_TIME_STYLE)

// Areas cleared before repopulating the sheet.
internal const val TOP_SUMMARY_CLEAR_START_COLUMN_INDEX = 4
internal const val TOP_SUMMARY_CLEAR_END_COLUMN_INDEX = 20
internal const val TOP_SUMMARY_CLEAR_START_ROW = 1
internal const val TOP_SUMMARY_CLEAR_END_ROW = 5

internal const val DAILY_ENTRIES_CLEAR_START_COLUMN_INDEX = 2 // B
internal const val DAILY_ENTRIES_CLEAR_END_COLUMN_INDEX = 32 // AF
internal val HEADER_CLEAR_CELLS = listOf("B2", "B3", "B4", "B5", "H1", "H2", "H3")
