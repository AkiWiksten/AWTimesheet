@file:Suppress("MagicNumber")

package com.akiwiksten.awtimesheet.feature.timesheet.workbook

import com.akiwiksten.awtimesheet.feature.timesheet.entry.DAILY_ENTRIES_SEPARATOR_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.entry.DAILY_ENTRIES_START_ROW
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.SPREADSHEET_NAMESPACE
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml.childElementSequence
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

internal object TimesheetFreezePaneEditor {
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
                val insertBeforeNode = worksheet.childNodes.asSequence()
                    .firstOrNull { candidate ->
                        candidate.nodeType == Node.ELEMENT_NODE &&
                            candidate.localName in setOf("sheetFormatPr", "cols", "sheetData")
                    }
                if (insertBeforeNode != null) {
                    worksheet.insertBefore(created, insertBeforeNode)
                } else {
                    worksheet.appendChild(created)
                }
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
                if (firstSelection != null) {
                    sheetView.insertBefore(created, firstSelection)
                } else {
                    sheetView.appendChild(created)
                }
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
            val rowNumber = row.getAttribute("r").toIntOrNull()
            if (rowNumber != null && isCandidateDayOfMonthRow(rowNumber)) {
                val dayCount = countDayMarkers(row)

                // Track the row with the most day markers (should be the day-of-month row)
                if (dayCount > bestCount) {
                    bestCount = dayCount
                    bestRow = rowNumber
                }
            }
        }

        // Return the row if we found at least 15 day markers (more than half)
        return if (bestCount >= 15) bestRow else null
    }

    private fun isCandidateDayOfMonthRow(rowNumber: Int): Boolean {
        return rowNumber in 2..(DAILY_ENTRIES_START_ROW + 50)
    }

    private fun countDayMarkers(row: Element): Int {
        var dayCount = 0
        row.childElementSequence("c").forEach { cell ->
            val valueNode = cell.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "v").item(0)
            val value = valueNode?.textContent?.toIntOrNull()
            if (value != null && value in 1..31) {
                dayCount++
            }
        }
        return dayCount
    }
}
private fun NodeList.asSequence(): Sequence<Node> = sequence {
    for (index in 0 until length) {
        val node = item(index)
        if (node != null) {
            yield(node)
        }
    }
}
