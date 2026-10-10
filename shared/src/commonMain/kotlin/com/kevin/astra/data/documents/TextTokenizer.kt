package com.kevin.astra.data.documents

/**
 * Shared tokenizer for retrieval (TF-IDF, keyword and bag-of-words embeddings).
 *
 * Splitting on `[^a-z0-9]` broke every accented word ("sécurité" → "s" + "curit") and dropped
 * non-Latin scripts entirely. This splits on any non letter/digit (Unicode-aware), folds Latin
 * diacritics so "sécurité" and "securite" match, and removes English and French stop words.
 */
object TextTokenizer {
    private const val MIN_LENGTH = 3

    fun tokenize(text: String): List<String> =
        words(text).filter { it.length >= MIN_LENGTH && it !in STOP_WORDS }

    /** Lowercased, accent-folded words, before stop-word and length filtering. */
    fun words(text: String): List<String> {
        val out = ArrayList<String>()
        val current = StringBuilder()
        for (ch in text) {
            if (ch.isLetterOrDigit()) {
                current.append(fold(ch.lowercaseChar()))
            } else if (current.isNotEmpty()) {
                out += current.toString()
                current.clear()
            }
        }
        if (current.isNotEmpty()) out += current.toString()
        return out
    }

    private fun fold(ch: Char): String = when (ch) {
        'à', 'á', 'â', 'ã', 'ä', 'å' -> "a"
        'ç' -> "c"
        'è', 'é', 'ê', 'ë' -> "e"
        'ì', 'í', 'î', 'ï' -> "i"
        'ñ' -> "n"
        'ò', 'ó', 'ô', 'õ', 'ö' -> "o"
        'ù', 'ú', 'û', 'ü' -> "u"
        'ý', 'ÿ' -> "y"
        'œ' -> "oe"
        'æ' -> "ae"
        'ß' -> "ss"
        else -> ch.toString()
    }

    // Folded forms (no accents), length >= 3 only — shorter words are filtered anyway.
    private val STOP_WORDS = setOf(
        // English
        "the", "and", "for", "are", "but", "not", "you", "all", "can", "was", "this", "that",
        "with", "have", "from", "they", "will", "one", "been", "has", "its", "also", "what",
        "which", "who", "how", "when", "where", "why", "about", "into", "than", "then", "there",
        "their", "these", "those", "does", "did", "our", "your", "any", "some", "such", "only",
        // French
        "les", "des", "une", "est", "que", "qui", "pour", "par", "dans", "sur", "avec", "pas",
        "plus", "son", "ses", "aux", "ont", "sont", "etre", "avoir", "fait", "cette", "ces",
        "mais", "comme", "tout", "tous", "toute", "toutes", "leur", "leurs", "nous", "vous",
        "elle", "elles", "ils", "quoi", "quel", "quelle", "quels", "quelles", "dont", "car",
        "entre", "sans", "sous", "chez", "vers", "donc", "alors", "aussi", "tres", "peu",
        "etait", "sera", "peut", "doit", "comment", "pourquoi", "quand", "lequel", "laquelle",
        "moi", "toi", "lui", "notre", "votre", "nos", "vos", "mes", "tes", "meme", "ceci", "cela",
    )
}
