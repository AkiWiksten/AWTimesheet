@file:Suppress("MagicNumber")

package com.akiwiksten.worktime30.core

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.TimeGeneratorModel.Companion.parseDate
import com.akiwiksten.worktime30.data.database.Project
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.collections.filter

class PdfGenerator {

    fun getMaxLengthOfProjectAttributes(list: Collection<String>, paintText: Paint): Float {
        var maxLength = 0.0f
        for (item in list) {
            val length = paintText.measureText(item)
            if (length > maxLength) {
                maxLength = length
            }
        }
        return maxLength
    }

    fun getMaxLengthOfProjectAttributesMap(
        map: Map<String, String>,
        paintText: Paint
    ): Float {
        var maxLength = 0.0f
        for (item in map) {
            val length = paintText.measureText(item.key)
            if (length > maxLength) {
                maxLength = length
            }
        }
        return maxLength
    }

    private fun createUniqueProjects(
        endOfMonth: String,
        params: PrintWorkDaysParams
    ): Map<String, String> {
        val uniqueProjects: MutableMap<String, String> = mutableMapOf()
        for (day in 1..endOfMonth.toInt()) {
            // Projects of one specific day
            val projectsPerDay = params.projectsByMonth.filter { p ->
                parseDate(p.date).toInt() == day &&
                    p.projectTime != ZERO_TIME
            }
            for (project in projectsPerDay) {
                if (uniqueProjects[project.projectName] == null) {
                    uniqueProjects[project.projectName] = project.projectTime
                } else {
                    uniqueProjects[project.projectName] = TimeGeneratorModel.calculateTotalMinutes(
                        initialTime = uniqueProjects[project.projectName] ?: ZERO_TIME,
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
        return uniqueProjects.toMap()
    }

    private fun drawProjectTitles(
        drawProjectDaysParams: DrawProjectDaysParams
    ) {
        drawProjectDaysParams.printWorkDaysParams.canvas.drawRect(
            /* left = */
            drawProjectDaysParams.left,
            /* top = */
            ORIGIN_TOP_FIRST,
            /* right = */
            drawProjectDaysParams.right,
            /* bottom = */
            ORIGIN_TOP_FIRST + TEXT_SIZE * 2,
            /* paint = */
            paintBoldRect
        )
        drawProjectDaysParams.printWorkDaysParams.canvas.drawText(
            /* text = */
            drawProjectDaysParams.projectAttributes[0],
            /* x = */
            drawProjectDaysParams.x + drawProjectDaysParams.measuredPaintTextRight / 3,
            /* y = */
            ORIGIN_TOP_FIRST + TEXT_SIZE + TEXT_SIZE / 2,
            /* paint = */
            paintBoldText
        )
        for (titleIndex in 1..<drawProjectDaysParams.projectAttributes.size) {
            drawProjectDaysParams.printWorkDaysParams.canvas.drawRect(
                /* left = */
                drawProjectDaysParams.left,
                /* top = */
                ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (titleIndex),
                /* right = */
                drawProjectDaysParams.right,
                /* bottom = */
                ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (titleIndex + 1),
                /* paint = */
                paintRect
            )
            drawProjectDaysParams.printWorkDaysParams.canvas.drawText(
                /* text = */
                drawProjectDaysParams.projectAttributes[titleIndex],
                /* x = */
                drawProjectDaysParams.x + 5f,
                /* y = */
                ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (titleIndex + 1) - TEXT_SIZE,
                /* paint = */
                paintText
            )
        }
    }

    private fun drawProjectDays(
        drawProjectDaysParams: DrawProjectDaysParams
    ) {
        drawProjectDaysParams.printWorkDaysParams.canvas.drawRect(
            /* left = */
            drawProjectDaysParams.left,
            /* top = */
            ORIGIN_TOP_FIRST,
            /* right = */
            drawProjectDaysParams.right,
            /* bottom = */
            ORIGIN_TOP_FIRST + TEXT_SIZE * 2,
            /* paint = */
            paintBoldRect
        )
        drawProjectDaysParams.printWorkDaysParams.canvas.drawText(
            /* text = */
            drawProjectDaysParams.day.toString(),
            /* x = */
            drawProjectDaysParams.x + drawProjectDaysParams.measuredPaintTextRight / 2,
            /* y = */
            ORIGIN_TOP_FIRST + TEXT_SIZE + TEXT_SIZE / 2,
            /* paint = */
            paintBoldText
        )

        for (attrIndex in 1..drawProjectDaysParams.projectAttributes.size) {
            drawProjectDaysParams.printWorkDaysParams.canvas.drawRect(
                /* left = */
                drawProjectDaysParams.left,
                /* top = */
                ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (attrIndex),
                /* right = */
                drawProjectDaysParams.right,
                /* bottom = */
                ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (attrIndex + 1),
                /* paint = */
                paintRect
            )
            if ((attrIndex % 8) == 1) {
                drawProjectDaysParams.printWorkDaysParams.canvas.drawText(
                    /* text = */
                    drawProjectDaysParams.projectAttributes[attrIndex - 1],
                    /* x = */
                    drawProjectDaysParams.x + 5f,
                    /* y = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (attrIndex + 1) - TEXT_SIZE,
                    /* paint = */
                    paintBoldText
                )
            } else {
                drawProjectDaysParams.printWorkDaysParams.canvas.drawText(
                    /* text = */
                    drawProjectDaysParams.projectAttributes[attrIndex - 1],
                    /* x = */
                    drawProjectDaysParams.x + 5f,
                    /* y = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (attrIndex + 1) - TEXT_SIZE,
                    /* paint = */
                    paintText
                )
            }
        }
    }

    private val paintText = Paint().apply {
        textSize = TEXT_SIZE
        color = Color.Black.toArgb()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val paintBoldText = Paint().apply {
        textSize = TEXT_SIZE
        color = Color.Black.toArgb()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val paintRect = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        textSize = TEXT_SIZE
        color = Color.Black.toArgb()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val paintBoldRect = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        textSize = TEXT_SIZE
        color = Color.Black.toArgb()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    @Suppress("LongMethod")
    private fun printWorkDays(
        params: PrintWorkDaysParams
    ) {
        val endOfMonth = parseDate(params.endOfMonthDate)
        var measuredPaintTextLeft = 0.0f
        var left = ORIGIN_LEFT_FIRST
        var right = ORIGIN_LEFT_FIRST
        var x = ORIGIN_LEFT_FIRST
        val padding = 5.0f
        var measuredPaintTextRight =
            getMaxLengthOfProjectAttributes(params.projectTitles, paintText) + padding
        if (measuredPaintTextRight == padding) {
            measuredPaintTextRight = 30.0f
        }
        left += measuredPaintTextLeft
        right += measuredPaintTextRight
        x += measuredPaintTextLeft

        drawProjectTitles(
            DrawProjectDaysParams(
                printWorkDaysParams = params,
                left = left,
                right = right,
                x = x,
                measuredPaintTextRight = measuredPaintTextRight,
                projectAttributes = params.projectTitles.toMutableList(),
                day = 0
            )
        )
        measuredPaintTextLeft = measuredPaintTextRight
        for (day in params.startDate..params.endDate) {
            // Projects of one specific day
            val projects = params.projectsByMonth.filter { p -> parseDate(p.date).toInt() == day }
            val projectAttributes: MutableList<String> = mutableListOf()
            for (project in projects) {
                if (project.projectStartTime != ZERO_TIME && project.projectEndTime != ZERO_TIME) {
                    project.projectTime = TimeGeneratorModel.calculateWorkTimeBalance(
                        project.projectEndTime,
                        "-" + project.projectStartTime
                    )
                    projectAttributes.add(project.projectName)
                    projectAttributes.add(project.projectStartTime)
                    projectAttributes.add(project.projectEndTime)
                    projectAttributes.add(project.projectTime)
                    projectAttributes.add(project.allowance)
                    projectAttributes.add(project.workType)
                    projectAttributes.add(project.kilometres.toString() + " km")
                    projectAttributes.add("")
                }
            }
            val padding = 5.0f
            measuredPaintTextRight =
                getMaxLengthOfProjectAttributes(projectAttributes, paintText) + padding
            if (measuredPaintTextRight == padding) {
                measuredPaintTextRight = 30.0f
            }
            left += measuredPaintTextLeft
            right += measuredPaintTextRight
            x += measuredPaintTextLeft
            drawProjectDays(
                DrawProjectDaysParams(
                    printWorkDaysParams = params,
                    left = left,
                    right = right,
                    x = x,
                    measuredPaintTextRight = measuredPaintTextRight,
                    projectAttributes = projectAttributes,
                    day = day
                )
            )
            measuredPaintTextLeft = measuredPaintTextRight
        }

        if (params.showTotals) {
            left += measuredPaintTextLeft
            right += measuredPaintTextRight
            x += measuredPaintTextLeft
            val uniqueProjects = createUniqueProjects(endOfMonth, params)

            val padding = 5.0f
            measuredPaintTextRight =
                getMaxLengthOfProjectAttributesMap(uniqueProjects, paintText) + padding
            measuredPaintTextLeft =
                getMaxLengthOfProjectAttributesMap(uniqueProjects, paintText) + padding
            val list = uniqueProjects.toList()
            for (index in 1..list.size) {
                params.canvas.drawRect(
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
                params.canvas.drawText(
                    /* text = */
                    list[index - 1].first,
                    /* x = */
                    x + 5f,
                    /* y = */
                    ORIGIN_TOP_FIRST + TEXT_SIZE * 2 * (index + 1) - TEXT_SIZE,
                    /* paint = */
                    paintText
                )
                params.canvas.drawRect(
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
                params.canvas.drawText(
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

    private fun createPage(
        pageParams: CreatePageParams
    ) {
        // we are adding page info to our PDF file
        // in which we will be passing our pageWidth,
        // pageHeight and number of pages and after that
        // we are calling it to create our PDF.
        val myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(
                /* pageWidth = */
                PAGE_WIDTH,
                /* pageHeight = */
                PAGE_HEIGHT,
                /* pageNumber = */
                pageParams.pageNumber
            ).create()

        // below line is used for setting
        // start page for our PDF file.
        val myPage: PdfDocument.Page = pageParams.pdfDocument.startPage(myPageInfo)

        // creating a variable for canvas
        // from our page of PDF.
        val canvas: Canvas = myPage.canvas

        if (pageParams.pageNumber == 1) {
            canvas.drawText(pageParams.monthlyReportLabel, 396F, 50F, pageParams.title)
        }
        pageParams.title.textAlign = Paint.Align.LEFT
        val formattedDate = LocalDate.parse(pageParams.endOfMonthDate)
        val yearAndMonth = DateTimeFormatter.ofPattern("MM/yyyy").format(formattedDate)

        // below line is used to draw text in our PDF file.
        // the first parameter is our text, second parameter
        // is position from start, third parameter is position from top
        // and then we are passing our variable of paint which is title.
        canvas.drawText(pageParams.name, 50F, 80F, pageParams.title)
        canvas.drawText(pageParams.employer, 50F, 100F, pageParams.title)
        canvas.drawText(yearAndMonth, 50F, 120F, pageParams.title)
        printWorkDays(
            PrintWorkDaysParams(
                projectsByMonth = pageParams.projectsByMonth,
                canvas = canvas,
                endOfMonthDate = pageParams.endOfMonthDate,
                startDate = pageParams.startDate,
                endDate = pageParams.endDate,
                totalSumLabel = pageParams.totalSumLabel,
                showTotals = pageParams.showTotals,
                projectTitles = pageParams.projectTitles
            )
        )
        // after adding all attributes to our
        // PDF file we will be finishing our page.
        pageParams.pdfDocument.finishPage(myPage)
    }

    private fun getApplicationName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) {
            applicationInfo.nonLocalizedLabel.toString()
        } else {
            context.getString(
                stringId
            )
        }
    }

    // on below line we are creating a generate PDF
    // method which is use to generate our PDF file.
    @Suppress("LongMethod")
    fun generatePdf(
        params: GeneratePdfParams
    ) {
        // creating an object variable
        // for our PDF document.
        val pdfDocument = PdfDocument()

        // two variables for paint "paint" is used
        // for drawing shapes and we will use "title"
        // for adding text in our PDF file.
        // val paint: Paint = Paint()
        val title = Paint()

        // below line is used for adding typeface for
        // our text which we will be adding in our PDF file.
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))

        // below line is used for setting text size
        // which we will be displaying in our PDF file.
        title.textSize = 15F

        // below line is sued for setting color
        // of our text inside our PDF file.
        title.setColor(ContextCompat.getColor(params.ctx, R.color.black))
        title.textAlign = Paint.Align.CENTER

        createPage(
            CreatePageParams(
                name = params.name,
                employer = params.employer,
                projectsByMonth = params.projectsByMonth,
                endOfMonthDate = params.endOfMonthDate,
                totalSumLabel = params.totalSumLabel,
                monthlyReportLabel = params.monthlyReportLabel,
                pdfDocument = pdfDocument,
                title = title,
                pageNumber = 1,
                startDate = DATE_01,
                endDate = DATE_08,
                showTotals = false,
                projectTitles = params.projectTitles
            )
        )

        createPage(
            CreatePageParams(
                name = params.name,
                employer = params.employer,
                projectsByMonth = params.projectsByMonth,
                endOfMonthDate = params.endOfMonthDate,
                totalSumLabel = params.totalSumLabel,
                monthlyReportLabel = params.monthlyReportLabel,
                pdfDocument = pdfDocument,
                title = title,
                pageNumber = 2,
                startDate = DATE_09,
                endDate = DATE_16,
                showTotals = false,
                projectTitles = params.projectTitles
            )
        )

        createPage(
            CreatePageParams(
                name = params.name,
                employer = params.employer,
                projectsByMonth = params.projectsByMonth,
                endOfMonthDate = params.endOfMonthDate,
                totalSumLabel = params.totalSumLabel,
                monthlyReportLabel = params.monthlyReportLabel,
                pdfDocument = pdfDocument,
                title = title,
                pageNumber = 3,
                startDate = DATE_17,
                endDate = DATE_24,
                showTotals = false,
                projectTitles = params.projectTitles
            )
        )

        val formattedDate = LocalDate.parse(params.endOfMonthDate)
        val day = DateTimeFormatter.ofPattern("dd").format(formattedDate)

        createPage(
            CreatePageParams(
                name = params.name,
                employer = params.employer,
                projectsByMonth = params.projectsByMonth,
                endOfMonthDate = params.endOfMonthDate,
                totalSumLabel = params.totalSumLabel,
                monthlyReportLabel = params.monthlyReportLabel,
                pdfDocument = pdfDocument,
                title = title,
                pageNumber = 4,
                startDate = DATE_25,
                endDate = day.toInt(),
                showTotals = true,
                projectTitles = params.projectTitles
            )
        )

        val yearAndMonth = DateTimeFormatter.ofPattern("MM_yyyy").format(formattedDate)
        val directory = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ).toString() + "/" +
                getApplicationName(params.ctx)
        )

        if (!directory.exists()) {
            val success = directory.mkdir()
            if (!success) {
                Log.e("PDfGenerator", "Failed mkdir!")
            }
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }
        val fileName = params.name + yearAndMonth
        // below line is used to set the name of
        // our PDF file and its path.
        val file = File(
            directory,
            "${fileName.replace(" ", "")}.pdf"
        )
        try {
            // after creating a file name we will
            // write our PDF file to that location.
            pdfDocument.writeTo(FileOutputStream(file))

            // on below line we are displaying a toast message as PDF file generated..
            Toast.makeText(params.ctx, "PDF file generated..", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            // below line is used
            // to handle error
            Log.e("generatePDF", e.message!!)

            // on below line we are displaying a toast message as fail to generate PDF
            Toast.makeText(params.ctx, "Fail to generate PDF file..", Toast.LENGTH_SHORT)
                .show()
        }
        // after storing our pdf to that
        // location we are closing our PDF file.
        pdfDocument.close()
    }
}

data class PrintWorkDaysParams(
    var projectsByMonth: List<Project>,
    var canvas: Canvas,
    var showTotals: Boolean,
    var startDate: Int,
    var endDate: Int,
    var totalSumLabel: String,
    var endOfMonthDate: String,
    var projectTitles: List<String>
)

data class DrawProjectDaysParams(
    var printWorkDaysParams: PrintWorkDaysParams,
    var left: Float,
    var right: Float,
    var x: Float,
    var measuredPaintTextRight: Float,
    var projectAttributes: MutableList<String>,
    var day: Int
)

data class CreatePageParams(
    var name: String,
    var employer: String,
    var projectsByMonth: List<Project>,
    var endOfMonthDate: String,
    var totalSumLabel: String,
    var monthlyReportLabel: String,
    var pdfDocument: PdfDocument,
    var title: Paint,
    var pageNumber: Int,
    var startDate: Int,
    var endDate: Int,
    var showTotals: Boolean,
    var projectTitles: List<String>
)

data class GeneratePdfParams(
    var ctx: Context,
    var projectsByMonth: List<Project>,
    var endOfMonthDate: String,
    var totalSumLabel: String,
    var monthlyReportLabel: String,
    var name: String,
    var employer: String,
    var projectTitles: List<String>
)
