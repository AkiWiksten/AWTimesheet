package com.akiwiksten.awtimesheet.feature.timesheet.model

import org.w3c.dom.Document
import org.w3c.dom.Element

internal data class ProjectSummarySectionContext(
    val document: Document,
    val sheetData: Element,
    val exportData: TimesheetExportData,
    val labelColumnIndex: Int,
    val projectSummaryColumns: List<String>,
    val totalColumnLetters: String,
    val startColumnIndex: Int,
    val endColumnIndex: Int
)

internal data class AllowanceSectionContext(
    val document: Document,
    val sheetData: Element,
    val exportData: TimesheetExportData,
    val allProjectNames: List<String>,
    val labelColumnIndex: Int,
    val startColumnIndex: Int,
    val totalColumnIndex: Int
)

internal data class WorkTypeSectionContext(
    val document: Document,
    val sheetData: Element,
    val exportData: TimesheetExportData,
    val allProjectNames: List<String>,
    val labelColumnIndex: Int,
    val startColumnIndex: Int,
    val totalColumnIndex: Int
)

internal data class SectionBodyStyleSpec(
    val labelColumnIndex: Int,
    val valueColumnRange: IntRange,
    val rowRange: IntRange,
    val valueStyle: Int
)
