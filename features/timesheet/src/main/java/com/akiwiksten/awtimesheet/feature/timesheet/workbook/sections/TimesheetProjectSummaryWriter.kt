package com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections

import com.akiwiksten.awtimesheet.feature.timesheet.model.ProjectSummarySectionContext
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.BOLD_TEXT_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PLAIN_INTEGER_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PLAIN_TEXT_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PLAIN_TIME_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PROJECT_SUMMARY_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PROJECT_SUMMARY_KILOMETRES_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PROJECT_SUMMARY_START_COLUMN_INDEX
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PROJECT_SUMMARY_TOTAL_HEADER_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PROJECT_SUMMARY_TOTAL_KILOMETRES_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PROJECT_SUMMARY_TOTAL_WORK_TIME_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PROJECT_SUMMARY_WORK_TIME_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.SUMMARY_KILOMETRES_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.SUMMARY_LABEL_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.SUMMARY_WORK_TIME_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.buildCellReference
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.columnIndexToLetters
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.projectSummaryTotalColumnIndex
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.toExcelTimeFractionNumberString
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.TimesheetXmlHelper
import org.w3c.dom.Document
import org.w3c.dom.Element

internal object TimesheetProjectSummaryWriter {

    fun populateProjectSummary(document: Document, sheetData: Element, exportData: TimesheetExportData) {
        val allProjectNames = exportData.summaryProjectNames + exportData.hiddenProjectNames
        val projectCount = allProjectNames.size
        val totalIndex = projectSummaryTotalColumnIndex(projectCount)
        val context = ProjectSummarySectionContext(
            document = document,
            sheetData = sheetData,
            exportData = exportData,
            labelColumnIndex = PROJECT_SUMMARY_START_COLUMN_INDEX - 1,
            projectSummaryColumns = (0 until projectCount).map { offset ->
                columnIndexToLetters(PROJECT_SUMMARY_START_COLUMN_INDEX + offset)
            },
            totalColumnLetters = columnIndexToLetters(totalIndex),
            startColumnIndex = PROJECT_SUMMARY_START_COLUMN_INDEX - 1,
            endColumnIndex = totalIndex
        )

        writeProjectSummaryLabels(context)
        writeProjectSummaryProjects(context, allProjectNames)
        writeProjectSummaryTotals(context)
        applyProjectSummaryStyles(context)
    }

    private fun writeProjectSummaryLabels(context: ProjectSummarySectionContext) {
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, SUMMARY_LABEL_ROW),
            value = context.exportData.generalLabel,
            styleIndex = BOLD_TEXT_STYLE
        )
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, SUMMARY_WORK_TIME_ROW),
            value = context.exportData.workTimeTotalLabel,
            styleIndex = PLAIN_TEXT_STYLE
        )
        TimesheetXmlHelper.setStringCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, SUMMARY_KILOMETRES_ROW),
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
                cellReference = "${columnLetters}$SUMMARY_LABEL_ROW",
                value = projectName,
                styleIndex = PROJECT_SUMMARY_HEADER_STYLE
            )
            TimesheetXmlHelper.setNumericCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = "${columnLetters}$SUMMARY_WORK_TIME_ROW",
                numericValue = context.exportData
                    .summaryProjectTimes
                    .getValue(projectName)
                    .toExcelTimeFractionNumberString(),
                styleIndex = PROJECT_SUMMARY_WORK_TIME_STYLE
            )
            TimesheetXmlHelper.setNumericCell(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = "${columnLetters}$SUMMARY_KILOMETRES_ROW",
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
            cellReference = "${context.totalColumnLetters}$SUMMARY_LABEL_ROW",
            value = context.exportData.totalLabel,
            styleIndex = PROJECT_SUMMARY_TOTAL_HEADER_STYLE
        )
        TimesheetXmlHelper.setNumericCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = "${context.totalColumnLetters}$SUMMARY_WORK_TIME_ROW",
            numericValue = context.exportData.totalWorkTime.toExcelTimeFractionNumberString(),
            styleIndex = PROJECT_SUMMARY_TOTAL_WORK_TIME_STYLE
        )
        TimesheetXmlHelper.setNumericCell(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = "${context.totalColumnLetters}$SUMMARY_KILOMETRES_ROW",
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
                cellReference = buildCellReference(columnIndex, SUMMARY_WORK_TIME_ROW),
                styleIndex = PLAIN_TIME_STYLE
            )
            TimesheetXmlHelper.setCellStyle(
                document = context.document,
                sheetData = context.sheetData,
                cellReference = buildCellReference(columnIndex, SUMMARY_KILOMETRES_ROW),
                styleIndex = PLAIN_INTEGER_STYLE
            )
        }
        TimesheetXmlHelper.setCellStyle(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, SUMMARY_WORK_TIME_ROW),
            styleIndex = PLAIN_TEXT_STYLE
        )
        TimesheetXmlHelper.setCellStyle(
            document = context.document,
            sheetData = context.sheetData,
            cellReference = buildCellReference(context.labelColumnIndex, SUMMARY_KILOMETRES_ROW),
            styleIndex = PLAIN_TEXT_STYLE
        )
    }
}
