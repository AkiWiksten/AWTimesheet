package com.akiwiksten.worktime30.feature.settings

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.core.ORIGIN_LEFT_FIRST
import com.akiwiksten.worktime30.core.ORIGIN_TOP_FIRST
import com.akiwiksten.worktime30.core.PdfGenerator
import com.akiwiksten.worktime30.core.TEXT_SIZE
import com.akiwiksten.worktime30.core.TimeGeneratorModel
import com.akiwiksten.worktime30.core.TimeGeneratorModel.Companion.parseDate
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.Project

/**
 * For debugging purpose
 */
@Suppress(
    "LongMethod",
    "MagicNumber",
    "CyclomaticComplexMethod"
)
@Composable
fun PrintWorkDaysComposable(
    params: PrintWorkDaysComposableParams
) {
    val pdfGenerator = PdfGenerator()
    val paintText = Paint().apply {
        textSize = TEXT_SIZE
        color = Color.Black.toArgb()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val paintBoldText = Paint().apply {
        textSize = TEXT_SIZE
        color = Color.Black.toArgb()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val paintRect = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        textSize = TEXT_SIZE
        color = Color.Black.toArgb()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val paintBoldRect = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        textSize = TEXT_SIZE
        color = Color.Black.toArgb()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    val endOfMonth = parseDate(params.endOfMonthDate)

    Canvas(
        modifier = Modifier
            .height(1500.dp)
            .width(1300.dp)
            .padding(vertical = 70.dp, horizontal = 0.dp)
    ) {
        var measuredPaintTextLeft = 0.0f
        var measuredPaintTextRight = 0.0f
        var left = ORIGIN_LEFT_FIRST
        var right = ORIGIN_LEFT_FIRST
        var x = ORIGIN_LEFT_FIRST
        for (day in params.startDate..params.endDate) {
            // Projects of one specific day
            val projects = params.projectsByMonth.filter { p -> parseDate(p.date).toInt() == day }
            val projectAttributes: MutableList<String> = mutableListOf()
            for (project in projects) {
                if (project.projectTime != ZERO_TIME) {
                    projectAttributes.add(project.projectName)
                    projectAttributes.add(project.projectTime)
                    projectAttributes.add(project.allowance)
                    projectAttributes.add(project.workType)
                    projectAttributes.add(project.kilometres.toString() + " km")
                    projectAttributes.add("")
                }
            }
            val padding = 5.0f
            measuredPaintTextRight =
                pdfGenerator.getMaxLengthOfProjectAttributes(projectAttributes, paintText) + padding
            if (measuredPaintTextRight == padding) {
                measuredPaintTextRight = 30.0f
            }
            left += measuredPaintTextLeft
            right += measuredPaintTextRight
            x += measuredPaintTextLeft
            drawContext.canvas.nativeCanvas.drawRect(
                /* left = */
                left,
                /* top = */
                ORIGIN_TOP_FIRST,
                /* right = */
                right,
                /* bottom = */
                ORIGIN_TOP_FIRST + TEXT_SIZE * 2,
                /* paint = */
                paintBoldRect
            )
            drawContext.canvas.nativeCanvas.drawText(
                /* text = */
                day.toString(),
                /* x = */
                x + measuredPaintTextRight / 2,
                /* y = */
                ORIGIN_TOP_FIRST + TEXT_SIZE + TEXT_SIZE / 2,
                /* paint = */
                paintBoldText
            )

            for (attrIndex in 1..projectAttributes.size) {
                drawContext.canvas.nativeCanvas.drawRect(
                    /* left = */
                    left,
                    /* top = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (attrIndex),
                    /* right = */
                    right,
                    /* bottom = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (attrIndex + 1),
                    /* paint = */
                    paintRect
                )
                drawContext.canvas.nativeCanvas.drawText(
                    /* text = */
                    projectAttributes[attrIndex - 1],
                    /* x = */
                    x + 5f,
                    /* y = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (attrIndex + 1) - TEXT_SIZE,
                    /* paint = */
                    paintText
                )
            }
            measuredPaintTextLeft = measuredPaintTextRight
        }

        if (params.showTotals) {
            left += measuredPaintTextLeft
            right += measuredPaintTextRight
            x += measuredPaintTextLeft
            val uniqueProjects: MutableMap<String, String> = mutableMapOf()
            for (day in 1..endOfMonth.toInt()) {
                // Projects of one specific day
                val projects = params.projectsByMonth.filter { p ->
                    parseDate(p.date).toInt() == day &&
                        p.projectTime != ZERO_TIME
                }
                for (project in projects) {
                    if (uniqueProjects[project.projectName] == null) {
                        uniqueProjects[project.projectName] = project.projectTime
                    } else {
                        uniqueProjects[project.projectName] =
                            TimeGeneratorModel.calculateTotalMinutes(
                                initialTime = uniqueProjects[project.projectName]!!,
                                addedTime = project.projectTime,
                                isInitialTimeNegative = false,
                                isAddedTimeNegative = false
                            )
                    }
                }
            }

            var projectTimeTotalSum = ZERO_TIME
            for (project in uniqueProjects) {
                projectTimeTotalSum = TimeGeneratorModel.calculateTotalMinutes(
                    initialTime = projectTimeTotalSum,
                    addedTime = project.value,
                    isInitialTimeNegative = false,
                    isAddedTimeNegative = false
                )
            }
            uniqueProjects[params.totalSumLabel] = projectTimeTotalSum

            val padding = 5.0f
            measuredPaintTextRight =
                pdfGenerator.getMaxLengthOfProjectAttributesMap(uniqueProjects, paintText) + padding
            measuredPaintTextLeft =
                pdfGenerator.getMaxLengthOfProjectAttributesMap(uniqueProjects, paintText) + padding
            val list = uniqueProjects.toList()
            for (index in 1..list.size) {
                drawContext.canvas.nativeCanvas.drawRect(
                    /* left = */
                    left,
                    /* top = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index),
                    /* right = */
                    right + measuredPaintTextRight,
                    /* bottom = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index + 1),
                    /* paint = */
                    paintRect
                )
                drawContext.canvas.nativeCanvas.drawText(
                    /* text = */
                    list[index - 1].first,
                    /* x = */
                    x + 5f,
                    /* y = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index + 1) - TEXT_SIZE,
                    /* paint = */
                    paintText
                )
                drawContext.canvas.nativeCanvas.drawRect(
                    /* left = */
                    left + measuredPaintTextLeft,
                    /* top = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index),
                    /* right = */
                    right + measuredPaintTextRight,
                    /* bottom = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index + 1),
                    /* paint = */
                    paintRect
                )
                drawContext.canvas.nativeCanvas.drawText(
                    /* text = */
                    list[index - 1].second,
                    /* x = */
                    x + 5f + measuredPaintTextLeft,
                    /* y = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index + 1) - TEXT_SIZE,
                    /* paint = */
                    paintText
                )
            }
        }
    }
}

@Suppress("UNUSED_DECLARATION")
data class PrintWorkDaysComposableParams(
    val projectsByMonth: MutableList<Project>,
    val endOfMonthDate: String,
    val startDate: Int,
    val endDate: Int,
    val totalSumLabel: String,
    val showTotals: Boolean
)
