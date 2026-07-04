package com.kevin.astra.domain.documents

interface DocumentIndexer {
    suspend fun index(document: AstraDocument): List<IndexedDocumentChunk>

    /** Chunks arbitrary plain text (email, Gmail, …) under the given source id. */
    fun indexText(text: String, sourceId: String): List<IndexedDocumentChunk>
}
