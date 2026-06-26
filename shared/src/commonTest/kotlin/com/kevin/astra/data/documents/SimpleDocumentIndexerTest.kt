package com.kevin.astra.data.documents

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleDocumentIndexerTest {
    @Test
    fun indexesEmbeddedDocumentIntoSectionChunks() = runBlocking {
        val document = EmbeddedMaintenanceDocument.industrialPumpMaintenanceGuide
        val indexer = SimpleDocumentIndexer()

        val chunks = indexer.index(document)

        assertEquals(document.sections.size, chunks.size)
        assertTrue(chunks.any { it.title == "Pump Restart Procedure" })
        assertTrue(chunks.all { it.documentId == document.id })
    }
}
