@file:Suppress("MagicNumber", "TooManyFunctions")

package com.akiwiksten.awtimesheet.feature.settings.report

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private const val TEMPLATE_ASSET_NAME = "timesheet_template.xlsx"
private const val MAX_SUMMARY_PROJECTS = 3
private const val DAILY_ENTRY_ROW_HEIGHT = 6
private const val DAILY_ENTRIES_START_ROW = 9
private const val DAILY_ENTRIES_SEPARATOR_ROW = DAILY_ENTRIES_START_ROW - 1
private const val TEMPLATE_DAILY_ENTRY_BLOCKS = 1
private const val MINUTES_PER_DAY = 1440L

// Workbook style ids.
private const val PROJECT_SUMMARY_START_COLUMN_INDEX = 5 // E
private const val LOG_TAG = "TimesheetGenerator"
// Date cells B4/B5 keep template styles; avoid hardcoded style indices.
// Style indices match template cellXfs directly (template has 11 xf entries, 0-based):
//   0 = default plain text (no bold, no border)
//   1 = bold, no border
//   2 = bold + all thin borders          (A8 "Day of Month" label)
//   3 = bold + all thin borders + center  (B8-AF8 day-of-month numbers)
//   6 = hh:mm time format
//   7 = [hh]:mm cumulative time format
//   8 = integer number format
private const val PLAIN_TEXT_STYLE = 0
private const val BOLD_TEXT_STYLE = 1
private const val DAY_OF_MONTH_VALUE_STYLE = 3 // bold + border + center; B8-AF8
private const val PLAIN_TIME_STYLE = 18 // normalizes to 18-11=7 = [hh]:mm
private const val PLAIN_INTEGER_STYLE = 19 // normalizes to 19-11=8 = integer
private const val PROJECT_SUMMARY_HEADER_STYLE = PLAIN_TEXT_STYLE
private const val PROJECT_SUMMARY_WORK_TIME_STYLE = PLAIN_TIME_STYLE
private const val PROJECT_SUMMARY_KILOMETRES_STYLE = PLAIN_INTEGER_STYLE
private const val PROJECT_SUMMARY_TOTAL_HEADER_STYLE = PLAIN_TEXT_STYLE
private const val PROJECT_SUMMARY_TOTAL_WORK_TIME_STYLE = PLAIN_TIME_STYLE
private const val PROJECT_SUMMARY_TOTAL_KILOMETRES_STYLE = PLAIN_INTEGER_STYLE
private const val ALLOWANCE_HEADER_STYLE = PLAIN_TEXT_STYLE
private const val ALLOWANCE_TOTAL_HEADER_STYLE = PLAIN_TEXT_STYLE
private val ALLOWANCE_PROJECT_VALUE_STYLES = listOf(PLAIN_INTEGER_STYLE, PLAIN_INTEGER_STYLE, PLAIN_INTEGER_STYLE)
private val ALLOWANCE_TOTAL_VALUE_STYLES = listOf(PLAIN_INTEGER_STYLE, PLAIN_INTEGER_STYLE, PLAIN_INTEGER_STYLE)
private const val WORK_TYPE_HEADER_STYLE = PLAIN_TEXT_STYLE
private const val WORK_TYPE_TOTAL_HEADER_STYLE = PLAIN_TEXT_STYLE

// Fixed template layout cells.
private val PROJECT_NAME_HEADER_CELLS = listOf("E1", "F1", "G1")
private val PROJECT_TIME_SUMMARY_CELLS = listOf("E2", "F2", "G2")
private val PROJECT_KILOMETRES_SUMMARY_CELLS = listOf("E3", "F3", "G3")
private val ALLOWANCE_HEADER_CELLS = listOf("H1", "I1", "J1", "K1")
private val ALLOWANCE_LABEL_CELLS = listOf("G2", "G3", "G4")
private val WORK_TYPE_HEADER_CELLS = listOf("N1", "O1", "P1", "Q1")
private val WORK_TYPE_LABEL_CELLS = listOf("M2", "M3", "M4")
private val WORK_TYPE_VALUE_CELLS = listOf(
    listOf("N2", "O2", "P2"),
    listOf("N3", "O3", "P3"),
    listOf("N4", "O4", "P4")
)
private val WORK_TYPE_TOTAL_CELLS = listOf("Q2", "Q3", "Q4")
private val WORK_TYPE_VALUE_STYLES = listOf(PLAIN_TIME_STYLE, PLAIN_TIME_STYLE, PLAIN_TIME_STYLE)
private val WORK_TYPE_TOTAL_STYLES = listOf(PLAIN_TIME_STYLE, PLAIN_TIME_STYLE, PLAIN_TIME_STYLE)
private val ALLOWANCE_ORDER = listOf(
    TimesheetAllowanceType.NONE,
    TimesheetAllowanceType.HALF_DAY,
    TimesheetAllowanceType.FULL
)

// Areas cleared before repopulating the sheet.
private const val TOP_SUMMARY_CLEAR_START_COLUMN_INDEX = 4
private const val TOP_SUMMARY_CLEAR_END_COLUMN_INDEX = 20
private const val TOP_SUMMARY_CLEAR_START_ROW = 1
private const val TOP_SUMMARY_CLEAR_END_ROW = 5

private data class TimesheetEntryAggregates(
    val summaryProjectTimes: Map<String, Long>,
    val summaryProjectKilometres: Map<String, Long>,
    val allowanceCountsByProjectAndType: Map<Pair<String, TimesheetAllowanceType>, Int>,
    val allowanceTotalCountsByType: Map<TimesheetAllowanceType, Int>,
    val workTypeTimeByProjectAndType: Map<Pair<String, String>, Long>,
    val workTypeTotalTime: Map<String, Long>,
    val totalWorkTime: Long,
    val totalKilometres: Long
)

private data class TimesheetDisplayData(
    val displayedEntriesByDay: Map<Int, List<TimesheetEntry>>,
    val overflowedDays: List<Int>
)

private data class ProjectSummarySectionContext(
    val document: Document,
    val sheetData: Element,
    val exportData: TimesheetExportData,
    val labelColumnIndex: Int,
    val projectSummaryColumns: List<String>,
    val totalColumnLetters: String,
    val startColumnIndex: Int,
    val endColumnIndex: Int
)

private data class AllowanceSectionContext(
    val document: Document,
    val sheetData: Element,
    val exportData: TimesheetExportData,
    val allProjectNames: List<String>,
    val labelColumnIndex: Int,
    val startColumnIndex: Int,
    val totalColumnIndex: Int
)

private data class WorkTypeSectionContext(
    val document: Document,
    val sheetData: Element,
    val exportData: TimesheetExportData,
    val allProjectNames: List<String>,
    val labelColumnIndex: Int,
    val startColumnIndex: Int,
    val totalColumnIndex: Int
)

