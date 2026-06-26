package com.kevin.astra.domain.documents

interface DocumentIndexer {
    suspend fun index(document: AstraDocument): List<IndexedDocumentChunk>
}
