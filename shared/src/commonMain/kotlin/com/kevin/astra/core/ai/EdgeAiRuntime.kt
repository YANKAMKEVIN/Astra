package com.kevin.astra.core.ai

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

data class LocalModelAsset(
    val id: String,
    val displayName: String,
    val path: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is LocalModelAsset &&
            id == other.id &&
            displayName == other.displayName &&
            path == other.path &&
            bytes.contentEquals(other.bytes)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

sealed interface LocalModelLoadResult {
    data class Loaded(val asset: LocalModelAsset) : LocalModelLoadResult
    data class Unavailable(val message: String) : LocalModelLoadResult
}

interface LocalModelLoader {
    suspend fun loadModel(request: PromptRequest): LocalModelLoadResult
}

interface EdgeRuntimeSession {
    suspend fun initialize(model: LocalModelAsset): EdgeRuntimeStatus
    suspend fun generate(request: PromptRequest, model: LocalModelAsset): GenerationResult
    fun close()
}

sealed interface EdgeRuntimeStatus {
    data object Ready : EdgeRuntimeStatus
    data class Unavailable(val message: String) : EdgeRuntimeStatus
}

class LiteRtInferenceEngine(
    private val modelLoader: LocalModelLoader,
    private val runtimeSession: EdgeRuntimeSession,
    private val fallbackEngine: InferenceEngine = MockInferenceEngine(),
    private val logger: EdgeAiLogger = ConsoleEdgeAiLogger,
) : InferenceEngine {
    override suspend fun generate(request: PromptRequest): GenerationResult {
        if (request.backend != InferenceBackend.LiteRt) {
            logger.info("LiteRT engine bypassed for backend=${request.backend.label}; delegating to fallback.")
            return fallbackEngine.generate(request)
        }

        logger.info("LiteRT inference requested for model=${request.model.label}.")

        val model = when (val loaded = modelLoader.loadModel(request)) {
            is LocalModelLoadResult.Loaded -> loaded.asset
            is LocalModelLoadResult.Unavailable -> {
                logger.warn("LiteRT model unavailable: ${loaded.message}")
                return fallback(request, loaded.message)
            }
        }

        return try {
            when (val status = runtimeSession.initialize(model)) {
                EdgeRuntimeStatus.Ready -> {
                    logger.info("LiteRT runtime ready with model=${model.id}.")
                    runtimeSession.generate(request, model)
                }

                is EdgeRuntimeStatus.Unavailable -> {
                    logger.warn("LiteRT runtime unavailable: ${status.message}")
                    fallback(request, status.message)
                }
            }
        } catch (error: Throwable) {
            logger.error("LiteRT inference failed. Activating Mock fallback.", error)
            fallback(request, error.message ?: "Unknown LiteRT runtime failure.")
        }
    }

    private suspend fun fallback(request: PromptRequest, reason: String): GenerationResult {
        val fallbackRequest = request.copy(backend = InferenceBackend.Mock)
        val fallbackResult = fallbackEngine.generate(fallbackRequest)
        return fallbackResult.copy(
            text = """
                ASTRA fallback active

                LiteRT could not execute this request locally:
                $reason

                ${fallbackResult.text}
            """.trimIndent(),
        )
    }
}

class UnavailableLocalModelLoader(
    private val reason: String,
) : LocalModelLoader {
    override suspend fun loadModel(request: PromptRequest): LocalModelLoadResult =
        LocalModelLoadResult.Unavailable(reason)
}

class UnavailableEdgeRuntimeSession(
    private val reason: String,
) : EdgeRuntimeSession {
    override suspend fun initialize(model: LocalModelAsset): EdgeRuntimeStatus =
        EdgeRuntimeStatus.Unavailable(reason)

    override suspend fun generate(request: PromptRequest, model: LocalModelAsset): GenerationResult =
        error(reason)

    override fun close() = Unit
}

class EchoEdgeRuntimeSession(
    private val timestampProvider: () -> String = ::currentEdgeTimestamp,
) : EdgeRuntimeSession {
    override suspend fun initialize(model: LocalModelAsset): EdgeRuntimeStatus = EdgeRuntimeStatus.Ready

    override suspend fun generate(request: PromptRequest, model: LocalModelAsset): GenerationResult {
        val mark = TimeSource.Monotonic.markNow()
        val compactPrompt = request.prompt.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(4)
            .joinToString(separator = " ")
            .take(280)
        val latency = mark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L)
        val tokens = compactPrompt.split(" ").filter { it.isNotBlank() }.size.coerceAtLeast(1)

        return GenerationResult(
            text = """
                LiteRT local inference initialized

                ASTRA loaded ${model.displayName} from ${model.path} and executed the local runtime path.

                Prompt digest:
                $compactPrompt
            """.trimIndent(),
            metrics = GenerationMetrics(
                latencyMillis = latency,
                timeToFirstTokenMillis = latency,
                tokensGenerated = tokens,
                tokensPerSecond = tokens.coerceAtLeast(1),
                memoryUsageMb = (model.bytes.size / (1024 * 1024)).coerceAtLeast(1),
            ),
            model = request.model,
            backend = InferenceBackend.LiteRt,
            generatedAt = timestampProvider(),
        )
    }

    override fun close() = Unit
}

interface EdgeAiLogger {
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, throwable: Throwable? = null)
}

object ConsoleEdgeAiLogger : EdgeAiLogger {
    override fun info(message: String) {
        println("[ASTRA][EdgeAI][INFO] $message")
    }

    override fun warn(message: String) {
        println("[ASTRA][EdgeAI][WARN] $message")
    }

    override fun error(message: String, throwable: Throwable?) {
        println("[ASTRA][EdgeAI][ERROR] $message ${throwable?.message.orEmpty()}")
    }
}

@OptIn(ExperimentalTime::class)
private fun currentEdgeTimestamp(): String = Clock.System.now().toString()

