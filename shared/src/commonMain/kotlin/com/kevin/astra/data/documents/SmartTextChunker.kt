package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.AstraDocument
import com.kevin.astra.domain.documents.DocumentIndexer
import com.kevin.astra.domain.documents.IndexedDocumentChunk
import com.kevin.astra.domain.documents.LoadedPdfDocument

private const val CHUNK_SIZE_WORDS = 400
private const val OVERLAP_WORDS = 80

class SmartTextChunker : DocumentIndexer {
    override suspend fun index(document: AstraDocument): List<IndexedDocumentChunk> =
        chunkText(
            text = document.content,
            documentId = document.id,
        )

    fun indexPdf(pdf: LoadedPdfDocument): List<IndexedDocumentChunk> =
        chunkText(
            text = pdf.rawText,
            documentId = pdf.fileName,
        )

    private fun chunkText(text: String, documentId: String): List<IndexedDocumentChunk> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val chunks = mutableListOf<IndexedDocumentChunk>()
        var start = 0
        var chunkIndex = 0

        while (start < words.size) {
            val end = minOf(start + CHUNK_SIZE_WORDS, words.size)
            val chunkWords = words.subList(start, end)
            val content = chunkWords.joinToString(" ")

            chunks += IndexedDocumentChunk(
                id = "$documentId-chunk-${chunkIndex + 1}",
                documentId = documentId,
                title = "Chunk ${chunkIndex + 1}",
                content = content,
                pageHint = estimatePage(start, words.size),
            )

            chunkIndex++
            start += CHUNK_SIZE_WORDS - OVERLAP_WORDS
        }

        return chunks
    }

    private fun estimatePage(wordOffset: Int, totalWords: Int): Int {
        if (totalWords == 0) return 0
        val wordsPerPage = 350
        return (wordOffset / wordsPerPage) + 1
    }
}
