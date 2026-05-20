@file:Suppress("MagicNumber", "TooManyFunctions")

package com.akiwiksten.awtimesheet.feature.settings.report

import android.content.Context
import com.akiwiksten.awtimesheet.R
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