private data class SectionBodyStyleSpec(
    val labelColumnIndex: Int,
    val valueColumnRange: IntRange,
    val rowRange: IntRange,
    val valueStyle: Int
)

// Public entry point.
object TimesheetGenerator {
    fun generateXlsx(params: GenerateTimesheetParams) {
        runCatching {
            val exportData = TimesheetExportDataBuilder.build(params)
            exportData.logIfTruncated()

            val templateBytes = params.ctx.assets.open(TEMPLATE_ASSET_NAME).use { it.readBytes() }
            val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
                templateBytes = templateBytes,
                exportData = exportData,
                ctx = params.ctx
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
    val totalLabel: String,
    val generalLabel: String,
    val workTimeTotalLabel: String,
    val kilometresLabel: String,
    val flexTimeTotalLabel: String,
    val totalFlexTimeTotal: String = ZERO_TIME
)

internal object TimesheetExportDataBuilder {
    fun build(params: GenerateTimesheetParams): TimesheetExportData {
        val labels = params.toTimesheetLabels()
        val endDate = LocalDate.parse(params.endOfMonthDate)
        val startDate = endDate.withDayOfMonth(1)
        val entries = params.projectsByMonth.toSortedTimesheetEntries(labels)

        val allProjectNames = entries.allDistinctProjectNames()
        val summaryProjectNames = allProjectNames.take(MAX_SUMMARY_PROJECTS)
        val allWorkTypes = entries.allDistinctWorkTypes()
        val summaryWorkTypes = allWorkTypes
        val displayData = createDisplayData(entries)
        val aggregates = aggregateEntries(entries, allProjectNames)

        val allowanceRows = buildAllowanceRows(
            allProjectNames = allProjectNames,
            labels = labels,
            allowanceCountsByProjectAndType = aggregates.allowanceCountsByProjectAndType,
            allowanceTotalCountsByType = aggregates.allowanceTotalCountsByType
        )
        val workTypeRows = buildWorkTypeRows(
            allProjectNames = allProjectNames,
            summaryWorkTypes = summaryWorkTypes,
            workTypeTimeByProjectAndType = aggregates.workTypeTimeByProjectAndType,
            workTypeTotalTime = aggregates.workTypeTotalTime
        )

        return TimesheetExportData(
            name = params.name,
            employer = params.employer,
            startDate = startDate,
            endDate = endDate,
            totalLabel = params.totalLabel,
            generalLabel = params.generalLabel,
            workTimeTotalLabel = params.workTimeTotalLabel,
            kilometresLabel = params.kilometresLabel,
            summaryProjectNames = summaryProjectNames,
            summaryProjectTimes = aggregates.summaryProjectTimes,
            summaryProjectKilometres = aggregates.summaryProjectKilometres,
            totalWorkTime = aggregates.totalWorkTime,
            totalKilometres = aggregates.totalKilometres,
            allowanceRows = allowanceRows,
            workTypeRows = workTypeRows,
            displayedEntriesByDay = displayData.displayedEntriesByDay,
            overflowedDays = displayData.overflowedDays,
            hiddenProjectNames = allProjectNames.drop(MAX_SUMMARY_PROJECTS),
            hiddenWorkTypes = emptyList(),
            flexTimeTotalLabel = params.flexTimeTotalLabel,
            totalFlexTimeTotal = params.totalFlexTimeTotal
        )
    }

    private fun createDisplayData(entries: List<TimesheetEntry>): TimesheetDisplayData {
        val entriesByDay = entries.groupBy { it.dayOfMonth }
        return TimesheetDisplayData(
            displayedEntriesByDay = entriesByDay,
            overflowedDays = emptyList()
        )
    }

    private fun aggregateEntries(
        entries: List<TimesheetEntry>,
        allProjectNames: List<String>
    ): TimesheetEntryAggregates {
        val summaryProjectTimes = allProjectNames.associateWith { 0L }.toMutableMap()
        val summaryProjectKilometres = allProjectNames.associateWith { 0L }.toMutableMap()
        val allowanceCountsByProjectAndType = mutableMapOf<Pair<String, TimesheetAllowanceType>, Int>()
        val allowanceTotalCountsByType = mutableMapOf<TimesheetAllowanceType, Int>()
        val workTypeTimeByProjectAndType = mutableMapOf<Pair<String, String>, Long>()
        val workTypeTotalTime = mutableMapOf<String, Long>()
        var totalWorkTime = 0L
        var totalKilometres = 0L

        entries.forEach { entry ->
            val projectTimeMinutes = entry.projectTime.toMinutesOrNull() ?: return@forEach
            val kilometres = entry.kilometres.toLongOrNull() ?: 0L
            if (entry.projectName in summaryProjectTimes) {
                summaryProjectTimes.compute(entry.projectName) { _, current ->
                    (current ?: 0L) + projectTimeMinutes
                }
                summaryProjectKilometres.compute(entry.projectName) { _, current ->
                    (current ?: 0L) + kilometres
                }
            }
            allowanceCountsByProjectAndType.compute(entry.projectName to entry.allowanceType) { _, current ->
                (current ?: 0) + 1
            }
            allowanceTotalCountsByType.compute(entry.allowanceType) { _, current ->
                (current ?: 0) + 1
            }
            workTypeTimeByProjectAndType.compute(entry.projectName to entry.workType) { _, current ->
                (current ?: 0L) + projectTimeMinutes
            }
            workTypeTotalTime.compute(entry.workType) { _, current ->
                (current ?: 0L) + projectTimeMinutes
            }
            totalWorkTime += projectTimeMinutes
            totalKilometres += kilometres
        }

        return TimesheetEntryAggregates(
            summaryProjectTimes = summaryProjectTimes,
            summaryProjectKilometres = summaryProjectKilometres,
            allowanceCountsByProjectAndType = allowanceCountsByProjectAndType,
            allowanceTotalCountsByType = allowanceTotalCountsByType,
            workTypeTimeByProjectAndType = workTypeTimeByProjectAndType,
            workTypeTotalTime = workTypeTotalTime,
            totalWorkTime = totalWorkTime,
            totalKilometres = totalKilometres
        )
    }
}

// Label translation helpers.
private fun GenerateTimesheetParams.toTimesheetLabels() = TimesheetLabels(
    defaultWorkTypeLabel = defaultWorkTypeLabel,
    noAllowanceSourceLabel = noAllowanceSourceLabel,
    halfDayAllowanceSourceLabel = halfDayAllowanceSourceLabel,
    fullAllowanceSourceLabel = fullAllowanceSourceLabel,
    noAllowanceExportLabel = noAllowanceExportLabel,
    halfDayAllowanceExportLabel = halfDayAllowanceExportLabel,
    fullAllowanceExportLabel = fullAllowanceExportLabel
)

private fun List<SingleProjectState>.toSortedTimesheetEntries(
    labels: TimesheetLabels
): List<TimesheetEntry> {
    return filter { it.projectTime != ZERO_TIME }
        .sortedWith(
            compareBy<SingleProjectState>(
                { it.date },
                { if (it.index >= 0) it.index else Int.MAX_VALUE },
                { it.projectName },
                { it.workType },
                { it.allowance }
            )
        )
        .mapNotNull { it.toTimesheetEntry(labels) }
}

private fun buildAllowanceRows(
    allProjectNames: List<String>,
    labels: TimesheetLabels,
    allowanceCountsByProjectAndType: Map<Pair<String, TimesheetAllowanceType>, Int>,
    allowanceTotalCountsByType: Map<TimesheetAllowanceType, Int>
): List<TimesheetAllowanceSummaryRow> {
    return ALLOWANCE_ORDER.map { allowanceType ->
        TimesheetAllowanceSummaryRow(
            label = allowanceType.toExportLabel(labels),
            countByProjectName = allProjectNames.associateWith { projectName ->
                allowanceCountsByProjectAndType[projectName to allowanceType] ?: 0
            },
            totalCount = allowanceTotalCountsByType[allowanceType] ?: 0
        )
    }
}

private fun buildWorkTypeRows(
    allProjectNames: List<String>,
    summaryWorkTypes: List<String>,
    workTypeTimeByProjectAndType: Map<Pair<String, String>, Long>,
    workTypeTotalTime: Map<String, Long>
): List<TimesheetWorkTypeSummaryRow> {
    return summaryWorkTypes.map { workType ->
        TimesheetWorkTypeSummaryRow(
            label = workType,
            timeByProjectName = allProjectNames.associateWith { projectName ->
                workTypeTimeByProjectAndType[projectName to workType] ?: 0L
            },
            totalTime = workTypeTotalTime[workType] ?: 0L
        )
    }
}

// Sheet XML editing – orchestration.
private object TimesheetSheetEditor {
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
        sheetData.childElementSequence("row").forEach { row ->
            val rowNumber = row.getAttribute("r").toIntOrNull() ?: return@forEach
            if (rowNumber !in DAILY_ENTRIES_START_ROW..clearEndRow) {
                return@forEach
            }
            row.childElementSequence("c").forEach { cell ->
                val columnIndex = extractColumnIndex(cell.getAttribute("r"))
                if (columnIndex in startColumnIndex..endColumnIndex) {
                    cell.clearContents()
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

// Freeze-pane XML editing.
private object TimesheetFreezePaneEditor {
    fun ensureTopRowFrozen(document: Document, sheetData: Element) {
        // Compute freeze row dynamically: find the day-of-month row (contains days 1-31)
        // and freeze through it, accounting for any row shifts from populate functions.
        // Fall back to DAILY_ENTRIES_SEPARATOR_ROW if detection fails.
        val freezeThroughRow = findDayOfMonthRow(sheetData) ?: DAILY_ENTRIES_SEPARATOR_ROW
        val worksheet = document.documentElement ?: return
        val sheetViews = ensureSheetViewsElement(document, worksheet)
        val sheetView = ensureSheetViewElement(document, sheetViews)
        // Template carries a fixed topLeftCell (A13); clear it so pane config controls opening view.
        sheetView.removeAttribute("topLeftCell")

        val frozenRows = freezeThroughRow.coerceAtLeast(1)
        val firstScrollableRow = frozenRows + 1
        configureFreezePane(document, sheetView, frozenRows, firstScrollableRow)
        configureFrozenPaneSelections(document, sheetView, firstScrollableRow)
    }

    private fun ensureSheetViewsElement(document: Document, worksheet: Element): Element =
        (document.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetViews").item(0) as? Element)
            ?: document.createElementNS(SPREADSHEET_NAMESPACE, "sheetViews").also { created ->
                var insertBeforeNode: Node? = null
                var candidate = worksheet.firstChild
                while (candidate != null) {
                    if (candidate.nodeType == Node.ELEMENT_NODE &&
                        candidate.localName in setOf("sheetFormatPr", "cols", "sheetData")
                    ) {
                        insertBeforeNode = candidate
                        break
                    }
                    candidate = candidate.nextSibling
                }
                if (insertBeforeNode != null) worksheet.insertBefore(created, insertBeforeNode)
                else worksheet.appendChild(created)
            }

    private fun ensureSheetViewElement(document: Document, sheetViews: Element): Element =
        (sheetViews.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetView").item(0) as? Element)
            ?: document.createElementNS(SPREADSHEET_NAMESPACE, "sheetView").also { created ->
                created.setAttribute("workbookViewId", "0")
                sheetViews.appendChild(created)
            }

    private fun configureFreezePane(
        document: Document,
        sheetView: Element,
        frozenRows: Int,
        firstScrollableRow: Int
    ) {
        // Freeze top rows through the day-of-month row so headers stay visible.
        val pane = (sheetView.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "pane").item(0) as? Element)
            ?: document.createElementNS(SPREADSHEET_NAMESPACE, "pane").also { created ->
                val firstSelection = sheetView.childElementSequence("selection").firstOrNull()
                if (firstSelection != null) sheetView.insertBefore(created, firstSelection)
                else sheetView.appendChild(created)
            }
        pane.removeAttribute("xSplit")
        pane.setAttribute("ySplit", frozenRows.toString())
        pane.setAttribute("topLeftCell", "A$firstScrollableRow")
        pane.setAttribute("activePane", "bottomLeft")
        pane.setAttribute("state", "frozen")
    }

    private fun configureFrozenPaneSelections(document: Document, sheetView: Element, firstScrollableRow: Int) {
        val selections = sheetView.childElementSequence("selection").toList()
        if (selections.none { it.getAttribute("pane") == "bottomLeft" }) {
            val selection = document.createElementNS(SPREADSHEET_NAMESPACE, "selection")
            selection.setAttribute("pane", "bottomLeft")
            selection.setAttribute("activeCell", "A$firstScrollableRow")
            selection.setAttribute("sqref", "A$firstScrollableRow")
            sheetView.appendChild(selection)
        } else {
            selections
                .filter { it.getAttribute("pane") == "bottomLeft" }
                .forEach { selection ->
                    selection.setAttribute("activeCell", "A$firstScrollableRow")
                    selection.setAttribute("sqref", "A$firstScrollableRow")
                }
        }
    }

    fun ensureFirstColumnFrozen(document: Document) {
        val sheetView = (document.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetView").item(0) as? Element)
            ?: return
        // Prevent inherited sheetView topLeftCell from forcing the view to start below header rows.
        sheetView.removeAttribute("topLeftCell")
        val pane = (sheetView.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "pane").item(0) as? Element)
            ?: return

        pane.setAttribute("xSplit", "2")
        val ySplit = pane.getAttribute("ySplit").toIntOrNull() ?: 0
        val firstScrollableRow = if (ySplit > 0) ySplit + 1 else 1
        val topLeftCell = "C$firstScrollableRow"
        val targetPane = if (ySplit > 0) "bottomRight" else "topRight"

        pane.setAttribute("topLeftCell", topLeftCell)
        pane.setAttribute("activePane", targetPane)
        pane.setAttribute("state", "frozen")

        val selections = sheetView.childElementSequence("selection").toList()
        if (selections.none { it.getAttribute("pane") == targetPane }) {
            val selection = document.createElementNS(SPREADSHEET_NAMESPACE, "selection")
            selection.setAttribute("pane", targetPane)
            selection.setAttribute("activeCell", topLeftCell)
            selection.setAttribute("sqref", topLeftCell)
            sheetView.appendChild(selection)
        } else {
            selections
                .filter { it.getAttribute("pane") == targetPane }
                .forEach { selection ->
                    selection.setAttribute("activeCell", topLeftCell)
                    selection.setAttribute("sqref", topLeftCell)
                }
        }
    }

    /**
     * Scans sheetData to find the row containing day-of-month markers (1-31).
     * The day-of-month row is identified by having numeric cell values 1-31.
     * Searches all rows below row 2 (to allow for header area) since row shifts can occur.
     */
    fun findDayOfMonthRow(sheetData: Element): Int? {
        val rows = sheetData.childElementSequence("row").toList()
        var bestRow: Int? = null
        var bestCount = 0

        for (row in rows) {
            val rowNumber = row.getAttribute("r").toIntOrNull() ?: continue
            // Skip very early rows (header area) and rows far beyond where daily entries should start
            if (rowNumber < 2 || rowNumber > DAILY_ENTRIES_START_ROW + 50) continue

            val cells = row.childElementSequence("c").toList()
            var dayCount = 0

            // Count numeric values 1-31 in this row
            for (cell in cells) {
                val vElement = cell.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "v").item(0)
                val value = vElement?.textContent?.toIntOrNull()
                if (value != null && value in 1..31) {
                    dayCount++
                }
            }

            // Track the row with the most day markers (should be the day-of-month row)
            if (dayCount > bestCount) {
                bestCount = dayCount
                bestRow = rowNumber
            }
        }

        // Return the row if we found at least 15 day markers (more than half)
        return if (bestCount >= 15) bestRow else null
    }
}

// Sheet section population.
private object TimesheetSectionWriter {
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
            TimesheetXmlHelper.setStringCell(document, sheetData, "A${baseRow + 2}", dailyEntryLabels[0], BOLD_TEXT_STYLE)
            TimesheetXmlHelper.setStringCell(document, sheetData, "A${baseRow + 3}", dailyEntryLabels[1], BOLD_TEXT_STYLE)
            TimesheetXmlHelper.setStringCell(document, sheetData, "A${baseRow + 4}", dailyEntryLabels[2], BOLD_TEXT_STYLE)
            TimesheetXmlHelper.setStringCell(document, sheetData, "A${baseRow + 5}", dailyEntryLabels[3], BOLD_TEXT_STYLE)
            TimesheetXmlHelper.setStringCell(document, sheetData, "A${baseRow + 6}", dailyEntryLabels[4], BOLD_TEXT_STYLE)
            if (entryIndex > 0) {
                TimesheetXmlHelper.setStringCell(document, sheetData, "A${baseRow + 1}", "", BOLD_TEXT_STYLE)
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
            context.document, context.sheetData,
            buildCellReference(context.labelColumnIndex, 1),
            context.exportData.generalLabel, BOLD_TEXT_STYLE
        )
        TimesheetXmlHelper.setStringCell(
            context.document, context.sheetData,
            buildCellReference(context.labelColumnIndex, 2),
            context.exportData.workTimeTotalLabel, PLAIN_TEXT_STYLE
        )
        TimesheetXmlHelper.setStringCell(
            context.document, context.sheetData,
            buildCellReference(context.labelColumnIndex, 3),
            context.exportData.kilometresLabel, PLAIN_TEXT_STYLE
        )
    }

    private fun writeProjectSummaryProjects(context: ProjectSummarySectionContext, allProjectNames: List<String>) {
        allProjectNames.forEachIndexed { index, projectName ->
            val columnLetters = context.projectSummaryColumns[index]
            TimesheetXmlHelper.setStringCell(
                context.document, context.sheetData, "${columnLetters}1",
                projectName, PROJECT_SUMMARY_HEADER_STYLE
            )
            TimesheetXmlHelper.setNumericCell(
                context.document, context.sheetData, "${columnLetters}2",
                context.exportData.summaryProjectTimes.getValue(projectName).toExcelTimeFractionNumberString(),
                PROJECT_SUMMARY_WORK_TIME_STYLE
            )
            TimesheetXmlHelper.setNumericCell(
                context.document, context.sheetData, "${columnLetters}3",
                context.exportData.summaryProjectKilometres.getValue(projectName).toString(),
                PROJECT_SUMMARY_KILOMETRES_STYLE
            )
        }
    }

    private fun writeProjectSummaryTotals(context: ProjectSummarySectionContext) {
        TimesheetXmlHelper.setStringCell(
            context.document, context.sheetData, "${context.totalColumnLetters}1",
            context.exportData.totalLabel, PROJECT_SUMMARY_TOTAL_HEADER_STYLE
        )
        TimesheetXmlHelper.setNumericCell(
            context.document, context.sheetData, "${context.totalColumnLetters}2",
            context.exportData.totalWorkTime.toExcelTimeFractionNumberString(),
            PROJECT_SUMMARY_TOTAL_WORK_TIME_STYLE
        )
        TimesheetXmlHelper.setNumericCell(
            context.document, context.sheetData, "${context.totalColumnLetters}3",
            context.exportData.totalKilometres.toString(),
            PROJECT_SUMMARY_TOTAL_KILOMETRES_STYLE
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
            TimesheetXmlHelper.setCellStyle(context.document, context.sheetData, buildCellReference(columnIndex, 2), PLAIN_TIME_STYLE)
            TimesheetXmlHelper.setCellStyle(context.document, context.sheetData, buildCellReference(columnIndex, 3), PLAIN_INTEGER_STYLE)
        }
        TimesheetXmlHelper.setCellStyle(context.document, context.sheetData, buildCellReference(context.labelColumnIndex, 2), PLAIN_TEXT_STYLE)
        TimesheetXmlHelper.setCellStyle(context.document, context.sheetData, buildCellReference(context.labelColumnIndex, 3), PLAIN_TEXT_STYLE)
    }

    private fun writeAllowanceHeader(context: AllowanceSectionContext, ctx: Context) {
        TimesheetXmlHelper.setStringCell(
            context.document, context.sheetData,
            buildCellReference(context.labelColumnIndex, 1),
            ctx.safeString(R.string.allowance, "Allowance"), BOLD_TEXT_STYLE
        )
        context.allProjectNames.forEachIndexed { index, projectName ->
            val columnLetters = columnIndexToLetters(context.startColumnIndex + index)
            TimesheetXmlHelper.setStringCell(
                context.document, context.sheetData, "${columnLetters}1",
                projectName, ALLOWANCE_HEADER_STYLE
            )
        }
        val totalColumnLetters = columnIndexToLetters(context.totalColumnIndex)
        TimesheetXmlHelper.setStringCell(
            context.document, context.sheetData, "${totalColumnLetters}1",
            context.exportData.totalLabel, ALLOWANCE_TOTAL_HEADER_STYLE
        )
    }

    private fun writeAllowanceRows(context: AllowanceSectionContext) {
        context.exportData.allowanceRows.forEachIndexed { rowIndex, allowanceRow ->
            TimesheetXmlHelper.setStringCell(
                context.document, context.sheetData,
                buildCellReference(context.labelColumnIndex, rowIndex + 2),
                allowanceRow.label, PLAIN_TEXT_STYLE
            )
            context.allProjectNames.forEachIndexed { columnIndex, projectName ->
                TimesheetXmlHelper.setNumericCell(
                    context.document, context.sheetData,
                    buildCellReference(context.startColumnIndex + columnIndex, rowIndex + 2),
                    allowanceRow.countByProjectName.getValue(projectName).toString(),
                    ALLOWANCE_PROJECT_VALUE_STYLES[rowIndex]
                )
            }
            TimesheetXmlHelper.setNumericCell(
                context.document, context.sheetData,
                buildCellReference(context.totalColumnIndex, rowIndex + 2),
                allowanceRow.totalCount.toString(),
                ALLOWANCE_TOTAL_VALUE_STYLES[rowIndex]
            )
        }
    }

    private fun writeWorkTypeHeader(context: WorkTypeSectionContext, ctx: Context) {
        TimesheetXmlHelper.setStringCell(
            context.document, context.sheetData,
            buildCellReference(context.labelColumnIndex, 1),
            ctx.safeString(R.string.work_type, "Work type"), BOLD_TEXT_STYLE
        )
        context.allProjectNames.forEachIndexed { index, projectName ->
            TimesheetXmlHelper.setStringCell(
                context.document, context.sheetData,
                buildCellReference(context.startColumnIndex + index, 1),
                projectName, WORK_TYPE_HEADER_STYLE
            )
        }
        TimesheetXmlHelper.setStringCell(
            context.document, context.sheetData,
            buildCellReference(context.totalColumnIndex, 1),
            context.exportData.totalLabel, WORK_TYPE_TOTAL_HEADER_STYLE
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
                context.document, context.sheetData,
                buildCellReference(context.labelColumnIndex, rowIndex + 2),
                workTypeRow.label, PLAIN_TEXT_STYLE
            )
            context.allProjectNames.forEachIndexed { columnIndex, projectName ->
                val value = workTypeRow.timeByProjectName.getValue(projectName)
                if (value > 0L) {
                    TimesheetXmlHelper.setNumericCell(
                        context.document, context.sheetData,
                        buildCellReference(context.startColumnIndex + columnIndex, rowIndex + 2),
                        value.toExcelTimeFractionNumberString(),
                        WORK_TYPE_VALUE_STYLES.getOrElse(rowIndex) { PLAIN_TIME_STYLE }
                    )
                }
            }
            if (workTypeRow.totalTime > 0L) {
                TimesheetXmlHelper.setNumericCell(
                    context.document, context.sheetData,
                    buildCellReference(context.totalColumnIndex, rowIndex + 2),
                    workTypeRow.totalTime.toExcelTimeFractionNumberString(),
                    WORK_TYPE_TOTAL_STYLES.getOrElse(rowIndex) { PLAIN_TIME_STYLE }
                )
            }
        }
    }
}

// Low-level XML cell/row helpers.
private object TimesheetXmlHelper {
    fun dailyEntryBaseRow(entryIndex: Int): Int =
        DAILY_ENTRIES_START_ROW + (entryIndex * DAILY_ENTRY_ROW_HEIGHT)

    fun clearCell(sheetData: Element, cellReference: String) {
        getCell(sheetData, cellReference)?.clearContents()
    }

    fun setStringCell(
        document: Document,
        sheetData: Element,
        cellReference: String,
        value: String,
        styleIndex: Int? = null
    ) {
        val cell = getOrCreateCell(document, sheetData, cellReference, styleIndex)
        if (value.isBlank()) {
            cell.clearContents()
            return
        }
        cell.clearContents()
        cell.setAttribute("t", "inlineStr")
        val isElement = document.createElementNS(SPREADSHEET_NAMESPACE, "is")
        val textElement = document.createElementNS(SPREADSHEET_NAMESPACE, "t")
        if (value.firstOrNull()?.isWhitespace() == true || value.lastOrNull()?.isWhitespace() == true) {
            textElement.setAttributeNS(XML_NAMESPACE, "xml:space", "preserve")
        }
        textElement.textContent = value
        isElement.appendChild(textElement)
        cell.appendChild(isElement)
    }

    fun setNumericCell(
        document: Document,
        sheetData: Element,
        cellReference: String,
        numericValue: String,
        styleIndex: Int? = null
    ) {
        val cell = getOrCreateCell(document, sheetData, cellReference, styleIndex)
        cell.clearContents()
        cell.removeAttribute("t")
        val valueElement = document.createElementNS(SPREADSHEET_NAMESPACE, "v")
        valueElement.textContent = numericValue
        cell.appendChild(valueElement)
    }

    fun setCellStyle(document: Document, sheetData: Element, cellReference: String, styleIndex: Int) {
        getOrCreateCell(document, sheetData, cellReference, styleIndex)
    }

    fun applySectionHeaderStyles(
        document: Document,
        sheetData: Element,
        labelColumnIndex: Int,
        startColumnIndex: Int,
        endColumnIndex: Int
    ) {
        val rowCache = mutableMapOf<Int, Element>()
        val headerRow = getOrCreateRowCached(document, sheetData, rowNumber = 1, rowCache = rowCache)
        for (columnIndex in startColumnIndex..endColumnIndex) {
            val firstRowStyle = if (columnIndex == labelColumnIndex) BOLD_TEXT_STYLE else PLAIN_TEXT_STYLE
            getOrCreateCellInRow(
                document = document,
                row = headerRow,
                cellReference = buildCellReference(columnIndex, 1),
                styleIndex = firstRowStyle
            )
        }
    }

    fun applySectionBodyStyles(document: Document, sheetData: Element, spec: SectionBodyStyleSpec) {
        val rowCache = mutableMapOf<Int, Element>()
        for (rowNumber in spec.rowRange) {
            val row = getOrCreateRowCached(document, sheetData, rowNumber, rowCache)
            getOrCreateCellInRow(document, row, buildCellReference(spec.labelColumnIndex, rowNumber), PLAIN_TEXT_STYLE)
            for (columnIndex in spec.valueColumnRange) {
                getOrCreateCellInRow(document, row, buildCellReference(columnIndex, rowNumber), spec.valueStyle)
            }
        }
    }

    fun insertBlankRowRange(sheetData: Element, rowNumber: Int, startColumn: Int, endColumn: Int) {
        val document = sheetData.ownerDocument
        val lastDefinedRow = sheetData.childElementSequence("row")
            .mapNotNull { row -> row.getAttribute("r").toIntOrNull() }
            .maxOrNull()
            ?: return

        // Shift cells down by one row inside the requested column range.
        for (currentRow in lastDefinedRow downTo rowNumber) {
            for (columnIndex in startColumn..endColumn) {
                val sourceRef = buildCellReference(columnIndex, currentRow)
                val sourceCell = getCell(sheetData, sourceRef) ?: continue
                val targetRef = buildCellReference(columnIndex, currentRow + 1)

                getCell(sheetData, targetRef)?.let { existingTarget ->
                    existingTarget.parentNode?.removeChild(existingTarget)
                }

                val sourceRow = getRow(sheetData, currentRow) ?: continue
                val targetRow = getOrCreateRow(document, sheetData, currentRow + 1)
                sourceRow.removeChild(sourceCell)
                sourceCell.setAttribute("r", targetRef)
                insertCellInRowOrder(targetRow, sourceCell)
            }
        }

        // Ensure the inserted row is blank in the requested range.
        for (columnIndex in startColumn..endColumn) {
            clearCell(sheetData, buildCellReference(columnIndex, rowNumber))
        }
    }

    private fun insertCellInRowOrder(row: Element, cell: Element) {
        val newColumnIndex = extractColumnIndex(cell.getAttribute("r"))
        val nextCell = row.childElementSequence("c")
            .firstOrNull { extractColumnIndex(it.getAttribute("r")) > newColumnIndex }
        if (nextCell != null) row.insertBefore(cell, nextCell) else row.appendChild(cell)
    }

    fun getCell(sheetData: Element, cellReference: String): Element? {
        val row = getRow(sheetData, extractRowNumber(cellReference)) ?: return null
        return row.childElementSequence("c").firstOrNull { it.getAttribute("r") == cellReference }
    }

    fun getRow(sheetData: Element, rowNumber: Int): Element? =
        sheetData.childElementSequence("row").firstOrNull { it.getAttribute("r") == rowNumber.toString() }

    fun getOrCreateCell(
        document: Document,
        sheetData: Element,
        cellReference: String,
        styleIndex: Int?
    ): Element {
        val rowNumber = extractRowNumber(cellReference)
        val row = getOrCreateRow(document, sheetData, rowNumber)
        return getOrCreateCellInRow(document, row, cellReference, styleIndex)
    }

    fun getOrCreateCellInRow(
        document: Document,
        row: Element,
        cellReference: String,
        styleIndex: Int?
    ): Element {
        val existingCell = row.childElementSequence("c").firstOrNull { it.getAttribute("r") == cellReference }
        if (existingCell != null) {
            if (styleIndex != null) existingCell.setAttribute("s", styleIndex.toString())
            return existingCell
        }
        val newCell = document.createElementNS(SPREADSHEET_NAMESPACE, "c")
        newCell.setAttribute("r", cellReference)
        styleIndex?.let { newCell.setAttribute("s", it.toString()) }
        val newColumnIndex = extractColumnIndex(cellReference)
        val nextCell = row.childElementSequence("c")
            .firstOrNull { extractColumnIndex(it.getAttribute("r")) > newColumnIndex }
        if (nextCell != null) row.insertBefore(newCell, nextCell) else row.appendChild(newCell)
        return newCell
    }

    fun getOrCreateRow(document: Document, sheetData: Element, rowNumber: Int): Element {
        getRow(sheetData, rowNumber)?.let { return it }
        val newRow = document.createElementNS(SPREADSHEET_NAMESPACE, "row")
        newRow.setAttribute("r", rowNumber.toString())
        newRow.setAttribute("spans", "1:32")
        val nextRow = sheetData.childElementSequence("row")
            .firstOrNull { it.getAttribute("r").toInt() > rowNumber }
        if (nextRow != null) sheetData.insertBefore(newRow, nextRow) else sheetData.appendChild(newRow)
        return newRow
    }

    fun getOrCreateRowCached(
        document: Document,
        sheetData: Element,
        rowNumber: Int,
        rowCache: MutableMap<Int, Element>
    ): Element {
        rowCache[rowNumber]?.let { return it }
        return getOrCreateRow(document, sheetData, rowNumber).also { rowCache[rowNumber] = it }
    }
}

internal object TimesheetWorkbookEditor {
    fun createWorkbook(templateBytes: ByteArray, exportData: TimesheetExportData, ctx: Context): ByteArray {
        val zipEntries = unzipEntries(templateBytes)
        zipEntries["xl/sharedStrings.xml"] = localizeSharedStrings(
            sharedStringsXml = zipEntries.getValue("xl/sharedStrings.xml"),
            ctx = ctx
        )
        zipEntries["[Content_Types].xml"] = removeCalcChainContentType(
            zipEntries.getValue("[Content_Types].xml")
        )
        zipEntries["xl/_rels/workbook.xml.rels"] = removeCalcChainRelationship(
            zipEntries.getValue("xl/_rels/workbook.xml.rels")
        )
        zipEntries.remove("xl/calcChain.xml")
        zipEntries["xl/styles.xml"] = ensureLeftAlignmentInStyles(
            zipEntries.getValue("xl/styles.xml")
        )
        val updatedSheetXml = TimesheetSheetEditor.updateSheet(
            sheetXml = zipEntries.getValue("xl/worksheets/sheet1.xml"),
            exportData = exportData,
            ctx = ctx
        )
        zipEntries["xl/worksheets/sheet1.xml"] = normalizeStyleReferences(
            sheetXml = updatedSheetXml,
            stylesXml = zipEntries.getValue("xl/styles.xml")
        )
        return zipEntries(zipEntries)
    }

    private fun localizeSharedStrings(sharedStringsXml: ByteArray, ctx: Context): ByteArray {
        val labelMappings = mapOf(
            0 to ctx.safeString(R.string.timesheet_day_of_month, "Day of Month"),
            1 to ctx.safeString(R.string.project_name, "Project name"),
            2 to ctx.safeString(R.string.work_time_by_date, "Work time by date"),
            3 to ctx.safeString(R.string.allowance, "Allowance"),
            4 to ctx.safeString(R.string.work_type, "Work type"),
            5 to ctx.safeString(R.string.kilometres, "Kilometres"),
            6 to ctx.safeString(R.string.employer, "Employer"),
            7 to ctx.safeString(R.string.name, "Name"),
            14 to ctx.safeString(R.string.timesheet_work_time_total, "Work time total"),
            16 to ctx.safeString(R.string.total_sum, "Sum"),
            17 to ctx.safeString(R.string.timesheet_no_allowance_short, "No"),
            18 to ctx.safeString(R.string.timesheet_half_day_allowance_short, "Half-day"),
            19 to ctx.safeString(R.string.timesheet_full_allowance_short, "Full"),
            20 to ctx.safeString(R.string.timesheet_start_date, "Start date"),
            21 to ctx.safeString(R.string.timesheet_title, "Timesheet"),
            22 to ctx.safeString(R.string.timesheet_end_date, "End date"),
            23 to ctx.safeString(R.string.timesheet_general, "General"),
            24 to ctx.safeString(R.string.timesheet_flex_time_total, "Flex time total"),
            25 to ctx.safeString(R.string.project_time, "Project time")
        )
        return replaceSharedStrings(sharedStringsXml, labelMappings)
    }

    private fun normalizeStyleReferences(sheetXml: ByteArray, stylesXml: ByteArray): ByteArray {
        val stylesDocument = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(stylesXml))
        val xfCount = (
                stylesDocument.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "cellXfs")
                    .item(0) as? Element)
                    ?.childElementSequence("xf")
                    ?.count()
        if (xfCount == null || xfCount <= 0) {
            return sheetXml
        }

        val sheetDocument = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(sheetXml))
        val cells = sheetDocument.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "c")
        for (index in 0 until cells.length) {
            val cell = cells.item(index) as? Element
            val styleIndex = cell?.getAttribute("s")?.toIntOrNull()
            if (cell != null && styleIndex != null && styleIndex >= xfCount) {
                val fallbackCandidate = styleIndex - xfCount
                val normalizedStyleIndex = if (fallbackCandidate in 0 until xfCount) {
                    fallbackCandidate
                } else {
                    0
                }
                cell.setAttribute("s", normalizedStyleIndex.toString())
            }
        }
        return sheetDocument.toByteArray()
    }


