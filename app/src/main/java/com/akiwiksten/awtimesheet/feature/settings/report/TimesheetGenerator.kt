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
private const val MAX_DAILY_ENTRIES = 3
private const val MAX_SUMMARY_PROJECTS = 3
private const val MAX_SUMMARY_WORK_TYPES = 3
private const val LOG_TAG = "TimesheetGenerator"
private const val PROJECT_NAME_STYLE = 5
private const val WORK_TIME_STYLE = 6
private const val START_DATE_STYLE = 28
private const val END_DATE_STYLE = 29
private const val WORK_TYPE_THIRD_ROW_LABEL_STYLE = 7
private val DAILY_SLOT_BASE_ROWS = listOf(8, 14, 20)
private val PROJECT_NAME_HEADER_CELLS = listOf("E1", "F1", "G1")
private val PROJECT_TIME_SUMMARY_CELLS = listOf("E2", "F2", "G2")
private val PROJECT_KILOMETRES_SUMMARY_CELLS = listOf("E3", "F3", "G3")
private val ALLOWANCE_LABEL_CELLS = listOf("J2", "J3", "J4")
private val ALLOWANCE_COUNT_CELLS = listOf(
    listOf("K2", "L2", "M2"),
    listOf("K3", "L3", "M3"),
    listOf("K4", "L4", "M4")
)
private val ALLOWANCE_TOTAL_CELLS = listOf("N2", "N3", "N4")
private val WORK_TYPE_LABEL_CELLS = listOf("P2", "P3", "P4")
private val WORK_TYPE_VALUE_CELLS = listOf(
    listOf("Q2", "R2", "S2"),
    listOf("Q3", "R3", "S3"),
    listOf("Q4", "R4", "S4")
)
private val WORK_TYPE_TOTAL_CELLS = listOf("T2", "T3", "T4")
private val WORK_TYPE_VALUE_STYLES = listOf(16, 23, 23)
private val WORK_TYPE_TOTAL_STYLES = listOf(17, 24, 24)
private val ALLOWANCE_ORDER = listOf(
    TimesheetAllowanceType.NONE,
    TimesheetAllowanceType.HALF_DAY,
    TimesheetAllowanceType.FULL
)

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
    val fullAllowanceExportLabel: String
)

internal object TimesheetExportDataBuilder {
    fun build(params: GenerateTimesheetParams): TimesheetExportData {
        val labels = params.toTimesheetLabels()
        val endDate = LocalDate.parse(params.endOfMonthDate)
        val startDate = endDate.withDayOfMonth(1)
        val entries = params.projectsByMonth.toSortedTimesheetEntries(labels)

        val summaryProjectNames = entries.extractDistinctProjectNames()
        val summaryWorkTypes = entries.extractDistinctWorkTypes()
        val entriesByDay = entries.groupBy { it.dayOfMonth }
        val displayedEntriesByDay = entriesByDay.mapValues { (_, dayEntries) ->
            dayEntries.take(MAX_DAILY_ENTRIES)
        }
        val allowanceRows = buildAllowanceRows(entries, summaryProjectNames, labels)
        val workTypeRows = buildWorkTypeRows(entries, summaryProjectNames, summaryWorkTypes)

        return TimesheetExportData(
            name = params.name,
            employer = params.employer,
            startDate = startDate,
            endDate = endDate,
            summaryProjectNames = summaryProjectNames,
            summaryProjectTimes = summaryProjectNames.associateWith { projectName ->
                entries.filter { it.projectName == projectName }.sumOf { it.workTimeFraction }
            },
            summaryProjectKilometres = summaryProjectNames.associateWith { projectName ->
                entries.filter { it.projectName == projectName }.sumOf { it.kilometres }
            },
            totalWorkTime = entries.sumOf { it.workTimeFraction },
            totalKilometres = entries.sumOf { it.kilometres },
            allowanceRows = allowanceRows,
            workTypeRows = workTypeRows,
            displayedEntriesByDay = displayedEntriesByDay,
            overflowedDays = entriesByDay
                .filterValues { dayEntries -> dayEntries.size > MAX_DAILY_ENTRIES }
                .keys
                .sorted(),
            hiddenProjectNames = entries.allDistinctProjectNames().drop(MAX_SUMMARY_PROJECTS),
            hiddenWorkTypes = entries.allDistinctWorkTypes().drop(MAX_SUMMARY_WORK_TYPES)
        )
    }
}

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
    entries: List<TimesheetEntry>,
    summaryProjectNames: List<String>,
    labels: TimesheetLabels
): List<TimesheetAllowanceSummaryRow> {
    return ALLOWANCE_ORDER.map { allowanceType ->
        TimesheetAllowanceSummaryRow(
            label = allowanceType.toExportLabel(labels),
            countByProjectName = summaryProjectNames.associateWith { projectName ->
                entries.count { entry ->
                    entry.projectName == projectName && entry.allowanceType == allowanceType
                }
            },
            totalCount = entries.count { it.allowanceType == allowanceType }
        )
    }
}

