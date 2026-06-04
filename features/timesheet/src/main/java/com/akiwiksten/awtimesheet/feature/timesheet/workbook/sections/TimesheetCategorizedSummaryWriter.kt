package com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections

import com.akiwiksten.awtimesheet.feature.timesheet.model.AllowanceSectionContext
import com.akiwiksten.awtimesheet.feature.timesheet.model.SectionBodyStyleSpec
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.timesheet.model.WorkTypeSectionContext
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.ALLOWANCE_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.ALLOWANCE_PROJECT_VALUE_STYLES
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.ALLOWANCE_TOTAL_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.ALLOWANCE_TOTAL_VALUE_STYLES
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.BOLD_TEXT_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRIES_START_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PLAIN_INTEGER_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PLAIN_TEXT_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PLAIN_TIME_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.WORK_TYPE_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.WORK_TYPE_TOTAL_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.WORK_TYPE_TOTAL_STYLES
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.WORK_TYPE_VALUE_STYLES
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.allowanceLabelColumnIndex
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.allowanceStartColumnIndex
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.buildCellReference
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.columnIndexToLetters
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.projectSummaryTotalColumnIndex
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.toExcelTimeFractionNumberString
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.workTypeLabelColumnIndex
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.TimesheetXmlHelper
import org.w3c.dom.Document
import org.w3c.dom.Element

internal object TimesheetCategorizedSummaryWriter {

    fun populateAllowanceSummary(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData
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

        writeAllowanceHeader(context)
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
        exportData: TimesheetExportData
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

        writeWorkTypeHeader(context)
        writeWorkTypeRows(context, sheetData, exportData)

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

    private fun writeAllowanceHeader(context: AllowanceSectionContext) {
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, 1),
            value = context.exportData.allowanceLabel,
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

    private fun writeWorkTypeHeader(context: WorkTypeSectionContext) {
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, 1),
            value = context.exportData.workTypeLabel,
            styleIndex = BOLD_TEXT_STYLE
        )
        context.allProjectNames.forEachIndexed { index, projectName ->
            val cellReference = buildCellReference(context.startColumnIndex + index, 1)
            TimesheetXmlHelper.setStringCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = cellReference,
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

    private fun writeWorkTypeRows(
        context: WorkTypeSectionContext,
        sheetData: Element,
        exportData: TimesheetExportData,
    ) {
        val additionalRowNumber =
            if (context.totalColumnIndex > 31) {
                (exportData.workTypeRows.size + 1) - (DAILY_ENTRIES_START_ROW - 3)
            } else {
                0
            }
        for (i in 0 until additionalRowNumber) {
            TimesheetXmlHelper.insertBlankRowRange(
                sheetData = sheetData,
                startColumn = 1,
                rowNumber = i + DAILY_ENTRIES_START_ROW - 2,
                endColumn = context.totalColumnIndex
            )
        }
        exportData.workTypeRows.forEachIndexed { rowIndex, workTypeRow ->
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
