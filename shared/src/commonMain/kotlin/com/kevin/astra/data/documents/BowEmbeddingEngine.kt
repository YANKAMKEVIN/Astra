package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.EmbeddingEngine
import kotlin.math.sqrt

/**
 * Bag-of-words embedding with L2 normalization — zero dependencies.
 * The vocabulary is built lazily from the first [MAX_VOCAB] unique tokens seen.
 * Used as a universal fallback when no TFLite model is available.
 */
class BowEmbeddingEngine : EmbeddingEngine {
    override val isNeural = false

    private val vocab = mutableMapOf<String, Int>()

    override fun embed(text: String): FloatArray {
        val tokens = tokenize(text)
        tokens.forEach { token ->
            if (token !in vocab && vocab.size < MAX_VOCAB) {
                vocab[token] = vocab.size
            }
        }
        val dim = vocab.size.coerceAtLeast(1)
        val vec = FloatArray(dim)
        tokens.forEach { token -> vocab[token]?.let { idx -> vec[idx] += 1f } }
        return l2Normalize(vec)
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }

    private fun l2Normalize(vec: FloatArray): FloatArray {
        val norm = sqrt(vec.fold(0f) { acc, v -> acc + v * v })
        if (norm == 0f) return vec
        return FloatArray(vec.size) { vec[it] / norm }
    }

    private companion object {
        const val MAX_VOCAB = 8_192
    }
}
