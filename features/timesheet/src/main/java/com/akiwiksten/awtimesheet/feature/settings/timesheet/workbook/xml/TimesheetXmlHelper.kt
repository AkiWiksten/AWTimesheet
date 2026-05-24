@file:Suppress("NestedBlockDepth", "TooManyFunctions")

package com.akiwiksten.awtimesheet.feature.settings.timesheet.workbook.xml

import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.BOLD_TEXT_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.DAILY_ENTRIES_START_ROW
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.DAILY_ENTRY_ROW_HEIGHT
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.PLAIN_TEXT_STYLE
import com.akiwiksten.awtimesheet.feature.settings.timesheet.model.SectionBodyStyleSpec
import com.akiwiksten.awtimesheet.feature.settings.timesheet.workbook.buildCellReference
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

internal object TimesheetXmlHelper {
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
                shiftCellDownIfPresent(
                    document = document,
                    sheetData = sheetData,
                    currentRow = currentRow,
                    columnIndex = columnIndex
                )
            }
        }

        // Ensure the inserted row is blank in the requested range.
        for (columnIndex in startColumn..endColumn) {
            clearCell(sheetData, buildCellReference(columnIndex, rowNumber))
        }
    }

    private fun insertCellInRowOrder(row: Element, cell: Element) {
        val newColumnIndex = columnIndex(cell.getAttribute("r"))
        val nextCell = row.childElementSequence("c")
            .firstOrNull { columnIndex(it.getAttribute("r")) > newColumnIndex }
        if (nextCell != null) row.insertBefore(cell, nextCell) else row.appendChild(cell)
    }

    private fun shiftCellDownIfPresent(
        document: Document,
        sheetData: Element,
        currentRow: Int,
        columnIndex: Int
    ) {
        val sourceRef = buildCellReference(columnIndex, currentRow)
        val sourceCell = getCell(sheetData, sourceRef)
        val sourceRow = getRow(sheetData, currentRow)
        if (sourceCell != null && sourceRow != null) {
            val targetRef = buildCellReference(columnIndex, currentRow + 1)
            getCell(sheetData, targetRef)?.let { existingTarget ->
                existingTarget.parentNode?.removeChild(existingTarget)
            }

            val targetRow = getOrCreateRow(document, sheetData, currentRow + 1)
            sourceRow.removeChild(sourceCell)
            sourceCell.setAttribute("r", targetRef)
            insertCellInRowOrder(targetRow, sourceCell)
        }
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
        val newColumnIndex = columnIndex(cellReference)
        val nextCell = row.childElementSequence("c")
            .firstOrNull { columnIndex(it.getAttribute("r")) > newColumnIndex }
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

private fun extractRowNumber(cellReference: String): Int {
    return cellReference.dropWhile { it.isLetter() }.toInt()
}

internal fun columnIndex(cellReference: String): Int {
    val letters = cellReference.takeWhile { it.isLetter() }
    var result = 0
    letters.forEach { character ->
        result = (result * 26) + (character.code - 'A'.code + 1)
    }
    return result
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
