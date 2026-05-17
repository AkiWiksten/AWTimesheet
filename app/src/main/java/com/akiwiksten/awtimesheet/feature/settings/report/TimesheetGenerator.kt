@file:Suppress("MagicNumber", "TooManyFunctions")

package com.akiwiksten.awtimesheet.feature.settings.report

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
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
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

private const val TEMPLATE_ASSET_NAME = "timesheet_template.xlsx"
private const val MAX_SUMMARY_PROJECTS = 3
private const val MAX_SUMMARY_WORK_TYPES = 3
private const val DAILY_ENTRY_ROW_HEIGHT = 6
private const val DAILY_ENTRIES_START_ROW = 8
private const val EXCEL_MAX_ROW_INDEX = 1_048_576

// Workbook style ids.
private const val PROJECT_SUMMARY_START_COLUMN_INDEX = 5 // E
private const val LOG_TAG = "TimesheetGenerator"
private const val PROJECT_NAME_STYLE = 5
private const val WORK_TIME_STYLE = 6
// Date cells B4/B5 keep template styles; avoid hardcoded style indices.
private const val PLAIN_TEXT_STYLE = 15
private const val BOLD_TEXT_STYLE = 16
private const val PLAIN_TIME_STYLE = 18
private const val PLAIN_INTEGER_STYLE = 19
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
    val summaryProjectTimes: Map<String, Double>,
    val summaryProjectKilometres: Map<String, Double>,
    val allowanceCountsByProjectAndType: Map<Pair<String, TimesheetAllowanceType>, Int>,
    val allowanceTotalCountsByType: Map<TimesheetAllowanceType, Int>,
    val workTypeTimeByProjectAndType: Map<Pair<String, String>, Double>,
    val workTypeTotalTime: Map<String, Double>,
    val totalWorkTime: Double,
    val totalKilometres: Double
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
    val kilometresLabel: String
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
        val summaryWorkTypes = allWorkTypes.take(MAX_SUMMARY_WORK_TYPES)
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
            hiddenWorkTypes = allWorkTypes.drop(MAX_SUMMARY_WORK_TYPES)
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
        val summaryProjectTimes = allProjectNames.associateWith { 0.0 }.toMutableMap()
        val summaryProjectKilometres = allProjectNames.associateWith { 0.0 }.toMutableMap()
        val allowanceCountsByProjectAndType = mutableMapOf<Pair<String, TimesheetAllowanceType>, Int>()
        val allowanceTotalCountsByType = mutableMapOf<TimesheetAllowanceType, Int>()
        val workTypeTimeByProjectAndType = mutableMapOf<Pair<String, String>, Double>()
        val workTypeTotalTime = mutableMapOf<String, Double>()
        var totalWorkTime = 0.0
        var totalKilometres = 0.0

        entries.forEach { entry ->
            if (entry.projectName in summaryProjectTimes) {
                summaryProjectTimes.compute(entry.projectName) { _, current ->
                    (current ?: 0.0) + entry.workTimeFraction
                }
                summaryProjectKilometres.compute(entry.projectName) { _, current ->
                    (current ?: 0.0) + entry.kilometres
                }
            }
            allowanceCountsByProjectAndType.compute(entry.projectName to entry.allowanceType) { _, current ->
                (current ?: 0) + 1
            }
            allowanceTotalCountsByType.compute(entry.allowanceType) { _, current ->
                (current ?: 0) + 1
            }
            workTypeTimeByProjectAndType.compute(entry.projectName to entry.workType) { _, current ->
                (current ?: 0.0) + entry.workTimeFraction
            }
            workTypeTotalTime.compute(entry.workType) { _, current ->
                (current ?: 0.0) + entry.workTimeFraction
            }
            totalWorkTime += entry.workTimeFraction
            totalKilometres += entry.kilometres
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
    workTypeTimeByProjectAndType: Map<Pair<String, String>, Double>,
    workTypeTotalTime: Map<String, Double>
): List<TimesheetWorkTypeSummaryRow> {
    return summaryWorkTypes.map { workType ->
        TimesheetWorkTypeSummaryRow(
            label = workType,
            timeByProjectName = allProjectNames.associateWith { projectName ->
                workTypeTimeByProjectAndType[projectName to workType] ?: 0.0
            },
            totalTime = workTypeTotalTime[workType] ?: 0.0
        )
    }
}

