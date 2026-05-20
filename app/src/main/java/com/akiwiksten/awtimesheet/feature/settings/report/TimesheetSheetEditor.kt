@file:Suppress("MagicNumber")

package com.akiwiksten.awtimesheet.feature.settings.report

import android.content.Context
import org.w3c.dom.Element
import java.io.ByteArrayInputStream

internal object TimesheetSheetEditor {
    fun updateSheet(sheetXml: ByteArray, exportData: TimesheetExportData, ctx: Context): ByteArray {
        val document = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(sheetXml))
        val sheetData = document.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetData")
            .item(0) as Element
        val dailyEntriesRowOffset = dailyEntriesRowOffset(exportData)

        clearDynamicCells(sheetData, dailyEntriesRowOffset)
        // Keep the full top summary area deterministic even when some populate* calls are disabled.
        clearTopSummaryArea(sheetData)
        TimesheetSectionWriter.populateHeader(document, sheetData, exportData)
        TimesheetSectionWriter.populateDayOfMonthRow(document, sheetData)
        TimesheetSectionWriter.populateDailyEntryLabels(document, sheetData, exportData, ctx)
        TimesheetSectionWriter.populateDailyEntries(document, sheetData, exportData, dailyEntriesRowOffset)
        TimesheetSectionWriter.populateProjectSummary(document, sheetData, exportData)
        TimesheetSectionWriter.populateAllowanceSummary(document, sheetData, exportData, ctx)
        TimesheetSectionWriter.populateWorkTypeSummary(document, sheetData, exportData, ctx)
        TimesheetFreezePaneEditor.ensureTopRowFrozen(document, sheetData)
        TimesheetFreezePaneEditor.ensureFirstColumnFrozen(document)

        return document.toByteArray()
    }

    private fun clearDynamicCells(sheetData: Element, dailyEntriesRowOffset: Int) {
        listOf("B2", "B3", "B4", "B5", "H1", "H2", "H3").forEach { cellReference ->
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
        for (columnIndex in 2..32) { // column 1 (A8) is the "Day of Month" label — keep it
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
        val startColumnIndex = 2
        val endColumnIndex = 32
        sheetData.childElementSequence("row")
            .mapNotNull { row -> row.getAttribute("r").toIntOrNull()?.let { rowNumber -> rowNumber to row } }
            .filter { (rowNumber, _) -> rowNumber in DAILY_ENTRIES_START_ROW..clearEndRow }
            .forEach { (_, row) ->
                row.childElementSequence("c").forEach { cell ->
                    val columnIndex = columnIndex(cell.getAttribute("r"))
                    if (columnIndex in startColumnIndex..endColumnIndex) {
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
