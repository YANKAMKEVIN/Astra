package com.kevin.astra.core.ai

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockInferenceEngineTest {
    @Test
    fun returnsOfflineGenerationResultWithMetrics() = runBlocking {
        val engine = MockInferenceEngine(timestampProvider = { "2026-06-26T10:15:30Z" })

        val result = engine.generate(
            PromptRequest(
                prompt = "Restart pump A",
                industry = PromptIndustry.IndustrialMaintenance,
            ),
        )

        assertTrue(result.text.contains("Emergency restart procedure"))
        assertEquals(AiModel.Mock, result.model)
        assertEquals(InferenceBackend.Mock, result.backend)
        assertEquals("2026-06-26T10:15:30Z", result.generatedAt)
        assertEquals(1_200, result.metrics.latencyMillis)
        assertEquals(18, result.metrics.tokensPerSecond)
    }

    @Test
    fun variesResponseByIndustry() = runBlocking {
        val engine = MockInferenceEngine(timestampProvider = { "timestamp" })

        val aerospace = engine.generate(
            PromptRequest(
                prompt = "Review checklist",
                industry = PromptIndustry.Aerospace,
            ),
        )
        val healthcare = engine.generate(
            PromptRequest(
                prompt = "Troubleshoot alarm",
                industry = PromptIndustry.Healthcare,
            ),
        )

        assertTrue(aerospace.text.contains("Cockpit checklist assistance"))
        assertTrue(healthcare.text.contains("Medical device troubleshooting"))
    }
}
