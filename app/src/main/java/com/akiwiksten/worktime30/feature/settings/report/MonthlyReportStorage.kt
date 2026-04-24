package com.akiwiksten.worktime30.feature.settings.report

import android.content.ContentValues
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object MonthlyReportStorage {
    fun savePdf(ctx: Context, doc: PdfDocument, name: String, date: String) {
        try {
            val formattedDate = LocalDate.parse(date)
            val yearMonth = DateTimeFormatter.ofPattern("MM_yyyy").format(formattedDate)
            val fileName = "${name.ifEmpty { "Work_Time_Report" }}_$yearMonth.pdf".replace(" ", "_")
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
}
