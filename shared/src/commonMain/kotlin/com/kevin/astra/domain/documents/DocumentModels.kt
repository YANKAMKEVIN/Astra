package com.kevin.astra.domain.documents

data class AstraDocument(
    val id: String,
    val title: String,
    val sections: List<DocumentSection>,
) {
    val content: String
        get() = sections.joinToString(separator = "\n\n") { section ->
            "${section.title}\n${section.body}"
        }

    val estimatedSizeKb: Int
        get() = (content.encodeToByteArray().size / 1_024).coerceAtLeast(1)
}

data class DocumentSection(
    val title: String,
    val body: String,
)

data class IndexedDocumentChunk(
    val id: String,
    val documentId: String,
    val title: String,
    val content: String,
    val pageHint: Int = 0,
)

enum class DocumentStatus(val label: String) {
    NotIndexed("Not Indexed"),
    Indexed("Indexed"),
    Processing("Processing"),
    Error("Error"),
}

data class RetrievedDocumentContext(
    val chunks: List<IndexedDocumentChunk>,
) {
    val text: String
        get() = chunks.joinToString(separator = "\n\n") { chunk ->
            if (chunk.pageHint > 0) "[Page ${chunk.pageHint}] ${chunk.content}"
            else chunk.content
        }
}

data class LoadedPdfDocument(
    val fileName: String,
    val rawText: String,
    val pageCount: Int,
)