// Sheet XML editing.
private object TimesheetSheetEditor {
    fun updateSheet(sheetXml: ByteArray, exportData: TimesheetExportData): ByteArray {
        val document = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(sheetXml))
        ensureTopRowFrozen(document)
        val sheetData = document.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetData")
            .item(0) as Element
        val dailyEntriesRowOffset = dailyEntriesRowOffset(exportData)

        clearDynamicCells(sheetData, dailyEntriesRowOffset)
        // Keep the full top summary area deterministic even when some populate* calls are disabled.
        clearTopSummaryArea(sheetData)
        populateHeader(document, sheetData, exportData)
        populateDailyEntries(document, sheetData, exportData, dailyEntriesRowOffset)
        populateProjectSummary(document, sheetData, exportData)
        populateAllowanceSummary(document, sheetData, exportData)
        populateWorkTypeSummary(document, sheetData, exportData)

        return document.toByteArray()
    }

    private fun ensureTopRowFrozen(document: Document) {
        val worksheet = document.documentElement ?: return
        val sheetViews = (document.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetViews").item(0) as? Element)
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
                if (insertBeforeNode != null) {
                    worksheet.insertBefore(created, insertBeforeNode)
                } else {
                    worksheet.appendChild(created)
                }
            }
        val sheetView = (sheetViews.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetView").item(0) as? Element)
            ?: document.createElementNS(SPREADSHEET_NAMESPACE, "sheetView").also { created ->
                created.setAttribute("workbookViewId", "0")
                sheetViews.appendChild(created)
            }

        // Freeze rows 1..7 and columns A:B so header area stays visible while scrolling.
        val pane = (sheetView.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "pane").item(0) as? Element)
            ?: document.createElementNS(SPREADSHEET_NAMESPACE, "pane").also { created ->
                val firstSelection = sheetView.childElementSequence("selection").firstOrNull()
                if (firstSelection != null) {
                    sheetView.insertBefore(created, firstSelection)
                } else {
                    sheetView.appendChild(created)
                }
            }
        pane.setAttribute("xSplit", "2")
        pane.setAttribute("ySplit", "7")
        pane.setAttribute("topLeftCell", "C8")
        pane.setAttribute("activePane", "bottomRight")
        pane.setAttribute("state", "frozen")

        val selections = sheetView.childElementSequence("selection").toList()
        if (selections.none { it.getAttribute("pane") == "bottomRight" }) {
            val selection = document.createElementNS(SPREADSHEET_NAMESPACE, "selection")
            selection.setAttribute("pane", "bottomRight")
            selection.setAttribute("activeCell", "C8")
            selection.setAttribute("sqref", "C8")
            sheetView.appendChild(selection)
        }
    }

    private fun clearDynamicCells(sheetData: Element, dailyEntriesRowOffset: Int) {
        listOf("B2", "B3", "B4", "B5", "H1", "H2", "H3").forEach { cellReference ->
            clearCell(sheetData, cellReference)
        }
        PROJECT_NAME_HEADER_CELLS.forEach { clearCell(sheetData, it) }
        PROJECT_TIME_SUMMARY_CELLS.forEach { clearCell(sheetData, it) }
        PROJECT_KILOMETRES_SUMMARY_CELLS.forEach { clearCell(sheetData, it) }
        ALLOWANCE_HEADER_CELLS.forEach { clearCell(sheetData, it) }
        ALLOWANCE_LABEL_CELLS.forEach { clearCell(sheetData, it) }
        WORK_TYPE_HEADER_CELLS.forEach { clearCell(sheetData, it) }
        WORK_TYPE_LABEL_CELLS.forEach { clearCell(sheetData, it) }
        WORK_TYPE_VALUE_CELLS.flatten().forEach { clearCell(sheetData, it) }
        WORK_TYPE_TOTAL_CELLS.forEach { clearCell(sheetData, it) }

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

        for (day in 1..31) {
            val column = dayToColumn(day)
            for (rowNumber in DAILY_ENTRIES_START_ROW..clearEndRow) {
                clearCell(sheetData, "$column$rowNumber")
            }
        }
    }

    private fun clearTopSummaryArea(sheetData: Element) {
        for (row in TOP_SUMMARY_CLEAR_START_ROW..TOP_SUMMARY_CLEAR_END_ROW) {
            for (columnIndex in TOP_SUMMARY_CLEAR_START_COLUMN_INDEX..TOP_SUMMARY_CLEAR_END_COLUMN_INDEX) {
                clearCell(sheetData, buildCellReference(columnIndex, row))
            }
        }
    }

    private fun populateHeader(document: Document, sheetData: Element, exportData: TimesheetExportData) {
        setStringCell(document, sheetData, "B2", exportData.name)
        setStringCell(document, sheetData, "B3", exportData.employer)
        setNumericCell(
            document = document,
            sheetData = sheetData,
            cellReference = "B4",
            numericValue = exportData.startDate.toExcelSerialDate()
        )
        setNumericCell(
            document = document,
            sheetData = sheetData,
            cellReference = "B5",
            numericValue = exportData.endDate.toExcelSerialDate()
        )
    }

    private fun populateProjectSummary(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData
    ) {
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

    private fun populateAllowanceSummary(
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

        applySectionHeaderStyles(
            document = document,
            sheetData = sheetData,
            labelColumnIndex = context.labelColumnIndex,
            startColumnIndex = context.labelColumnIndex,
            endColumnIndex = context.totalColumnIndex
        )
        applySectionBodyStyles(
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

    private fun populateWorkTypeSummary(
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
        writeWorkTypeRows(context)

        applySectionHeaderStyles(
            document = document,
            sheetData = sheetData,
            labelColumnIndex = context.labelColumnIndex,
            startColumnIndex = context.labelColumnIndex,
            endColumnIndex = context.totalColumnIndex
        )
        applySectionBodyStyles(
            document = document,
            sheetData = sheetData,
            spec = SectionBodyStyleSpec(
                labelColumnIndex = context.labelColumnIndex,
                valueColumnRange = context.startColumnIndex..context.totalColumnIndex,
                rowRange = 2..4,
                valueStyle = PLAIN_TIME_STYLE
            )
        )
    }

    private fun dailyEntriesRowOffset(exportData: TimesheetExportData): Int {
        val workTypeLastRow = exportData.workTypeRows.size + 1
        return if (workTypeLastRow == DAILY_ENTRIES_START_ROW - 1) 1 else 0
    }

    private fun writeProjectSummaryLabels(context: ProjectSummarySectionContext) {
        setStringCell(
            context.document,
            context.sheetData,
            buildCellReference(context.labelColumnIndex, 1),
            context.exportData.generalLabel,
            BOLD_TEXT_STYLE
        )
        setStringCell(
            context.document,
            context.sheetData,
            buildCellReference(context.labelColumnIndex, 2),
            context.exportData.workTimeTotalLabel,
            PLAIN_TEXT_STYLE
        )
        setStringCell(
            context.document,
            context.sheetData,
            buildCellReference(context.labelColumnIndex, 3),
            context.exportData.kilometresLabel,
            PLAIN_TEXT_STYLE
        )
    }

    private fun writeProjectSummaryProjects(
        context: ProjectSummarySectionContext,
        allProjectNames: List<String>
    ) {
        allProjectNames.forEachIndexed { index, projectName ->
            val columnLetters = context.projectSummaryColumns[index]
            setStringCell(
                context.document,
                context.sheetData,
                "${columnLetters}1",
                projectName,
                PROJECT_SUMMARY_HEADER_STYLE
            )
            setNumericCell(
                context.document,
                context.sheetData,
                "${columnLetters}2",
                context.exportData.summaryProjectTimes.getValue(projectName),
                PROJECT_SUMMARY_WORK_TIME_STYLE
            )
            setNumericCell(
                context.document,
                context.sheetData,
                "${columnLetters}3",
                context.exportData.summaryProjectKilometres.getValue(projectName),
                PROJECT_SUMMARY_KILOMETRES_STYLE
            )
        }
    }

    private fun writeProjectSummaryTotals(context: ProjectSummarySectionContext) {
        setStringCell(
            context.document,
            context.sheetData,
            "${context.totalColumnLetters}1",
            context.exportData.totalLabel,
            PROJECT_SUMMARY_TOTAL_HEADER_STYLE
        )
        setNumericCell(
            context.document,
            context.sheetData,
            "${context.totalColumnLetters}2",
            context.exportData.totalWorkTime,
            PROJECT_SUMMARY_TOTAL_WORK_TIME_STYLE
        )
        setNumericCell(
            context.document,
            context.sheetData,
            "${context.totalColumnLetters}3",
            context.exportData.totalKilometres,
            PROJECT_SUMMARY_TOTAL_KILOMETRES_STYLE
        )
    }

    private fun applyProjectSummaryStyles(context: ProjectSummarySectionContext) {
        applySectionHeaderStyles(
            document = context.document,
            sheetData = context.sheetData,
            labelColumnIndex = context.labelColumnIndex,
            startColumnIndex = context.startColumnIndex,
            endColumnIndex = context.endColumnIndex
        )
        for (columnIndex in context.startColumnIndex..context.endColumnIndex) {
            setCellStyle(
                context.document,
                context.sheetData,
                buildCellReference(columnIndex, 2),
                PLAIN_TIME_STYLE
            )
            setCellStyle(
                context.document,
                context.sheetData,
                buildCellReference(columnIndex, 3),
                PLAIN_INTEGER_STYLE
            )
        }
        setCellStyle(
            context.document,
            context.sheetData,
            buildCellReference(context.labelColumnIndex, 2),
            PLAIN_TEXT_STYLE
        )
        setCellStyle(
            context.document,
            context.sheetData,
            buildCellReference(context.labelColumnIndex, 3),
            PLAIN_TEXT_STYLE
        )
    }

    private fun writeAllowanceHeader(context: AllowanceSectionContext) {
        setStringCell(
            context.document,
            context.sheetData,
            buildCellReference(context.labelColumnIndex, 1),
            "Allowance",
            BOLD_TEXT_STYLE
        )
        context.allProjectNames.forEachIndexed { index, projectName ->
            val columnLetters = columnIndexToLetters(context.startColumnIndex + index)
            setStringCell(
                context.document,
                context.sheetData,
                "${columnLetters}1",
                projectName,
                ALLOWANCE_HEADER_STYLE
            )
        }
        val totalColumnLetters = columnIndexToLetters(context.totalColumnIndex)
        setStringCell(
            context.document,
            context.sheetData,
            "${totalColumnLetters}1",
            context.exportData.totalLabel,
            ALLOWANCE_TOTAL_HEADER_STYLE
        )
    }

    private fun writeAllowanceRows(context: AllowanceSectionContext) {
        context.exportData.allowanceRows.forEachIndexed { rowIndex, allowanceRow ->
            setStringCell(
                context.document,
                context.sheetData,
                buildCellReference(context.labelColumnIndex, rowIndex + 2),
                allowanceRow.label,
                PLAIN_TEXT_STYLE
            )
            context.allProjectNames.forEachIndexed { columnIndex, projectName ->
                val cellReference = buildCellReference(context.startColumnIndex + columnIndex, rowIndex + 2)
                setNumericCell(
                    context.document,
                    context.sheetData,
                    cellReference,
                    allowanceRow.countByProjectName.getValue(projectName).toDouble(),
                    ALLOWANCE_PROJECT_VALUE_STYLES[rowIndex]
                )
            }
            setNumericCell(
                context.document,
                context.sheetData,
                buildCellReference(context.totalColumnIndex, rowIndex + 2),
                allowanceRow.totalCount.toDouble(),
                ALLOWANCE_TOTAL_VALUE_STYLES[rowIndex]
            )
        }
    }

    private fun writeWorkTypeHeader(context: WorkTypeSectionContext) {
        setStringCell(
            context.document,
            context.sheetData,
            buildCellReference(context.labelColumnIndex, 1),
            "Work type",
            BOLD_TEXT_STYLE
        )
        context.allProjectNames.forEachIndexed { index, projectName ->
            setStringCell(
                context.document,
                context.sheetData,
                buildCellReference(context.startColumnIndex + index, 1),
                projectName,
                WORK_TYPE_HEADER_STYLE
            )
        }
        setStringCell(
            context.document,
            context.sheetData,
            buildCellReference(context.totalColumnIndex, 1),
            context.exportData.totalLabel,
            WORK_TYPE_TOTAL_HEADER_STYLE
        )
    }

    private fun writeWorkTypeRows(context: WorkTypeSectionContext) {
        context.exportData.workTypeRows.forEachIndexed { rowIndex, workTypeRow ->
            setStringCell(
                context.document,
                context.sheetData,
                buildCellReference(context.labelColumnIndex, rowIndex + 2),
                workTypeRow.label,
                PLAIN_TEXT_STYLE
            )
            context.allProjectNames.forEachIndexed { columnIndex, projectName ->
                val value = workTypeRow.timeByProjectName.getValue(projectName)
                if (value > 0.0) {
                    setNumericCell(
                        context.document,
                        context.sheetData,
                        buildCellReference(context.startColumnIndex + columnIndex, rowIndex + 2),
                        value,
                        WORK_TYPE_VALUE_STYLES[rowIndex]
                    )
                }
            }
            if (workTypeRow.totalTime > 0.0) {
                setNumericCell(
                    context.document,
                    context.sheetData,
                    buildCellReference(context.totalColumnIndex, rowIndex + 2),
                    workTypeRow.totalTime,
                    WORK_TYPE_TOTAL_STYLES[rowIndex]
                )
            }
        }
    }

    private fun populateDailyEntries(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData,
        dailyEntriesRowOffset: Int
    ) {
        val rowOverflowDays = mutableSetOf<Int>()
        exportData.displayedEntriesByDay.forEach { (day, dayEntries) ->
            val column = dayToColumn(day)
            for ((index, entry) in dayEntries.withIndex()) {
                val baseRow = dailyEntryBaseRow(index) + dailyEntriesRowOffset
                if (baseRow + 4 > EXCEL_MAX_ROW_INDEX) {
                    rowOverflowDays += day
                    break
                }
                setStringCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column$baseRow",
                    value = entry.projectName,
                    styleIndex = PROJECT_NAME_STYLE
                )
                setNumericCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + 1}",
                    numericValue = entry.workTimeFraction,
                    styleIndex = WORK_TIME_STYLE
                )
                setStringCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + 2}",
                    value = entry.allowanceLabel,
                    styleIndex = PROJECT_NAME_STYLE
                )
                setStringCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + 3}",
                    value = entry.workType,
                    styleIndex = PROJECT_NAME_STYLE
                )
                setNumericCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = "$column${baseRow + 4}",
                    numericValue = entry.kilometres
                )
            }
        }
        if (rowOverflowDays.isNotEmpty()) {
            Log.w(
                LOG_TAG,
                "Daily entries exceeded Excel row limit; truncated days=$rowOverflowDays"
            )
        }
    }

    private fun dailyEntryBaseRow(entryIndex: Int): Int {
        return DAILY_ENTRIES_START_ROW + (entryIndex * DAILY_ENTRY_ROW_HEIGHT)
    }

    private fun clearCell(sheetData: Element, cellReference: String) {
        getCell(sheetData, cellReference)?.clearContents()
    }

    private fun setStringCell(
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

    private fun setNumericCell(
        document: Document,
        sheetData: Element,
        cellReference: String,
        numericValue: Double,
        styleIndex: Int? = null
    ) {
        val cell = getOrCreateCell(document, sheetData, cellReference, styleIndex)
        cell.clearContents()
        cell.removeAttribute("t")
        val valueElement = document.createElementNS(SPREADSHEET_NAMESPACE, "v")
        valueElement.textContent = numericValue.toExcelNumberString()
        cell.appendChild(valueElement)
    }

    private fun setCellStyle(
        document: Document,
        sheetData: Element,
        cellReference: String,
        styleIndex: Int
    ) {
        getOrCreateCell(document, sheetData, cellReference, styleIndex)
    }

    private fun applySectionHeaderStyles(
        document: Document,
        sheetData: Element,
        labelColumnIndex: Int,
        startColumnIndex: Int,
        endColumnIndex: Int
    ) {
        for (columnIndex in startColumnIndex..endColumnIndex) {
            val firstRowStyle = if (columnIndex == labelColumnIndex) {
                BOLD_TEXT_STYLE
            } else {
                PLAIN_TEXT_STYLE
            }
            setCellStyle(document, sheetData, buildCellReference(columnIndex, 1), firstRowStyle)
        }
    }

    private fun applySectionBodyStyles(
        document: Document,
        sheetData: Element,
        spec: SectionBodyStyleSpec
    ) {
        for (rowNumber in spec.rowRange) {
            setCellStyle(
                document,
                sheetData,
                buildCellReference(spec.labelColumnIndex, rowNumber),
                PLAIN_TEXT_STYLE
            )
            for (columnIndex in spec.valueColumnRange) {
                setCellStyle(
                    document,
                    sheetData,
                    buildCellReference(columnIndex, rowNumber),
                    spec.valueStyle
                )
            }
        }
    }

    private fun getCell(sheetData: Element, cellReference: String): Element? {
        val row = getRow(sheetData, extractRowNumber(cellReference)) ?: return null
        return row.childElementSequence("c").firstOrNull { it.getAttribute("r") == cellReference }
    }

    private fun getRow(sheetData: Element, rowNumber: Int): Element? {
        return sheetData.childElementSequence("row")
            .firstOrNull { it.getAttribute("r") == rowNumber.toString() }
    }

    private fun getOrCreateCell(
        document: Document,
        sheetData: Element,
        cellReference: String,
        styleIndex: Int?
    ): Element {
        val rowNumber = extractRowNumber(cellReference)
        val row = getOrCreateRow(document, sheetData, rowNumber)
        val existingCell = row.childElementSequence("c")
            .firstOrNull { it.getAttribute("r") == cellReference }
        if (existingCell != null) {
            if (styleIndex != null) {
                existingCell.setAttribute("s", styleIndex.toString())
            }
            return existingCell
        }

        val newCell = document.createElementNS(SPREADSHEET_NAMESPACE, "c")
        newCell.setAttribute("r", cellReference)
        styleIndex?.let { newCell.setAttribute("s", it.toString()) }
        val newColumnIndex = extractColumnIndex(cellReference)
        val nextCell = row.childElementSequence("c")
            .firstOrNull { extractColumnIndex(it.getAttribute("r")) > newColumnIndex }
        if (nextCell != null) {
            row.insertBefore(newCell, nextCell)
        } else {
            row.appendChild(newCell)
        }
        return newCell
    }

    private fun getOrCreateRow(document: Document, sheetData: Element, rowNumber: Int): Element {
        getRow(sheetData, rowNumber)?.let { return it }
        val newRow = document.createElementNS(SPREADSHEET_NAMESPACE, "row")
        newRow.setAttribute("r", rowNumber.toString())
        newRow.setAttribute("spans", "1:32")
        val nextRow = sheetData.childElementSequence("row")
            .firstOrNull { it.getAttribute("r").toInt() > rowNumber }
        if (nextRow != null) {
            sheetData.insertBefore(newRow, nextRow)
        } else {
            sheetData.appendChild(newRow)
        }
        return newRow
    }
}

