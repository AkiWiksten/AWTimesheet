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
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.buildCellReference
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.projectSummaryColumnLetters
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.projectSummaryTotalColumnLetters
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.projectSummaryTotalColumnIndex
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.toExcelTimeFractionNumberString
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.TimesheetXmlHelper
import org.w3c.dom.Document
import org.w3c.dom.Element

internal object TimesheetProjectSummaryWriter {

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
}
