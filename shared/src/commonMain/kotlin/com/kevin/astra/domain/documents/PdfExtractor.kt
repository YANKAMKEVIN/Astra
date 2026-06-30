package com.kevin.astra.domain.documents

interface PdfExtractor {
    fun extract(pdfBytes: ByteArray, fileName: String): LoadedPdfDocument
}

expect fun createPdfExtractor(): PdfExtractor
