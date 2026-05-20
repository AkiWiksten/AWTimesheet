@file:Suppress("MagicNumber", "TooManyFunctions")

package com.akiwiksten.awtimesheet.feature.settings.timesheet.workbook

import android.content.Context
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.ALLOWANCE_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.ALLOWANCE_PROJECT_VALUE_STYLES
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.ALLOWANCE_TOTAL_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.ALLOWANCE_TOTAL_VALUE_STYLES
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.BOLD_TEXT_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.DAILY_ENTRIES_SEPARATOR_ROW
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.DAILY_ENTRIES_START_ROW
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.DAY_OF_MONTH_VALUE_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PLAIN_INTEGER_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PLAIN_TEXT_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PLAIN_TIME_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PROJECT_SUMMARY_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PROJECT_SUMMARY_KILOMETRES_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PROJECT_SUMMARY_START_COLUMN_INDEX
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PROJECT_SUMMARY_TOTAL_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PROJECT_SUMMARY_TOTAL_KILOMETRES_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PROJECT_SUMMARY_TOTAL_WORK_TIME_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PROJECT_SUMMARY_WORK_TIME_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.TEMPLATE_DAILY_ENTRY_BLOCKS
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.WORK_TYPE_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.WORK_TYPE_TOTAL_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.WORK_TYPE_TOTAL_STYLES
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.WORK_TYPE_VALUE_STYLES
import com.akiwiksten.awtimesheet.feature.settings.timesheet.model.AllowanceSectionContext
import com.akiwiksten.awtimesheet.feature.settings.timesheet.model.ProjectSummarySectionContext
import com.akiwiksten.awtimesheet.feature.settings.timesheet.model.SectionBodyStyleSpec
import com.akiwiksten.awtimesheet.feature.settings.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.settings.timesheet.model.WorkTypeSectionContext
import com.akiwiksten.awtimesheet.feature.settings.timesheet.workbook.xml.TimesheetXmlHelper
import com.akiwiksten.awtimesheet.feature.settings.timesheet.workbook.xml.safeString
import org.w3c.dom.Document
import org.w3c.dom.Element

internal object TimesheetSectionWriter {
    fun populateHeader(document: Document, sheetData: Element, exportData: TimesheetExportData) {
        TimesheetXmlHelper.setStringCell(document, sheetData, "B2", exportData.name)
        TimesheetXmlHelper.setStringCell(document, sheetData, "B3", exportData.employer)
        TimesheetXmlHelper.setNumericCell(
            document = document,
            sheetData = sheetData,
            cellReference = "B4",
            numericValue = exportData.startDate.toExcelSerialDate().toString()
        )
        TimesheetXmlHelper.setNumericCell(
            document = document,
            sheetData = sheetData,
            cellReference = "B5",
            numericValue = exportData.endDate.toExcelSerialDate().toString()
        )
        // Add Flex time total at the bottom of header.
        // B6 is stored as a plain HH:mm string so it renders correctly regardless of the
        // viewer's number-format support (and avoids the fraction>1 double-division bug).
        TimesheetXmlHelper.setStringCell(document, sheetData, "A6", exportData.flexTimeTotalLabel, BOLD_TEXT_STYLE)
        TimesheetXmlHelper.setStringCell(document, sheetData, "B6", exportData.totalFlexTimeTotal)
    }

    fun populateDayOfMonthRow(document: Document, sheetData: Element) {
        for (day in 1..31) {
            TimesheetXmlHelper.setNumericCell(
                document = document,
                sheetData = sheetData,
                cellReference = "${dayToColumn(day)}$DAILY_ENTRIES_SEPARATOR_ROW",
                numericValue = day.toString(),
                styleIndex = DAY_OF_MONTH_VALUE_STYLE
            )
        }
    }

