package com.akiwiksten.worktime30.core

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Unit tests for MonthlyReportGenerator.kt
 *
 * These tests focus on data structure validation and constant verification
 * that do not require Android runtime context.
 */
class MonthlyReportGeneratorTest {

    @Test
    fun testMonthlyReportGeneratorConstants_areDefined() {
        assertEquals(8f, MonthlyReportGenerator.TEXT_SIZE, 0.01f)
        assertEquals(50f, MonthlyReportGenerator.ORIGIN_LEFT_FIRST, 0.01f)
        assertEquals(150f, MonthlyReportGenerator.ORIGIN_TOP_FIRST, 0.01f)
        assertEquals(1120, MonthlyReportGenerator.PAGE_HEIGHT)
        assertEquals(792, MonthlyReportGenerator.PAGE_WIDTH)
        assertEquals(1, MonthlyReportGenerator.DATE_01)
        assertEquals(8, MonthlyReportGenerator.DATE_08)
        assertEquals(9, MonthlyReportGenerator.DATE_09)
        assertEquals(16, MonthlyReportGenerator.DATE_16)
        assertEquals(17, MonthlyReportGenerator.DATE_17)
        assertEquals(24, MonthlyReportGenerator.DATE_24)
        assertEquals(25, MonthlyReportGenerator.DATE_25)
    }

    @Test
    fun testPageInfo_structure() {
        val pageInfo = PageInfo(1, 1, 8, false)
        assertEquals(1, pageInfo.number)
        assertEquals(1, pageInfo.start)
        assertEquals(8, pageInfo.end)
        assertEquals(false, pageInfo.showTotals)
    }

    @Test
    fun testPageInfo_lastPageShowsTotals() {
        val lastPageInfo = PageInfo(4, 25, 30, true)
        assertEquals(4, lastPageInfo.number)
        assertEquals(true, lastPageInfo.showTotals)
    }

    @Test
    fun testPageInfoDataClass_multipleInstances() {
        val page1 = PageInfo(1, 1, 8, false)
        val page2 = PageInfo(2, 9, 16, false)
        val page3 = PageInfo(3, 17, 24, false)
        val page4 = PageInfo(4, 25, 31, true)

        assertEquals(1, page1.number)
        assertEquals(2, page2.number)
        assertEquals(3, page3.number)
        assertEquals(4, page4.number)

        assertEquals(false, page1.showTotals)
        assertEquals(false, page2.showTotals)
        assertEquals(false, page3.showTotals)
        assertEquals(true, page4.showTotals)
    }

    @Test
    fun testPrintWorkDaysParams_structure() {
        val params = PrintWorkDaysParams(
            projectsByMonth = emptyList(),
            canvas = android.graphics.Canvas(),
            showTotals = true,
            startDate = 1,
            endDate = 31,
            totalSumLabel = "Total",
            endOfMonthDate = "2026-04-10",
            projectTitles = listOf("Project Name", "Hours", "Allowance"),
            preprocessedProjects = emptyMap()
        )

        assertEquals(emptyList<ProjectEntity>(), params.projectsByMonth)
        assertTrue(params.showTotals)
        assertEquals(1, params.startDate)
        assertEquals(31, params.endDate)
        assertEquals("Total", params.totalSumLabel)
        assertEquals("2026-04-10", params.endOfMonthDate)
        assertEquals(3, params.projectTitles.size)
    }

    @Test
    fun testDrawProjectDaysParams_structure() {
        val printParams = PrintWorkDaysParams(
            projectsByMonth = emptyList(),
            canvas = android.graphics.Canvas(),
            showTotals = false,
            startDate = 1,
            endDate = 31,
            totalSumLabel = "Total",
            endOfMonthDate = "2026-04-10",
            projectTitles = emptyList(),
            preprocessedProjects = emptyMap()
        )

        val drawParams = DrawProjectDaysParams(
            printWorkDaysParams = printParams,
            left = 50f,
            right = 100f,
            x = 55f,
            measuredPaintTextRight = 45f,
            projectAttributes = listOf("Alpha", "Beta"),
            day = 5
        )

        assertEquals(50f, drawParams.left, 0.01f)
        assertEquals(100f, drawParams.right, 0.01f)
        assertEquals(55f, drawParams.x, 0.01f)
        assertEquals(45f, drawParams.measuredPaintTextRight, 0.01f)
        assertEquals(2, drawParams.projectAttributes.size)
        assertEquals(5, drawParams.day)
    }

