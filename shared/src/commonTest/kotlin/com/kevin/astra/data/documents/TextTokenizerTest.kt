package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.IndexedDocumentChunk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextTokenizerTest {
    @Test
    fun accentedWordsStayWholeAndAreFolded() {
        assertEquals(listOf("securite", "electrique"), TextTokenizer.tokenize("Sécurité électrique"))
        assertEquals(listOf("coeur", "procedure"), TextTokenizer.tokenize("cœur, procédure."))
    }

    @Test
    fun frenchAndEnglishStopWordsAreDropped() {
        assertEquals(
            listOf("pression", "maximale", "pompe"),
            TextTokenizer.tokenize("Quelle est la pression maximale pour la pompe ?"),
        )
        assertEquals(listOf("pump", "pressure"), TextTokenizer.tokenize("What is the pump pressure?"))
    }

    @Test
    fun nonLatinScriptsProduceTokens() {
        assertEquals(listOf("давление"), TextTokenizer.tokenize("давление"))
    }

    @Test
    fun tfIdfFindsTheFrenchPassageWithOrWithoutAccents() {
        val chunks = listOf(
            chunk("intro", "Présentation générale de l'installation et de son historique."),
            chunk("safety", "Consignes de sécurité : couper l'alimentation électrique avant toute intervention."),
            chunk("maint", "Planning de maintenance trimestriel des pompes hydrauliques."),
        )
        val retriever = TfIdfContextRetriever()

        listOf("Quelles sont les consignes de sécurité ?", "consignes de securite").forEach { question ->
            val top = retriever.retrieve(question, chunks, maxChunks = 1).chunks.single()
            assertEquals("safety", top.id, "Question: $question")
        }
    }

    @Test
    fun hybridRetrieverRanksTheFrenchMatchFirst() {
        val chunks = List(8) { chunk("filler-$it", "Rapport annuel numéro $it, sans rapport avec la question.") } +
            chunk("answer", "La pression maximale de la pompe P-3 est de 12 bars.")
        val result = HybridContextRetriever(BowEmbeddingEngine())
            .retrieve("Quelle est la pression maximale de la pompe ?", chunks, maxChunks = 2)

        assertEquals("answer", result.chunks.first().id)
        assertTrue(result.chunks.size <= 2)
    }

    private fun chunk(id: String, content: String) = IndexedDocumentChunk(
        id = id,
        documentId = "doc",
        title = id,
        content = content,
        pageHint = 1,
    )
}
