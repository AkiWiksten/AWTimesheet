package com.akiwiksten.awtimesheet.feature.settings.report

import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import org.junit.Assert.assertEquals
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


