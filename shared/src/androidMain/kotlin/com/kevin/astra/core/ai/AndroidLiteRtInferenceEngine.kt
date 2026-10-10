package com.kevin.astra.core.ai

import android.content.Context
import com.kevin.astra.app.di.androidAppContext
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

actual fun createInferenceEngine(): InferenceEngine {
    val context: Context = androidAppContext()
    val mockEngine = MockInferenceEngine()
    return RoutingInferenceEngine(
        mockEngine = mockEngine,
        liteRtEngine = LiteRtInferenceEngine(
            modelLoader = if (context != null) {
                AndroidAssetLocalModelLoader(context = context)
            } else {
                UnavailableLocalModelLoader("Android context is not initialized yet.")
            },
            runtimeSession = AndroidLiteRtRuntimeSession(),
            fallbackEngine = mockEngine,
        ),
        liteRtLmEngine = LiteRtLmInferenceEngine(
            modelLoader = if (context != null) {
                AndroidAssetLiteRtLmModelLoader(context = context)
            } else {
                UnsupportedLiteRtLmModelLoader("Android context is not initialized yet.")
            },
            runtimeSession = if (context != null) {
                AndroidLiteRtLmRuntimeSession(context = context)
            } else {
                UnavailableLiteRtLmRuntimeSession("Android context is not initialized yet.")
            },
            fallbackEngine = mockEngine,
        ),
    )
}

class AndroidAssetLocalModelLoader(
    private val context: Context,
    private val assetPath: String = DefaultLiteRtModelAssetPath,
) : LocalModelLoader {
    override suspend fun loadModel(request: PromptRequest): LocalModelLoadResult =
        try {
            context.assets.open(assetPath).use { input ->
                val bytes = input.readBytes()
                if (bytes.isEmpty()) {
                    LocalModelLoadResult.Unavailable("Local LiteRT model asset '$assetPath' is empty.")
                } else {
                    LocalModelLoadResult.Loaded(
                        LocalModelAsset(
                            id = request.model.name.lowercase(),
                            displayName = request.model.label,
                            path = assetPath,
                            bytes = bytes,
                        ),
                    )
                }
            }
        } catch (error: Throwable) {
            LocalModelLoadResult.Unavailable(
                "Local LiteRT model asset '$assetPath' was not found. Add a compatible .tflite model under shared Android assets to enable real inference.",
            )
        }
}

class AndroidLiteRtRuntimeSession(
    private val logger: EdgeAiLogger = ConsoleEdgeAiLogger,
) : EdgeRuntimeSession {
    private var interpreter: Interpreter? = null
    private var activeModelId: String? = null

    override suspend fun initialize(model: LocalModelAsset): EdgeRuntimeStatus {
        if (interpreter != null && activeModelId == model.id) {
            return EdgeRuntimeStatus.Ready
        }

        close()

        return try {
            val modelBuffer = ByteBuffer.allocateDirect(model.bytes.size)
                .order(ByteOrder.nativeOrder())
                .apply {
                    put(model.bytes)
                    rewind()
                }
            interpreter = Interpreter(
                modelBuffer,
                Interpreter.Options()
                    .setNumThreads(2)
                    .setUseXNNPACK(true),
            ).also { it.allocateTensors() }
            activeModelId = model.id
            logger.info("LiteRT Interpreter initialized for ${model.path}.")
            EdgeRuntimeStatus.Ready
        } catch (error: Throwable) {
            logger.error("LiteRT Interpreter initialization failed.", error)
            close()
            EdgeRuntimeStatus.Unavailable(error.message ?: "LiteRT Interpreter initialization failed.")
        }
    }

    override suspend fun generate(request: PromptRequest, model: LocalModelAsset): GenerationResult {
        val runtime = interpreter
            ?: throw IllegalStateException("LiteRT Interpreter was not initialized.")

        val inputTensor = runtime.getInputTensor(0)
        val outputTensor = runtime.getOutputTensor(0)
        val input = zeroedBufferFor(inputTensor.dataType(), inputTensor.numBytes())
        val output = zeroedBufferFor(outputTensor.dataType(), outputTensor.numBytes())
        val mark = TimeSource.Monotonic.markNow()

        runtime.run(input, output)

        val latencyMillis = mark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L)
        val nativeLatencyMillis = runtime.lastNativeInferenceDurationNanoseconds
            ?.let { it / 1_000_000L }
            ?.coerceAtLeast(1L)
            ?: latencyMillis
        output.rewind()

        return GenerationResult(
            text = """
                LiteRT local inference completed

                ASTRA loaded ${model.displayName} from ${model.path} and executed a real local LiteRT Interpreter pass.

                Input tensor:
                ${inputTensor.name()} ${inputTensor.dataType()} ${inputTensor.shape().contentToString()}

                Output tensor:
                ${outputTensor.name()} ${outputTensor.dataType()} ${outputTensor.shape().contentToString()}

                Output preview:
                ${output.preview(outputTensor.dataType())}
            """.trimIndent(),
            metrics = GenerationMetrics(
                latencyMillis = nativeLatencyMillis,
                timeToFirstTokenMillis = nativeLatencyMillis,
                tokensGenerated = outputTensor.numElements().coerceAtLeast(1),
                tokensPerSecond = 0,
                memoryUsageMb = ((model.bytes.size + input.capacity() + output.capacity()) / (1024 * 1024)).coerceAtLeast(1),
            ),
            model = request.model,
            backend = InferenceBackend.LiteRt,
            generatedAt = currentAndroidEdgeTimestamp(),
            runtimeInfo = GenerationRuntimeInfo(
                mode = RuntimeMode.Real,
                inferenceLatencyMillis = nativeLatencyMillis,
                totalExecutionTimeMillis = latencyMillis,
            ),
        )
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        activeModelId = null
    }
}