private fun buildWorkTypeRows(
    entries: List<TimesheetEntry>,
    summaryProjectNames: List<String>,
    summaryWorkTypes: List<String>
): List<TimesheetWorkTypeSummaryRow> {
    return summaryWorkTypes.map { workType ->
        TimesheetWorkTypeSummaryRow(
            label = workType,
            timeByProjectName = summaryProjectNames.associateWith { projectName ->
                entries.filter { entry ->
                    entry.projectName == projectName && entry.workType == workType
                }.sumOf { it.workTimeFraction }
            },
            totalTime = entries.filter { it.workType == workType }.sumOf { it.workTimeFraction }
        )
    }
}

private object TimesheetWorkbookEditor {
    fun createWorkbook(templateBytes: ByteArray, exportData: TimesheetExportData): ByteArray {
        val zipEntries = unzipEntries(templateBytes)
        zipEntries["[Content_Types].xml"] = removeCalcChainContentType(
            zipEntries.getValue("[Content_Types].xml")
        )
        zipEntries["xl/_rels/workbook.xml.rels"] = removeCalcChainRelationship(
            zipEntries.getValue("xl/_rels/workbook.xml.rels")
        )
        zipEntries.remove("xl/calcChain.xml")
        zipEntries["xl/worksheets/sheet1.xml"] = updateSheet(
            sheetXml = zipEntries.getValue("xl/worksheets/sheet1.xml"),
            exportData = exportData
        )
        return zipEntries(zipEntries)
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
        return contentTypesXml.toUtf8String()
            .replace(
                Regex("<Override PartName=\"/xl/calcChain\\.xml\"[^>]*/>"),
                ""
            )
            .toByteArray(Charsets.UTF_8)
    }

    private fun removeCalcChainRelationship(workbookRelsXml: ByteArray): ByteArray {
        return workbookRelsXml.toUtf8String()
            .replace(
                Regex("<Relationship Id=\"[^\"]+\" Type=\"[^\"]*/calcChain\" Target=\"calcChain\\.xml\"/>"),
                ""
            )
            .toByteArray(Charsets.UTF_8)
    }

    private fun updateSheet(sheetXml: ByteArray, exportData: TimesheetExportData): ByteArray {
        val document = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(sheetXml))
        val sheetData = document.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetData")
            .item(0) as Element

        clearDynamicCells(sheetData)
        populateHeader(document, sheetData, exportData)
        populateProjectSummary(document, sheetData, exportData)
        populateAllowanceSummary(document, sheetData, exportData)
        populateWorkTypeSummary(document, sheetData, exportData)
        populateDailyEntries(document, sheetData, exportData)

