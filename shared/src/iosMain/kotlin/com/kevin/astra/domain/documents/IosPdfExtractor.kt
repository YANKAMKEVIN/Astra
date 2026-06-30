package com.kevin.astra.domain.documents

class IosPdfExtractor : PdfExtractor {
    override fun extract(pdfBytes: ByteArray, fileName: String): LoadedPdfDocument =
        LoadedPdfDocument(fileName = fileName, rawText = "", pageCount = 0)
}

actual fun createPdfExtractor(): PdfExtractor = IosPdfExtractor()
