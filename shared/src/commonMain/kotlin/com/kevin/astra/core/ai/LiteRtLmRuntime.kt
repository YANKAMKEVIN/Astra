package com.kevin.astra.core.ai

import kotlin.time.TimeSource

data class LiteRtLmModelBundle(
    val id: String,
    val displayName: String,
    val rootPath: String,
    val modelPath: String,
    val tokenizerPath: String,
    val configPath: String? = null,
)

sealed interface LiteRtLmModelLoadResult {
    data class Loaded(val bundle: LiteRtLmModelBundle) : LiteRtLmModelLoadResult
    data class Missing(val message: String) : LiteRtLmModelLoadResult
    data class Unsupported(val message: String) : LiteRtLmModelLoadResult
}

interface LiteRtLmModelLoader {
    suspend fun loadModel(request: PromptRequest): LiteRtLmModelLoadResult
}

class LiteRtLmInferenceEngine(
    private val modelLoader: LiteRtLmModelLoader,
    private val fallbackEngine: InferenceEngine = MockInferenceEngine(),
    private val logger: EdgeAiLogger = ConsoleEdgeAiLogger,
) : InferenceEngine {
    override suspend fun generate(request: PromptRequest): GenerationResult {
        if (request.backend != InferenceBackend.LiteRtLm) {
            return fallbackEngine.generate(request.copy(backend = InferenceBackend.Mock))
        }

        val totalMark = TimeSource.Monotonic.markNow()
        val loadMark = TimeSource.Monotonic.markNow()

        return when (val loaded = modelLoader.loadModel(request)) {
            is LiteRtLmModelLoadResult.Loaded -> {
                val loadTime = loadMark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L)
                logger.info("LiteRT-LM bundle located at ${loaded.bundle.rootPath}; controlled fallback remains active until the generative session is implemented.")
                controlledFallback(
                    request = request,
                    mode = RuntimeMode.LiteRtLmGenerative,
                    reason = "LiteRT-LM bundle was found, but token generation loop integration is intentionally deferred.",
                    modelLoadTimeMillis = loadTime,
                    totalExecutionTimeMillis = totalMark.elapsedNow().inWholeMilliseconds,
                )
            }

            is LiteRtLmModelLoadResult.Missing -> {
                logger.warn("LiteRT-LM model missing: ${loaded.message}")
                controlledFallback(
                    request = request,
                    mode = RuntimeMode.ModelMissing,
                    reason = loaded.message,
                    modelLoadTimeMillis = loadMark.elapsedNow().inWholeMilliseconds,
                    totalExecutionTimeMillis = totalMark.elapsedNow().inWholeMilliseconds,
                )
            }

            is LiteRtLmModelLoadResult.Unsupported -> {
                logger.warn("LiteRT-LM unsupported: ${loaded.message}")
                controlledFallback(
                    request = request,
                    mode = RuntimeMode.UnsupportedPlatform,
                    reason = loaded.message,
                    modelLoadTimeMillis = loadMark.elapsedNow().inWholeMilliseconds,
                    totalExecutionTimeMillis = totalMark.elapsedNow().inWholeMilliseconds,
                )
            }
        }
    }

    private suspend fun controlledFallback(
        request: PromptRequest,
        mode: RuntimeMode,
        reason: String,
        modelLoadTimeMillis: Long,
        totalExecutionTimeMillis: Long,
    ): GenerationResult {
        val fallbackResult = fallbackEngine.generate(request.copy(backend = InferenceBackend.Mock))
        return fallbackResult.copy(
            text = """
                LiteRT-LM generative runtime not active

                $reason

                ${fallbackResult.text}
            """.trimIndent(),
            runtimeInfo = fallbackResult.runtimeInfo.copy(
                mode = mode,
                modelLoadTimeMillis = modelLoadTimeMillis,
                inferenceLatencyMillis = fallbackResult.metrics.latencyMillis,
                totalExecutionTimeMillis = (totalExecutionTimeMillis + fallbackResult.metrics.latencyMillis).coerceAtLeast(fallbackResult.metrics.latencyMillis),
                fallbackReason = reason,
            ),
        )
    }
}

class UnsupportedLiteRtLmModelLoader(
    private val reason: String,
) : LiteRtLmModelLoader {
    override suspend fun loadModel(request: PromptRequest): LiteRtLmModelLoadResult =
        LiteRtLmModelLoadResult.Unsupported(reason)
}