        return document.toByteArray()
    }

    private fun clearDynamicCells(sheetData: Element) {
        listOf(
            "B2", "B3", "B4", "B5", "H2", "H3"
        ).forEach { cellReference ->
            clearCell(sheetData, cellReference)
        }
        PROJECT_NAME_HEADER_CELLS.forEach { clearCell(sheetData, it) }
        PROJECT_TIME_SUMMARY_CELLS.forEach { clearCell(sheetData, it) }
        PROJECT_KILOMETRES_SUMMARY_CELLS.forEach { clearCell(sheetData, it) }
        ALLOWANCE_LABEL_CELLS.forEach { clearCell(sheetData, it) }
        ALLOWANCE_COUNT_CELLS.flatten().forEach { clearCell(sheetData, it) }
        ALLOWANCE_TOTAL_CELLS.forEach { clearCell(sheetData, it) }
        WORK_TYPE_LABEL_CELLS.forEach { clearCell(sheetData, it) }
        WORK_TYPE_VALUE_CELLS.flatten().forEach { clearCell(sheetData, it) }
        WORK_TYPE_TOTAL_CELLS.forEach { clearCell(sheetData, it) }

        DAILY_SLOT_BASE_ROWS.forEach { baseRow ->
            for (day in 1..31) {
                val column = dayToColumn(day)
                clearCell(sheetData, "$column$baseRow")
                clearCell(sheetData, "$column${baseRow + 1}")
                clearCell(sheetData, "$column${baseRow + 2}")
                clearCell(sheetData, "$column${baseRow + 3}")
                clearCell(sheetData, "$column${baseRow + 4}")
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
            numericValue = exportData.startDate.toExcelSerialDate(),
            styleIndex = START_DATE_STYLE
        )
        setNumericCell(
            document = document,
            sheetData = sheetData,
            cellReference = "B5",
            numericValue = exportData.endDate.toExcelSerialDate(),
            styleIndex = END_DATE_STYLE
        )
    }

    private fun populateProjectSummary(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData
    ) {
        exportData.summaryProjectNames.forEachIndexed { index, projectName ->
            setStringCell(document, sheetData, PROJECT_NAME_HEADER_CELLS[index], projectName)
            setNumericCell(
                document = document,
                sheetData = sheetData,
                cellReference = PROJECT_TIME_SUMMARY_CELLS[index],
                numericValue = exportData.summaryProjectTimes.getValue(projectName)
            )
            setNumericCell(
                document = document,
                sheetData = sheetData,
                cellReference = PROJECT_KILOMETRES_SUMMARY_CELLS[index],
                numericValue = exportData.summaryProjectKilometres.getValue(projectName)
            )
        }
        setNumericCell(document, sheetData, "H2", exportData.totalWorkTime)
        setNumericCell(document, sheetData, "H3", exportData.totalKilometres)
    }

    private fun populateAllowanceSummary(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData
    ) {
        exportData.allowanceRows.forEachIndexed { rowIndex, allowanceRow ->
            setStringCell(document, sheetData, ALLOWANCE_LABEL_CELLS[rowIndex], allowanceRow.label)
            exportData.summaryProjectNames.forEachIndexed { columnIndex, projectName ->
                setNumericCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = ALLOWANCE_COUNT_CELLS[rowIndex][columnIndex],
                    numericValue = allowanceRow.countByProjectName.getValue(projectName).toDouble()
                )
            }
            setNumericCell(
                document = document,
                sheetData = sheetData,
                cellReference = ALLOWANCE_TOTAL_CELLS[rowIndex],
                numericValue = allowanceRow.totalCount.toDouble()
            )
        }
    }

    private fun populateWorkTypeSummary(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData
    ) {
        exportData.workTypeRows.forEachIndexed { rowIndex, workTypeRow ->
            val labelStyle = if (rowIndex == 2) WORK_TYPE_THIRD_ROW_LABEL_STYLE else null
            setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = WORK_TYPE_LABEL_CELLS[rowIndex],
                value = workTypeRow.label,
                styleIndex = labelStyle
            )
            exportData.summaryProjectNames.forEachIndexed { columnIndex, projectName ->
                val value = workTypeRow.timeByProjectName.getValue(projectName)
                if (value > 0.0) {
                    setNumericCell(
                        document = document,
                        sheetData = sheetData,
                        cellReference = WORK_TYPE_VALUE_CELLS[rowIndex][columnIndex],
                        numericValue = value,
                        styleIndex = WORK_TYPE_VALUE_STYLES[rowIndex]
                    )
                }
            }
            if (workTypeRow.totalTime > 0.0) {
                setNumericCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = WORK_TYPE_TOTAL_CELLS[rowIndex],
                    numericValue = workTypeRow.totalTime,
                    styleIndex = WORK_TYPE_TOTAL_STYLES[rowIndex]
                )
            }
        }
    }

    private fun populateDailyEntries(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData
    ) {
        exportData.displayedEntriesByDay.forEach { (day, dayEntries) ->
            val column = dayToColumn(day)
            dayEntries.forEachIndexed { index, entry ->
                val baseRow = DAILY_SLOT_BASE_ROWS[index]
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
            if (styleIndex != null && !existingCell.hasAttribute("s")) {
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

internal data class TimesheetExportData(
    val name: String,
    val employer: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
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

private fun List<TimesheetEntry>.extractDistinctProjectNames(): List<String> {
    return allDistinctProjectNames().take(MAX_SUMMARY_PROJECTS)
}

private fun List<TimesheetEntry>.allDistinctProjectNames(): List<String> {
    return map { it.projectName }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun List<TimesheetEntry>.extractDistinctWorkTypes(): List<String> {
    return allDistinctWorkTypes().take(MAX_SUMMARY_WORK_TYPES)
}

private fun List<TimesheetEntry>.allDistinctWorkTypes(): List<String> {
    return map { it.workType }
        .filter { it.isNotBlank() }
        .distinct()
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

private fun ByteArray.toUtf8String(): String = String(this, Charsets.UTF_8)

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

