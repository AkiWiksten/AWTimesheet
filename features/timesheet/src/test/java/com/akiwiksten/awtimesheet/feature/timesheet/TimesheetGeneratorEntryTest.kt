package com.akiwiksten.awtimesheet.feature.timesheet

import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.timesheet.model.GenerateTimesheetParams
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportDataBuilder
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.TimesheetWorkbookEditor
import com.akiwiksten.awtimesheet.test.projectState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TimesheetGeneratorEntryTest {

    @Test
    fun build_mapsMonthlyProjectsIntoTimesheetLayoutData() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(
                listOf(
                    sampleProject(
                        ProjectSpec("2026-05-01", 0, "Project 1", "06:30", "70", "No allowance", "Other")
                    ),
                    sampleProject(
                        ProjectSpec("2026-05-01", 1, "Project 2", "01:30", "90", "No allowance", "Other")
                    ),
                    sampleProject(
                        ProjectSpec("2026-05-04", 0, "Project 2", "04:30", "40", "Half-day allowance", "Other")
                    ),
                    sampleProject(
                        ProjectSpec("2026-05-04", 1, "Project 3", "01:30", "120", "Full allowance", "Design")
                    ),
                    sampleProject(
                        ProjectSpec("2026-05-06", 0, "Project 1", "02:30", "70", "Full allowance", "Other")
                    ),
                    sampleProject(
                        ProjectSpec("2026-05-06", 1, "Project 2", "03:30", "90", "Full allowance", "Design")
                    ),
                    sampleProject(
                        ProjectSpec("2026-05-06", 2, "Project 3", "04:30", "120", "Full allowance", "Other")
                    )
                )
            )
        )

        assertProjectSummaries(exportData)
        assertDisplayedEntries(exportData)
        assertAllowanceSummaries(exportData)
        assertWorkTypeSummaries(exportData)
        assertTrue(exportData.overflowedDays.isEmpty())
        assertTrue(exportData.hiddenProjectNames.isEmpty())
        assertTrue(exportData.hiddenWorkTypes.isEmpty())
    }

    @Test
    fun build_limitsTemplateDataWhenMonthExceedsWorkbookCapacity() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(
                listOf(
                    sampleProject(
                        ProjectSpec("2026-05-01", 0, "Project 1", "01:00", "10", "No allowance", "Other")
                    ),
                    sampleProject(
                        ProjectSpec("2026-05-01", 1, "Project 2", "01:00", "20", "No allowance", "Type 2")
                    ),
                    sampleProject(
                        ProjectSpec("2026-05-01", 2, "Project 3", "01:00", "30", "Half-day allowance", "Type 3")
                    ),
                    sampleProject(
                        ProjectSpec("2026-05-01", 3, "Project 4", "01:00", "40", "Full allowance", "Type 4")
                    )
                )
            )
        )

        assertTrue(exportData.overflowedDays.isEmpty())
        assertEquals(listOf("Project 4"), exportData.hiddenProjectNames)
        assertTrue(exportData.hiddenWorkTypes.isEmpty())
        assertEquals(4, exportData.displayedEntriesByDay.getValue(1).size)
        assertEquals(listOf("Project 1", "Project 2", "Project 3"), exportData.summaryProjectNames)
        assertEquals(60L, exportData.summaryProjectTimes.getValue("Project 4"))
        assertEquals(40L, exportData.summaryProjectKilometres.getValue("Project 4"))
        assertEquals(0, exportData.allowanceRows[0].countByProjectName.getValue("Project 4"))
        assertEquals(0, exportData.allowanceRows[1].countByProjectName.getValue("Project 4"))
        assertEquals(1, exportData.allowanceRows[2].countByProjectName.getValue("Project 4"))
        assertEquals(0L, exportData.workTypeRows[0].timeByProjectName.getValue("Project 4"))
    }

    @Test
    fun createWorkbook_placesAllProjectSummaryColumnsContiguously_whenProjectsExceedTemplateBlock() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(
                listOf(
                    sampleProject(ProjectSpec("2026-05-01", 0, "Project 1", "01:00", "10", "No allowance", "Other")),
                    sampleProject(ProjectSpec("2026-05-01", 1, "Project 2", "01:00", "20", "No allowance", "Other")),
                    sampleProject(
                        ProjectSpec(
                            "2026-05-01",
                            2,
                            "Project 3",
                            "01:00",
                            "30",
                            "Half-day allowance",
                            "Other"
                        )
                    ),
                    sampleProject(ProjectSpec("2026-05-01", 3, "Project 4", "01:00", "40", "Full allowance", "Other"))
                )
            )
        )
        val templateBytes = loadTemplateBytes()

        val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
            templateBytes = templateBytes,
            exportData = exportData,
            ctx = RuntimeEnvironment.getApplication()
        )
        val sheetXml = workbookBytes.readWorksheetXml("xl/worksheets/sheet1.xml")

        assertEquals("General", sheetXml.cellInlineString("D1"))
        assertEquals("Work time total", sheetXml.cellInlineString("D2"))
        assertEquals("Kilometres", sheetXml.cellInlineString("D3"))
        assertEquals("Project 1", sheetXml.cellInlineString("E1"))
        assertEquals("Project 2", sheetXml.cellInlineString("F1"))
        assertEquals("Project 3", sheetXml.cellInlineString("G1"))
        assertEquals("Project 4", sheetXml.cellInlineString("H1"))
        assertEquals("Total", sheetXml.cellInlineString("I1"))
        assertNotNull(sheetXml.cellNumericValue("E2"))
        assertNotNull(sheetXml.cellNumericValue("H3"))
        assertNotNull(sheetXml.cellNumericValue("I2"))
        assertNotNull(sheetXml.cellNumericValue("I3"))
    }

    @Test
    fun createWorkbook_keepsLegacySummaryBlock_whenProjectCountFitsTemplate() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(
                listOf(
                    sampleProject(ProjectSpec("2026-05-01", 0, "Project 1", "02:00", "10", "No allowance", "Other")),
                    sampleProject(ProjectSpec("2026-05-02", 0, "Project 2", "03:00", "20", "No allowance", "Other")),
                    sampleProject(ProjectSpec("2026-05-03", 0, "Project 3", "04:00", "30", "No allowance", "Other"))
                )
            )
        )
        val templateBytes = loadTemplateBytes()

        val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
            templateBytes = templateBytes,
            exportData = exportData,
            ctx = RuntimeEnvironment.getApplication()
        )
        val sheetXml = workbookBytes.readWorksheetXml("xl/worksheets/sheet1.xml")

        assertEquals("General", sheetXml.cellInlineString("D1"))
        assertEquals("Work time total", sheetXml.cellInlineString("D2"))
        assertEquals("Kilometres", sheetXml.cellInlineString("D3"))
        assertEquals("Project 1", sheetXml.cellInlineString("E1"))
        assertEquals("Project 2", sheetXml.cellInlineString("F1"))
        assertEquals("Project 3", sheetXml.cellInlineString("G1"))
        assertEquals("Total", sheetXml.cellInlineString("H1"))
        assertNotNull(sheetXml.cellNumericValue("E2"))
        assertNotNull(sheetXml.cellNumericValue("G3"))
        assertNotNull(sheetXml.cellNumericValue("H2"))
        assertNotNull(sheetXml.cellNumericValue("H3"))
    }

    @Test
    fun createWorkbook_writesAllWorkTypeRows_whenWorkTypesExceedThree() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(
                listOf(
                    sampleProject(ProjectSpec("2026-05-01", 0, "Project 1", "01:00", "0", "No allowance", "Type 1")),
                    sampleProject(ProjectSpec("2026-05-02", 0, "Project 1", "01:00", "0", "No allowance", "Type 2")),
                    sampleProject(ProjectSpec("2026-05-03", 0, "Project 1", "01:00", "0", "No allowance", "Type 3")),
                    sampleProject(ProjectSpec("2026-05-04", 0, "Project 1", "01:00", "0", "No allowance", "Type 4"))
                )
            )
        )
        val templateBytes = loadTemplateBytes()

        val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
            templateBytes = templateBytes,
            exportData = exportData,
            ctx = RuntimeEnvironment.getApplication()
        )
        val sheetXml = workbookBytes.readWorksheetXml("xl/worksheets/sheet1.xml")

        // Dynamic work type block starts after project summary and allowance sections.
        // With one project this places labels in L2..L5 and project values in M2..M5.
        assertEquals("Type 1", sheetXml.cellInlineString("L2"))
        assertEquals("Type 2", sheetXml.cellInlineString("L3"))
        assertEquals("Type 3", sheetXml.cellInlineString("L4"))
        assertEquals("Type 4", sheetXml.cellInlineString("L5"))
        assertNotNull(sheetXml.cellNumericValue("M2"))
        assertNotNull(sheetXml.cellNumericValue("M3"))
        assertNotNull(sheetXml.cellNumericValue("M4"))
        assertNotNull(sheetXml.cellNumericValue("M5"))
        assertTrue(exportData.hiddenWorkTypes.isEmpty())
    }

    @Test
    fun createWorkbook_writesAllWorkTypeRows_withMultipleProjectsAndShiftedColumns() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(
                listOf(
                    sampleProject(ProjectSpec("2026-05-01", 0, "Project 1", "01:00", "0", "No allowance", "Type 1")),
                    sampleProject(ProjectSpec("2026-05-01", 1, "Project 2", "01:00", "0", "No allowance", "Type 2")),
                    sampleProject(ProjectSpec("2026-05-01", 2, "Project 3", "01:00", "0", "No allowance", "Type 3")),
                    sampleProject(ProjectSpec("2026-05-01", 3, "Project 4", "01:00", "0", "No allowance", "Type 4"))
                )
            )
        )
        val templateBytes = loadTemplateBytes()

        val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
            templateBytes = templateBytes,
            exportData = exportData,
            ctx = RuntimeEnvironment.getApplication()
        )
        val sheetXml = workbookBytes.readWorksheetXml("xl/worksheets/sheet1.xml")

        val allProjects = exportData.summaryProjectNames + exportData.hiddenProjectNames
        val workTypeLabelColumn = calculateWorkTypeLabelColumnIndex(allProjects.size)
        val workTypeTotalColumn = workTypeLabelColumn + allProjects.size + 1

        exportData.workTypeRows.forEachIndexed { index, row ->
            val rowNumber = index + 2
            val labelCell = "${toColumnLetters(workTypeLabelColumn)}$rowNumber"
            val totalCell = "${toColumnLetters(workTypeTotalColumn)}$rowNumber"

            assertEquals(row.label, sheetXml.cellInlineString(labelCell))
            assertNotNull(sheetXml.cellNumericValue(totalCell))
        }

        assertEquals(4, exportData.workTypeRows.size)
        assertTrue(exportData.hiddenWorkTypes.isEmpty())
    }

    @Test
    fun createWorkbook_removesCalcChainEntryAndReferences() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(emptyList())
        )
        val templateBytes = loadTemplateBytes()

        val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
            templateBytes = templateBytes,
            exportData = exportData,
            ctx = RuntimeEnvironment.getApplication()
        )

        assertTrue(!workbookBytes.hasZipEntry("xl/calcChain.xml"))
        assertTrue(!workbookBytes.readZipEntryText("[Content_Types].xml").contains("/xl/calcChain.xml"))
        assertTrue(!workbookBytes.readZipEntryText("xl/_rels/workbook.xml.rels").contains("calcChain"))
    }

    @Test
    fun createWorkbook_usesOnlyExistingStyleIndexes() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(emptyList())
        )
        val templateBytes = loadTemplateBytes()

        val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
            templateBytes = templateBytes,
            exportData = exportData,
            ctx = RuntimeEnvironment.getApplication()
        )

        val maxStyleIndex = workbookBytes.maxSheetStyleIndex()
        val cellXfCount = workbookBytes.cellXfCount()

        assertTrue("No style entries found in styles.xml", cellXfCount > 0)
        assertTrue(
            "Worksheet style index $maxStyleIndex exceeds available cellXfs ($cellXfCount)",
            maxStyleIndex < cellXfCount
        )
    }

    @Test
    fun createWorkbook_hasDayOfMonthNumbersInRow8() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(emptyList())
        )
        val templateBytes = loadTemplateBytes()

        val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
            templateBytes = templateBytes,
            exportData = exportData,
            ctx = RuntimeEnvironment.getApplication()
        )
        val sheetXml = workbookBytes.readWorksheetXml("xl/worksheets/sheet1.xml")

        // Day columns: day 1 → column B (index 2) … day 31 → column AF (index 32)
        for (day in 1..31) {
            val columnIndex = day + 1
            val cellReference = "${toColumnLetters(columnIndex)}8"
            assertEquals(
                "Expected day $day in cell $cellReference",
                day.toString(),
                sheetXml.cellNumericValue(cellReference)
            )
        }
        // Column A8 retains the "Day of Month" label from the template (not cleared)
        assertEquals("Day of Month", sheetXml.cellSharedString("A8", templateBytes))
    }

    @Test
    fun createWorkbook_alignsDailyValuesToTemplateRows() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(
                listOf(
                    sampleProject(ProjectSpec("2026-05-01", 0, "Project A", "02:30", "10", "No allowance", "Other"))
                )
            )
        )
        val templateBytes = loadTemplateBytes()

        val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
            templateBytes = templateBytes,
            exportData = exportData,
            ctx = RuntimeEnvironment.getApplication()
        )
        val sheetXml = workbookBytes.readWorksheetXml("xl/worksheets/sheet1.xml")

        assertEquals("02:30", sheetXml.cellInlineString("B9"))
        assertEquals("Project name", sheetXml.cellInlineString("A11"))
        assertEquals("Project A", sheetXml.cellInlineString("B11"))
        assertEquals("Project time", sheetXml.cellInlineString("A12"))
        assertEquals("02:30", sheetXml.cellInlineString("B12"))
        assertEquals("Allowance", sheetXml.cellInlineString("A13"))
        assertEquals("No", sheetXml.cellInlineString("B13"))
        assertEquals("Work type", sheetXml.cellInlineString("A14"))
        assertEquals("Other", sheetXml.cellInlineString("B14"))
        assertEquals("Kilometres", sheetXml.cellInlineString("A15"))
        assertEquals("10", sheetXml.cellNumericValue("B15"))
    }

    @Test
    fun createWorkbook_expandsDailyEntryLabelsToFiveBlocks() {
        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(
                listOf(
                    sampleProject(ProjectSpec("2026-05-01", 0, "Project 1", "01:00", "1", "No allowance", "Other")),
                    sampleProject(ProjectSpec("2026-05-01", 1, "Project 2", "01:00", "2", "No allowance", "Other")),
                    sampleProject(
                        ProjectSpec("2026-05-01", 2, "Project 3", "01:00", "3", "Half-day allowance", "Other")
                    ),
                    sampleProject(ProjectSpec("2026-05-01", 3, "Project 4", "01:00", "4", "Full allowance", "Other")),
                    sampleProject(ProjectSpec("2026-05-01", 4, "Project 5", "01:00", "5", "No allowance", "Other"))
                )
            )
        )
        val templateBytes = loadTemplateBytes()

        val workbookBytes = TimesheetWorkbookEditor.createWorkbook(
            templateBytes = templateBytes,
            exportData = exportData,
            ctx = RuntimeEnvironment.getApplication()
        )
        val sheetXml = workbookBytes.readWorksheetXml("xl/worksheets/sheet1.xml")

        // 4th and 5th blocks are created dynamically beyond template defaults.
        assertEquals("Project name", sheetXml.cellInlineString("A29"))
        assertEquals("Project time", sheetXml.cellInlineString("A30"))
        assertEquals("Allowance", sheetXml.cellInlineString("A31"))
        assertEquals("Work type", sheetXml.cellInlineString("A32"))
        assertEquals("Kilometres", sheetXml.cellInlineString("A33"))

        assertEquals("Project name", sheetXml.cellInlineString("A35"))
        assertEquals("Project time", sheetXml.cellInlineString("A36"))
        assertEquals("Allowance", sheetXml.cellInlineString("A37"))
        assertEquals("Work type", sheetXml.cellInlineString("A38"))
        assertEquals("Kilometres", sheetXml.cellInlineString("A39"))

        assertEquals("Project 4", sheetXml.cellInlineString("B29"))
        assertEquals("Project 5", sheetXml.cellInlineString("B35"))
    }

    private fun createParams(projects: List<SingleProjectState>) = GenerateTimesheetParams(
        ctx = RuntimeEnvironment.getApplication(),
        projectsByMonth = projects,
        endOfMonthDate = "2026-05-31",
        name = "Aki Wiksten",
        employer = "AJVW Inc.",
        defaultWorkTypeLabel = "Other",
        noAllowanceSourceLabel = "No allowance",
        halfDayAllowanceSourceLabel = "Half-day allowance",
        fullAllowanceSourceLabel = "Full allowance",
        noAllowanceExportLabel = "No",
        halfDayAllowanceExportLabel = "Half-day",
        fullAllowanceExportLabel = "Full",
        totalLabel = "Total",
        generalLabel = "General",
        workTimeTotalLabel = "Work time total",
        kilometresLabel = "Kilometres",
        flexTimeTotalLabel = "Flex time total",
        totalFlexTimeTotal = "00:00"
    )

    private fun sampleProject(spec: ProjectSpec) = projectState(
        index = spec.index,
        date = spec.date,
        projectName = spec.name,
        projectTime = spec.time,
        kilometres = spec.km,
        allowance = spec.allowance,
        workType = spec.workType
    )

    private fun assertProjectSummaries(exportData: TimesheetExportData) {
        assertEquals(listOf("Project 1", "Project 2", "Project 3"), exportData.summaryProjectNames)
        assertEquals(540L, exportData.summaryProjectTimes.getValue("Project 1"))
        assertEquals(570L, exportData.summaryProjectTimes.getValue("Project 2"))
        assertEquals(360L, exportData.summaryProjectTimes.getValue("Project 3"))
        assertEquals(140L, exportData.summaryProjectKilometres.getValue("Project 1"))
        assertEquals(220L, exportData.summaryProjectKilometres.getValue("Project 2"))
        assertEquals(240L, exportData.summaryProjectKilometres.getValue("Project 3"))
        assertEquals(1470L, exportData.totalWorkTime)
        assertEquals(600L, exportData.totalKilometres)
    }

    private fun assertDisplayedEntries(exportData: TimesheetExportData) {
        val daySixEntries = exportData.displayedEntriesByDay.getValue(6)
        assertEquals(3, daySixEntries.size)
        assertEquals("Project 1", daySixEntries[0].projectName)
        assertEquals("Project 2", daySixEntries[1].projectName)
        assertEquals("Project 3", daySixEntries[2].projectName)
    }

    private fun assertAllowanceSummaries(exportData: TimesheetExportData) {
        val noAllowanceRow = exportData.allowanceRows[0]
        val halfDayRow = exportData.allowanceRows[1]
        val fullAllowanceRow = exportData.allowanceRows[2]
        assertEquals("No", noAllowanceRow.label)
        assertEquals(1, noAllowanceRow.countByProjectName.getValue("Project 1"))
        assertEquals(1, noAllowanceRow.countByProjectName.getValue("Project 2"))
        assertEquals(0, noAllowanceRow.countByProjectName.getValue("Project 3"))
        assertEquals(2, noAllowanceRow.totalCount)
        assertEquals(1, halfDayRow.countByProjectName.getValue("Project 2"))
        assertEquals(1, halfDayRow.totalCount)
        assertEquals(1, fullAllowanceRow.countByProjectName.getValue("Project 1"))
        assertEquals(1, fullAllowanceRow.countByProjectName.getValue("Project 2"))
        assertEquals(2, fullAllowanceRow.countByProjectName.getValue("Project 3"))
        assertEquals(4, fullAllowanceRow.totalCount)
    }

    private fun assertWorkTypeSummaries(exportData: TimesheetExportData) {
        val otherRow = exportData.workTypeRows[0]
        val designRow = exportData.workTypeRows[1]
        assertEquals("Other", otherRow.label)
        assertEquals(540L, otherRow.timeByProjectName.getValue("Project 1"))
        assertEquals(360L, otherRow.timeByProjectName.getValue("Project 2"))
        assertEquals(270L, otherRow.timeByProjectName.getValue("Project 3"))
        assertEquals(1170L, otherRow.totalTime)
        assertEquals("Design", designRow.label)
        assertEquals(210L, designRow.timeByProjectName.getValue("Project 2"))
        assertEquals(90L, designRow.timeByProjectName.getValue("Project 3"))
        assertEquals(300L, designRow.totalTime)
    }

    private data class ProjectSpec(
        val date: String,
        val index: Int,
        val name: String,
        val time: String,
        val km: String,
        val allowance: String,
        val workType: String
    )

    private fun calculateWorkTypeLabelColumnIndex(projectCount: Int): Int {
        // Mirrors production layout formula:
        // project summary starts at E(5), total at (5 + projectCount),
        // allowance label = +2, allowance start = +1, allowance total = +projectCount,
        // work type label = allowance total +2
        val projectSummaryTotal = 5 + projectCount
        val allowanceLabel = projectSummaryTotal + 2
        val allowanceStart = allowanceLabel + 1
        val allowanceTotal = allowanceStart + projectCount
        return allowanceTotal + 2
    }

    private fun toColumnLetters(columnIndex: Int): String {
        var value = columnIndex
        val builder = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            builder.insert(0, ('A'.code + remainder).toChar())
            value = (value - 1) / 26
        }
        return builder.toString()
    }
}

