@file:Suppress("MagicNumber", "TooManyFunctions")

package com.akiwiksten.awtimesheet.feature.settings.timesheet.entry

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.settings.timesheet.build.TimesheetExportDataBuilder
import com.akiwiksten.awtimesheet.feature.settings.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.settings.timesheet.workbook.TimesheetStorage
import com.akiwiksten.awtimesheet.feature.settings.timesheet.workbook.TimesheetWorkbookEditor

private const val TEMPLATE_ASSET_NAME = "timesheet_template.xlsx"
internal const val MAX_SUMMARY_PROJECTS = 3
internal const val DAILY_ENTRY_ROW_HEIGHT = 6
internal const val DAILY_ENTRIES_START_ROW = 9
internal const val DAILY_ENTRIES_SEPARATOR_ROW = DAILY_ENTRIES_START_ROW - 1
internal const val TEMPLATE_DAILY_ENTRY_BLOCKS = 1

// Workbook style ids.
internal const val PROJECT_SUMMARY_START_COLUMN_INDEX = 5 // E
private const val LOG_TAG = "TimesheetGeneratorEntry"

// Date cells B4/B5 keep template styles; avoid hardcoded style indices.
// Style indices match template cellXfs directly (template has 11 xf entries, 0-based):
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

// Public entry point.
object TimesheetGeneratorEntry {
    fun generateXlsx(params: GenerateTimesheetParams) {
        runCatching {
            val exportData = TimesheetExportDataBuilder.build(params)
            exportData.logIfTruncated()

            val templateBytes = params.ctx.assets.open(TEMPLATE_ASSET_NAME).use { it.readBytes() }
            val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
                templateBytes = templateBytes,
                exportData = exportData
            )

            TimesheetStorage.saveXlsx(
                ctx = params.ctx,
                workbook = workbookBytes,
                name = params.name,
                date = params.endOfMonthDate
            )
        }.onFailure { exception ->
            Log.e(LOG_TAG, "Failed to generate timesheet XLSX", exception)
            Toast.makeText(
                params.ctx,
                "Failed to generate XLSX: ${exception.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

// Input params.
data class GenerateTimesheetParams(
    val ctx: Context,
    val projectsByMonth: List<SingleProjectState>,
    val endOfMonthDate: String,
    val name: String,
    val employer: String,
    val defaultWorkTypeLabel: String,
    val noAllowanceSourceLabel: String,
    val halfDayAllowanceSourceLabel: String,
    val fullAllowanceSourceLabel: String,
    val noAllowanceExportLabel: String,
    val halfDayAllowanceExportLabel: String,
    val fullAllowanceExportLabel: String,
    val dayOfMonthLabel: String = "Day of Month",
    val projectNameLabel: String = "Project name",
    val workTimeByDateLabel: String = "Work time by date",
    val allowanceLabel: String = "Allowance",
    val workTypeLabel: String = "Work type",
    val employerLabel: String = "Employer",
    val nameLabel: String = "Name",
    val totalSumLabel: String = "TOTAL SUM",
    val startDateLabel: String = "Start date",
    val titleLabel: String = "Timesheet",
    val endDateLabel: String = "End date",
    val projectTimeLabel: String = "Project time",
    val totalLabel: String,
    val generalLabel: String,
    val workTimeTotalLabel: String,
    val kilometresLabel: String,
    val flexTimeTotalLabel: String,
    val totalFlexTimeTotal: String = ZERO_TIME
)

private fun TimesheetExportData.logIfTruncated() {
    if (overflowedDays.isEmpty() && hiddenProjectNames.isEmpty() && hiddenWorkTypes.isEmpty()) {
        return
    }
    Log.w(
        LOG_TAG,
        "Timesheet export truncated to template capacity. " +
            "overflowedDays=$overflowedDays, " +
            "hiddenProjects=$hiddenProjectNames, " +
            "hiddenWorkTypes=$hiddenWorkTypes"
    )
}