private const val DefaultLiteRtModelAssetPath = "models/astra-slm.tflite"
private const val DefaultLiteRtLmModelAssetRoot = "models/litert-lm"
private const val AstraModelsDir = "astra-models"

class AndroidAssetLiteRtLmModelLoader(
    private val context: Context,
    private val assetRoot: String = DefaultLiteRtLmModelAssetRoot,
) : LiteRtLmModelLoader {
    override suspend fun loadModel(request: PromptRequest): LiteRtLmModelLoadResult {
        // Priorité 1 : modèle téléchargé dans filesDir (par modelId)
        val filesDirResult = loadFromFilesDir(request)
        if (filesDirResult is LiteRtLmModelLoadResult.Loaded) return filesDirResult

        // Priorité 2 : modèle bundlé dans les assets
        return loadFromAssets(request)
    }

    private fun loadFromFilesDir(request: PromptRequest): LiteRtLmModelLoadResult {
        val modelDir = File(context.filesDir, "$AstraModelsDir/${request.model.filesystemId}")
        if (!modelDir.exists()) return LiteRtLmModelLoadResult.Missing("No downloaded model for ${request.model.filesystemId}.")
        val files = modelDir.listFiles() ?: return LiteRtLmModelLoadResult.Missing("Empty model directory.")
        val modelFile = files.firstOrNull { it.name.endsWith(".litertlm") || it.name.endsWith(".task") }
            ?: files.firstOrNull { it.name.endsWith(".tflite") || it.name.endsWith(".bin") }
            ?: return LiteRtLmModelLoadResult.Missing("No model file found in ${modelDir.path}.")
        return LiteRtLmModelLoadResult.Loaded(
            LiteRtLmModelBundle(
                id = request.model.filesystemId,
                displayName = request.model.label,
                rootPath = modelDir.absolutePath,
                modelPath = modelFile.absolutePath,
                sourceModelPath = modelFile.absolutePath,
                sizeBytes = modelFile.length(),
            ),
        )
    }

    private fun loadFromAssets(request: PromptRequest): LiteRtLmModelLoadResult =
        try {
            val files = context.assets.list(assetRoot).orEmpty().toSet()
            val bundleFile = files.firstOrNull { it.endsWith(".task") || it.endsWith(".litertlm") }
            val splitModelFile = files.firstOrNull { it.endsWith(".tflite") || it.endsWith(".bin") }
            val modelFile = bundleFile ?: splitModelFile
            val tokenizerFile = files.firstOrNull { it.endsWith(".model") || it.endsWith(".spm") || it.contains("tokenizer", ignoreCase = true) }
            val configFile = files.firstOrNull { it.endsWith(".json") }

            when {
                files.isEmpty() -> LiteRtLmModelLoadResult.Missing(
                    "No LiteRT-LM assets found under '$assetRoot'. Download a model or add a bundle via developer setup.",
                )
                modelFile == null -> LiteRtLmModelLoadResult.Missing(
                    "LiteRT-LM bundle under '$assetRoot' is missing a .task or .litertlm model bundle.",
                )
                bundleFile == null && tokenizerFile == null -> LiteRtLmModelLoadResult.Missing(
                    "LiteRT-LM bundle under '$assetRoot' is missing a tokenizer file.",
                )
                else -> {
                    val assetModelPath = "$assetRoot/$modelFile"
                    val localModelFile = copyAssetToCache(assetModelPath)
                    LiteRtLmModelLoadResult.Loaded(
                        LiteRtLmModelBundle(
                            id = request.model.filesystemId,
                            displayName = request.model.label,
                            rootPath = assetRoot,
                            modelPath = localModelFile.absolutePath,
                            sourceModelPath = assetModelPath,
                            tokenizerPath = tokenizerFile?.let { "$assetRoot/$it" },
                            configPath = configFile?.let { "$assetRoot/$it" },
                            sizeBytes = localModelFile.length(),
                        ),
                    )
                }
            }
        } catch (error: Throwable) {
            LiteRtLmModelLoadResult.Missing(
                "Unable to inspect LiteRT-LM assets under '$assetRoot': ${error.message ?: "unknown asset error"}",
            )
        }

    private fun copyAssetToCache(assetPath: String): File {
        val outputDir = File(context.cacheDir, "astra-litert-lm").apply { mkdirs() }
        val output = File(outputDir, assetPath.substringAfterLast('/'))
        context.assets.open(assetPath).use { input ->
            output.outputStream().use { stream ->
                input.copyTo(stream)
            }
        }
        return output
    }
}