    private fun ensureLeftAlignmentInStyles(stylesXml: ByteArray): ByteArray {
        val stylesDocument = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(stylesXml))

        val styleSheet = stylesDocument.documentElement
        val cellXfs = (styleSheet.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "cellXfs").item(0) as? Element)
            ?: return stylesXml

        // Add left alignment to all cell format styles
        cellXfs.childElementSequence("xf").forEach { xf ->
            // Remove existing alignment if present
            xf.childElementSequence("alignment").firstOrNull()?.let { xf.removeChild(it) }
            
            // Add new alignment with left horizontal and center vertical
            val alignment = stylesDocument.createElementNS(SPREADSHEET_NAMESPACE, "alignment")
            alignment.setAttribute("horizontal", "left")
            alignment.setAttribute("vertical", "center")
            xf.appendChild(alignment)
            
            xf.setAttribute("applyAlignment", "1")
        }

        return stylesDocument.toByteArray()
    }

    private fun unzipEntries(templateBytes: ByteArray): LinkedHashMap<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(templateBytes)).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                result[entry.name] = input.readBytes()
                input.closeEntry()
                entry = input.nextEntry
            }
        }
        return result
    }

    private fun zipEntries(entries: LinkedHashMap<String, ByteArray>): ByteArray {
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                entries.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
    }

    private fun removeCalcChainContentType(contentTypesXml: ByteArray): ByteArray {
        val document = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(contentTypesXml))
        val overrides = document.getElementsByTagNameNS(CONTENT_TYPES_NAMESPACE, "Override")
        val nodesToRemove = (0 until overrides.length)
            .mapNotNull { index -> overrides.item(index) as? Element }
            .filter { it.getAttribute("PartName") == "/xl/calcChain.xml" }
        nodesToRemove.forEach { node -> node.parentNode?.removeChild(node) }
        return document.toByteArray()
    }

    private fun removeCalcChainRelationship(workbookRelsXml: ByteArray): ByteArray {
        val document = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(workbookRelsXml))
        val relationships = document.getElementsByTagNameNS(RELATIONSHIPS_NAMESPACE, "Relationship")
        val nodesToRemove = (0 until relationships.length)
            .mapNotNull { index -> relationships.item(index) as? Element }
            .filter { relationship ->
                relationship.getAttribute("Target") == "calcChain.xml" ||
                    relationship.getAttribute("Type").endsWith("/calcChain")
            }
        nodesToRemove.forEach { node -> node.parentNode?.removeChild(node) }
        return document.toByteArray()
    }

}

