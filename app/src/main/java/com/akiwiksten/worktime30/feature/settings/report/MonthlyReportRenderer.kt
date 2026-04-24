@file:Suppress("MagicNumber")

package com.akiwiksten.worktime30.feature.settings.report

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ZERO_TIME

internal object MonthlyReportRenderer {
    private const val PADDING = 5.0f
    private const val DEFAULT_WIDTH = 30.0f

    private val paintText = createPaint(Typeface.NORMAL, 1f, Paint.Style.FILL)
    private val paintBoldText = createPaint(Typeface.BOLD, 1f, Paint.Style.FILL)
    private val paintRect = createPaint(Typeface.NORMAL, 1f, Paint.Style.STROKE)
    private val paintBoldRect = createPaint(Typeface.NORMAL, 3f, Paint.Style.STROKE)

    fun getMaxLengthOfProjectAttributes(list: Collection<String>, paint: Paint): Float {
        return (list.maxOfOrNull { paint.measureText(it) } ?: 0f) + PADDING
    }

    fun getMaxLengthOfProjectAttributesMap(map: Map<String, String>, paint: Paint): Float {
        val maxKeyWidth = map.keys.maxOfOrNull { paint.measureText(it) } ?: 0f
        val maxValueWidth = map.values.maxOfOrNull { paint.measureText(it) } ?: 0f
        return maxOf(maxKeyWidth, maxValueWidth) + PADDING
    }

    fun printWorkDays(params: PrintWorkDaysParams) {
        var currentXOffset = MonthlyReportGenerator.ORIGIN_LEFT_FIRST

        val attrCount = params.projectTitles.size
        val maxRowsRaw = (params.startDate..params.endDate).maxOf { day ->
            params.preprocessedProjects[day]?.size ?: 0
        }
        val maxRows = if (maxRowsRaw <= attrCount) attrCount else ((maxRowsRaw + attrCount - 1) / attrCount) * attrCount

        val titleWidth = getMaxLengthOfProjectAttributes(params.projectTitles, paintText)
            .coerceAtLeast(minimumValue = DEFAULT_WIDTH)
        val titleParams = DrawProjectDaysParams(
            printWorkDaysParams = params,
            left = currentXOffset,
            right = currentXOffset + titleWidth,
            x = currentXOffset,
            measuredPaintTextRight = titleWidth,
            projectAttributes = params.projectTitles,
            day = 0
        )
        drawProjectTitles(titleParams, maxRows)
        currentXOffset += titleWidth

        for (day in params.startDate..params.endDate) {
            val projectAttrs = params.preprocessedProjects[day] ?: emptyList()
            val dayWidth = getMaxLengthOfProjectAttributes(projectAttrs, paintText)
                .coerceAtLeast(minimumValue = DEFAULT_WIDTH)
            val dayParams = DrawProjectDaysParams(
                printWorkDaysParams = params,
                left = currentXOffset,
                right = currentXOffset + dayWidth,
                x = currentXOffset,
                measuredPaintTextRight = dayWidth,
                projectAttributes = projectAttrs,
                day = day
            )
            drawProjectDays(dayParams, maxRows)
            currentXOffset += dayWidth
        }

        if (params.showTotals) drawTotals(params, currentXOffset)
    }

