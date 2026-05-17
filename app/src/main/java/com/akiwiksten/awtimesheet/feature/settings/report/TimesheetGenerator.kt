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
private val DAILY_SLOT_BASE_ROWS = listOf(8, 14, 20)
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
        val summaryWorkTypes = entries.extractDistinctWorkTypes()
        val entriesByDay = entries.groupBy { it.dayOfMonth }
        val displayedEntriesByDay = entriesByDay.mapValues { (_, dayEntries) ->
            dayEntries.take(MAX_DAILY_ENTRIES)
        }
        val allowanceRows = buildAllowanceRows(entries, allProjectNames, labels)
        val workTypeRows = buildWorkTypeRows(entries, allProjectNames, summaryWorkTypes)

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
            summaryProjectTimes = allProjectNames.associateWith { projectName ->
                entries.filter { it.projectName == projectName }.sumOf { it.workTimeFraction }
            },
            summaryProjectKilometres = allProjectNames.associateWith { projectName ->
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
            hiddenProjectNames = allProjectNames.drop(MAX_SUMMARY_PROJECTS),
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
    allProjectNames: List<String>,
    labels: TimesheetLabels
): List<TimesheetAllowanceSummaryRow> {
    return ALLOWANCE_ORDER.map { allowanceType ->
        TimesheetAllowanceSummaryRow(
            label = allowanceType.toExportLabel(labels),
            countByProjectName = allProjectNames.associateWith { projectName ->
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
    allProjectNames: List<String>,
    summaryWorkTypes: List<String>
): List<TimesheetWorkTypeSummaryRow> {
    return summaryWorkTypes.map { workType ->
        TimesheetWorkTypeSummaryRow(
            label = workType,
            timeByProjectName = allProjectNames.associateWith { projectName ->
                entries.filter { entry ->
                    entry.projectName == projectName && entry.workType == workType
                }.sumOf { it.workTimeFraction }
            },
            totalTime = entries.filter { it.workType == workType }.sumOf { it.workTimeFraction }
        )
    }
}

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
        val updatedSheetXml = updateSheet(
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
            ?: return sheetXml
        if (xfCount <= 0) return sheetXml

        val sheetDocument = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(sheetXml))
        val cells = sheetDocument.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "c")
        for (index in 0 until cells.length) {
            val cell = cells.item(index) as? Element ?: continue
            val styleIndex = cell.getAttribute("s").toIntOrNull() ?: continue
            if (styleIndex >= xfCount) {
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

    private fun firstRowEndColumnIndex(exportData: TimesheetExportData): Int {
        val allProjectNames = exportData.summaryProjectNames + exportData.hiddenProjectNames
        val projectCount = allProjectNames.size
        val workTypeStartColumnIndex = workTypeLabelColumnIndex(projectCount) + 1
        return workTypeStartColumnIndex + projectCount
    }

    private class FirstRowBoldResult(
        val sheetXml: ByteArray,
        val stylesXml: ByteArray
    )

    private fun applyFirstRowBoldOnly(
        sheetXml: ByteArray,
        stylesXml: ByteArray,
        firstRowEndColumnIndex: Int
    ): FirstRowBoldResult {
        val stylesDocument = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(stylesXml))
        val boldStyleByBaseStyle = buildBoldStyleMapping(stylesDocument)

        val sheetDocument = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(sheetXml))
        val sheetData = sheetDocument.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetData")
            .item(0) as? Element ?: return FirstRowBoldResult(sheetXml = sheetXml, stylesXml = stylesDocument.toByteArray())

        applyBoldStylesToFirstRow(
            document = sheetDocument,
            sheetData = sheetData,
            boldStyleByBaseStyle = boldStyleByBaseStyle,
            firstRowEndColumnIndex = firstRowEndColumnIndex
        )

        return FirstRowBoldResult(
            sheetXml = sheetDocument.toByteArray(),
            stylesXml = stylesDocument.toByteArray()
        )
    }

    private fun buildBoldStyleMapping(stylesDocument: Document): Map<Int, Int> {
        val cellXfs = stylesDocument.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "cellXfs")
            .item(0) as? Element ?: return emptyMap()
        val fonts = stylesDocument.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "fonts")
            .item(0) as? Element ?: return emptyMap()

        val fontElements = fonts.childElementSequence("font").toMutableList()
        val boldFontByBaseFont = mutableMapOf<Int, Int>()

        fun resolveBoldFontId(fontId: Int): Int {
            boldFontByBaseFont[fontId]?.let { return it }
            val baseFont = fontElements.getOrNull(fontId) ?: return fontId

            if (baseFont.childElementSequence("b").any()) {
                boldFontByBaseFont[fontId] = fontId
                return fontId
            }

            val boldFont = baseFont.cloneNode(true) as Element
            boldFont.appendChild(stylesDocument.createElementNS(SPREADSHEET_NAMESPACE, "b"))
            fonts.appendChild(boldFont)

            val boldFontId = fontElements.size
            fontElements += boldFont
            boldFontByBaseFont[fontId] = boldFontId
            fonts.setAttribute("count", fontElements.size.toString())
            return boldFontId
        }

        val xfElements = cellXfs.childElementSequence("xf").toMutableList()
        val boldStyleByBaseStyle = mutableMapOf<Int, Int>()
        val originalXfCount = xfElements.size

        for (styleIndex in 0 until originalXfCount) {
            val xf = xfElements[styleIndex]
            val baseFontId = xf.getAttribute("fontId").toIntOrNull() ?: 0
            val boldFontId = resolveBoldFontId(baseFontId)

            if (boldFontId == baseFontId) {
                boldStyleByBaseStyle[styleIndex] = styleIndex
                continue
            }

            val boldXf = xf.cloneNode(true) as Element
            boldXf.setAttribute("fontId", boldFontId.toString())
            boldXf.setAttribute("applyFont", "1")
            cellXfs.appendChild(boldXf)

            val boldStyleIndex = xfElements.size
            xfElements += boldXf
            boldStyleByBaseStyle[styleIndex] = boldStyleIndex
            cellXfs.setAttribute("count", xfElements.size.toString())
        }

        return boldStyleByBaseStyle
    }

    private fun applyBoldStylesToFirstRow(
        document: Document,
        sheetData: Element,
        boldStyleByBaseStyle: Map<Int, Int>,
        firstRowEndColumnIndex: Int
    ) {
        for (columnIndex in 1..firstRowEndColumnIndex) {
            val cellReference = buildCellReference(columnIndex, 1)
            val cell = getOrCreateCell(document, sheetData, cellReference, styleIndex = null)
            val baseStyleIndex = cell.getAttribute("s").toIntOrNull() ?: 0
            val boldStyleIndex = boldStyleByBaseStyle[baseStyleIndex]
                ?: boldStyleByBaseStyle[0]
                ?: baseStyleIndex
            cell.setAttribute("s", boldStyleIndex.toString())
        }
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

    private fun updateSheet(sheetXml: ByteArray, exportData: TimesheetExportData): ByteArray {
        val document = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(sheetXml))
        val sheetData = document.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "sheetData")
            .item(0) as Element

        clearDynamicCells(sheetData)
        // Keep the full top summary area deterministic even when some populate* calls are disabled.
        clearCellRange(sheetData, startColumnIndex = 4, endColumnIndex = 20, startRow = 1, endRow = 5)
        populateHeader(document, sheetData, exportData)
        populateDailyEntries(document, sheetData, exportData)
        populateProjectSummary(document, sheetData, exportData)
        populateAllowanceSummary(document, sheetData, exportData)
        populateWorkTypeSummary(document, sheetData, exportData)

        return document.toByteArray()
    }


    private fun clearDynamicCells(sheetData: Element) {
        listOf(
            "B2", "B3", "B4", "B5", "H1", "H2", "H3"
        ).forEach { cellReference ->
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

    private fun clearCellRange(
        sheetData: Element,
        startColumnIndex: Int,
        endColumnIndex: Int,
        startRow: Int,
        endRow: Int
    ) {
        for (row in startRow..endRow) {
            for (columnIndex in startColumnIndex..endColumnIndex) {
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
        val projectSummaryLabelColumnIndex = PROJECT_SUMMARY_START_COLUMN_INDEX - 1
        setStringCell(
            document = document,
            sheetData = sheetData,
            cellReference = buildCellReference(projectSummaryLabelColumnIndex, 1),
            value = exportData.generalLabel,
            styleIndex = BOLD_TEXT_STYLE
        )
        setStringCell(
            document = document,
            sheetData = sheetData,
            cellReference = buildCellReference(projectSummaryLabelColumnIndex, 2),
            value = exportData.workTimeTotalLabel,
            styleIndex = PLAIN_TEXT_STYLE
        )
        setStringCell(
            document = document,
            sheetData = sheetData,
            cellReference = buildCellReference(projectSummaryLabelColumnIndex, 3),
            value = exportData.kilometresLabel,
            styleIndex = PLAIN_TEXT_STYLE
        )

        val allProjectNames = exportData.summaryProjectNames + exportData.hiddenProjectNames
        val projectSummaryColumns = projectSummaryColumnLetters(allProjectNames.size)

        allProjectNames.forEachIndexed { index, projectName ->
            val columnLetters = projectSummaryColumns[index]
            setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = "${columnLetters}1",
                value = projectName,
                styleIndex = PROJECT_SUMMARY_HEADER_STYLE
            )
            setNumericCell(
                document = document,
                sheetData = sheetData,
                cellReference = "${columnLetters}2",
                numericValue = exportData.summaryProjectTimes.getValue(projectName),
                styleIndex = PROJECT_SUMMARY_WORK_TIME_STYLE
            )
            setNumericCell(
                document = document,
                sheetData = sheetData,
                cellReference = "${columnLetters}3",
                numericValue = exportData.summaryProjectKilometres.getValue(projectName),
                styleIndex = PROJECT_SUMMARY_KILOMETRES_STYLE
            )
        }

        val totalColumnLetters = projectSummaryTotalColumnLetters(allProjectNames.size)
        setStringCell(
            document = document,
            sheetData = sheetData,
            cellReference = "${totalColumnLetters}1",
            value = exportData.totalLabel,
            styleIndex = PROJECT_SUMMARY_TOTAL_HEADER_STYLE
        )
        setNumericCell(
            document = document,
            sheetData = sheetData,
            cellReference = "${totalColumnLetters}2",
            numericValue = exportData.totalWorkTime,
            styleIndex = PROJECT_SUMMARY_TOTAL_WORK_TIME_STYLE
        )
        setNumericCell(
            document = document,
            sheetData = sheetData,
            cellReference = "${totalColumnLetters}3",
            numericValue = exportData.totalKilometres,
            styleIndex = PROJECT_SUMMARY_TOTAL_KILOMETRES_STYLE
        )

        val summaryStartColumnIndex = projectSummaryLabelColumnIndex
        val summaryEndColumnIndex = projectSummaryTotalColumnIndex(allProjectNames.size)
        for (columnIndex in summaryStartColumnIndex..summaryEndColumnIndex) {
            val firstRowStyle = if (columnIndex == projectSummaryLabelColumnIndex) {
                BOLD_TEXT_STYLE
            } else {
                PLAIN_TEXT_STYLE
            }
            setCellStyle(document, sheetData, buildCellReference(columnIndex, 1), firstRowStyle)
            setCellStyle(document, sheetData, buildCellReference(columnIndex, 2), PLAIN_TIME_STYLE)
            setCellStyle(document, sheetData, buildCellReference(columnIndex, 3), PLAIN_INTEGER_STYLE)
        }
        setCellStyle(
            document,
            sheetData,
            buildCellReference(projectSummaryLabelColumnIndex, 2),
            PLAIN_TEXT_STYLE
        )
        setCellStyle(
            document,
            sheetData,
            buildCellReference(projectSummaryLabelColumnIndex, 3),
            PLAIN_TEXT_STYLE
        )
    }

    private fun populateAllowanceSummary(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData
    ) {
        val allProjectNames = exportData.summaryProjectNames + exportData.hiddenProjectNames
        val allowanceLabelColumnIndex = allowanceLabelColumnIndex(allProjectNames.size)
        val allowanceStartColumnIndex = allowanceStartColumnIndex(allProjectNames.size)
        val allowanceTotalColumnIndex = allowanceStartColumnIndex + allProjectNames.size

        setStringCell(
            document = document,
            sheetData = sheetData,
            cellReference = buildCellReference(allowanceLabelColumnIndex, 1),
            value = "Allowance",
            styleIndex = BOLD_TEXT_STYLE
        )

        allProjectNames.forEachIndexed { index, projectName ->
            val columnLetters = columnIndexToLetters(allowanceStartColumnIndex + index)
            setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = "${columnLetters}1",
                value = projectName,
                styleIndex = ALLOWANCE_HEADER_STYLE
            )
        }
        val totalColumnLetters = columnIndexToLetters(allowanceTotalColumnIndex)
        setStringCell(
            document = document,
            sheetData = sheetData,
            cellReference = "${totalColumnLetters}1",
            value = exportData.totalLabel,
            styleIndex = ALLOWANCE_TOTAL_HEADER_STYLE
        )

        exportData.allowanceRows.forEachIndexed { rowIndex, allowanceRow ->
            setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = buildCellReference(allowanceLabelColumnIndex, rowIndex + 2),
                value = allowanceRow.label,
                styleIndex = PLAIN_TEXT_STYLE
            )
            allProjectNames.forEachIndexed { columnIndex, projectName ->
                val cellReference = buildCellReference(
                    columnIndex = allowanceStartColumnIndex + columnIndex,
                    rowNumber = rowIndex + 2
                )
                setNumericCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = cellReference,
                    numericValue = allowanceRow.countByProjectName.getValue(projectName).toDouble(),
                    styleIndex = ALLOWANCE_PROJECT_VALUE_STYLES[rowIndex]
                )
            }
            setNumericCell(
                document = document,
                sheetData = sheetData,
                cellReference = buildCellReference(
                    columnIndex = allowanceTotalColumnIndex,
                    rowNumber = rowIndex + 2
                ),
                numericValue = allowanceRow.totalCount.toDouble(),
                styleIndex = ALLOWANCE_TOTAL_VALUE_STYLES[rowIndex]
            )
        }

        for (columnIndex in allowanceLabelColumnIndex..allowanceTotalColumnIndex) {
            val firstRowStyle = if (columnIndex == allowanceLabelColumnIndex) {
                BOLD_TEXT_STYLE
            } else {
                PLAIN_TEXT_STYLE
            }
            setCellStyle(document, sheetData, buildCellReference(columnIndex, 1), firstRowStyle)
        }
        for (rowNumber in 2..4) {
            setCellStyle(document, sheetData, buildCellReference(allowanceLabelColumnIndex, rowNumber), PLAIN_TEXT_STYLE)
            for (columnIndex in allowanceStartColumnIndex..allowanceTotalColumnIndex) {
                setCellStyle(document, sheetData, buildCellReference(columnIndex, rowNumber), PLAIN_INTEGER_STYLE)
            }
        }
    }

    private fun populateWorkTypeSummary(
        document: Document,
        sheetData: Element,
        exportData: TimesheetExportData
    ) {
        val allProjectNames = exportData.summaryProjectNames + exportData.hiddenProjectNames
        val workTypeLabelColumnIndex = workTypeLabelColumnIndex(allProjectNames.size)
        val workTypeStartColumnIndex = workTypeLabelColumnIndex + 1
        val workTypeTotalColumnIndex = workTypeStartColumnIndex + allProjectNames.size

        setStringCell(
            document = document,
            sheetData = sheetData,
            cellReference = buildCellReference(workTypeLabelColumnIndex, 1),
            value = "Work type",
            styleIndex = BOLD_TEXT_STYLE
        )

        allProjectNames.forEachIndexed { index, projectName ->
            setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = buildCellReference(workTypeStartColumnIndex + index, 1),
                value = projectName,
                styleIndex = WORK_TYPE_HEADER_STYLE
            )
        }
        setStringCell(
            document = document,
            sheetData = sheetData,
            cellReference = buildCellReference(workTypeTotalColumnIndex, 1),
            value = exportData.totalLabel,
            styleIndex = WORK_TYPE_TOTAL_HEADER_STYLE
        )

        exportData.workTypeRows.forEachIndexed { rowIndex, workTypeRow ->
            setStringCell(
                document = document,
                sheetData = sheetData,
                cellReference = buildCellReference(workTypeLabelColumnIndex, rowIndex + 2),
                value = workTypeRow.label,
                styleIndex = PLAIN_TEXT_STYLE
            )
            allProjectNames.forEachIndexed { columnIndex, projectName ->
                val value = workTypeRow.timeByProjectName.getValue(projectName)
                if (value > 0.0) {
                    setNumericCell(
                        document = document,
                        sheetData = sheetData,
                        cellReference = buildCellReference(
                            workTypeStartColumnIndex + columnIndex,
                            rowIndex + 2
                        ),
                        numericValue = value,
                        styleIndex = WORK_TYPE_VALUE_STYLES[rowIndex]
                    )
                }
            }
            if (workTypeRow.totalTime > 0.0) {
                setNumericCell(
                    document = document,
                    sheetData = sheetData,
                    cellReference = buildCellReference(workTypeTotalColumnIndex, rowIndex + 2),
                    numericValue = workTypeRow.totalTime,
                    styleIndex = WORK_TYPE_TOTAL_STYLES[rowIndex]
                )
            }
        }

        for (columnIndex in workTypeLabelColumnIndex..workTypeTotalColumnIndex) {
            val firstRowStyle = if (columnIndex == workTypeLabelColumnIndex) {
                BOLD_TEXT_STYLE
            } else {
                PLAIN_TEXT_STYLE
            }
            setCellStyle(document, sheetData, buildCellReference(columnIndex, 1), firstRowStyle)
        }
        for (rowNumber in 2..4) {
            setCellStyle(document, sheetData, buildCellReference(workTypeLabelColumnIndex, rowNumber), PLAIN_TEXT_STYLE)
            for (columnIndex in workTypeStartColumnIndex..workTypeTotalColumnIndex) {
                setCellStyle(document, sheetData, buildCellReference(columnIndex, rowNumber), PLAIN_TIME_STYLE)
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

    private fun setCellStyle(
        document: Document,
        sheetData: Element,
        cellReference: String,
        styleIndex: Int
    ) {
        getOrCreateCell(document, sheetData, cellReference, styleIndex)
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

private fun List<TimesheetEntry>.extractDistinctWorkTypes(): List<String> {
    return allDistinctWorkTypes().take(MAX_SUMMARY_WORK_TYPES)
}

private fun List<TimesheetEntry>.allDistinctWorkTypes(): List<String> {
    return map { it.workType }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun projectSummaryColumnLetters(projectCount: Int): List<String> {
    val startIndex = projectSummaryStartColumnIndex(projectCount)
    return (0 until projectCount).map { offset ->
        columnIndexToLetters(startIndex + offset)
    }
}

private fun projectSummaryTotalColumnLetters(projectCount: Int): String {
    return columnIndexToLetters(projectSummaryTotalColumnIndex(projectCount))
}

private fun projectSummaryStartColumnIndex(projectCount: Int): Int {
    return PROJECT_SUMMARY_START_COLUMN_INDEX
}

private fun projectSummaryTotalColumnIndex(projectCount: Int): Int {
    return projectSummaryStartColumnIndex(projectCount) + projectCount
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
