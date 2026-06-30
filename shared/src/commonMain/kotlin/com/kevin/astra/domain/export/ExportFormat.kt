package com.kevin.astra.domain.export

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    Markdown("Markdown", "md", "text/markdown"),
    PlainText("Plain Text", "txt", "text/plain"),
    Pdf("PDF", "pdf", "application/pdf"),
}