// Workbook ZIP-level editing.
internal object TimesheetWorkbookEditor {
    fun createWorkbook(templateBytes: ByteArray, exportData: TimesheetExportData): ByteArray {
        val zipEntries = unzipEntries(templateBytes)
        zipEntries["[Content_Types].xml"] = removeCalcChainContentType(
            zipEntries.getValue("[Content_Types].xml")
        )
        zipEntries["xl/_rels/workbook.xml.rels"] = removeCalcChainRelationship(
            zipEntries.getValue("xl/_rels/workbook.xml.rels")
        )
        zipEntries.remove("xl/calcChain.xml")
        val updatedSheetXml = TimesheetSheetEditor.updateSheet(
            sheetXml = zipEntries.getValue("xl/worksheets/sheet1.xml"),
            exportData = exportData
        )
        zipEntries["xl/worksheets/sheet1.xml"] = normalizeStyleReferences(
            sheetXml = updatedSheetXml,
            stylesXml = zipEntries.getValue("xl/styles.xml")
        )
        return zipEntries(zipEntries)
    }

    private fun normalizeStyleReferences(sheetXml: ByteArray, stylesXml: ByteArray): ByteArray {
        val stylesDocument = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(stylesXml))
        val xfCount = (stylesDocument.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "cellXfs")
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
    val summaryProjectTimes: Map<String, Double>,
    val summaryProjectKilometres: Map<String, Double>,
    val totalWorkTime: Double,
    val totalKilometres: Double,
    val allowanceRows: List<TimesheetAllowanceSummaryRow>,
    val workTypeRows: List<TimesheetWorkTypeSummaryRow>,
    val displayedEntriesByDay: Map<Int, List<TimesheetEntry>>,
    val overflowedDays: List<Int>,
    val hiddenProjectNames: List<String>,
    val hiddenWorkTypes: List<String>
)