    private fun createPaint(style: Int, strokeWidth: Float, paintStyle: Paint.Style): Paint {
        return Paint().apply {
            textSize = MonthlyReportGenerator.TEXT_SIZE
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, style)
            this.strokeWidth = strokeWidth
            this.style = paintStyle
        }
    }

    private fun createUniqueProjects(params: PrintWorkDaysParams): Map<String, String> {
        val uniqueProjects = mutableMapOf<String, String>()
        params.projectsByMonth
            .filter { it.projectTime != ZERO_TIME }
            .groupBy { it.projectName }
            .forEach { (name, projects) ->
                uniqueProjects[name] = projects.fold(ZERO_TIME) { acc, project ->
                    WorkTimeCalculator.calculateTotalMinutes(
                        initialTime = acc,
                        addedTime = project.projectTime,
                        isInitialTimeNegative = false,
                        isAddedTimeNegative = false
                    )
                }
            }
        val totalSum = uniqueProjects.values.fold(ZERO_TIME) { acc, time ->
            WorkTimeCalculator.calculateTotalMinutes(
                initialTime = acc,
                addedTime = time,
                isInitialTimeNegative = false,
                isAddedTimeNegative = false
            )
        }
        uniqueProjects[params.totalSumLabel] = totalSum
        return uniqueProjects
    }

    private fun drawCell(canvas: Canvas, rect: RectF, text: String, textPos: PointF, paints: PaintGroup) {
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paints.rectPaint)
        canvas.drawText(text, textPos.x, textPos.y, paints.textPaint)
    }

    private fun drawProjectTitles(params: DrawProjectDaysParams, maxRows: Int) {
        val rectHeight = MonthlyReportGenerator.TEXT_SIZE * 2
        val top = MonthlyReportGenerator.ORIGIN_TOP_FIRST
        val bottom = top + rectHeight

        drawCell(
            params.printWorkDaysParams.canvas,
            RectF(params.left, top, params.right, bottom),
            params.projectAttributes[0],
            PointF(params.x + params.measuredPaintTextRight / 3, top + MonthlyReportGenerator.TEXT_SIZE * 1.5f),
            PaintGroup(paintBoldRect, paintBoldText)
        )

        for (i in 1..maxRows) {
            val cellTop = top + i * rectHeight
            val cellBottom = cellTop + rectHeight
            val titleIndex = i % params.projectAttributes.size
            val text = if (titleIndex == 0) "" else params.projectAttributes[titleIndex]

            drawCell(
                canvas = params.printWorkDaysParams.canvas,
                rect = RectF(params.left, cellTop, params.right, cellBottom),
                text = text,
                textPos = PointF(params.x + PADDING, cellBottom - MonthlyReportGenerator.TEXT_SIZE),
                paints = PaintGroup(paintRect, paintText)
            )
        }
    }

    private fun drawProjectDays(params: DrawProjectDaysParams, maxRows: Int) {
        val rectHeight = MonthlyReportGenerator.TEXT_SIZE * 2
        val top = MonthlyReportGenerator.ORIGIN_TOP_FIRST
        val bottom = top + rectHeight

        drawCell(
            canvas = params.printWorkDaysParams.canvas,
            rect = RectF(params.left, top, params.right, bottom),
            text = params.day.toString(),
            textPos = PointF(
                params.x + params.measuredPaintTextRight / 2,
                top + MonthlyReportGenerator.TEXT_SIZE * 1.5f
            ),
            paints = PaintGroup(paintBoldRect, paintBoldText)
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
                PointF(params.x + PADDING, cellBottom - MonthlyReportGenerator.TEXT_SIZE),
                paints
            )
        }
    }

    private fun drawTotals(params: PrintWorkDaysParams, startX: Float) {
        val uniqueProjects = createUniqueProjects(params)
        val maxWidth = getMaxLengthOfProjectAttributesMap(uniqueProjects, paintText)
        val rectHeight = MonthlyReportGenerator.TEXT_SIZE * 2

        uniqueProjects.toList().forEachIndexed { index, pair ->
            val top = MonthlyReportGenerator.ORIGIN_TOP_FIRST + (index + 1) * rectHeight
            val bottom = top + rectHeight

            drawCell(
                canvas = params.canvas,
                rect = RectF(
                    startX,
                    top,
                    startX + maxWidth,
                    bottom
                ),
                text = pair.first,
                textPos = PointF(startX + PADDING, bottom - MonthlyReportGenerator.TEXT_SIZE),
                paints = PaintGroup(paintRect, paintText)
            )
            drawCell(
                params.canvas,
                rect = RectF(startX + maxWidth, top, startX + maxWidth * 2, bottom),
                text = pair.second,
                textPos = PointF(startX + maxWidth + PADDING, bottom - MonthlyReportGenerator.TEXT_SIZE),
                paints = PaintGroup(paintRect, paintText)
            )
        }
    }

    private data class PaintGroup(val rectPaint: Paint, val textPaint: Paint)
}
