@file:Suppress("MagicNumber")

package com.akiwiksten.worktime30.core

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.WorkTimeCalculator.parseDate
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
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
        val maxKeyWidth = map.keys.maxOfOrNull { paint.measureText(it) } ?: 0f
        val maxValueWidth = map.values.maxOfOrNull { paint.measureText(it) } ?: 0f
        return maxOf(maxKeyWidth, maxValueWidth) + PADDING
    }

    private fun createUniqueProjects(params: PrintWorkDaysParams): Map<String, String> {
        val uniqueProjects = mutableMapOf<String, String>()
        params.projectsByMonth
            .filter { it.projectTime != ZERO_TIME }
            .groupBy { it.projectName }
            .forEach { (name, projects) ->
                uniqueProjects[name] = projects.fold(ZERO_TIME) { acc, project ->
                    WorkTimeCalculator.calculateTotalMinutes(acc, project.projectTime, false, false)
                }
            }
        val totalSum = uniqueProjects.values.fold(ZERO_TIME) { acc, time ->
            WorkTimeCalculator.calculateTotalMinutes(acc, time, false, false)
        }
        uniqueProjects[params.totalSumLabel] = totalSum
        return uniqueProjects
    }

    private fun drawCell(canvas: Canvas, rect: RectF, text: String, textPos: PointF, paints: PaintGroup) {
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paints.rectPaint)
        canvas.drawText(text, textPos.x, textPos.y, paints.textPaint)
    }

    private fun drawProjectTitles(params: DrawProjectDaysParams, maxRows: Int) {
        val rectHeight = TEXT_SIZE * 2
        val top = ORIGIN_TOP_FIRST
        val bottom = top + rectHeight

        // Draw header cell
        drawCell(
            params.printWorkDaysParams.canvas,
            RectF(params.left, top, params.right, bottom),
            params.projectAttributes[0],
            PointF(params.x + params.measuredPaintTextRight / 3, top + TEXT_SIZE * 1.5f),
            PaintGroup(paintBoldRect, paintBoldText)
        )

        // Draw rows, repeating titles if multiple projects exist
        for (i in 1..maxRows) {
            val cellTop = top + i * rectHeight
            val cellBottom = cellTop + rectHeight
            val titleIndex = i % params.projectAttributes.size
            val text = if (titleIndex == 0) "" else params.projectAttributes[titleIndex]
            
            drawCell(
                params.printWorkDaysParams.canvas,
                RectF(params.left, cellTop, params.right, cellBottom),
                text,
                PointF(params.x + PADDING, cellBottom - TEXT_SIZE),
                PaintGroup(paintRect, paintText)
            )
        }
    }

    private fun drawProjectDays(params: DrawProjectDaysParams, maxRows: Int) {
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

        for (i in 1..maxRows) {
            val cellTop = top + i * rectHeight
            val cellBottom = cellTop + rectHeight
            val text = params.projectAttributes.getOrNull(i - 1) ?: ""
            val paints = PaintGroup(paintRect, if ((i % 6) == 1 && text.isNotEmpty()) paintBoldText else paintText)
            
            drawCell(
                params.printWorkDaysParams.canvas,
                RectF(params.left, cellTop, params.right, cellBottom),
                text,
                PointF(params.x + PADDING, cellBottom - TEXT_SIZE),
                paints
            )
        }
    }

    private fun printWorkDays(params: PrintWorkDaysParams) {
        var currentXOffset = ORIGIN_LEFT_FIRST

        val attrCount = params.projectTitles.size
        // Calculate max rows needed for this page range, ensuring we align with project attribute count
        val maxRowsRaw = (params.startDate..params.endDate).maxOf { day ->
            params.preprocessedProjects[day]?.size ?: 0
        }
        val maxRows = if (maxRowsRaw <= attrCount) attrCount else ((maxRowsRaw + attrCount - 1) / attrCount) * attrCount

        val titleWidth = getMaxLengthOfProjectAttributes(params.projectTitles, paintText)
            .coerceAtLeast(DEFAULT_WIDTH)
        val titleParams = DrawProjectDaysParams(
            params, currentXOffset, currentXOffset + titleWidth,
            currentXOffset, titleWidth, params.projectTitles, 0
        )
        drawProjectTitles(titleParams, maxRows)
        currentXOffset += titleWidth

        for (day in params.startDate..params.endDate) {
            val projectAttrs = params.preprocessedProjects[day] ?: emptyList()
            val dayWidth = getMaxLengthOfProjectAttributes(projectAttrs, paintText)
                .coerceAtLeast(DEFAULT_WIDTH)
            val dayParams = DrawProjectDaysParams(
                params, currentXOffset, currentXOffset + dayWidth,
                currentXOffset, dayWidth, projectAttrs, day
            )
            drawProjectDays(dayParams, maxRows)
            currentXOffset += dayWidth
        }

        if (params.showTotals) drawTotals(params, currentXOffset)
    }

    private fun drawTotals(params: PrintWorkDaysParams, startX: Float) {
        val uniqueProjects = createUniqueProjects(params)
        val maxWidth = getMaxLengthOfProjectAttributesMap(uniqueProjects, paintText)
        val rectHeight = TEXT_SIZE * 2

        uniqueProjects.toList().forEachIndexed { index, pair ->
            val top = ORIGIN_TOP_FIRST + (index + 1) * rectHeight
            val bottom = top + rectHeight

            drawCell(
                params.canvas, RectF(startX, top, startX + maxWidth, bottom),
                pair.first, PointF(startX + PADDING, bottom - TEXT_SIZE),
                PaintGroup(paintRect, paintText)
            )
            drawCell(
                params.canvas, RectF(startX + maxWidth, top, startX + maxWidth * 2, bottom),
                pair.second, PointF(startX + maxWidth + PADDING, bottom - TEXT_SIZE),
                PaintGroup(paintRect, paintText)
            )
        }
    }

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
        printWorkDays(printParams)

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
            .groupBy { parseDate(it.date).toInt() }
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
        createPage(createPageParams(
            params = params,
            doc = pdfDocument,
            paint = titlePaint,
            info = PageInfo(4, DATE_25, lastDay, true),
            preprocessedProjects = preprocessedProjects
        ))

        savePdf(params.ctx, pdfDocument, params.name, params.endOfMonthDate)
        pdfDocument.close()
    }

    private fun createPageParams(
        params: GeneratePdfParams,
        doc: PdfDocument,
        paint: Paint,
        info: PageInfo,
        preprocessedProjects: Map<Int, List<String>>
    ) = CreatePageParams(
        params.name, params.employer, params.projectsByMonth, params.endOfMonthDate,
        params.totalSumLabel, params.monthlyReportLabel, doc, paint, info.number,
        info.start, info.end, info.showTotals, params.projectTitles, preprocessedProjects
    )

    private fun savePdf(ctx: Context, doc: PdfDocument, name: String, date: String) {
        try {
            val formattedDate = LocalDate.parse(date)
            val yearMonth = DateTimeFormatter.ofPattern("MM_yyyy").format(formattedDate)
            val fileName = "${name.ifEmpty { "Report" }}_${yearMonth}.pdf".replace(" ", "_")
            val appName = getApplicationName(ctx)
            
            Log.d("PdfGenerator", "Starting save for $fileName into folder $appName")

            val relativePath = Environment.DIRECTORY_DOWNLOADS + File.separator + appName
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = ctx.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    doc.writeTo(outputStream)
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                
                Log.d("PdfGenerator", "PDF saved successfully via MediaStore to: $relativePath/$fileName")
                Toast.makeText(ctx, "PDF saved to Downloads/$appName", Toast.LENGTH_LONG).show()
            } else {
                throw IOException("Failed to create MediaStore entry")
            }
        } catch (e: IOException) {
            Log.e("PdfGenerator", "IO error saving PDF", e)
            Toast.makeText(ctx, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getApplicationName(context: Context): String {
        return context.applicationInfo.loadLabel(context.packageManager).toString()
    }

    private data class PaintGroup(val rectPaint: Paint, val textPaint: Paint)
    private data class PageInfo(val number: Int, val start: Int, val end: Int, val showTotals: Boolean)
}

data class PrintWorkDaysParams(
    val projectsByMonth: List<ProjectEntity>,
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
    val projectsByMonth: List<ProjectEntity>,
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
    val projectsByMonth: List<ProjectEntity>,
    val endOfMonthDate: String,
    val totalSumLabel: String,
    val monthlyReportLabel: String,
    val name: String,
    val employer: String,
    val projectTitles: List<String>
)