internal data class TimesheetAllowanceSummaryRow(
    val label: String,
    val countByProjectName: Map<String, Int>,
    val totalCount: Int
)

internal data class TimesheetWorkTypeSummaryRow(
    val label: String,
    val timeByProjectName: Map<String, Double>,
    val totalTime: Double
)

internal data class TimesheetEntry(
    val dayOfMonth: Int,
    val projectName: String,
    val workTimeFraction: Double,
    val allowanceType: TimesheetAllowanceType,
    val allowanceLabel: String,
    val workType: String,
    val kilometres: Double
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
    val workTimeFraction = projectTime.toExcelTimeFraction()
    val isValidEntry = workTimeFraction > 0.0
    return if (isValidEntry) {
        val normalizedProjectName = projectName.trim()
        val normalizedWorkType = workType.trim().ifBlank { labels.defaultWorkTypeLabel }
        val allowanceType = allowance.toAllowanceType(labels)
        TimesheetEntry(
            dayOfMonth = parsedDate.dayOfMonth,
            projectName = normalizedProjectName,
            workTimeFraction = workTimeFraction,
            allowanceType = allowanceType,
            allowanceLabel = allowanceType.toExportLabel(labels),
            workType = normalizedWorkType,
            kilometres = kilometres.toDoubleOrNull() ?: 0.0
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

private fun String.toExcelTimeFraction(): Double {
    val normalized = trim()
    val isNegative = normalized.startsWith("-")
    val parts = normalized.removePrefix("-").split(':')
    val hours = parts.getOrNull(index = 0)?.toLongOrNull()
    val minutes = parts.getOrNull(index = 1)?.toLongOrNull()
    val isValidTime = normalized.isNotBlank() && parts.size == 2 && hours != null && minutes != null
    return if (isValidTime) {
        val totalMinutes = (hours * 60) + minutes
        val signedMinutes = if (isNegative) -totalMinutes else totalMinutes
        signedMinutes / 1440.0
    } else {
        0.0
    }
}

private fun Double.toExcelNumberString(): String {
    return BigDecimal.valueOf(this)
        .setScale(15, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}

private fun LocalDate.toExcelSerialDate(): Double {
    val excelEpoch = LocalDate.of(1899, 12, 30)
    return ChronoUnit.DAYS.between(excelEpoch, this).toDouble()
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
