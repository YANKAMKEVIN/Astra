package com.kevin.astra.domain.documents

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument

actual fun createPdfExtractor(): PdfExtractor = IosPdfExtractor()

@OptIn(ExperimentalForeignApi::class)
private class IosPdfExtractor : PdfExtractor {
    override fun extract(pdfBytes: ByteArray, fileName: String): LoadedPdfDocument {
        if (pdfBytes.isEmpty()) return LoadedPdfDocument(fileName = fileName, rawText = "", pageCount = 0)

        val nsData = pdfBytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = pdfBytes.size.toULong())
        }

        val document = PDFDocument(data = nsData)
            ?: return LoadedPdfDocument(fileName = fileName, rawText = "", pageCount = 0)

        val pageCount = document.pageCount.toInt()
        val sb = StringBuilder()

        for (i in 0 until pageCount) {
            val page = document.pageAtIndex(i.toULong()) ?: continue
            page.string?.let { sb.append(it).append('\n') }
        }

        return LoadedPdfDocument(
            fileName = fileName,
            rawText = sb.toString().trim(),
            pageCount = pageCount,
        )
    }
}
