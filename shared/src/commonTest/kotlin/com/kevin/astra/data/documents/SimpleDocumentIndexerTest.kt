package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.LoadedPdfDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartTextChunkerTest {

    private val chunker = SmartTextChunker()

    @Test
    fun returnsEmptyListForBlankText() {
        val pdf = LoadedPdfDocument(fileName = "test.pdf", rawText = "   ", pageCount = 1)
        val chunks = chunker.indexPdf(pdf)
        assertTrue(chunks.isEmpty())
    }

    @Test
    fun producesAtLeastOneChunkForShortText() {
        val pdf = LoadedPdfDocument(
            fileName = "test.pdf",
            rawText = "This is a short document with just a few words.",
            pageCount = 1,
        )
        val chunks = chunker.indexPdf(pdf)
        assertEquals(1, chunks.size)
        assertEquals("test.pdf", chunks.first().documentId)
    }

    @Test
    fun splitsLongTextIntoMultipleChunks() {
        val words = (1..1_000).joinToString(" ") { "word$it" }
        val pdf = LoadedPdfDocument(fileName = "long.pdf", rawText = words, pageCount = 4)
        val chunks = chunker.indexPdf(pdf)
        assertTrue(chunks.size >= 2, "Expected multiple chunks but got ${chunks.size}")
    }

    @Test
    fun chunksHaveUniqueIds() {
        val words = (1..900).joinToString(" ") { "w$it" }
        val pdf = LoadedPdfDocument(fileName = "doc.pdf", rawText = words, pageCount = 3)
        val chunks = chunker.indexPdf(pdf)
        val ids = chunks.map { it.id }.toSet()
        assertEquals(chunks.size, ids.size)
    }

    @Test
    fun chunksOverlapSoNoBoundaryIsLost() {
        val words = (1..500).map { "word$it" }
        val pdf = LoadedPdfDocument(
            fileName = "overlap.pdf",
            rawText = words.joinToString(" "),
            pageCount = 2,
        )
        val chunks = chunker.indexPdf(pdf)
        assertTrue(chunks.size >= 2)
        val firstChunkLastWord = chunks[0].content.split(" ").last()
        val secondChunkContent = chunks[1].content
        assertTrue(
            secondChunkContent.contains(firstChunkLastWord),
            "Expected overlap between chunks 0 and 1",
        )
    }
}
