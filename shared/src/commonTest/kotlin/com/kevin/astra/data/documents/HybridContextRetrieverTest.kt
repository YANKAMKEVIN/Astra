package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.IndexedDocumentChunk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HybridContextRetrieverTest {

    private val engine = BowEmbeddingEngine()
    private val retriever = HybridContextRetriever(embeddingEngine = engine)

    private fun chunk(id: String, content: String) = IndexedDocumentChunk(
        id = id,
        documentId = "doc",
        title = id,
        content = content,
        pageHint = 1,
    )

    @Test
    fun returnsEmptyForEmptyChunkList() {
        val result = retriever.retrieve("anything", emptyList(), maxChunks = 3)
        assertTrue(result.chunks.isEmpty())
    }

    @Test
    fun returnsUpToMaxChunks() {
        val chunks = (1..20).map { chunk("c$it", "keyword maintenance pump valve procedure step $it") }
        val result = retriever.retrieve("pump maintenance valve", chunks, maxChunks = 3)
        assertTrue(result.chunks.size <= 3)
    }

    @Test
    fun prioritizesSemanticallySimilarChunk() {
        val chunks = listOf(
            chunk("weather", "Sunny day temperatures forecast humidity precipitation wind"),
            chunk("pump", "Pump shutdown restart procedure hydraulic valve pressure relief"),
            chunk("finance", "Quarterly earnings revenue profit margin shareholder dividend"),
        )
        val result = retriever.retrieve(
            question = "How do I restart the pump after shutdown?",
            chunks = chunks,
            maxChunks = 1,
        )
        assertEquals("pump", result.chunks.first().id)
    }

    @Test
    fun worksWhenCandidatesFewerThanMaxChunks() {
        val chunks = listOf(
            chunk("only", "maintenance pump system"),
        )
        val result = retriever.retrieve("pump", chunks, maxChunks = 5)
        assertEquals(1, result.chunks.size)
    }
}
