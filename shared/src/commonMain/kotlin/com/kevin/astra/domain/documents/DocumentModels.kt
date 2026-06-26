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
)

enum class DocumentStatus(val label: String) {
    NotIndexed("Not Indexed"),
    Indexed("Indexed"),
    Processing("Processing"),
}

data class RetrievedDocumentContext(
    val chunks: List<IndexedDocumentChunk>,
) {
    val text: String
        get() = chunks.joinToString(separator = "\n\n") { chunk ->
            "${chunk.title}\n${chunk.content}"
        }
}
