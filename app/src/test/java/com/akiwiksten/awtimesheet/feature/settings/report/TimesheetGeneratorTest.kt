package com.akiwiksten.awtimesheet.feature.settings.report

import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TimesheetGeneratorTest {

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

        assertEquals(listOf(1), exportData.overflowedDays)
        assertEquals(listOf("Project 4"), exportData.hiddenProjectNames)
        assertEquals(listOf("Type 4"), exportData.hiddenWorkTypes)
        assertEquals(3, exportData.displayedEntriesByDay.getValue(1).size)
        assertEquals(listOf("Project 1", "Project 2", "Project 3"), exportData.summaryProjectNames)
        assertEquals(1.0 / 24.0, exportData.summaryProjectTimes.getValue("Project 4"), 0.000001)
        assertEquals(40.0, exportData.summaryProjectKilometres.getValue("Project 4"), 0.000001)
        assertEquals(0, exportData.allowanceRows[0].countByProjectName.getValue("Project 4"))
        assertEquals(0, exportData.allowanceRows[1].countByProjectName.getValue("Project 4"))
        assertEquals(1, exportData.allowanceRows[2].countByProjectName.getValue("Project 4"))
        assertEquals(0.0, exportData.workTypeRows[0].timeByProjectName.getValue("Project 4"), 0.000001)
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
            exportData = exportData
        )
        val sheetXml = workbookBytes.readWorksheetXml("xl/worksheets/sheet1.xml")

        assertEquals("Project 1", sheetXml.cellInlineString("U1"))
        assertEquals("Project 2", sheetXml.cellInlineString("V1"))
        assertEquals("Project 3", sheetXml.cellInlineString("W1"))
        assertEquals("Project 4", sheetXml.cellInlineString("X1"))
        assertEquals("Total", sheetXml.cellInlineString("Y1"))
        assertNotNull(sheetXml.cellNumericValue("U2"))
        assertNotNull(sheetXml.cellNumericValue("X3"))
        assertNotNull(sheetXml.cellNumericValue("Y2"))
        assertNotNull(sheetXml.cellNumericValue("Y3"))
        assertEquals("Project 1", sheetXml.cellInlineString("Z1"))
        assertEquals("Project 2", sheetXml.cellInlineString("AA1"))
        assertEquals("Project 3", sheetXml.cellInlineString("AB1"))
        assertEquals("Project 4", sheetXml.cellInlineString("AC1"))
        assertEquals("Total", sheetXml.cellInlineString("AD1"))
        assertNotNull(sheetXml.cellNumericValue("AC4"))
        assertEquals("Project 1", sheetXml.cellInlineString("AG1"))
        assertEquals("Project 2", sheetXml.cellInlineString("AH1"))
        assertEquals("Project 3", sheetXml.cellInlineString("AI1"))
        assertEquals("Project 4", sheetXml.cellInlineString("AJ1"))
        assertEquals("Total", sheetXml.cellInlineString("AK1"))
        assertNotNull(sheetXml.cellNumericValue("AJ2"))
        assertNotNull(sheetXml.cellNumericValue("AK2"))
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
            exportData = exportData
        )
        val sheetXml = workbookBytes.readWorksheetXml("xl/worksheets/sheet1.xml")

        assertEquals("Project 1", sheetXml.cellInlineString("E1"))
        assertEquals("Project 2", sheetXml.cellInlineString("F1"))
        assertEquals("Project 3", sheetXml.cellInlineString("G1"))
        assertEquals("Total", sheetXml.cellInlineString("H1"))
        assertNotNull(sheetXml.cellNumericValue("E2"))
        assertNotNull(sheetXml.cellNumericValue("G3"))
        assertNotNull(sheetXml.cellNumericValue("H2"))
        assertNotNull(sheetXml.cellNumericValue("H3"))
        assertEquals("Project 1", sheetXml.cellInlineString("K1"))
        assertEquals("Project 2", sheetXml.cellInlineString("L1"))
        assertEquals("Project 3", sheetXml.cellInlineString("M1"))
        assertEquals("Total", sheetXml.cellInlineString("N1"))
        assertNotNull(sheetXml.cellNumericValue("M4"))
        assertNotNull(sheetXml.cellNumericValue("N4"))
        assertEquals("Project 1", sheetXml.cellInlineString("Q1"))
        assertEquals("Project 2", sheetXml.cellInlineString("R1"))
        assertEquals("Project 3", sheetXml.cellInlineString("S1"))
        assertEquals("Total", sheetXml.cellInlineString("T1"))
        assertNotNull(sheetXml.cellNumericValue("S2"))
        assertNotNull(sheetXml.cellNumericValue("T2"))

        assertNull(sheetXml.cellInlineString("U1"))
        assertNull(sheetXml.cellNumericValue("U2"))
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
        fullAllowanceExportLabel = "Full"
    )

    private fun sampleProject(spec: ProjectSpec) = SingleProjectState(
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
        assertEquals(0.375, exportData.summaryProjectTimes.getValue("Project 1"), 0.000001)
        assertEquals(0.3958333333, exportData.summaryProjectTimes.getValue("Project 2"), 0.000001)
        assertEquals(0.25, exportData.summaryProjectTimes.getValue("Project 3"), 0.000001)
        assertEquals(140.0, exportData.summaryProjectKilometres.getValue("Project 1"), 0.000001)
        assertEquals(220.0, exportData.summaryProjectKilometres.getValue("Project 2"), 0.000001)
        assertEquals(240.0, exportData.summaryProjectKilometres.getValue("Project 3"), 0.000001)
        assertEquals(1.0208333333, exportData.totalWorkTime, 0.000001)
        assertEquals(600.0, exportData.totalKilometres, 0.000001)
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
        assertEquals(0.375, otherRow.timeByProjectName.getValue("Project 1"), 0.000001)
        assertEquals(0.25, otherRow.timeByProjectName.getValue("Project 2"), 0.000001)
        assertEquals(0.1875, otherRow.timeByProjectName.getValue("Project 3"), 0.000001)
        assertEquals(0.8125, otherRow.totalTime, 0.000001)
        assertEquals("Design", designRow.label)
        assertEquals(0.1458333333, designRow.timeByProjectName.getValue("Project 2"), 0.000001)
        assertEquals(0.0625, designRow.timeByProjectName.getValue("Project 3"), 0.000001)
        assertEquals(0.2083333333, designRow.totalTime, 0.000001)
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
}