internal fun replaceSharedStrings(
    sharedStringsXml: ByteArray,
    replacements: Map<Int, String>
): ByteArray {
    val document = createDocumentBuilderFactory().newDocumentBuilder()
        .parse(ByteArrayInputStream(sharedStringsXml))
    val sharedStrings = document.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "si")

    replacements.forEach { (index, value) ->
        val sharedString = sharedStrings.item(index) as? Element ?: return@forEach
        val textNodes = sharedString.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "t")
        for (i in 0 until textNodes.length) {
            (textNodes.item(i) as? Element)?.apply {
                textContent = value
                if (value.firstOrNull()?.isWhitespace() == true || value.lastOrNull()?.isWhitespace() == true) {
                    setAttributeNS(XML_NAMESPACE, "xml:space", "preserve")
                } else {
                    removeAttributeNS(XML_NAMESPACE, "space")
                }
            }
        }
    }

    return document.toByteArray()
}

private fun Context.safeString(resId: Int, fallback: String): String {
    return runCatching { getString(resId) }.getOrDefault(fallback)
}

// Export model objects.
internal data class TimesheetExportData(
    val name: String,
    val employer: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalLabel: String,
    val generalLabel: String,
    val workTimeTotalLabel: String,
    val kilometresLabel: String,
    val summaryProjectNames: List<String>,
    val summaryProjectTimes: Map<String, Long>,
    val summaryProjectKilometres: Map<String, Long>,
    val totalWorkTime: Long,
    val totalKilometres: Long,
    val allowanceRows: List<TimesheetAllowanceSummaryRow>,
    val workTypeRows: List<TimesheetWorkTypeSummaryRow>,
    val displayedEntriesByDay: Map<Int, List<TimesheetEntry>>,
    val overflowedDays: List<Int>,
    val hiddenProjectNames: List<String>,
    val hiddenWorkTypes: List<String>,
    val flexTimeTotalLabel: String,
    val totalFlexTimeTotal: String
)

