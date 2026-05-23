@file:Suppress("NestedBlockDepth", "ReturnCount")

package com.akiwiksten.awtimesheet.feature.settings.timesheet

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

internal fun ByteArray.readWorksheetXml(entryPath: String): org.w3c.dom.Document {
    val xmlBytes = readZipEntryBytes(entryPath)
    return DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
}

internal fun loadTemplateBytes(): ByteArray {
    val templatePathCandidates = listOf(
        Paths.get("src", "main", "assets", "timesheet_template.xlsx"),
        Paths.get("app", "src", "main", "assets", "timesheet_template.xlsx")
    )
    val templatePath = templatePathCandidates.firstOrNull(Files::exists)
        ?: error("timesheet_template.xlsx not found in known asset paths")
    return Files.readAllBytes(templatePath)
}

internal fun org.w3c.dom.Document.cellSharedString(cellReference: String, workbookBytes: ByteArray): String? {
    val cell = findCell(cellReference) ?: return null
    if (cell.getAttribute("t") != "s") return null
    val sharedStringIndex = cell.getElementsByTagNameNS(
        "http://schemas.openxmlformats.org/spreadsheetml/2006/main", "v"
    ).item(0)?.textContent?.toIntOrNull() ?: return null

    val sharedStringsBytes = workbookBytes.readZipEntryBytes("xl/sharedStrings.xml")
    val ssDoc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(java.io.ByteArrayInputStream(sharedStringsBytes))
    val siNodes = ssDoc.getElementsByTagNameNS(
        "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
        "si"
    )
    val si = siNodes.item(sharedStringIndex) ?: return null
    return (si as? org.w3c.dom.Element)
        ?.getElementsByTagNameNS("http://schemas.openxmlformats.org/spreadsheetml/2006/main", "t")
        ?.item(0)?.textContent
}

internal fun org.w3c.dom.Document.cellInlineString(cellReference: String): String? {
    val textNodes = findCell(cellReference)?.getElementsByTagNameNS(
        "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
        "t"
    )
    return textNodes?.itemOrNull(0)?.textContent
}

internal fun org.w3c.dom.Document.cellNumericValue(cellReference: String): String? {
    val valueNodes = findCell(cellReference)?.getElementsByTagNameNS(
        "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
        "v"
    )
    return valueNodes?.itemOrNull(0)?.textContent
}

internal fun ByteArray.readZipEntryText(entryPath: String): String {
    return readZipEntryBytes(entryPath).toString(Charsets.UTF_8)
}

internal fun ByteArray.hasZipEntry(entryPath: String): Boolean {
    val input = ZipInputStream(ByteArrayInputStream(this))
    try {
        var entry = input.nextEntry
        while (entry != null) {
            if (entry.name == entryPath) {
                return true
            }
            input.closeEntry()
            entry = input.nextEntry
        }
    } finally {
        input.close()
    }
    return false
}

internal fun ByteArray.maxSheetStyleIndex(sheetPath: String = "xl/worksheets/sheet1.xml"): Int {
    val sheetDoc = readWorksheetXml(sheetPath)
    val cells = sheetDoc.getElementsByTagNameNS(
        "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
        "c"
    )
    var maxStyle = -1
    for (index in 0 until cells.length) {
        val style = (cells.item(index) as? org.w3c.dom.Element)
            ?.getAttribute("s")
            ?.toIntOrNull()
        if (style != null && style > maxStyle) {
            maxStyle = style
        }
    }
    return maxStyle
}

internal fun ByteArray.cellXfCount(): Int {
    val stylesBytes = readZipEntryBytes("xl/styles.xml")
    val stylesDoc = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(ByteArrayInputStream(stylesBytes))
    val cellXfs = stylesDoc.getElementsByTagNameNS(
        "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
        "cellXfs"
    ).item(0) as? org.w3c.dom.Element ?: return 0
    return cellXfs.getElementsByTagNameNS(
        "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
        "xf"
    ).length
}

private fun org.w3c.dom.NodeList.asSequence(): Sequence<org.w3c.dom.Node> = sequence {
    for (index in 0 until length) {
        yield(item(index))
    }
}

private fun org.w3c.dom.Document.findCell(cellReference: String): org.w3c.dom.Element? {
    return getElementsByTagNameNS("http://schemas.openxmlformats.org/spreadsheetml/2006/main", "c")
        .asSequence()
        .map { it as org.w3c.dom.Element }
        .firstOrNull { it.getAttribute("r") == cellReference }
}

private fun org.w3c.dom.NodeList.itemOrNull(index: Int): org.w3c.dom.Node? {
    return if (index in 0 until length) item(index) else null
}

private fun ByteArray.readZipEntryBytes(entryPath: String): ByteArray {
    val input = ZipInputStream(ByteArrayInputStream(this))
    try {
        var entry = input.nextEntry
        while (entry != null) {
            if (entry.name == entryPath) {
                return input.readBytes()
            }
            input.closeEntry()
            entry = input.nextEntry
        }
    } finally {
        input.close()
    }
    error("Worksheet not found: $entryPath")
}