@RunWith(RobolectricTestRunner::class)
class TimesheetGeneratorExcelInspectionTest {
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
            params = createParams(startProjects.map { spec ->
                SingleProjectState(
                    index = spec.index,
                    date = spec.date,
                    projectName = spec.name,
                    projectTime = spec.time,
                    kilometres = spec.km,
                    allowance = spec.allowance,
                    workType = spec.workType
                )
            })
        )

        println("=== PROJECT SUMMARIES ===")
        exportData.summaryProjectNames.forEach { name ->
            val time = exportData.summaryProjectTimes[name] ?: 0.0
            val km = exportData.summaryProjectKilometres[name] ?: 0.0
            println("$name: time=$time km=$km")
        }

        println("\n=== ALLOWANCE COUNTS ===")
        exportData.allowanceRows.forEachIndexed { idx, row ->
            println("${row.label}:")
            row.countByProjectName.forEach { (proj, cnt) ->
                println("  $proj=$cnt")
            }
            println("  Total=$row.totalCount")
        }

        println("\n=== WORKTYPE TIMES ===")
        exportData.workTypeRows.forEachIndexed { idx, row ->
            println("${row.label}:")
            row.timeByProjectName.forEach { (proj, tm) ->
                if (tm > 0.0) println("  $proj=$tm")
            }
            if (row.totalTime > 0.0) println("  Total=$row.totalTime")
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
        fullAllowanceExportLabel = "Full"
    )
}

