package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.DocumentContextRetriever
import com.kevin.astra.domain.documents.IndexedDocumentChunk
import com.kevin.astra.domain.documents.RetrievedDocumentContext
import kotlin.math.ln
import kotlin.math.sqrt

class TfIdfContextRetriever : DocumentContextRetriever {

    override fun retrieve(
        question: String,
        chunks: List<IndexedDocumentChunk>,
        maxChunks: Int,
    ): RetrievedDocumentContext {
        if (chunks.isEmpty()) return RetrievedDocumentContext(emptyList())

        val queryTerms = tokenize(question)
        if (queryTerms.isEmpty()) return RetrievedDocumentContext(chunks.take(maxChunks))

        val N = chunks.size.toDouble()
        val df = mutableMapOf<String, Int>()
        val chunkTokens = chunks.map { tokenize(it.content) }

        chunkTokens.forEach { tokens ->
            tokens.toSet().forEach { term -> df[term] = (df[term] ?: 0) + 1 }
        }

        val scores = chunks.mapIndexed { i, chunk ->
            val tokens = chunkTokens[i]
            val tf = termFrequencies(tokens)
            val score = queryTerms.sumOf { term ->
                val termTf = tf[term] ?: 0.0
                val idf = ln((N + 1.0) / ((df[term] ?: 0).toDouble() + 1.0))
                termTf * idf
            }
            chunk to score
        }

        val selected = scores
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(maxChunks)
            .ifEmpty { chunks.take(maxChunks) }

        return RetrievedDocumentContext(chunks = selected)
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in STOP_WORDS }

    private fun termFrequencies(tokens: List<String>): Map<String, Double> {
        if (tokens.isEmpty()) return emptyMap()
        val counts = mutableMapOf<String, Int>()
        tokens.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        val total = tokens.size.toDouble()
        return counts.mapValues { it.value / total }
    }

    private companion object {
        val STOP_WORDS = setOf(
            "the", "and", "for", "are", "but", "not", "you", "all",
            "can", "was", "this", "that", "with", "have", "from",
            "they", "will", "one", "been", "has", "its", "also",
        )
    }
}
