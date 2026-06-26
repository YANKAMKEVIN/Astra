package com.kevin.astra.domain.documents

interface DocumentContextRetriever {
    fun retrieve(
        question: String,
        chunks: List<IndexedDocumentChunk>,
        maxChunks: Int = 3,
    ): RetrievedDocumentContext
}
