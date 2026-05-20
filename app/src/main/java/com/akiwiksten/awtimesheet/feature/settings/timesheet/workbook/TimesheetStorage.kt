package com.akiwiksten.awtimesheet.feature.settings.timesheet.workbook

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object TimesheetStorage {
    fun saveXlsx(ctx: Context, workbook: ByteArray, name: String, date: String) {
        try {
            val formattedDate = LocalDate.parse(date)
            val yearMonth = DateTimeFormatter.ofPattern("MM_yyyy").format(formattedDate)
            val fileName = "${name.ifEmpty { "Timesheet" }}_$yearMonth.xlsx".replace(" ", "_")
            val appName = getApplicationName(ctx)
            val relativePath = Environment.DIRECTORY_DOWNLOADS + File.separator + appName
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = ctx.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(workbook)
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Log.d("TimesheetGeneratorEntry", "XLSX saved successfully to: $relativePath/$fileName")
                Toast.makeText(ctx, "XLSX saved to Downloads/$appName", Toast.LENGTH_LONG).show()
            } else {
                throw IOException("Failed to create MediaStore entry")
            }
        } catch (e: IOException) {
            Log.e("TimesheetGeneratorEntry", "IO error saving XLSX", e)
            Toast.makeText(ctx, "Failed to generate XLSX: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getApplicationName(context: Context): String {
        return context.applicationInfo.loadLabel(context.packageManager).toString()
    }
}
