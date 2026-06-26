package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.DocumentContextRetriever
import com.kevin.astra.domain.documents.IndexedDocumentChunk
import com.kevin.astra.domain.documents.RetrievedDocumentContext

class KeywordDocumentContextRetriever : DocumentContextRetriever {
    override fun retrieve(
        question: String,
        chunks: List<IndexedDocumentChunk>,
        maxChunks: Int,
    ): RetrievedDocumentContext {
        val keywords = question
            .lowercase()
            .split(Regex("[^a-z0-9.]+"))
            .filter { it.length >= 3 }
            .toSet()

        val ranked = chunks
            .map { chunk -> chunk to chunk.scoreAgainst(keywords) }
            .sortedWith(
                compareByDescending<Pair<IndexedDocumentChunk, Int>> { it.second }
                    .thenBy { it.first.id },
            )

        val selected = ranked
            .filter { it.second > 0 }
            .map { it.first }
            .take(maxChunks)
            .ifEmpty { chunks.take(maxChunks) }

        return RetrievedDocumentContext(chunks = selected)
    }

    private fun IndexedDocumentChunk.scoreAgainst(keywords: Set<String>): Int {
        val titleHaystack = title.lowercase()
        val contentHaystack = content.lowercase()
        return keywords.sumOf { keyword ->
            val titleScore = if (titleHaystack.contains(keyword)) 3 else 0
            val contentScore = if (contentHaystack.contains(keyword)) 1 else 0
            titleScore + contentScore
        }
    }
}
