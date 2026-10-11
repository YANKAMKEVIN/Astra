package com.kevin.astra.core.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuntimePromptTest {
    private val system = "You are ASTRA.\n- If the answer is not in the document context, say so."
    private val question = "What is the torque for bolt B-12?"

    private fun request(context: String, maxTokens: Int = 512, window: Int = 4_096) = PromptRequest(
        prompt = "full",
        systemPrompt = system,
        userMessage = "$question\n\nDocument context:\n$context",
        maxTokens = maxTokens,
        contextWindow = window,
    )

    @Test
    fun systemPromptIsSentAheadOfTheUserMessage() {
        val input = RuntimePrompt.compose(request(context = "Bolt B-12 torque is 45 Nm."))

        assertTrue(input.startsWith(system), input)
        assertTrue(input.contains(question))
        assertTrue(input.contains("45 Nm"))
    }

    @Test
    fun budgetIsTheWindowMinusTheAnswerNotTheAnswerLength() {
        // Regression: maxTokens (512) used to be treated as the window → ~256 tokens of input.
        assertEquals(3_584, RuntimePrompt.inputBudget(contextWindow = 4_096, maxTokens = 512))
        // The answer reserve never takes more than half the window…
        assertEquals(2_048, RuntimePrompt.inputBudget(contextWindow = 4_096, maxTokens = 4_096))
        // …nor less than a minimal reply.
        assertEquals(1_920, RuntimePrompt.inputBudget(contextWindow = 2_048, maxTokens = 16))
    }

    @Test
    fun aTypicalRagPromptFitsWithoutAnyTrimming() {
        val fourChunks = List(4) { i -> "[Page ${i + 1}] " + "word ".repeat(400) }.joinToString("\n\n")
        val input = RuntimePrompt.compose(request(context = fourChunks))

        assertTrue(input.endsWith(fourChunks.trim()), "Context was trimmed although it fits")
    }

    @Test
    fun oversizedContextIsCutFromTheEndKeepingSystemAndQuestion() {
        val bestChunk = "BEST bolt B-12 torque is 45 Nm."
        val context = bestChunk + "\n\n" + "filler ".repeat(20_000)
        val input = RuntimePrompt.compose(request(context = context, window = 2_048))

        assertTrue(RuntimePrompt.estimateTokens(input) <= RuntimePrompt.inputBudget(2_048, 512))
        assertTrue(input.startsWith(system))
        assertTrue(input.contains(question))
        assertTrue(input.contains(bestChunk), "Most relevant (first) chunk must survive")
    }

    @Test
    fun usesTheRuntimeTokenizerWhenGiven() {
        // A tokenizer counting 1 token per character is 4x stricter than the estimate.
        val input = RuntimePrompt.compose(
            request(context = "x".repeat(10_000), window = 4_096),
            countTokens = { it.length },
        )
        assertTrue(input.length <= RuntimePrompt.inputBudget(4_096, 512))
        assertTrue(input.contains(question))
    }

    @Test
    fun withoutSystemPromptOnlyTheUserMessageIsSent() {
        val input = RuntimePrompt.compose(PromptRequest(prompt = "fallback prompt"))
        assertEquals("fallback prompt", input)
    }

    @Test
    fun fitKeepsSystemAndUserApartForChatTemplates() {
        val fitted = RuntimePrompt.fit(request(context = "Bolt B-12 torque is 45 Nm."))
        assertEquals(system, fitted.system)
        assertTrue(fitted.user.startsWith(question))
        assertEquals(RuntimePrompt.compose(request(context = "Bolt B-12 torque is 45 Nm.")), fitted.joined())
    }

    @Test
    fun kvCacheSizeIsReadFromLiteRtFileNames() {
        assertEquals(1_280, RuntimePrompt.contextWindowFromFileName("qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm"))
        assertEquals(4_096, RuntimePrompt.contextWindowFromFileName("Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm"))
        assertEquals(null, RuntimePrompt.contextWindowFromFileName("gemma-4-E2B-it.litertlm"))
    }

    @Test
    fun aSmallKvCacheStillKeepsTheQuestionAndBestChunk() {
        val bestChunk = "BEST bolt B-12 torque is 45 Nm."
        val context = bestChunk + "\n\n" + "remplissage ".repeat(2_000)
        val fitted = RuntimePrompt.fit(request(context = context, window = 1_280))

        assertTrue(RuntimePrompt.estimateTokens(fitted.joined()) <= RuntimePrompt.inputBudget(1_280, 512))
        assertTrue(fitted.user.contains(question) && fitted.user.contains(bestChunk))
    }
}
