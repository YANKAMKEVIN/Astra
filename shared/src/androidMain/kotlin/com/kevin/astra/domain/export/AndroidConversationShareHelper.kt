package com.kevin.astra.domain.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.kevin.astra.data.export.ConversationExportBuilder
import com.kevin.astra.domain.history.ChatConversation
import java.io.File

private lateinit var applicationContext: Context

fun initializeAndroidConversationShareHelper(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createConversationShareHelper(): ConversationShareHelper =
    if (::applicationContext.isInitialized) AndroidConversationShareHelper(applicationContext)
    else NoOpConversationShareHelper

private class AndroidConversationShareHelper(private val context: Context) : ConversationShareHelper {

    override fun share(conversation: ChatConversation, format: ExportFormat) {
        val exportDir = context.cacheDir.resolve("exports").also { it.mkdirs() }
        val safeTitle = conversation.title.take(40).replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
        val file: File
        when (format) {
            ExportFormat.Markdown -> {
                file = exportDir.resolve("$safeTitle.md")
                file.writeText(ConversationExportBuilder.toMarkdown(conversation))
            }
            ExportFormat.PlainText -> {
                file = exportDir.resolve("$safeTitle.txt")
                file.writeText(ConversationExportBuilder.toPlainText(conversation))
            }
            ExportFormat.Pdf -> {
                file = exportDir.resolve("$safeTitle.pdf")
                buildPdf(conversation, file)
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, conversation.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share conversation").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun buildPdf(conversation: ChatConversation, file: File) {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 48f
        val usableWidth = (pageWidth - margin * 2).toInt()

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; isAntiAlias = true }
        val metaPaint = Paint().apply { textSize = 11f; isAntiAlias = true; color = 0xFF666666.toInt() }
        val speakerPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; isAntiAlias = true }
        val bodyPaint = Paint().apply { textSize = 11f; isAntiAlias = true }

        var pageIndex = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
        var page = doc.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = margin + 24f

        fun newPageIfNeeded(lineHeight: Float): Boolean {
            if (y + lineHeight > pageHeight - margin) {
                doc.finishPage(page)
                pageIndex++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                page = doc.startPage(pageInfo)
                canvas = page.canvas
                y = margin + 16f
                return true
            }
            return false
        }

        fun drawWrappedText(text: String, paint: Paint, indent: Float = 0f): Float {
            var remaining = text
            var totalHeight = 0f
            val lineHeight = paint.textSize * 1.5f
            val lineWidth = usableWidth - indent
            while (remaining.isNotEmpty()) {
                newPageIfNeeded(lineHeight)
                val measured = FloatArray(1)
                val count = paint.breakText(remaining, true, lineWidth, measured)
                val line = remaining.take(count)
                canvas.drawText(line, margin + indent, y, paint)
                y += lineHeight
                totalHeight += lineHeight
                remaining = remaining.drop(count)
            }
            return totalHeight
        }

        // Title
        drawWrappedText(conversation.title, titlePaint)
        y += 4f

        // Meta
        for (line in listOf(
            "Model: ${conversation.modelName}",
            "Backend: ${conversation.backendName}",
            "Industry: ${conversation.industry}",
            "Date: ${conversation.createdAt}",
        )) {
            newPageIfNeeded(metaPaint.textSize * 1.5f)
            canvas.drawText(line, margin, y, metaPaint)
            y += metaPaint.textSize * 1.5f
        }
        y += 12f

        // Messages
        conversation.messages.forEach { message ->
            val speaker = if (message.role == "user") "You" else "ASTRA"
            newPageIfNeeded(speakerPaint.textSize * 1.6f)
            canvas.drawText(speaker, margin, y, speakerPaint)
            y += speakerPaint.textSize * 1.6f

            message.content.lines().forEach { line ->
                drawWrappedText(line.ifEmpty { " " }, bodyPaint)
            }
            y += 12f
        }

        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }
}

private object NoOpConversationShareHelper : ConversationShareHelper {
    override fun share(conversation: ChatConversation, format: ExportFormat) = Unit
}