/**
 * Runs on-device generation for downloaded or bundled LLMs.
 *
 * `.litertlm` files go through the LiteRT-LM runtime: it reads the tokenizer embedded in the file
 * (SentencePiece *or* HF BPE) and applies each model's own chat template, with the system prompt
 * as a real system turn. MediaPipe's LlmInference only understands SentencePiece, so it rejected
 * Qwen, SmolLM, Phi… ("SentencePiece tokenizer is not found in the model") and every answer fell
 * back to Mock. MediaPipe is kept for legacy `.task` bundles, which LiteRT-LM does not read.
 */
class AndroidLiteRtLmRuntimeSession(
    private val context: Context,
    private val logger: EdgeAiLogger = ConsoleEdgeAiLogger,
) : LiteRtLmRuntimeSession {
    private var engine: Engine? = null
    private var inference: LlmInference? = null
    private var activeModelPath: String? = null

    override suspend fun generate(
        request: PromptRequest,
        bundle: LiteRtLmModelBundle,
    ): GenerationResult {
        // The KV cache baked into the export is the hard limit; the catalog's window is the family's.
        val window = listOfNotNull(
            request.contextWindow.takeIf { it > 0 },
            RuntimePrompt.contextWindowFromFileName(File(bundle.modelPath).name),
            ENGINE_CONTEXT_TOKENS,
        ).min()

        val loadTime: Long
        val text: String
        val generationMark: TimeSource.Monotonic.ValueTimeMark
        if (bundle.modelPath.endsWith(".litertlm")) {
            loadTime = ensureEngine(bundle, window)
            generationMark = TimeSource.Monotonic.markNow()
            text = generateWithLiteRtLm(request, window)
        } else {
            loadTime = ensureInference(bundle)
            generationMark = TimeSource.Monotonic.markNow()
            text = generateWithMediaPipe(request, window)
        }
        val answer = text.trim().ifBlank { "LiteRT-LM returned an empty response." }
        val generationLatencyMillis = generationMark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L)
        val generatedTokens = RuntimePrompt.estimateTokens(answer).coerceAtLeast(1)
        val tokensPerSecond = ((generatedTokens * 1_000L) / generationLatencyMillis).toInt().coerceAtLeast(1)

        return GenerationResult(
            text = answer,
            metrics = GenerationMetrics(
                latencyMillis = generationLatencyMillis,
                timeToFirstTokenMillis = generationLatencyMillis,
                tokensGenerated = generatedTokens,
                tokensPerSecond = tokensPerSecond,
                memoryUsageMb = (bundle.sizeBytes / (1024 * 1024)).toInt().coerceAtLeast(1),
            ),
            model = request.model,
            backend = InferenceBackend.LiteRtLm,
            generatedAt = currentAndroidEdgeTimestamp(),
            runtimeInfo = GenerationRuntimeInfo(
                mode = RuntimeMode.LiteRtLmGenerative,
                modelLoadTimeMillis = loadTime,
                inferenceLatencyMillis = generationLatencyMillis,
                totalExecutionTimeMillis = loadTime + generationLatencyMillis,
                fallbackReason = null,
            ),
        )
    }

    private fun generateWithLiteRtLm(request: PromptRequest, window: Int): String {
        val runtime = engine ?: error("LiteRT-LM engine was not initialized.")
        // No tokenizer is exposed, so the fit uses the (pessimistic) estimate. Overflowing the
        // KV cache would fail the call rather than truncate.
        val prompt = RuntimePrompt.fit(request, contextWindow = window)
        val config = ConversationConfig(
            systemInstruction = prompt.system.takeIf { it.isNotEmpty() }?.let { Contents.of(it) },
            samplerConfig = SamplerConfig(
                topK = 40,
                topP = 0.95,
                temperature = request.temperature,
                seed = 0,
            ),
        )
        return runtime.createConversation(config).use { conversation ->
            conversation.sendMessage(prompt.user)
                .contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString(separator = "") { it.text }
        }
    }

    private fun generateWithMediaPipe(request: PromptRequest, window: Int): String {
        val runtime = inference ?: error("LiteRT-LM session was not initialized.")
        // LiteRT-LM aborts the whole process natively (SIGABRT) when input + output exceed the
        // model context, bypassing Kotlin try/catch, so the prompt MUST fit before the call.
        val inputText = RuntimePrompt.compose(
            request = request,
            contextWindow = window,
            countTokens = { text ->
                runCatching { runtime.sizeInTokens(text) }.getOrElse { RuntimePrompt.estimateTokens(text) }
            },
        )
        return runtime.generateResponse(inputText)
    }

    override fun close() {
        engine?.close()
        engine = null
        inference?.close()
        inference = null
        activeModelPath = null
    }

    private fun ensureEngine(bundle: LiteRtLmModelBundle, window: Int): Long {
        if (engine != null && activeModelPath == bundle.modelPath) return 0L
        close()

        val loadMark = TimeSource.Monotonic.markNow()
        // CPU works on every device; GPU/NPU can be enabled once validated per chipset.
        engine = Engine(
            EngineConfig(
                modelPath = bundle.modelPath,
                backend = Backend.CPU(),
                maxNumTokens = window,
                cacheDir = context.cacheDir.absolutePath,
            ),
        ).also { it.initialize() }
        activeModelPath = bundle.modelPath
        logger.info("LiteRT-LM engine initialized from ${bundle.sourceModelPath} (window $window tokens).")
        return loadMark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L)
    }

    private fun ensureInference(bundle: LiteRtLmModelBundle): Long {
        if (inference != null && activeModelPath == bundle.modelPath) return 0L
        close()

        val loadMark = TimeSource.Monotonic.markNow()
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(bundle.modelPath)
            .setMaxTokens(ENGINE_CONTEXT_TOKENS) // total context budget (input + output)
            .setMaxTopK(40)
            .build()
        inference = LlmInference.createFromOptions(context, options)
        activeModelPath = bundle.modelPath
        logger.info("MediaPipe LlmInference initialized from ${bundle.sourceModelPath}.")
        return loadMark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L)
    }
}

