package com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections

import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.BOLD_TEXT_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRIES_SEPARATOR_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAY_OF_MONTH_VALUE_STYLE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.TEMPLATE_DAILY_ENTRY_BLOCKS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.dayToColumn
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.toExcelSerialDate
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.TimesheetXmlHelper
import org.w3c.dom.Document
import org.w3c.dom.Element

internal object TimesheetHeaderWriter {
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
        TimesheetXmlHelper.setStringCell(document, sheetData, "A6", exportData.flexTimeTotalLabel, BOLD_TEXT_STYLE)
        TimesheetXmlHelper.setStringCell(document, sheetData, "B6", exportData.totalFlexTimeTotal)
    }

    fun populateDayOfMonthRow(document: Document, sheetData: Element) {
        val maxDays = 31
        for (day in 1..maxDays) {
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
        exportData: TimesheetExportData
    ) {
        val maxEntriesOnAnyDay = exportData.displayedEntriesByDay.values.maxOfOrNull { it.size } ?: 0
        val blockCount = maxEntriesOnAnyDay.coerceAtLeast(TEMPLATE_DAILY_ENTRY_BLOCKS)
        val dailyEntryLabels = listOf(
            exportData.projectNameLabel,
            exportData.projectTimeLabel,
            exportData.allowanceLabel,
            exportData.workTypeLabel,
            exportData.kilometresLabel
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
}
