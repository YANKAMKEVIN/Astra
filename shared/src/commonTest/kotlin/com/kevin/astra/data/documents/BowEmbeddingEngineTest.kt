package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.cosineSimilarity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class BowEmbeddingEngineTest {

    @Test
    fun isNeuralIsFalse() {
        assertEquals(false, BowEmbeddingEngine().isNeural)
    }

    @Test
    fun embedReturnsNonEmptyVector() {
        val engine = BowEmbeddingEngine()
        val vec = engine.embed("hello world test")
        assertTrue(vec.isNotEmpty())
    }

    @Test
    fun embeddingIsL2Normalized() {
        val engine = BowEmbeddingEngine()
        val vec = engine.embed("normalize this vector please")
        val norm = vec.fold(0f) { acc, v -> acc + v * v }
        assertTrue(abs(norm - 1f) < 0.001f, "Expected unit vector but norm=$norm")
    }

    @Test
    fun identicalTextsProduceSimilarEmbeddings() {
        val engine = BowEmbeddingEngine()
        val a = engine.embed("pump restart procedure valve")
        val b = engine.embed("pump restart procedure valve")
        val sim = cosineSimilarity(a, b)
        assertTrue(sim > 0.99f, "Expected near-identical similarity but got $sim")
    }

    @Test
    fun dissimilarTextsHaveLowerSimilarity() {
        val engine = BowEmbeddingEngine()
        val a = engine.embed("industrial pump maintenance hydraulic valve pressure")
        engine.embed("industrial pump maintenance hydraulic valve pressure") // warm vocab
        val b = engine.embed("quarterly financial earnings revenue profit margin")
        val sim = cosineSimilarity(a, b)
        assertTrue(sim < 0.5f, "Expected low similarity for unrelated texts but got $sim")
    }
}
