package com.kevin.astra.data.documents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TfIdfContextRetrieverTest {

    private val retriever = TfIdfContextRetriever()

    private fun makeChunk(id: String, content: String) =
        com.kevin.astra.domain.documents.IndexedDocumentChunk(
            id = id,
            documentId = "doc",
            title = id,
            content = content,
            pageHint = 1,
        )

    @Test
    fun returnsEmptyContextForEmptyChunkList() {
        val result = retriever.retrieve("anything", emptyList(), maxChunks = 3)
        assertTrue(result.chunks.isEmpty())
    }

    @Test
    fun ranksRelevantChunkFirst() {
        val chunks = listOf(
            makeChunk("irrelevant", "The weather is sunny and warm today."),
            makeChunk("relevant", "Pump restart procedure: open valve, prime system, start motor."),
            makeChunk("noise", "Annual maintenance schedule for cooling towers."),
        )
        val result = retriever.retrieve(
            question = "How do I restart the pump?",
            chunks = chunks,
            maxChunks = 1,
        )
        assertEquals("relevant", result.chunks.first().id)
    }

    @Test
    fun respectsMaxChunksLimit() {
        val chunks = (1..10).map { makeChunk("chunk-$it", "keyword repeated $it times keyword keyword") }
        val result = retriever.retrieve("keyword", chunks, maxChunks = 3)
        assertTrue(result.chunks.size <= 3)
    }

    @Test
    fun fallsBackToHeadChunksWhenNoQueryTermsMatch() {
        val chunks = listOf(
            makeChunk("a", "xyzqrp abcdef ghijkl"),
            makeChunk("b", "mnopqr stuvwx yzabcd"),
        )
        val result = retriever.retrieve("zzz", chunks, maxChunks = 2)
        assertEquals(2, result.chunks.size)
    }
}
