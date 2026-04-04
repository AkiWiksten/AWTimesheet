@file:Suppress("MagicNumber")

package com.akiwiksten.worktime30.core

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.WorkTimeCalculator.parseDate
import com.akiwiksten.worktime30.data.database.Project
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Handles PDF generation for monthly work reports.
 */
@Suppress("TooManyFunctions")
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
    private const val PADDING = 5.0f
    private const val DEFAULT_WIDTH = 30.0f
    private const val TITLE_SIZE = 15f

    private val paintText = createPaint(Typeface.NORMAL, 1f, Paint.Style.FILL)
    private val paintBoldText = createPaint(Typeface.BOLD, 1f, Paint.Style.FILL)
    private val paintRect = createPaint(Typeface.NORMAL, 1f, Paint.Style.STROKE)
    private val paintBoldRect = createPaint(Typeface.NORMAL, 3f, Paint.Style.STROKE)

    private fun createPaint(style: Int, strokeWidth: Float, paintStyle: Paint.Style): Paint {
        return Paint().apply {
            textSize = TEXT_SIZE
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, style)
            this.strokeWidth = strokeWidth
            this.style = paintStyle
        }
    }

    fun getMaxLengthOfProjectAttributes(list: Collection<String>, paint: Paint): Float {
        return (list.maxOfOrNull { paint.measureText(it) } ?: 0f) + PADDING
    }

    fun getMaxLengthOfProjectAttributesMap(map: Map<String, String>, paint: Paint): Float {
        return (map.keys.maxOfOrNull { paint.measureText(it) } ?: 0f) + PADDING
    }

    private fun createUniqueProjects(
        endOfMonth: String,
        params: PrintWorkDaysParams
    ): Map<String, String> {
        val uniqueProjects = mutableMapOf<String, String>()
        val endDay = endOfMonth.toInt()

        for (day in 1..endDay) {
            params.projectsByMonth
                .filter { parseDate(it.date).toInt() == day && it.projectTime != ZERO_TIME }
                .forEach { project ->
                    uniqueProjects[project.projectName] = WorkTimeCalculator.calculateTotalMinutes(
                        initialTime = uniqueProjects[project.projectName] ?: ZERO_TIME,
                        addedTime = project.projectTime,
                        isInitialTimeNegative = false,
                        isAddedTimeNegative = false
                    )
                }
        }

        val totalSum = uniqueProjects.values.fold(ZERO_TIME) { acc, time ->
            WorkTimeCalculator.calculateTotalMinutes(acc, time, false, false)
        }
        uniqueProjects[params.totalSumLabel] = totalSum
        return uniqueProjects
    }

    private fun drawCell(
        canvas: Canvas,
        rect: RectF,
        text: String,
        textPos: PointF,
        paints: PaintGroup
    ) {
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paints.rectPaint)
        canvas.drawText(text, textPos.x, textPos.y, paints.textPaint)
    }

    private fun drawProjectTitles(params: DrawProjectDaysParams) {
        val rectHeight = TEXT_SIZE * 2
        val top = ORIGIN_TOP_FIRST
        val bottom = top + rectHeight

        drawCell(
            params.printWorkDaysParams.canvas,
            RectF(params.left, top, params.right, bottom),
            params.projectAttributes[0],
            PointF(params.x + params.measuredPaintTextRight / 3, top + TEXT_SIZE * 1.5f),
            PaintGroup(paintBoldRect, paintBoldText)
        )

        for (i in 1 until params.projectAttributes.size) {
            val cellTop = top + i * rectHeight
            val cellBottom = cellTop + rectHeight
            drawCell(
                params.printWorkDaysParams.canvas,
                RectF(params.left, cellTop, params.right, cellBottom),
                params.projectAttributes[i],
                PointF(params.x + PADDING, cellBottom - TEXT_SIZE),
                PaintGroup(paintRect, paintText)
            )
        }
    }

    private fun drawProjectDays(params: DrawProjectDaysParams) {
        val rectHeight = TEXT_SIZE * 2
        val top = ORIGIN_TOP_FIRST
        val bottom = top + rectHeight

        drawCell(
            params.printWorkDaysParams.canvas,
            RectF(params.left, top, params.right, bottom),
            params.day.toString(),
            PointF(params.x + params.measuredPaintTextRight / 2, top + TEXT_SIZE * 1.5f),
            PaintGroup(paintBoldRect, paintBoldText)
        )

        for (i in 1..params.projectAttributes.size) {
            val cellTop = top + i * rectHeight
            val cellBottom = cellTop + rectHeight
            val paints = PaintGroup(paintRect, if ((i % 8) == 1) paintBoldText else paintText)
            drawCell(
                params.printWorkDaysParams.canvas,
                RectF(params.left, cellTop, params.right, cellBottom),
                params.projectAttributes[i - 1],
                PointF(params.x + PADDING, cellBottom - TEXT_SIZE),
                paints
            )
        }
    }

    private fun printWorkDays(params: PrintWorkDaysParams) {
        val endOfMonth = parseDate(params.endOfMonthDate)
        var currentXOffset = ORIGIN_LEFT_FIRST
        
        val titleWidth = getMaxLengthOfProjectAttributes(params.projectTitles, paintText)
            .coerceAtLeast(DEFAULT_WIDTH)
        val titleParams = DrawProjectDaysParams(
            params, currentXOffset, currentXOffset + titleWidth, 
            currentXOffset, titleWidth, params.projectTitles, 0
        )
        drawProjectTitles(titleParams)
        currentXOffset += titleWidth

        for (day in params.startDate..params.endDate) {
            val projectAttrs = getProjectAttributesForDay(params.projectsByMonth, day)
            val dayWidth = getMaxLengthOfProjectAttributes(projectAttrs, paintText)
                .coerceAtLeast(DEFAULT_WIDTH)
            val dayParams = DrawProjectDaysParams(
                params, currentXOffset, currentXOffset + dayWidth, 
                currentXOffset, dayWidth, projectAttrs, day
            )
            drawProjectDays(dayParams)
            currentXOffset += dayWidth
        }

        if (params.showTotals) drawTotals(params, endOfMonth, currentXOffset)
    }

    private fun getProjectAttributesForDay(projects: List<Project>, day: Int): List<String> {
        val attributes = mutableListOf<String>()
        projects.filter { parseDate(it.date).toInt() == day }
            .forEach { project ->
                if (project.projectStartTime != ZERO_TIME && project.projectEndTime != ZERO_TIME) {
                    val workTime = WorkTimeCalculator.calculateWorkTimeBalance(
                        project.projectEndTime, "-" + project.projectStartTime)
                    attributes.addAll(listOf(
                        project.projectName, project.projectStartTime, 
                        project.projectEndTime, workTime, project.allowance, 
                        project.workType, "${project.kilometres} km", ""
                    ))
                }
            }
        return attributes
    }

    private fun drawTotals(params: PrintWorkDaysParams, endOfMonth: String, startX: Float) {
        val uniqueProjects = createUniqueProjects(endOfMonth, params)
        val maxWidth = getMaxLengthOfProjectAttributesMap(uniqueProjects, paintText)
        val rectHeight = TEXT_SIZE * 2
        
        uniqueProjects.toList().forEachIndexed { index, pair ->
            val top = ORIGIN_TOP_FIRST + (index + 1) * rectHeight
            val bottom = top + rectHeight
            
            drawCell(params.canvas, RectF(startX, top, startX + maxWidth, bottom), 
                pair.first, PointF(startX + PADDING, bottom - TEXT_SIZE), 
                PaintGroup(paintRect, paintText))
            drawCell(params.canvas, RectF(startX + maxWidth, top, startX + maxWidth * 2, bottom), 
                pair.second, PointF(startX + maxWidth + PADDING, bottom - TEXT_SIZE), 
                PaintGroup(paintRect, paintText))
        }
    }

    private fun createPage(pageParams: CreatePageParams) {
        val myPageInfo = PdfDocument.PageInfo.Builder(
            PAGE_WIDTH, PAGE_HEIGHT, pageParams.pageNumber).create()
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
            pageParams.endOfMonthDate, pageParams.projectTitles
        )
        printWorkDays(printParams)
        
        pageParams.pdfDocument.finishPage(myPage)
    }

    /**
     * Entry point for PDF generation.
     */
    fun generatePdf(params: GeneratePdfParams) {
        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = TITLE_SIZE
            color = ContextCompat.getColor(params.ctx, R.color.black)
        }

        val pages = listOf(
            PageInfo(1, DATE_01, DATE_08, false),
            PageInfo(2, DATE_09, DATE_16, false),
            PageInfo(3, DATE_17, DATE_24, false)
        )

        pages.forEach { info ->
            createPage(createPageParams(params, pdfDocument, titlePaint, info))
        }

        val lastDay = LocalDate.parse(params.endOfMonthDate).dayOfMonth
        createPage(createPageParams(params, pdfDocument, titlePaint, 
            PageInfo(4, DATE_25, lastDay, true)))

        savePdf(params.ctx, pdfDocument, params.name, params.endOfMonthDate)
        pdfDocument.close()
    }

    private fun createPageParams(
        params: GeneratePdfParams,
        doc: PdfDocument,
        paint: Paint,
        info: PageInfo
    ) = CreatePageParams(
        params.name, params.employer, params.projectsByMonth,
        params.endOfMonthDate, params.totalSumLabel, params.monthlyReportLabel,
        doc, paint, info.number, info.start, info.end,
        info.showTotals, params.projectTitles
    )

    private fun savePdf(ctx: Context, doc: PdfDocument, name: String, date: String) {
        val directory = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), getApplicationName(ctx))
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e("PdfGenerator", "Failed to create directory")
        }

        val formattedDate = LocalDate.parse(date)
        val yearMonth = DateTimeFormatter.ofPattern("MM_yyyy").format(formattedDate)
        val fileName = "${name}${yearMonth}".replace(" ", "")
        val file = File(directory, "$fileName.pdf")

        try {
            doc.writeTo(FileOutputStream(file))
            Toast.makeText(ctx, "PDF generated at: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Log.e("PdfGenerator", "Error saving PDF", e)
            Toast.makeText(ctx, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getApplicationName(context: Context): String {
        return context.applicationInfo.loadLabel(context.packageManager).toString()
    }

    private data class PaintGroup(val rectPaint: Paint, val textPaint: Paint)
    private data class PageInfo(val number: Int, val start: Int, val end: Int, val showTotals: Boolean)
}

data class PrintWorkDaysParams(
    val projectsByMonth: List<Project>,
    val canvas: Canvas,
    val showTotals: Boolean,
    val startDate: Int,
    val endDate: Int,
    val totalSumLabel: String,
    val endOfMonthDate: String,
    val projectTitles: List<String>
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
    val projectsByMonth: List<Project>,
    val endOfMonthDate: String,
    val totalSumLabel: String,
    val monthlyReportLabel: String,
    val pdfDocument: PdfDocument,
    val title: Paint,
    val pageNumber: Int,
    val startDate: Int,
    val endDate: Int,
    val showTotals: Boolean,
    val projectTitles: List<String>
)

data class GeneratePdfParams(
    val ctx: Context,
    val projectsByMonth: List<Project>,
    val endOfMonthDate: String,
    val totalSumLabel: String,
    val monthlyReportLabel: String,
    val name: String,
    val employer: String,
    val projectTitles: List<String>
)