internal data class TimesheetAllowanceSummaryRow(
    val label: String,
    val countByProjectName: Map<String, Int>,
    val totalCount: Int
)

internal data class TimesheetWorkTypeSummaryRow(
    val label: String,
    val timeByProjectName: Map<String, Long>,
    val totalTime: Long
)

internal data class TimesheetEntry(
    val dayOfMonth: Int,
    val projectName: String,
    val projectTime: String,
    val allowanceType: TimesheetAllowanceType,
    val allowanceLabel: String,
    val workType: String,
    val kilometres: String
)

internal data class TimesheetLabels(
    val defaultWorkTypeLabel: String,
    val noAllowanceSourceLabel: String,
    val halfDayAllowanceSourceLabel: String,
    val fullAllowanceSourceLabel: String,
    val noAllowanceExportLabel: String,
    val halfDayAllowanceExportLabel: String,
    val fullAllowanceExportLabel: String
)

internal enum class TimesheetAllowanceType {
    NONE,
    HALF_DAY,
    FULL
}

// Entry conversion and formatting helpers.
private fun SingleProjectState.toTimesheetEntry(labels: TimesheetLabels): TimesheetEntry? {
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val normalizedProjectTime = (projectTime.toMinutesOrNull() ?: 0L).toHourMinuteString()
    val isValidEntry = normalizedProjectTime != ZERO_TIME
    return if (isValidEntry) {
        val normalizedProjectName = projectName.trim()
        val normalizedWorkType = workType.trim().ifBlank { labels.defaultWorkTypeLabel }
        val allowanceType = allowance.toAllowanceType(labels)
        TimesheetEntry(
            dayOfMonth = parsedDate.dayOfMonth,
            projectName = normalizedProjectName,
            projectTime = normalizedProjectTime,
            allowanceType = allowanceType,
            allowanceLabel = allowanceType.toExportLabel(labels),
            workType = normalizedWorkType,
            kilometres = kilometres.trim()
        )
    } else {
        null
    }
}

