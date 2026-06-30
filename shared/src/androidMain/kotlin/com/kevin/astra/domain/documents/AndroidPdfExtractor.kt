package com.kevin.astra.domain.documents

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import android.content.Context

private var applicationContext: Context? = null

fun initializeAndroidPdfExtractor(context: Context) {
    applicationContext = context.applicationContext
    PDFBoxResourceLoader.init(context.applicationContext)
}

class AndroidPdfExtractor : PdfExtractor {
    override fun extract(pdfBytes: ByteArray, fileName: String): LoadedPdfDocument {
        val document = PDDocument.load(pdfBytes)
        return try {
            val stripper = PDFTextStripper()
            val pageCount = document.numberOfPages
            val rawText = stripper.getText(document).trim()
            LoadedPdfDocument(
                fileName = fileName,
                rawText = rawText,
                pageCount = pageCount,
            )
        } finally {
            document.close()
        }
    }
}

actual fun createPdfExtractor(): PdfExtractor = AndroidPdfExtractor()
