@file:Suppress("NestedBlockDepth", "ReturnCount")

package com.akiwiksten.awtimesheet.feature.settings.report

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

