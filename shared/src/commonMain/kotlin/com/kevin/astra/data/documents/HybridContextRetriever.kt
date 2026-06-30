package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.DocumentContextRetriever
import com.kevin.astra.domain.documents.EmbeddingEngine
import com.kevin.astra.domain.documents.IndexedDocumentChunk
import com.kevin.astra.domain.documents.RetrievedDocumentContext
import com.kevin.astra.domain.documents.cosineSimilarity

/**
 * Two-stage retriever:
 *   1. TF-IDF narrows the corpus to [candidateMultiplier × maxChunks] chunks.
 *   2. Embedding cosine similarity re-ranks and returns the top [maxChunks].
 *
 * When [embeddingEngine.isNeural] is false (BOW fallback), the cosine step
 * still runs but degrades gracefully to a bag-of-words similarity — still
 * better than pure TF-IDF when the vocabulary overlap is low.
 */
class HybridContextRetriever(
    private val embeddingEngine: EmbeddingEngine,
    private val candidateMultiplier: Int = 4,
) : DocumentContextRetriever {

    private val tfidf = TfIdfContextRetriever()

    override fun retrieve(
        question: String,
        chunks: List<IndexedDocumentChunk>,
        maxChunks: Int,
    ): RetrievedDocumentContext {
        if (chunks.isEmpty()) return RetrievedDocumentContext(emptyList())

        // Stage 1 — TF-IDF candidates (broader pool)
        val candidates = tfidf.retrieve(
            question = question,
            chunks = chunks,
            maxChunks = maxChunks * candidateMultiplier,
        ).chunks

        if (candidates.size <= maxChunks) return RetrievedDocumentContext(candidates)

        // Stage 2 — embedding re-rank
        val queryVec = embeddingEngine.embed(question)
        val ranked = candidates
            .map { chunk ->
                val chunkVec = embeddingEngine.embed(chunk.content)
                chunk to cosineSimilarity(queryVec, chunkVec)
            }
            .sortedByDescending { it.second }
            .take(maxChunks)
            .map { it.first }

        return RetrievedDocumentContext(chunks = ranked)
    }
}
