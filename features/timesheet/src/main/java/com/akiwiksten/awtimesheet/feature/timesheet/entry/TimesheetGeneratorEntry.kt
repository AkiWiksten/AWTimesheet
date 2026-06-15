package com.akiwiksten.awtimesheet.feature.timesheet.entry

import android.util.Log
import android.widget.Toast
import com.akiwiksten.awtimesheet.feature.timesheet.R
import com.akiwiksten.awtimesheet.feature.timesheet.mapper.TimesheetExportDataBuilder
import com.akiwiksten.awtimesheet.feature.timesheet.model.GenerateTimesheetParams
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.TimesheetStorage
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.TimesheetWorkbookEditor

private const val TEMPLATE_ASSET_NAME = "timesheet_template.xlsx"
private const val LOG_TAG = "TimesheetGeneratorEntry"

/**
 * Public entry point for generating and saving the timesheet XLSX file.
 */
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
            val errorMessage = exception.message?.takeIf { it.isNotBlank() }
                ?: params.ctx.getString(R.string.timesheet_generate_error_unknown)
            Toast.makeText(
                params.ctx,
                params.ctx.getString(R.string.timesheet_generate_failed, errorMessage),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

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
