package com.kevin.astra.domain.documents

/**
 * Produces a normalized float vector for a text snippet.
 * The implementation can be bag-of-words (always available) or
 * TFLite MiniLM-L6 (Android, when model file is present).
 */
interface EmbeddingEngine {
    /** Returns true if a real neural embedding model is loaded. */
    val isNeural: Boolean

    /**
     * Embed [text] into a fixed-length L2-normalized vector.
     * Guaranteed to return a non-empty array; dimension is implementation-defined.
     */
    fun embed(text: String): FloatArray
}

expect fun createEmbeddingEngine(): EmbeddingEngine

/** Cosine similarity in [−1, 1]. Both vectors must be L2-normalized. */
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    var dot = 0f
    val len = minOf(a.size, b.size)
    for (i in 0 until len) dot += a[i] * b[i]
    return dot
}
