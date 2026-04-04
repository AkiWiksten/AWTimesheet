package com.akiwiksten.worktime30.feature.settings

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.core.MonthlyReportGenerator
import com.akiwiksten.worktime30.core.MonthlyReportGenerator.ORIGIN_LEFT_FIRST
import com.akiwiksten.worktime30.core.MonthlyReportGenerator.ORIGIN_TOP_FIRST
import com.akiwiksten.worktime30.core.MonthlyReportGenerator.TEXT_SIZE
import com.akiwiksten.worktime30.core.TimeGeneratorModel
import com.akiwiksten.worktime30.core.TimeGeneratorModel.Companion.parseDate
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.Project

/**
 * Composable used for debugging purposes to preview the work days report layout.
 */
@Composable
fun PrintWorkDaysComposable(params: PrintWorkDaysComposableParams) {
    val paints = rememberReportPaints()
    val endOfMonth = parseDate(params.endOfMonthDate)

    ComposeCanvas(
        modifier = Modifier
            .height(1500.dp)
            .width(1300.dp)
            .padding(vertical = 70.dp, horizontal = 0.dp)
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        var currentX = ORIGIN_LEFT_FIRST
        var prevWidth = 0f

        for (day in params.startDate..params.endDate) {
            val projectAttrs = getProjectAttributesForDay(params.projectsByMonth, day)
            if (projectAttrs.isEmpty()) continue

            val dayWidth = MonthlyReportGenerator.getMaxLengthOfProjectAttributes(
                projectAttrs, paints.textPaint
            ).coerceAtLeast(30f)

            currentX += prevWidth

            // Draw Day Header
            val headerRect = RectF(
                currentX,
                ORIGIN_TOP_FIRST,
                currentX + dayWidth,
                ORIGIN_TOP_FIRST + TEXT_SIZE * 2
            )
            drawCell(
                canvas = nativeCanvas,
                rect = headerRect,
                text = day.toString(),
                pos = PointF(currentX + dayWidth / 2, ORIGIN_TOP_FIRST + TEXT_SIZE * 1.5f),
                cellPaints = CellPaints(paints.boldRectPaint, paints.boldTextPaint)
            )

            // Draw Attributes
            projectAttrs.forEachIndexed { index, attr ->
                val top = ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index + 1)
                val rect = RectF(currentX, top, currentX + dayWidth, top + TEXT_SIZE * 2)
                drawCell(
                    canvas = nativeCanvas,
                    rect = rect,
                    text = attr,
                    pos = PointF(currentX + 5f, top + TEXT_SIZE * 1.5f),
                    cellPaints = CellPaints(paints.rectPaint, paints.textPaint)
                )
            }
            prevWidth = dayWidth
        }

        if (params.showTotals) {
            drawTotalsSection(nativeCanvas, params, endOfMonth, currentX + prevWidth, paints)
        }
    }
}

@Composable
private fun rememberReportPaints(): ReportPaints {
    val black = Color.Black.toArgb()
    return ReportPaints(
        textPaint = createPaint(Typeface.NORMAL, Paint.Style.FILL, black),
        boldTextPaint = createPaint(Typeface.BOLD, Paint.Style.FILL, black),
        rectPaint = createPaint(Typeface.NORMAL, Paint.Style.STROKE, black, 1f),
        boldRectPaint = createPaint(Typeface.NORMAL, Paint.Style.STROKE, black, 3f)
    )
}

private fun createPaint(
    style: Int,
    paintStyle: Paint.Style,
    color: Int,
    strokeWidth: Float = 1f
): Paint {
    return Paint().apply {
        textSize = TEXT_SIZE
        this.color = color
        typeface = Typeface.create(Typeface.DEFAULT, style)
        this.style = paintStyle
        this.strokeWidth = strokeWidth
    }
}

private fun drawCell(
    canvas: Canvas,
    rect: RectF,
    text: String,
    pos: PointF,
    cellPaints: CellPaints
) {
    canvas.drawRect(rect, cellPaints.rect)
    canvas.drawText(text, pos.x, pos.y, cellPaints.text)
}

private fun getProjectAttributesForDay(projects: List<Project>, day: Int): List<String> {
    return projects.filter { parseDate(it.date).toInt() == day && it.projectTime != ZERO_TIME }
        .flatMap { project ->
            listOf(
                project.projectName, project.projectTime, project.allowance,
                project.workType, "${project.kilometres} km", ""
            )
        }
}

private fun drawTotalsSection(
    canvas: Canvas,
    params: PrintWorkDaysComposableParams,
    endOfMonth: String,
    startX: Float,
    paints: ReportPaints
) {
    val uniqueProjects = mutableMapOf<String, String>()
    for (day in 1..endOfMonth.toInt()) {
        params.projectsByMonth
            .filter { parseDate(it.date).toInt() == day && it.projectTime != ZERO_TIME }
            .forEach { project ->
                uniqueProjects[project.projectName] = TimeGeneratorModel.calculateTotalMinutes(
                    initialTime = uniqueProjects[project.projectName] ?: ZERO_TIME,
                    addedTime = project.projectTime,
                    isInitialTimeNegative = false,
                    isAddedTimeNegative = false
                )
            }
    }

    val totalSum = uniqueProjects.values.fold(ZERO_TIME) { acc, time ->
        TimeGeneratorModel.calculateTotalMinutes(acc, time, false, false)
    }
    uniqueProjects[params.totalSumLabel] = totalSum

    val maxWidth = MonthlyReportGenerator.getMaxLengthOfProjectAttributesMap(
        uniqueProjects, paints.textPaint
    )

    uniqueProjects.toList().forEachIndexed { index, pair ->
        val top = ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index + 1)
        val rect = RectF(startX, top, startX + maxWidth, top + TEXT_SIZE * 2)
        val paints1 = CellPaints(paints.rectPaint, paints.textPaint)
        drawCell(canvas, rect, pair.first, PointF(startX + 5f, top + TEXT_SIZE * 1.5f), paints1)

        val valRect = RectF(startX + maxWidth, top, startX + maxWidth * 2, top + TEXT_SIZE * 2)
        val pos2 = PointF(startX + maxWidth + 5f, top + TEXT_SIZE * 1.5f)
        drawCell(canvas, valRect, pair.second, pos2, paints1)
    }
}

private data class ReportPaints(
    val textPaint: Paint,
    val boldTextPaint: Paint,
    val rectPaint: Paint,
    val boldRectPaint: Paint
)

private data class CellPaints(val rect: Paint, val text: Paint)

data class PrintWorkDaysComposableParams(
    val projectsByMonth: List<Project>,
    val endOfMonthDate: String,
    val startDate: Int,
    val endDate: Int,
    val totalSumLabel: String,
    val showTotals: Boolean
)
