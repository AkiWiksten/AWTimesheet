package com.akiwiksten.awtimesheet.feature.timesheet.workbook

import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections.TimesheetCategorizedSummaryWriter
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections.TimesheetDailyEntryWriter
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections.TimesheetFreezePaneEditor
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections.TimesheetHeaderWriter
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections.TimesheetProjectSummaryWriter
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.ALLOWANCE_HEADER_CELLS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.ALLOWANCE_LABEL_CELLS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRIES_CLEAR_END_COLUMN_INDEX
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRIES_CLEAR_START_COLUMN_INDEX
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRIES_SEPARATOR_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRIES_START_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.HEADER_CLEAR_CELLS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PROJECT_KILOMETRES_SUMMARY_CELLS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PROJECT_NAME_HEADER_CELLS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.PROJECT_TIME_SUMMARY_CELLS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.TOP_SUMMARY_CLEAR_END_COLUMN_INDEX
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.TOP_SUMMARY_CLEAR_END_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.TOP_SUMMARY_CLEAR_START_COLUMN_INDEX
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.TOP_SUMMARY_CLEAR_START_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.WORK_TYPE_HEADER_CELLS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.WORK_TYPE_LABEL_CELLS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.WORK_TYPE_TOTAL_CELLS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.WORK_TYPE_VALUE_CELLS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.buildCellReference
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.SPREADSHEET_NAMESPACE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.TimesheetXmlHelper
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.childElementSequence
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.columnIndex
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.createDocumentBuilderFactory
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.toByteArray
import org.w3c.dom.Element
import java.io.ByteArrayInputStream

internal object TimesheetSheetEditor {
    fun updateSheet(sheetXml: ByteArray, exportData: TimesheetExportData): ByteArray {
        val document = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(sheetXml))
        val sheetData = document.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetData")
            .item(0) as Element
        val dailyEntriesRowOffset = dailyEntriesRowOffset(exportData)

        clearDynamicCells(sheetData, dailyEntriesRowOffset)
        // Keep the full top summary area deterministic even when some populate* calls are disabled.
        clearTopSummaryArea(sheetData)
        TimesheetHeaderWriter.populateHeader(document, sheetData, exportData)
        TimesheetHeaderWriter.populateDayOfMonthRow(document, sheetData)
        TimesheetHeaderWriter.populateDailyEntryLabels(document, sheetData, exportData)
        TimesheetDailyEntryWriter.populateDailyEntries(document, sheetData, exportData, dailyEntriesRowOffset)
        TimesheetProjectSummaryWriter.populateProjectSummary(document, sheetData, exportData)
        TimesheetCategorizedSummaryWriter.populateAllowanceSummary(document, sheetData, exportData)
        TimesheetCategorizedSummaryWriter.populateWorkTypeSummary(document, sheetData, exportData)
        TimesheetFreezePaneEditor.ensureTopRowFrozen(document, sheetData)
        TimesheetFreezePaneEditor.ensureFirstColumnFrozen(document)

        return document.toByteArray()
    }

    private fun clearDynamicCells(sheetData: Element, dailyEntriesRowOffset: Int) {
        HEADER_CLEAR_CELLS.forEach { cellReference ->
            TimesheetXmlHelper.clearCell(sheetData, cellReference)
        }
        PROJECT_NAME_HEADER_CELLS.forEach { TimesheetXmlHelper.clearCell(sheetData, it) }
        PROJECT_TIME_SUMMARY_CELLS.forEach { TimesheetXmlHelper.clearCell(sheetData, it) }
        PROJECT_KILOMETRES_SUMMARY_CELLS.forEach { TimesheetXmlHelper.clearCell(sheetData, it) }
        ALLOWANCE_HEADER_CELLS.forEach { TimesheetXmlHelper.clearCell(sheetData, it) }
        ALLOWANCE_LABEL_CELLS.forEach { TimesheetXmlHelper.clearCell(sheetData, it) }
        WORK_TYPE_HEADER_CELLS.forEach { TimesheetXmlHelper.clearCell(sheetData, it) }
        WORK_TYPE_LABEL_CELLS.forEach { TimesheetXmlHelper.clearCell(sheetData, it) }
        WORK_TYPE_VALUE_CELLS.flatten().forEach { TimesheetXmlHelper.clearCell(sheetData, it) }
        WORK_TYPE_TOTAL_CELLS.forEach { TimesheetXmlHelper.clearCell(sheetData, it) }
        for (columnIndex in DAILY_ENTRIES_CLEAR_START_COLUMN_INDEX..DAILY_ENTRIES_CLEAR_END_COLUMN_INDEX) {
            TimesheetXmlHelper.clearCell(sheetData, buildCellReference(columnIndex, DAILY_ENTRIES_SEPARATOR_ROW))
        }

        clearDailyEntriesArea(sheetData, dailyEntriesRowOffset)
    }

    private fun clearDailyEntriesArea(sheetData: Element, dailyEntriesRowOffset: Int) {
        val lastDefinedRow = sheetData.childElementSequence("row")
            .mapNotNull { row -> row.getAttribute("r").toIntOrNull() }
            .maxOrNull()
            ?: return
        val clearEndRow = maxOf(lastDefinedRow, lastDefinedRow + dailyEntriesRowOffset)
        if (clearEndRow < DAILY_ENTRIES_START_ROW) {
            return
        }

        // Clear only existing B..AF daily-entry cells to avoid repeated lookup work.
        sheetData.childElementSequence("row")
            .mapNotNull { row -> row.getAttribute("r").toIntOrNull()?.let { rowNumber -> rowNumber to row } }
            .filter { (rowNumber, _) -> rowNumber in DAILY_ENTRIES_START_ROW..clearEndRow }
            .forEach { (_, row) ->
                row.childElementSequence("c").forEach { cell ->
                    val columnIndex = columnIndex(cell.getAttribute("r"))
                    if (columnIndex in DAILY_ENTRIES_CLEAR_START_COLUMN_INDEX..DAILY_ENTRIES_CLEAR_END_COLUMN_INDEX) {
                        TimesheetXmlHelper.clearCell(sheetData, cell.getAttribute("r"))
                    }
                }
            }
    }

    private fun clearTopSummaryArea(sheetData: Element) {
        for (row in TOP_SUMMARY_CLEAR_START_ROW..TOP_SUMMARY_CLEAR_END_ROW) {
            for (columnIndex in TOP_SUMMARY_CLEAR_START_COLUMN_INDEX..TOP_SUMMARY_CLEAR_END_COLUMN_INDEX) {
                TimesheetXmlHelper.clearCell(sheetData, buildCellReference(columnIndex, row))
            }
        }
    }

    fun dailyEntriesRowOffset(exportData: TimesheetExportData): Int {
        val workTypeLastRow = exportData.workTypeRows.size + 1
        return if (workTypeLastRow == DAILY_ENTRIES_START_ROW - 1) 1 else 0
    }
}
