package com.akiwiksten.worktime30.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.core.MonthlyReportGenerator.ORIGIN_LEFT_FIRST
import com.akiwiksten.worktime30.core.MonthlyReportGenerator.ORIGIN_TOP_FIRST
import com.akiwiksten.worktime30.core.MonthlyReportGenerator.TEXT_SIZE
import com.akiwiksten.worktime30.core.WorkTimeCalculator.parseDate
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import androidx.compose.foundation.Canvas as ComposeCanvas

@Composable
fun PrintWorkdaysComposable(params: PrintWorkdaysComposableParams) {
    val paints = rememberReportPaints()
    val endOfMonth = parseDate(workday = params.endOfMonthDate).toInt()

    ComposeCanvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp)
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        var currentX = ORIGIN_LEFT_FIRST
        var prevWidth = 0f

        for (day in params.startDate..params.endDate) {
            val projectAttrs = getProjectAttributesForDay(projects = params.projectsByMonth, day = day)
            if (projectAttrs.isEmpty()) continue

            val dayWidth = MonthlyReportGenerator.getMaxLengthOfProjectAttributes(
                list = projectAttrs,
                paint = paints.textPaint
            ).coerceAtLeast(40f)

            currentX += prevWidth
            drawDayColumn(
                canvas = nativeCanvas,
                day = day,
                attrs = projectAttrs,
                bounds = ColumnBounds(x = currentX, width = dayWidth),
                paints = paints
            )
            prevWidth = dayWidth
        }

        if (params.showTotals) {
            drawTotalsSection(
                canvas = nativeCanvas,
                params = params,
                endDay = endOfMonth,
                startX = currentX + prevWidth,
                paints = paints
            )
        }
    }
}

@Composable
private fun rememberReportPaints(): ReportPaints {
    val black = Color.Black.toArgb()
    return ReportPaints(
        textPaint = createPaint(style = Typeface.NORMAL, paintStyle = Paint.Style.FILL, color = black),
        boldTextPaint = createPaint(style = Typeface.BOLD, paintStyle = Paint.Style.FILL, color = black),
        rectPaint = createPaint(
            style = Typeface.NORMAL,
            paintStyle = Paint.Style.STROKE,
            color = black,
            strokeWidth = 1f),
        boldRectPaint = createPaint(
            style = Typeface.NORMAL,
            paintStyle = Paint.Style.STROKE,
            color = black,
            strokeWidth = 2f)
    )
}

private fun createPaint(
    style: Int,
    paintStyle: Paint.Style,
    color: Int,
    strokeWidth: Float = 1f
): Paint = Paint().apply {
    textSize = TEXT_SIZE
    this.color = color
    typeface = Typeface.create(Typeface.DEFAULT, style)
    this.style = paintStyle
    this.strokeWidth = strokeWidth
    isAntiAlias = true
}

private fun drawDayColumn(
    canvas: Canvas,
    day: Int,
    attrs: List<String>,
    bounds: ColumnBounds,
    paints: ReportPaints
) {
    // Header
    val headerRect = RectF(bounds.x, ORIGIN_TOP_FIRST, bounds.x + bounds.width, ORIGIN_TOP_FIRST + TEXT_SIZE * 2)
    drawCell(
        canvas = canvas,
        rect = headerRect,
        text = day.toString(),
        paints = PaintPair(rect = paints.boldRectPaint, text = paints.boldTextPaint),
        centerText = true
    )

    // Attributes
    attrs.forEachIndexed { index, attr ->
        val top = ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index + 1)
        val rect = RectF(bounds.x, top, bounds.x + bounds.width, top + TEXT_SIZE * 2)
        drawCell(
            canvas = canvas,
            rect = rect,
            text = attr,
            paints = PaintPair(rect = paints.rectPaint, text = paints.textPaint),
            centerText = false
        )
    }
}

private fun drawCell(
    canvas: Canvas,
    rect: RectF,
    text: String,
    paints: PaintPair,
    centerText: Boolean
) {
    canvas.drawRect(rect, paints.rect)
    val textX = if (centerText) rect.centerX() - paints.text.measureText(text) / 2 else rect.left + 4f
    canvas.drawText(text, textX, rect.bottom - (rect.height() - TEXT_SIZE) / 2 - 2f, paints.text)
}

private fun getProjectAttributesForDay(projects: List<ProjectEntity>, day: Int): List<String> {
    return projects.filter { parseDate(workday = it.date).toInt() == day && it.projectTime != ZERO_TIME }
        .flatMap { project ->
            listOf(
                project.projectName,
                project.projectTime,
                project.allowance,
                project.workType,
                "${project.kilometres}km",
                ""
            )
        }
}

private fun drawTotalsSection(
    canvas: Canvas,
    params: PrintWorkdaysComposableParams,
    endDay: Int,
    startX: Float,
    paints: ReportPaints
) {
    val totals = calculateTotals(projects = params.projectsByMonth, endDay = endDay)
    val maxWidth = MonthlyReportGenerator.getMaxLengthOfProjectAttributesMap(map = totals, paint = paints.textPaint)

    totals.toList().forEachIndexed { index, (name, value) ->
        val top = ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index + 1)
        val nameRect = RectF(startX, top, startX + maxWidth, top + TEXT_SIZE * 2)
        val valRect = RectF(startX + maxWidth, top, startX + maxWidth * 1.5f, top + TEXT_SIZE * 2)

        drawCell(
            canvas = canvas,
            rect = nameRect,
            text = name,
            paints = PaintPair(rect = paints.rectPaint, text = paints.textPaint),
            centerText = false
        )
        drawCell(
            canvas = canvas,
            rect = valRect,
            text = value,
            paints = PaintPair(rect = paints.rectPaint, text = paints.textPaint),
            centerText = false
        )
    }
}

private fun calculateTotals(projects: List<ProjectEntity>, endDay: Int): Map<String, String> {
    val totals = mutableMapOf<String, String>()
    for (day in 1..endDay) {
        projects.filter { parseDate(workday = it.date).toInt() == day && it.projectTime != ZERO_TIME }
            .forEach { p ->
                totals[p.projectName] = WorkTimeCalculator.calculateWorkTimeBalance(
                    initialTime = totals[p.projectName] ?: ZERO_TIME,
                    addedTime = p.projectTime
                )
            }
    }
    return totals
}

private data class ReportPaints(
    val textPaint: Paint,
    val boldTextPaint: Paint,
    val rectPaint: Paint,
    val boldRectPaint: Paint
)

private data class ColumnBounds(val x: Float, val width: Float)
private data class PaintPair(val rect: Paint, val text: Paint)

data class PrintWorkdaysComposableParams(
    val projectsByMonth: List<ProjectEntity>,
    val endOfMonthDate: String,
    val startDate: Int,
    val endDate: Int,
    val totalSumLabel: String,
    val showTotals: Boolean
)
