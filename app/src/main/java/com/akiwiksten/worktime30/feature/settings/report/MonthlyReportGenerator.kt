@file:Suppress("MagicNumber")

package com.akiwiksten.worktime30.feature.settings.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.WorkTimeCalculator.extractDayOfMonth
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Handles PDF generation for monthly work reports.
 */
object MonthlyReportGenerator {
    const val TEXT_SIZE = 8f
    const val ORIGIN_LEFT_FIRST = 50f
    const val ORIGIN_TOP_FIRST = 150f
    const val PAGE_HEIGHT = 1120
    const val PAGE_WIDTH = 792
    const val DATE_01 = 1
    const val DATE_08 = 8
    const val DATE_09 = 9
    const val DATE_16 = 16
    const val DATE_17 = 17
    const val DATE_24 = 24
    const val DATE_25 = 25
    private const val TITLE_SIZE = 15f

    private fun createPage(pageParams: CreatePageParams) {
        val myPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageParams.pageNumber).create()
        val myPage = pageParams.pdfDocument.startPage(myPageInfo)
        val canvas = myPage.canvas

        if (pageParams.pageNumber == 1) {
            pageParams.title.textAlign = Paint.Align.CENTER
            canvas.drawText(pageParams.monthlyReportLabel, PAGE_WIDTH / 2f, 50f, pageParams.title)
        }

        pageParams.title.textAlign = Paint.Align.LEFT
        val formattedDate = LocalDate.parse(pageParams.endOfMonthDate)
        val yearAndMonth = DateTimeFormatter.ofPattern("MM/yyyy").format(formattedDate)

        canvas.drawText(pageParams.name, 50f, 80f, pageParams.title)
        canvas.drawText(pageParams.employer, 50f, 100f, pageParams.title)
        canvas.drawText(yearAndMonth, 50f, 120f, pageParams.title)

        val printParams = PrintWorkDaysParams(
            pageParams.projectsByMonth, canvas, pageParams.showTotals,
            pageParams.startDate, pageParams.endDate, pageParams.totalSumLabel,
            pageParams.endOfMonthDate, pageParams.projectTitles,
            pageParams.preprocessedProjects
        )
        MonthlyReportRenderer.printWorkDays(params = printParams)

        pageParams.pdfDocument.finishPage(myPage)
    }

    fun generatePdf(params: GeneratePdfParams) {
        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = TITLE_SIZE
            color = ContextCompat.getColor(params.ctx, R.color.black)
        }

        val preprocessedProjects = params.projectsByMonth
            .filter { it.projectTime != ZERO_TIME }
            .groupBy { extractDayOfMonth(it.date).toInt() }
            .mapValues { (_, projects) ->
                projects.flatMap { project ->
                    listOf(
                        project.projectName,
                        project.projectTime,
                        project.allowance,
                        project.workType,
                        "${project.kilometres} km",
                        ""
                    )
                }
            }

        val pages = listOf(
            PageInfo(1, DATE_01, DATE_08, false),
            PageInfo(2, DATE_09, DATE_16, false),
            PageInfo(3, DATE_17, DATE_24, false)
        )

        pages.forEach { info ->
            createPage(createPageParams(params, pdfDocument, titlePaint, info, preprocessedProjects))
        }

        val lastDay = LocalDate.parse(params.endOfMonthDate).dayOfMonth
        createPage(
            pageParams = createPageParams(
                params = params,
                doc = pdfDocument,
                paint = titlePaint,
                info = PageInfo(4, DATE_25, lastDay, true),
                preprocessedProjects = preprocessedProjects
            )
        )

        MonthlyReportStorage.savePdf(
            ctx = params.ctx,
            doc = pdfDocument,
            name = params.name,
            date = params.endOfMonthDate
        )
        pdfDocument.close()
    }

    private fun createPageParams(
        params: GeneratePdfParams,
        doc: PdfDocument,
        paint: Paint,
        info: PageInfo,
        preprocessedProjects: Map<Int, List<String>>
    ) = CreatePageParams(
        params.name,
        params.employer, params.projectsByMonth, params.endOfMonthDate,
        params.totalSumLabel, params.monthlyReportLabel, doc, paint, info.number,
        info.start, info.end, info.showTotals, params.projectTitles, preprocessedProjects
    )

    private data class PageInfo(val number: Int, val start: Int, val end: Int, val showTotals: Boolean)
}

data class PrintWorkDaysParams(
    val projectsByMonth: List<SingleProjectState>,
    val canvas: Canvas,
    val showTotals: Boolean,
    val startDate: Int,
    val endDate: Int,
    val totalSumLabel: String,
    val endOfMonthDate: String,
    val projectTitles: List<String>,
    val preprocessedProjects: Map<Int, List<String>>
)

data class DrawProjectDaysParams(
    val printWorkDaysParams: PrintWorkDaysParams,
    val left: Float,
    val right: Float,
    val x: Float,
    val measuredPaintTextRight: Float,
    val projectAttributes: List<String>,
    val day: Int
)

data class CreatePageParams(
    val name: String,
    val employer: String,
    val projectsByMonth: List<SingleProjectState>,
    val endOfMonthDate: String,
    val totalSumLabel: String,
    val monthlyReportLabel: String,
    val pdfDocument: PdfDocument,
    val title: Paint,
    val pageNumber: Int,
    val startDate: Int,
    val endDate: Int,
    val showTotals: Boolean,
    val projectTitles: List<String>,
    val preprocessedProjects: Map<Int, List<String>>
)

data class GeneratePdfParams(
    val ctx: Context,
    val projectsByMonth: List<SingleProjectState>,
    val endOfMonthDate: String,
    val totalSumLabel: String,
    val monthlyReportLabel: String,
    val name: String,
    val employer: String,
    val projectTitles: List<String>
)
