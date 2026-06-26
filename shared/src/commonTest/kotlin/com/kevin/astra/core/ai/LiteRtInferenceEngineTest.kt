package com.kevin.astra.core.ai

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiteRtInferenceEngineTest {
    @Test
    fun delegatesNonLiteRtRequestsToFallback() = runBlocking {
        val engine = LiteRtInferenceEngine(
            modelLoader = UnavailableLocalModelLoader("not used"),
            runtimeSession = UnavailableEdgeRuntimeSession("not used"),
            fallbackEngine = MockInferenceEngine(timestampProvider = { "timestamp" }),
        )

        val result = engine.generate(testRequest(backend = InferenceBackend.Mock))

        assertEquals(InferenceBackend.Mock, result.backend)
        assertEquals(RuntimeMode.Simulated, result.runtimeInfo.mode)
        assertTrue(result.text.contains("Emergency restart procedure"))
    }

    @Test
    fun fallsBackWhenLocalModelIsUnavailable() = runBlocking {
        val engine = LiteRtInferenceEngine(
            modelLoader = UnavailableLocalModelLoader("model missing"),
            runtimeSession = UnavailableEdgeRuntimeSession("not used"),
            fallbackEngine = MockInferenceEngine(timestampProvider = { "timestamp" }),
        )

        val result = engine.generate(testRequest(backend = InferenceBackend.LiteRt))

        assertEquals(InferenceBackend.Mock, result.backend)
        assertEquals(RuntimeMode.Fallback, result.runtimeInfo.mode)
        assertEquals("model missing", result.runtimeInfo.fallbackReason)
        assertTrue(result.text.contains("ASTRA fallback active"))
        assertTrue(result.text.contains("model missing"))
    }

    @Test
    fun usesRuntimeSessionWhenModelAndRuntimeAreReady() = runBlocking {
        val engine = LiteRtInferenceEngine(
            modelLoader = object : LocalModelLoader {
                override suspend fun loadModel(request: PromptRequest): LocalModelLoadResult =
                    LocalModelLoadResult.Loaded(
                        LocalModelAsset(
                            id = "test-model",
                            displayName = "Test Model",
                            path = "models/test.tflite",
                            bytes = byteArrayOf(1, 2, 3),
                        ),
                    )
            },
            runtimeSession = EchoEdgeRuntimeSession(timestampProvider = { "timestamp" }),
            fallbackEngine = MockInferenceEngine(timestampProvider = { "fallback" }),
        )

        val result = engine.generate(testRequest(backend = InferenceBackend.LiteRt))

        assertEquals(InferenceBackend.LiteRt, result.backend)
        assertEquals(RuntimeMode.Real, result.runtimeInfo.mode)
        assertTrue(result.text.contains("LiteRT local inference initialized"))
        assertTrue(result.text.contains("models/test.tflite"))
    }

    private fun testRequest(backend: InferenceBackend): PromptRequest =
        PromptRequest(
            prompt = "Restart Pump A safely.",
            industry = PromptIndustry.IndustrialMaintenance,
            model = AiModel.Mock,
            backend = backend,
            maxTokens = 128,
            temperature = 0.2,
        )
}