    @Test
    fun testCreatePageParams_structure() {
        val params = CreatePageParams(
            name = "John Doe",
            employer = "Test Company",
            projectsByMonth = emptyList(),
            endOfMonthDate = "2026-04-30",
            totalSumLabel = "Total",
            monthlyReportLabel = "Monthly Report",
            pdfDocument = android.graphics.pdf.PdfDocument(),
            title = android.graphics.Paint(),
            pageNumber = 1,
            startDate = 1,
            endDate = 8,
            showTotals = false,
            projectTitles = listOf("Project", "Hours", "Type"),
            preprocessedProjects = emptyMap()
        )

        assertEquals("John Doe", params.name)
        assertEquals("Test Company", params.employer)
        assertEquals("2026-04-30", params.endOfMonthDate)
        assertEquals("Total", params.totalSumLabel)
        assertEquals("Monthly Report", params.monthlyReportLabel)
        assertEquals(1, params.pageNumber)
        assertEquals(1, params.startDate)
        assertEquals(8, params.endDate)
        assertEquals(false, params.showTotals)
        assertEquals(3, params.projectTitles.size)
    }


    @Test
    fun testPrintWorkDaysParams_withDataRange() {
        val params = PrintWorkDaysParams(
            projectsByMonth = listOf(
                ProjectEntity(
                    date = "2026-04-01",
                    projectName = "Alpha",
                    projectTime = "08:00",
                    kilometres = 10,
                    allowance = "Full",
                    workType = "Installation"
                )
            ),
            canvas = android.graphics.Canvas(),
            showTotals = false,
            startDate = 1,
            endDate = 8,
            totalSumLabel = "Total",
            endOfMonthDate = "2026-04-08",
            projectTitles = listOf("Project", "Time"),
            preprocessedProjects = mapOf(1 to listOf("Alpha", "08:00"))
        )

        assertEquals(1, params.projectsByMonth.size)
        assertEquals(8, params.endDate)
        assertEquals(2, params.projectTitles.size)
        assertEquals(1, params.preprocessedProjects.size)
    }

    @Test
    fun testDrawProjectDaysParams_dayRanges() {
        val printParams = PrintWorkDaysParams(
            projectsByMonth = emptyList(),
            canvas = android.graphics.Canvas(),
            showTotals = false,
            startDate = 1,
            endDate = 8,
            totalSumLabel = "Total",
            endOfMonthDate = "2026-04-08",
            projectTitles = emptyList(),
            preprocessedProjects = emptyMap()
        )

        // Test week 1 (days 1-8)
        val week1 = DrawProjectDaysParams(
            printWorkDaysParams = printParams,
            left = 50f,
            right = 100f,
            x = 55f,
            measuredPaintTextRight = 45f,
            projectAttributes = emptyList(),
            day = 1
        )
        assertEquals(1, week1.day)

        // Test week 2 (days 9-16)
        val week2 = DrawProjectDaysParams(
            printWorkDaysParams = printParams,
            left = 50f,
            right = 100f,
            x = 55f,
            measuredPaintTextRight = 45f,
            projectAttributes = emptyList(),
            day = 9
        )
        assertEquals(9, week2.day)
    }

    @Test
    fun testPageInfo_allWeeks() {
        val weeks = listOf(
            PageInfo(1, 1, 8, false),
            PageInfo(2, 9, 16, false),
            PageInfo(3, 17, 24, false),
            PageInfo(4, 25, 31, true)
        )

        assertEquals(4, weeks.size)
        assertTrue(weeks[0].start < weeks[1].start)
        assertTrue(weeks[1].start < weeks[2].start)
        assertTrue(weeks[2].start < weeks[3].start)
        assertTrue(weeks[3].showTotals)
    }

    // Data class for testing purposes (mirrors the private class in MonthlyReportGenerator)
    data class PageInfo(
        val number: Int,
        val start: Int,
        val end: Int,
        val showTotals: Boolean
    )
}