@RunWith(RobolectricTestRunner::class)
class TimesheetGeneratorEntryExcelInspectionTest {
    private data class SpecItem(
        val date: String,
        val index: Int,
        val name: String,
        val time: String,
        val km: String,
        val allowance: String,
        val workType: String
    )

    @Test
    fun inspectExcelLayout_withExtraAWProjectOn13May() {
        val startProjects = listOf(
            SpecItem("2026-05-13", 0, "Kala", "08:00", "0", "No allowance", "Other"),
            SpecItem("2026-05-14", 0, "AWProject", "04:30", "50", "No allowance", "Other"),
            SpecItem("2026-05-14", 1, "Lankku", "02:00", "75", "Half-day allowance", "Other"),
            SpecItem("2026-05-15", 0, "AWProject", "07:00", "60", "No allowance", "Other"),
            SpecItem("2026-05-15", 1, "Project 1", "06:00", "100", "No allowance", "Other"),
            SpecItem("2026-05-16", 0, "Project 1", "05:30", "85", "No allowance", "Other")
        )

        val exportData = TimesheetExportDataBuilder.build(
            params = createParams(
                startProjects.map { spec ->
                    projectState(
                        index = spec.index,
                        date = spec.date,
                        projectName = spec.name,
                        projectTime = spec.time,
                        kilometres = spec.km,
                        allowance = spec.allowance,
                        workType = spec.workType
                    )
                }
            )
        )

        println("=== PROJECT SUMMARIES ===")
        exportData.summaryProjectNames.forEach { name ->
            val time = exportData.summaryProjectTimes[name] ?: 0L
            val km = exportData.summaryProjectKilometres[name] ?: 0L
            println("$name: time=$time km=$km")
        }

        println("\n=== ALLOWANCE COUNTS ===")
        exportData.allowanceRows.forEach { row ->
            println("${row.label}:")
            row.countByProjectName.forEach { (proj, cnt) ->
                println("  $proj=$cnt")
            }
            println("  Total=$row.totalCount")
        }

        println("\n=== WORKTYPE TIMES ===")
        exportData.workTypeRows.forEach { row ->
            println("${row.label}:")
            row.timeByProjectName.forEach { (proj, tm) ->
                if (tm > 0L) println("  $proj=$tm")
            }
            if (row.totalTime > 0L) println("  Total=$row.totalTime")
        }
    }

    private fun createParams(projects: List<SingleProjectState>) = GenerateTimesheetParams(
        ctx = RuntimeEnvironment.getApplication(),
        projectsByMonth = projects,
        endOfMonthDate = "2026-05-31",
        name = "Aki Wiksten",
        employer = "AJVW Inc.",
        defaultWorkTypeLabel = "Other",
        noAllowanceSourceLabel = "No allowance",
        halfDayAllowanceSourceLabel = "Half-day allowance",
        fullAllowanceSourceLabel = "Full allowance",
        noAllowanceExportLabel = "No",
        halfDayAllowanceExportLabel = "Half-day",
        fullAllowanceExportLabel = "Full",
        totalLabel = "Total",
        generalLabel = "General",
        workTimeTotalLabel = "Work time total",
        kilometresLabel = "Kilometres",
        flexTimeTotalLabel = "Flex time total",
        totalFlexTimeTotal = "00:00"
    )
}
