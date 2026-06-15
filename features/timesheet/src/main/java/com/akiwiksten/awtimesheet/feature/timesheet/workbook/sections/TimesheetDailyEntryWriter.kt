package com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections

import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRY_ALLOWANCE_ROW_OFFSET
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRY_COMMENT_ROW_OFFSET
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRY_KILOMETRES_ROW_OFFSET
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRY_NAME_ROW_OFFSET
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRY_TIME_ROW_OFFSET
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.DAILY_ENTRY_WORK_TYPE_ROW_OFFSET
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.dayToColumn
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.toHourMinuteString
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.toMinutesOrNull
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.TimesheetXmlHelper
import org.w3c.dom.Document
import org.w3c.dom.Element

internal object TimesheetDailyEntryWriter {
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
                TimesheetXmlHelper.setStringCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + DAILY_ENTRY_NAME_ROW_OFFSET}",
                    value = entry.projectName
                )
                TimesheetXmlHelper.setStringCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + DAILY_ENTRY_TIME_ROW_OFFSET}",
                    value = entry.projectTime
                )
                TimesheetXmlHelper.setStringCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + DAILY_ENTRY_ALLOWANCE_ROW_OFFSET}",
                    value = entry.allowanceLabel
                )
                TimesheetXmlHelper.setStringCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + DAILY_ENTRY_WORK_TYPE_ROW_OFFSET}",
                    value = entry.workType
                )
                TimesheetXmlHelper.setStringCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + DAILY_ENTRY_COMMENT_ROW_OFFSET}",
                    value = entry.comment
                )
                TimesheetXmlHelper.setNumericCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + DAILY_ENTRY_KILOMETRES_ROW_OFFSET}",
                    numericValue = (entry.kilometres.toLongOrNull() ?: 0L).toString()
                )
            }
        }
    }
}
