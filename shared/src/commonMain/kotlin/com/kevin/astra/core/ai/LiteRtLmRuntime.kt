package com.kevin.astra.core.ai

import kotlin.time.TimeSource

data class LiteRtLmModelBundle(
    val id: String,
    val displayName: String,
    val rootPath: String,
    val modelPath: String,
    val sourceModelPath: String = modelPath,
    val tokenizerPath: String? = null,
    val configPath: String? = null,
    val sizeBytes: Long = 0L,
)

sealed interface LiteRtLmModelLoadResult {
    data class Loaded(val bundle: LiteRtLmModelBundle) : LiteRtLmModelLoadResult
    data class Missing(val message: String) : LiteRtLmModelLoadResult
    data class Unsupported(val message: String) : LiteRtLmModelLoadResult
}

interface LiteRtLmModelLoader {
    suspend fun loadModel(request: PromptRequest): LiteRtLmModelLoadResult
}

interface LiteRtLmRuntimeSession {
    suspend fun generate(
        request: PromptRequest,
        bundle: LiteRtLmModelBundle,
    ): GenerationResult

    fun close()
}

class LiteRtLmInferenceEngine(
    private val modelLoader: LiteRtLmModelLoader,
    private val runtimeSession: LiteRtLmRuntimeSession = UnavailableLiteRtLmRuntimeSession(
        "LiteRT-LM runtime session is not available on this platform.",
    ),
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
                logger.info("LiteRT-LM bundle located at ${loaded.bundle.sourceModelPath}; starting generative session.")
                runCatching {
                    runtimeSession.generate(request = request, bundle = loaded.bundle)
                }.getOrElse { error ->
                    logger.error("LiteRT-LM generation failed. Activating Mock fallback.", error)
                    controlledFallback(
                        request = request,
                        mode = RuntimeMode.Fallback,
                        reason = error.message ?: "LiteRT-LM generation failed.",
                        modelLoadTimeMillis = loadTime,
                        totalExecutionTimeMillis = totalMark.elapsedNow().inWholeMilliseconds,
                    )
                }
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

class UnavailableLiteRtLmRuntimeSession(
    private val reason: String,
) : LiteRtLmRuntimeSession {
    override suspend fun generate(
        request: PromptRequest,
        bundle: LiteRtLmModelBundle,
    ): GenerationResult =
        error(reason)

    override fun close() = Unit
}

class UnsupportedLiteRtLmModelLoader(
    private val reason: String,
) : LiteRtLmModelLoader {
    override suspend fun loadModel(request: PromptRequest): LiteRtLmModelLoadResult =
        LiteRtLmModelLoadResult.Unsupported(reason)
}