/** Upper bound on the context (input + output) any engine is created with. */
private const val ENGINE_CONTEXT_TOKENS = 4_096

private fun zeroedBufferFor(dataType: DataType, byteSize: Int): ByteBuffer =
    ByteBuffer.allocateDirect(byteSize)
        .order(ByteOrder.nativeOrder())
        .apply {
            repeat(byteSize) { put(0) }
            rewind()
        }

private fun ByteBuffer.preview(dataType: DataType): String {
    val duplicate = duplicate().order(ByteOrder.nativeOrder())
    return when (dataType) {
        DataType.FLOAT32 -> List(min(8, duplicate.remaining() / 4)) { duplicate.float }.joinToString(prefix = "[", postfix = "]")
        DataType.INT32 -> List(min(8, duplicate.remaining() / 4)) { duplicate.int }.joinToString(prefix = "[", postfix = "]")
        DataType.INT64 -> List(min(8, duplicate.remaining() / 8)) { duplicate.long }.joinToString(prefix = "[", postfix = "]")
        DataType.INT16 -> List(min(8, duplicate.remaining() / 2)) { duplicate.short }.joinToString(prefix = "[", postfix = "]")
        DataType.INT8, DataType.UINT8, DataType.BOOL -> List(min(8, duplicate.remaining())) { duplicate.get().toInt() }.joinToString(prefix = "[", postfix = "]")
        DataType.STRING -> "[string tensor preview unavailable]"
    }
}

@OptIn(ExperimentalTime::class)
private fun currentAndroidEdgeTimestamp(): String = Clock.System.now().toString()
