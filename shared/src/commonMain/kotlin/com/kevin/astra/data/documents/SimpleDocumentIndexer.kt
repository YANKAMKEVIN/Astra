package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.AstraDocument
import com.kevin.astra.domain.documents.DocumentIndexer
import com.kevin.astra.domain.documents.IndexedDocumentChunk
import kotlinx.coroutines.delay

class SimpleDocumentIndexer : DocumentIndexer {
    override suspend fun index(document: AstraDocument): List<IndexedDocumentChunk> {
        delay(650)

        return document.sections.mapIndexed { index, section ->
            IndexedDocumentChunk(
                id = "${document.id}-chunk-${index + 1}",
                documentId = document.id,
                title = section.title,
                content = section.body,
            )
        }
    }
}