    fun populateDailyEntryLabels(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData,
        ctx: Context
    ) {
        val maxEntriesOnAnyDay = exportData.displayedEntriesByDay.values.maxOfOrNull { it.size } ?: 0
        val blockCount = maxEntriesOnAnyDay.coerceAtLeast(TEMPLATE_DAILY_ENTRY_BLOCKS)
        val dailyEntryLabels = listOf(
            ctx.safeString(R.string.project_name, "Project name"),
            ctx.safeString(R.string.project_time, "Project time"),
            ctx.safeString(R.string.allowance, "Allowance"),
            ctx.safeString(R.string.work_type, "Work type"),
            ctx.safeString(R.string.kilometres, "Kilometres")
        )

        for (entryIndex in 0 until blockCount) {
            val baseRow = TimesheetXmlHelper.dailyEntryBaseRow(entryIndex)
            TimesheetXmlHelper.setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = "A${baseRow + 2}",
                value = dailyEntryLabels[0],
                styleIndex = BOLD_TEXT_STYLE
            )
            TimesheetXmlHelper.setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = "A${baseRow + 3}",
                value = dailyEntryLabels[1],
                styleIndex = BOLD_TEXT_STYLE
            )
            TimesheetXmlHelper.setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = "A${baseRow + 4}",
                value = dailyEntryLabels[2],
                styleIndex = BOLD_TEXT_STYLE
            )
            TimesheetXmlHelper.setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = "A${baseRow + 5}",
                value = dailyEntryLabels[3],
                styleIndex = BOLD_TEXT_STYLE
            )
            TimesheetXmlHelper.setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = "A${baseRow + 6}",
                value = dailyEntryLabels[4],
                styleIndex = BOLD_TEXT_STYLE
            )
            if (entryIndex > 0) {
                TimesheetXmlHelper.setStringCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "A${baseRow + 1}",
                    value = "",
                    styleIndex = BOLD_TEXT_STYLE
                )
            }
        }
    }

    fun populateDailyEntries(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData,
        dailyEntriesRowOffset: Int
    ) {
        exportData.displayedEntriesByDay.forEach { (day, dayEntries) ->
            val column = dayToColumn(day)
            val firstEntryBaseRow = TimesheetXmlHelper.dailyEntryBaseRow(0) + dailyEntriesRowOffset
            val dailyTotalMinutes = dayEntries.sumOf { it.projectTime.toMinutesOrNull() ?: 0L }
            TimesheetXmlHelper.setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = "$column$firstEntryBaseRow",
                value = dailyTotalMinutes.toHourMinuteString()
            )
            for ((index, entry) in dayEntries.withIndex()) {
                val baseRow = TimesheetXmlHelper.dailyEntryBaseRow(index) + dailyEntriesRowOffset
                TimesheetXmlHelper.setStringCell(document, sheetData, "$column${baseRow + 2}", entry.projectName)
                TimesheetXmlHelper.setStringCell(document, sheetData, "$column${baseRow + 3}", entry.projectTime)
                TimesheetXmlHelper.setStringCell(document, sheetData, "$column${baseRow + 4}", entry.allowanceLabel)
                TimesheetXmlHelper.setStringCell(document, sheetData, "$column${baseRow + 5}", entry.workType)
                TimesheetXmlHelper.setNumericCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + 6}",
                    numericValue = (entry.kilometres.toLongOrNull() ?: 0L).toString()
                )
            }
        }
    }

    fun populateProjectSummary(document: Document, sheetData: Element, exportData: TimesheetExportData) {
        val allProjectNames = exportData.summaryProjectNames + exportData.hiddenProjectNames
        val context = ProjectSummarySectionContext(
            document = document,
            sheetData = sheetData,
            exportData = exportData,
            labelColumnIndex = PROJECT_SUMMARY_START_COLUMN_INDEX - 1,
            projectSummaryColumns = projectSummaryColumnLetters(allProjectNames.size),
            totalColumnLetters = projectSummaryTotalColumnLetters(allProjectNames.size),
            startColumnIndex = PROJECT_SUMMARY_START_COLUMN_INDEX - 1,
            endColumnIndex = projectSummaryTotalColumnIndex(allProjectNames.size)
        )

        writeProjectSummaryLabels(context)
        writeProjectSummaryProjects(context, allProjectNames)
        writeProjectSummaryTotals(context)
        applyProjectSummaryStyles(context)
    }

    fun populateAllowanceSummary(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData,
        ctx: Context
    ) {
        val allProjectNames = exportData.summaryProjectNames + exportData.hiddenProjectNames
        val startColumnIndex = allowanceStartColumnIndex(allProjectNames.size)
        val context = AllowanceSectionContext(
            document = document,
            sheetData = sheetData,
            exportData = exportData,
            allProjectNames = allProjectNames,
            labelColumnIndex = allowanceLabelColumnIndex(allProjectNames.size),
            startColumnIndex = startColumnIndex,
            totalColumnIndex = startColumnIndex + allProjectNames.size
        )

        writeAllowanceHeader(context, ctx)
        writeAllowanceRows(context)

        TimesheetXmlHelper.applySectionHeaderStyles(
            document = document,
            sheetData = sheetData,
            labelColumnIndex = context.labelColumnIndex,
            startColumnIndex = context.labelColumnIndex,
            endColumnIndex = context.totalColumnIndex
        )
        TimesheetXmlHelper.applySectionBodyStyles(
            document = document,
            sheetData = sheetData,
            spec = SectionBodyStyleSpec(
                labelColumnIndex = context.labelColumnIndex,
                valueColumnRange = context.startColumnIndex..context.totalColumnIndex,
                rowRange = 2..4,
                valueStyle = PLAIN_INTEGER_STYLE
            )
        )
    }

    fun populateWorkTypeSummary(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData,
        ctx: Context
    ) {
        val allProjectNames = exportData.summaryProjectNames + exportData.hiddenProjectNames
        val startColumnIndex = workTypeLabelColumnIndex(allProjectNames.size) + 1
        val context = WorkTypeSectionContext(
            document = document,
            sheetData = sheetData,
            exportData = exportData,
            allProjectNames = allProjectNames,
            labelColumnIndex = workTypeLabelColumnIndex(allProjectNames.size),
            startColumnIndex = startColumnIndex,
            totalColumnIndex = startColumnIndex + allProjectNames.size
        )

        writeWorkTypeHeader(context, ctx)
        writeWorkTypeRows(context, sheetData)

        TimesheetXmlHelper.applySectionHeaderStyles(
            document = document,
            sheetData = sheetData,
            labelColumnIndex = context.labelColumnIndex,
            startColumnIndex = context.labelColumnIndex,
            endColumnIndex = context.totalColumnIndex
        )
        TimesheetXmlHelper.applySectionBodyStyles(
            document = document,
            sheetData = sheetData,
            spec = SectionBodyStyleSpec(
                labelColumnIndex = context.labelColumnIndex,
                valueColumnRange = context.startColumnIndex..context.totalColumnIndex,
                rowRange = 2..(context.exportData.workTypeRows.size + 1),
                valueStyle = PLAIN_TIME_STYLE
            )
        )
    }

    private fun writeProjectSummaryLabels(context: ProjectSummarySectionContext) {
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, 1),
            value = context.exportData.generalLabel,
            styleIndex = BOLD_TEXT_STYLE
        )
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, 2),
            value = context.exportData.workTimeTotalLabel,
            styleIndex = PLAIN_TEXT_STYLE
        )
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, 3),
            value = context.exportData.kilometresLabel,
            styleIndex = PLAIN_TEXT_STYLE
        )
    }

    private fun writeProjectSummaryProjects(context: ProjectSummarySectionContext, allProjectNames: List<String>) {
        allProjectNames.forEachIndexed { index, projectName ->
            val columnLetters = context.projectSummaryColumns[index]
            TimesheetXmlHelper.setStringCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = "${columnLetters}1",
                value = projectName,
                styleIndex = PROJECT_SUMMARY_HEADER_STYLE
            )
            TimesheetXmlHelper.setNumericCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = "${columnLetters}2",
                numericValue = context.exportData
                    .summaryProjectTimes
                    .getValue(projectName)
                    .toExcelTimeFractionNumberString(),
                styleIndex = PROJECT_SUMMARY_WORK_TIME_STYLE
            )
            TimesheetXmlHelper.setNumericCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = "${columnLetters}3",
                numericValue = context.exportData
                    .summaryProjectKilometres
                    .getValue(projectName)
                    .toString(),
                styleIndex = PROJECT_SUMMARY_KILOMETRES_STYLE
            )
        }
    }

    private fun writeProjectSummaryTotals(context: ProjectSummarySectionContext) {
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = "${context.totalColumnLetters}1",
            value = context.exportData.totalLabel,
            styleIndex = PROJECT_SUMMARY_TOTAL_HEADER_STYLE
        )
        TimesheetXmlHelper.setNumericCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = "${context.totalColumnLetters}2",
            numericValue = context.exportData.totalWorkTime.toExcelTimeFractionNumberString(),
            styleIndex = PROJECT_SUMMARY_TOTAL_WORK_TIME_STYLE
        )
        TimesheetXmlHelper.setNumericCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = "${context.totalColumnLetters}3",
            numericValue = context.exportData.totalKilometres.toString(),
            styleIndex = PROJECT_SUMMARY_TOTAL_KILOMETRES_STYLE
        )
    }

    private fun applyProjectSummaryStyles(context: ProjectSummarySectionContext) {
        TimesheetXmlHelper.applySectionHeaderStyles(
            document = context.document,
            sheetData = context.sheetData,
            labelColumnIndex = context.labelColumnIndex,
            startColumnIndex = context.startColumnIndex,
            endColumnIndex = context.endColumnIndex
        )
        for (columnIndex in context.startColumnIndex..context.endColumnIndex) {
            TimesheetXmlHelper.setCellStyle(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = buildCellReference(columnIndex, 2),
                styleIndex = PLAIN_TIME_STYLE
            )
            TimesheetXmlHelper.setCellStyle(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = buildCellReference(columnIndex, 3),
                styleIndex = PLAIN_INTEGER_STYLE
            )
        }
        TimesheetXmlHelper.setCellStyle(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, 2),
            styleIndex = PLAIN_TEXT_STYLE
        )
        TimesheetXmlHelper.setCellStyle(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, 3),
            styleIndex = PLAIN_TEXT_STYLE
        )
    }

    private fun writeAllowanceHeader(context: AllowanceSectionContext, ctx: Context) {
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, 1),
            value = ctx.safeString(R.string.allowance, "Allowance"),
            styleIndex = BOLD_TEXT_STYLE
        )
        context.allProjectNames.forEachIndexed { index, projectName ->
            val columnLetters = columnIndexToLetters(context.startColumnIndex + index)
            TimesheetXmlHelper.setStringCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = "${columnLetters}1",
                value = projectName,
                styleIndex = ALLOWANCE_HEADER_STYLE
            )
        }
        val totalColumnLetters = columnIndexToLetters(context.totalColumnIndex)
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = "${totalColumnLetters}1",
            value = context.exportData.totalLabel,
            styleIndex = ALLOWANCE_TOTAL_HEADER_STYLE
        )
    }

    private fun writeAllowanceRows(context: AllowanceSectionContext) {
        context.exportData.allowanceRows.forEachIndexed { rowIndex, allowanceRow ->
            TimesheetXmlHelper.setStringCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = buildCellReference(context.labelColumnIndex, rowIndex + 2),
                value = allowanceRow.label,
                styleIndex = PLAIN_TEXT_STYLE
            )
            context.allProjectNames.forEachIndexed { columnIndex, projectName ->
                TimesheetXmlHelper.setNumericCell(
                    document = context.document,
                    sheetData = context.sheetData,
                    cellReference = buildCellReference(context.startColumnIndex + columnIndex, rowIndex + 2),
                    numericValue = allowanceRow.countByProjectName.getValue(projectName).toString(),
                    styleIndex = ALLOWANCE_PROJECT_VALUE_STYLES[rowIndex]
                )
            }
            TimesheetXmlHelper.setNumericCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = buildCellReference(context.totalColumnIndex, rowIndex + 2),
                numericValue = allowanceRow.totalCount.toString(),
                styleIndex = ALLOWANCE_TOTAL_VALUE_STYLES[rowIndex]
            )
        }
    }

    private fun writeWorkTypeHeader(context: WorkTypeSectionContext, ctx: Context) {
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, 1),
            value = ctx.safeString(R.string.work_type, "Work type"),
            styleIndex = BOLD_TEXT_STYLE
        )
        context.allProjectNames.forEachIndexed { index, projectName ->
            TimesheetXmlHelper.setStringCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = buildCellReference(context.startColumnIndex + index, 1),
                value = projectName,
                styleIndex = WORK_TYPE_HEADER_STYLE
            )
        }
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.totalColumnIndex, 1),
            value = context.exportData.totalLabel,
            styleIndex = WORK_TYPE_TOTAL_HEADER_STYLE
        )
    }

    private fun writeWorkTypeRows(context: WorkTypeSectionContext, sheetData: Element) {
        val additionalRowNumber =
            if (context.totalColumnIndex > 31) {
                DAILY_ENTRIES_START_ROW - 2 - context.exportData.workTypeRows.size
            } else {
                0
            }
        for (i in 0 until additionalRowNumber) {
            TimesheetXmlHelper.insertBlankRowRange(
                sheetData = sheetData,
                startColumn = 1,
                rowNumber = i + context.exportData.workTypeRows.size + 1,
                endColumn = context.totalColumnIndex
            )
        }
        context.exportData.workTypeRows.forEachIndexed { rowIndex, workTypeRow ->
            TimesheetXmlHelper.setStringCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = buildCellReference(context.labelColumnIndex, rowIndex + 2),
                value = workTypeRow.label,
                styleIndex = PLAIN_TEXT_STYLE
            )
            context.allProjectNames.forEachIndexed { columnIndex, projectName ->
                val value = workTypeRow.timeByProjectName.getValue(projectName)
                if (value > 0L) {
                    TimesheetXmlHelper.setNumericCell(
                        document = context.document,
                        sheetData = context.sheetData,
                        cellReference = buildCellReference(context.startColumnIndex + columnIndex, rowIndex + 2),
                        numericValue = value.toExcelTimeFractionNumberString(),
                        styleIndex = WORK_TYPE_VALUE_STYLES.getOrElse(rowIndex) { PLAIN_TIME_STYLE }
                    )
                }
            }
            if (workTypeRow.totalTime > 0L) {
                TimesheetXmlHelper.setNumericCell(
                    document = context.document,
                    sheetData = context.sheetData,
                    cellReference = buildCellReference(context.totalColumnIndex, rowIndex + 2),
                    numericValue = workTypeRow.totalTime.toExcelTimeFractionNumberString(),
                    styleIndex = WORK_TYPE_TOTAL_STYLES.getOrElse(rowIndex) { PLAIN_TIME_STYLE }
                )
            }
        }
    }
}
