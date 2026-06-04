@file:Suppress("MagicNumber", "TooManyFunctions")

package com.akiwiksten.awtimesheet.feature.timesheet.workbook

import android.content.Context
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.CONTENT_TYPES_NAMESPACE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.RELATIONSHIPS_NAMESPACE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.SPREADSHEET_NAMESPACE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.childElementSequence
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.createDocumentBuilderFactory
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.replaceSharedStrings
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.toByteArray
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val STYLES_XML_ENTRY = "xl/styles.xml"

internal object TimesheetWorkbookEditor {
    fun createWorkbook(
        templateBytes: ByteArray,
        exportData: TimesheetExportData,
        @Suppress("UNUSED_PARAMETER") ctx: Context? = null
    ): ByteArray {
        val zipEntries = unzipEntries(templateBytes)
        zipEntries["xl/sharedStrings.xml"] = localizeSharedStrings(
            sharedStringsXml = zipEntries.getValue("xl/sharedStrings.xml"),
            exportData = exportData
        )
        zipEntries["[Content_Types].xml"] = removeCalcChainContentType(
            zipEntries.getValue("[Content_Types].xml")
        )
        zipEntries["xl/_rels/workbook.xml.rels"] = removeCalcChainRelationship(
            zipEntries.getValue("xl/_rels/workbook.xml.rels")
        )
        zipEntries.remove("xl/calcChain.xml")
        zipEntries[STYLES_XML_ENTRY] = ensureLeftAlignmentInStyles(
            zipEntries.getValue(STYLES_XML_ENTRY)
        )
        val updatedSheetXml = TimesheetSheetEditor.updateSheet(
            sheetXml = zipEntries.getValue("xl/worksheets/sheet1.xml"),
            exportData = exportData
        )
        zipEntries["xl/worksheets/sheet1.xml"] = normalizeStyleReferences(
            sheetXml = updatedSheetXml,
            stylesXml = zipEntries.getValue(STYLES_XML_ENTRY)
        )
        return zipEntries(zipEntries)
    }

    private fun localizeSharedStrings(sharedStringsXml: ByteArray, exportData: TimesheetExportData): ByteArray {
        val labelMappings = mapOf(
            0 to exportData.dayOfMonthLabel,
            1 to exportData.projectNameLabel,
            2 to exportData.workTimeByDateLabel,
            3 to exportData.allowanceLabel,
            4 to exportData.workTypeLabel,
            5 to exportData.kilometresLabel,
            6 to exportData.employerLabel,
            7 to exportData.nameLabel,
            14 to exportData.workTimeTotalLabel,
            16 to exportData.totalSumLabel,
            17 to exportData.allowanceRows[0].label,
            18 to exportData.allowanceRows[1].label,
            19 to exportData.allowanceRows[2].label,
            20 to exportData.startDateLabel,
            21 to exportData.titleLabel,
            22 to exportData.endDateLabel,
            23 to exportData.generalLabel,
            24 to exportData.flexTimeTotalLabel,
            25 to exportData.projectTimeLabel
        )
        return replaceSharedStrings(sharedStringsXml, labelMappings)
    }

    private fun normalizeStyleReferences(sheetXml: ByteArray, stylesXml: ByteArray): ByteArray {
        val stylesDocument = createDocumentBuilderFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(stylesXml))
        val cellXfs = stylesDocument.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "cellXfs")
            .item(0) as? Element
        val xfCount = cellXfs?.childElementSequence("xf")?.count()
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
