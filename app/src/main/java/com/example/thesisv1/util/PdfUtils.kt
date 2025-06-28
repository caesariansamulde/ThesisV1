package com.example.thesisv1.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.Q)
fun generatePDF(
    context: Context,
    fileName: String,
    content: String,
    photoPaths: List<String?> = emptyList()
) {
    try {
        val A4_WIDTH = 2480  // A4 width at 300 DPI
        val A4_HEIGHT = 3508 // A4 height at 300 DPI
        val pagePadding = 100f

        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply {
            textSize = 48f
            color = Color.BLACK
        }

        var y = pagePadding
        val maxTextWidth = A4_WIDTH - 2 * pagePadding
        val maxHeight = A4_HEIGHT - pagePadding

        // Draw text lines
        content.split("\n").forEach { line ->
            if (y + 60 > maxHeight) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = pagePadding
            }
            canvas.drawText(line, pagePadding, y, paint)
            y += 60f
        }

        // Draw scaled images
        for (path in photoPaths.filterNotNull()) {
            val file = File(path)
            if (!file.exists()) continue
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue

            val maxWidth = A4_WIDTH - 2 * pagePadding
            val scale = maxWidth / bitmap.width.toFloat()
            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale

            if (y + scaledHeight > A4_HEIGHT - pagePadding) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = pagePadding
            }

            val resized = Bitmap.createScaledBitmap(bitmap, scaledWidth.toInt(), scaledHeight.toInt(), true)
            canvas.drawBitmap(resized, pagePadding, y, null)
            y += scaledHeight + 60f
        }

        document.finishPage(page)

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$fileName.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
                Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        }

        document.close()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "PDF error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