private fun String.toAllowanceType(labels: TimesheetLabels): TimesheetAllowanceType {
    return when (trim()) {
        labels.fullAllowanceSourceLabel -> TimesheetAllowanceType.FULL
        labels.halfDayAllowanceSourceLabel -> TimesheetAllowanceType.HALF_DAY
        else -> TimesheetAllowanceType.NONE
    }
}

private fun TimesheetAllowanceType.toExportLabel(labels: TimesheetLabels): String {
    return when (this) {
        TimesheetAllowanceType.NONE -> labels.noAllowanceExportLabel
        TimesheetAllowanceType.HALF_DAY -> labels.halfDayAllowanceExportLabel
        TimesheetAllowanceType.FULL -> labels.fullAllowanceExportLabel
    }
}


private fun List<TimesheetEntry>.allDistinctProjectNames(): List<String> {
    return map { it.projectName }
        .filter { it.isNotBlank() }
        .distinct()
}


private fun List<TimesheetEntry>.allDistinctWorkTypes(): List<String> {
    return map { it.workType }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun projectSummaryColumnLetters(projectCount: Int): List<String> {
    val startIndex = projectSummaryStartColumnIndex()
    return (0 until projectCount).map { offset ->
        columnIndexToLetters(startIndex + offset)
    }
}

private fun projectSummaryTotalColumnLetters(projectCount: Int): String {
    return columnIndexToLetters(projectSummaryTotalColumnIndex(projectCount))
}

private fun projectSummaryStartColumnIndex(): Int {
    return PROJECT_SUMMARY_START_COLUMN_INDEX
}

private fun projectSummaryTotalColumnIndex(projectCount: Int): Int {
    return projectSummaryStartColumnIndex() + projectCount
}

private fun allowanceLabelColumnIndex(projectCount: Int): Int {
    // Keep one full blank column between project summary and allowance section.
    return projectSummaryTotalColumnIndex(projectCount) + 2
}

private fun allowanceStartColumnIndex(projectCount: Int): Int {
    return allowanceLabelColumnIndex(projectCount) + 1
}

private fun allowanceTotalColumnIndex(projectCount: Int): Int {
    return allowanceStartColumnIndex(projectCount) + projectCount
}

private fun workTypeLabelColumnIndex(projectCount: Int): Int {
    return allowanceTotalColumnIndex(projectCount) + 2
}

private fun buildCellReference(columnIndex: Int, rowNumber: Int): String {
    return "${columnIndexToLetters(columnIndex)}$rowNumber"
}

private fun String.toMinutesOrNull(): Long? {
    val normalized = trim()
    if (normalized.isBlank()) {
        return null
    }
    val parts = normalized.split(':')
    if (parts.size != 2) {
        return null
    }
    val hours = parts[0].toLongOrNull() ?: return null
    val minutes = parts[1].toLongOrNull() ?: return null
    if (minutes !in 0..59) {
        return null
    }
    return (hours * 60) + minutes
}

private fun Long.toHourMinuteString(): String {
    val hours = this / 60
    val minutes = this % 60
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

private fun Long.toExcelTimeFractionNumberString(): String {
    return BigDecimal.valueOf(this)
        .divide(BigDecimal.valueOf(MINUTES_PER_DAY), 15, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}


private fun LocalDate.toExcelSerialDate(): Long {
    val excelEpoch = LocalDate.of(1899, 12, 30)
    return ChronoUnit.DAYS.between(excelEpoch, this)
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

// XML / spreadsheet utility helpers.
private fun dayToColumn(day: Int): String {
    return columnIndexToLetters(day + 1)
}

private fun columnIndexToLetters(columnIndex: Int): String {
    var value = columnIndex
    val builder = StringBuilder()
    while (value > 0) {
        val remainder = (value - 1) % 26
        builder.insert(0, ('A'.code + remainder).toChar())
        value = (value - 1) / 26
    }
    return builder.toString()
}

private fun extractRowNumber(cellReference: String): Int {
    return cellReference.dropWhile { it.isLetter() }.toInt()
}

private fun extractColumnIndex(cellReference: String): Int {
    val letters = cellReference.takeWhile { it.isLetter() }
    var result = 0
    letters.forEach { character ->
        result = (result * 26) + (character.code - 'A'.code + 1)
    }
    return result
}

private fun Element.childElementSequence(localName: String): Sequence<Element> = sequence {
    var child = firstChild
    while (child != null) {
        if (child.nodeType == Node.ELEMENT_NODE && child.localName == localName) {
            yield(child as Element)
        }
        child = child.nextSibling
    }
}

private fun Element.clearContents() {
    val children = mutableListOf<Node>()
    var child = firstChild
    while (child != null) {
        children += child
        child = child.nextSibling
    }
    children.forEach(::removeChild)
    removeAttribute("t")
}

private fun createDocumentBuilderFactory(): DocumentBuilderFactory {
    return DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }
}

private fun Document.toByteArray(): ByteArray {
    return ByteArrayOutputStream().use { output ->
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        }
        transformer.transform(DOMSource(this), StreamResult(output))
        output.toByteArray()
    }
}

private const val SPREADSHEET_NAMESPACE = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
private const val XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"
private const val CONTENT_TYPES_NAMESPACE = "http://schemas.openxmlformats.org/package/2006/content-types"
private const val RELATIONSHIPS_NAMESPACE = "http://schemas.openxmlformats.org/package/2006/relationships"

