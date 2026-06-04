package com.akiwiksten.awtimesheet.feature.timesheet

import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.timesheet.model.GenerateTimesheetParams
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportDataBuilder
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.TimesheetWorkbookEditor
import com.akiwiksten.awtimesheet.test.projectState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.system.measureTimeMillis

/**
 * Performance benchmark for timesheet generation to measure impact of optimization passes.
 * Generates synthetic monthly data at increasing scales and records generation time.
 */
@RunWith(RobolectricTestRunner::class)
class TimesheetGeneratorEntryPerformanceTest {

    @Test
    fun benchmark_generatorPerformanceAtIncreasingScales() {
        val scales = listOf(
            Scale(name = "Light (3 projects, 5 days)", projectCount = 3, daysActive = 5),
            Scale(name = "Medium (5 projects, 15 days)", projectCount = 5, daysActive = 15),
            Scale(name = "Heavy (8 projects, 25 days)", projectCount = 8, daysActive = 25)
        )

        println("\n=== TIMESHEET GENERATION PERFORMANCE BENCHMARK ===\n")

        scales.forEach { scale ->
            val projects = generateSyntheticProjects(scale.projectCount, scale.daysActive)
            val params = createParams(projects)

            val buildTime = measureTimeMillis {
                TimesheetExportDataBuilder.build(params)
            }
            val templateBytes = loadTemplateBytes()
            val exportData = TimesheetExportDataBuilder.build(params)

            val workbookTime = measureTimeMillis {
                TimesheetWorkbookEditor.createWorkbook(
                    templateBytes = templateBytes,
                    exportData = exportData,
                    ctx = RuntimeEnvironment.getApplication()
                )
            }

            println(
                "${scale.name}\n" +
                    "  Build export data: ${buildTime}ms\n" +
                    "  Create workbook:   ${workbookTime}ms\n" +
                    "  Total:             ${buildTime + workbookTime}ms\n"
            )
        }
    }

    private fun generateSyntheticProjects(
        projectCount: Int,
        daysActive: Int
    ): List<SingleProjectState> {
        val projects = mutableListOf<SingleProjectState>()
        val projectNames = (1..projectCount).map { "Project $it" }
        val dayOfMonth = (1..daysActive).toList()

        for ((dayIndex, day) in dayOfMonth.withIndex()) {
            for ((projIndex, projectName) in projectNames.withIndex()) {
                val date = "2026-05-${day.toString().padStart(2, '0')}"
                val hours = (2 + (projIndex % 4))
                val minutes = (projIndex * 15) % 60
                val projectTime = "$hours:${minutes.toString().padStart(2, '0')}"
                val kilometres = ((projIndex + 1) * 10).toString()
                val allowance = when (dayIndex % 3) {
                    0 -> "No allowance"
                    1 -> "Half-day allowance"
                    else -> "Full allowance"
                }
                val workType = arrayOf("Other", "Design", "Consulting")[dayIndex % 3]

                projects.add(
                    projectState(
                        index = projIndex,
                        date = date,
                        projectName = projectName,
                        projectTime = projectTime,
                        kilometres = kilometres,
                        allowance = allowance,
                        workType = workType
                    )
                )
            }
        }

        return projects
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

    private data class Scale(
        val name: String,
        val projectCount: Int,
        val daysActive: Int
    )
}
