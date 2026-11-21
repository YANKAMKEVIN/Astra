package com.kevin.astra.domain.documents

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentQuestionRouterTest {

    private fun assertSummary(question: String) =
        assertTrue(DocumentQuestionRouter.isWholeDocumentQuestion(question), "should use the summary: $question")

    private fun assertRetrieval(question: String) =
        assertFalse(DocumentQuestionRouter.isWholeDocumentQuestion(question), "should search the document: $question")

    @Test
    fun ordinaryFrenchQuestionsAreNotSentToTheSummary() {
        assertRetrieval("Qu'est-ce que la clause de résiliation ?")
        assertRetrieval("C'est quoi le montant total de la facture ?")
        assertRetrieval("De quoi ai-je besoin pour m'inscrire ?")
        assertRetrieval("Quel est le sujet de la réunion de lundi ?")
        assertRetrieval("Au sujet du loyer, combien dois-je payer ?")
        assertRetrieval("Qu’est-ce que la photosynthèse ?")
        assertRetrieval("Qu'est-ce que ce document dit sur le délai de préavis ?")
        assertRetrieval("Quelle est la date indiquée dans le document ?")
        assertRetrieval("Que dit le document sur les pénalités ?")
    }

    @Test
    fun ordinaryEnglishQuestionsAreNotSentToTheSummary() {
        assertRetrieval("What is the notice period?")
        assertRetrieval("What does this document say about late fees?")
        assertRetrieval("Who signed the contract?")
    }

    @Test
    fun explicitSummaryRequestsUseTheSummary() {
        assertSummary("Résume ce document")
        assertSummary("Fais-moi un résumé")
        assertSummary("Peux-tu résumer ces mails ?")
        assertSummary("Une synthèse, s'il te plaît")
        assertSummary("Quels sont les points clés ?")
        assertSummary("Summarize")
        assertSummary("Give me an overview")
        assertSummary("What are the key points?")
    }

    @Test
    fun questionsAboutTheDocumentItselfUseTheSummary() {
        assertSummary("De quoi parle ce document ?")
        assertSummary("De quoi ça parle ?")
        assertSummary("Ça parle de quoi ?")
        assertSummary("C'est quoi ce PDF ?")
        assertSummary("Quel est le sujet de ce mail ?")
        assertSummary("Que contient ce fichier ?")
        assertSummary("Explique ce document")
        assertSummary("What is this document about?")
        assertSummary("What's this?")
        assertSummary("Describe this email")
    }

    @Test
    fun blankQuestionsAreNotSummaryRequests() {
        assertRetrieval("")
        assertRetrieval("  ?! ")
    }
}
