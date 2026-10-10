package com.kevin.astra.core.ai

/**
 * Builds the exact text handed to an on-device runtime and makes it fit the model's context.
 *
 * Runtimes take a single string, so the system prompt (persona + grounding rules such as
 * "say when the document doesn't contain the answer") is prepended to the user message —
 * dropping it silently let models answer from thin air. The budget is the context window
 * minus the tokens reserved for the answer; when the prompt is too long, the tail of the
 * user message is cut. That tail is the retrieved document context, ordered most-relevant
 * first, so the question and the best passages survive.
 */
object RuntimePrompt {
    /** Never let the answer reserve eat more than half the window. */
    private const val MIN_OUTPUT_RESERVE = 128

    fun inputBudget(contextWindow: Int, maxTokens: Int): Int {
        val window = contextWindow.takeIf { it > 0 } ?: 4_096
        val reserve = maxTokens.coerceIn(MIN_OUTPUT_RESERVE, window / 2)
        return window - reserve
    }

    /** System prompt and user message, kept apart for runtimes with a chat template. */
    data class Fitted(val system: String, val user: String) {
        /** Single-string form for runtimes that take one prompt. */
        fun joined(): String = if (system.isEmpty()) user else "$system\n\n$user"
    }

    /**
     * @param countTokens the runtime's tokenizer when it exposes one; [estimateTokens] otherwise.
     */
    fun compose(
        request: PromptRequest,
        contextWindow: Int = request.contextWindow,
        countTokens: (String) -> Int = ::estimateTokens,
    ): String = fit(request, contextWindow, countTokens).joined()

    /** Like [compose], but returns the parts so a runtime can apply its own chat template. */
    fun fit(
        request: PromptRequest,
        contextWindow: Int = request.contextWindow,
        countTokens: (String) -> Int = ::estimateTokens,
    ): Fitted {
        val system = request.systemPrompt.trim()
        val user = request.userMessage.ifBlank { request.prompt }.trim()
        val budget = inputBudget(contextWindow, request.maxTokens)

        var current = user
        repeat(MAX_PASSES) {
            val tokens = countTokens(Fitted(system, current).joined())
            if (tokens <= budget) return Fitted(system, current)
            // Scale the user part down by the overshoot (+10% margin) and re-measure.
            val systemTokens = if (system.isEmpty()) 0 else countTokens(system) + 2
            val userBudget = (budget - systemTokens).coerceAtLeast(budget / 4)
            val userTokens = (tokens - systemTokens).coerceAtLeast(1)
            val keep = (current.length.toLong() * userBudget * 9 / (userTokens.toLong() * 10))
                .toInt()
                .coerceIn(0, current.length)
            current = current.take(keep).trimEnd()
        }
        return Fitted(system, current)
    }

    /**
     * LiteRT exports encode their KV-cache size in the file name ("…_ekv1280.litertlm").
     * That is the hard limit the engine accepts, often far below the model family's window.
     */
    fun contextWindowFromFileName(fileName: String): Int? =
        Regex("""ekv(\d{3,6})""").find(fileName)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * ~3 characters per token: deliberately pessimistic. BPE tokenizers (Qwen, Llama) cut French
     * and accented text finer than English, and overflowing the KV cache fails the whole call.
     */
    fun estimateTokens(text: String): Int = (text.length + 2) / 3

    private const val MAX_PASSES = 4
}
