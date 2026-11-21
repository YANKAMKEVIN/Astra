package com.kevin.astra.domain.documents

/**
 * Decides whether a question asks about the document as a whole ("Résume ce document", "De quoi
 * parle ce mail ?"), in which case the pre-generated summary answers it, or about something in it,
 * which needs retrieval and generation.
 *
 * The previous check matched loose substrings ("qu'est-ce que", "c'est quoi", "de quoi", "sujet"),
 * so ordinary French questions such as "Qu'est-ce que la clause de résiliation ?" or "C'est quoi
 * le montant total ?" were answered with the summary. Now a question is a whole-document question
 * only when it asks for a summary, is a bare deictic question ("De quoi ça parle ?"), or ends on a
 * reference to the document itself ("C'est quoi ce PDF ?", "What is this email about?").
 */
object DocumentQuestionRouter {

    fun isWholeDocumentQuestion(question: String): Boolean {
        val words = normalize(question)
        if (words.isEmpty()) return false
        val text = words.joinToString(" ")
        if (words.any(::isSummaryWord) || SUMMARY_PHRASES.any { " $text ".contains(" $it ") }) return true
        if (text in BARE_WHOLE_DOCUMENT_QUESTIONS) return true

        // "… ce document" / "… this email (about)": the question is about the document itself
        // only when the document reference closes it, otherwise it narrows to a detail
        // ("Qu'est-ce que ce document dit sur le délai ?").
        val withoutTrailingAbout = text.removeSuffix(" about")
        val reference = DOCUMENT_REFERENCES.firstOrNull { withoutTrailingAbout.endsWith(" $it") } ?: return false
        val rest = withoutTrailingAbout.removeSuffix(" $reference").split(' ')
        return rest.any { it in ASKING_WORDS }
    }

    // Lowercase words; apostrophes and hyphens split ("qu'est-ce" → qu est ce).
    private fun normalize(question: String): List<String> =
        question.lowercase()
            .split(Regex("[^\\p{L}0-9]+"))
            .filter { it.isNotBlank() }

    private fun isSummaryWord(word: String): Boolean =
        word.startsWith("résum") || word.startsWith("resum") ||
            word.startsWith("summar") || word.startsWith("synth") ||
            word.startsWith("récapitul") || word.startsWith("recapitul") ||
            word in setOf("overview", "tldr", "récap", "recap")

    private val SUMMARY_PHRASES = listOf(
        "points clés", "points cles", "points principaux", "idées principales", "idees principales",
        "idée principale", "idee principale", "en bref", "key points", "main points", "main ideas",
        "in short", "tl dr",
    )

    private val BARE_WHOLE_DOCUMENT_QUESTIONS = setOf(
        "de quoi ça parle", "de quoi ca parle", "ça parle de quoi", "ca parle de quoi",
        "de quoi il parle", "de quoi elle parle", "de quoi parle t il", "de quoi parle t elle",
        "c est quoi", "qu est ce que c est", "quel est le sujet", "c est quoi le sujet",
        "what is this", "what s this", "what is this about", "what s this about",
        "what is it about", "what s it about",
    )

    private val DOCUMENT_REFERENCES = listOf(
        "ce document", "le document", "ce doc", "ce fichier", "le fichier", "ce pdf", "le pdf",
        "ce texte", "ce mail", "cet email", "cet e mail", "ce courriel", "ce message",
        "ces mails", "ces emails", "ces e mails", "ces messages", "ces courriels",
        "this document", "the document", "this doc", "this file", "the file", "this pdf", "the pdf",
        "this text", "this email", "this e mail", "this mail", "this message", "these emails",
        "these messages",
    )

    private val ASKING_WORDS = setOf(
        "quoi", "sujet", "parle", "contient", "dit", "explique", "décris", "decris", "présente",
        "presente", "what", "explain", "describe", "contains", "contain", "say", "says", "about",
    )
}
