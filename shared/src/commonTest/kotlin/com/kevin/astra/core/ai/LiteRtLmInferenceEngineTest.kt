package com.kevin.astra.core.ai

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiteRtLmInferenceEngineTest {
    @Test
    fun reportsModelMissingAndFallsBackSafely() = runBlocking {
        val engine = LiteRtLmInferenceEngine(
            modelLoader = object : LiteRtLmModelLoader {
                override suspend fun loadModel(request: PromptRequest): LiteRtLmModelLoadResult =
                    LiteRtLmModelLoadResult.Missing("bundle missing")
            },
            fallbackEngine = MockInferenceEngine(timestampProvider = { "timestamp" }),
        )

        val result = engine.generate(testRequest())

        assertEquals(InferenceBackend.Mock, result.backend)
        assertEquals(RuntimeMode.ModelMissing, result.runtimeInfo.mode)
        assertEquals("bundle missing", result.runtimeInfo.fallbackReason)
        assertTrue(result.text.contains("LiteRT-LM generative runtime not active"))
    }

    @Test
    fun reportsUnsupportedPlatformAndFallsBackSafely() = runBlocking {
        val engine = LiteRtLmInferenceEngine(
            modelLoader = UnsupportedLiteRtLmModelLoader("unsupported platform"),
            fallbackEngine = MockInferenceEngine(timestampProvider = { "timestamp" }),
        )

        val result = engine.generate(testRequest())

        assertEquals(InferenceBackend.Mock, result.backend)
        assertEquals(RuntimeMode.UnsupportedPlatform, result.runtimeInfo.mode)
        assertEquals("unsupported platform", result.runtimeInfo.fallbackReason)
    }

    @Test
    fun reportsGenerativeRuntimeSkeletonWhenBundleExists() = runBlocking {
        val engine = LiteRtLmInferenceEngine(
            modelLoader = object : LiteRtLmModelLoader {
                override suspend fun loadModel(request: PromptRequest): LiteRtLmModelLoadResult =
                    LiteRtLmModelLoadResult.Loaded(
                        LiteRtLmModelBundle(
                            id = "gemma",
                            displayName = "Gemma",
                            rootPath = "models/litert-lm",
                            modelPath = "models/litert-lm/model.tflite",
                            tokenizerPath = "models/litert-lm/tokenizer.model",
                        ),
                    )
            },
            fallbackEngine = MockInferenceEngine(timestampProvider = { "timestamp" }),
        )

        val result = engine.generate(testRequest())

        assertEquals(InferenceBackend.Mock, result.backend)
        assertEquals(RuntimeMode.LiteRtLmGenerative, result.runtimeInfo.mode)
        assertTrue(result.runtimeInfo.fallbackReason.orEmpty().contains("deferred"))
    }

    private fun testRequest(): PromptRequest =
        PromptRequest(
            prompt = "Diagnose a pump issue.",
            industry = PromptIndustry.IndustrialMaintenance,
            model = AiModel.Gemma,
            backend = InferenceBackend.LiteRtLm,
            maxTokens = 128,
            temperature = 0.2,
        )
}

