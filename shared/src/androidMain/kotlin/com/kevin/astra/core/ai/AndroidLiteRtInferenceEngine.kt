package com.kevin.astra.core.ai

import android.content.Context
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

private var applicationContext: Context? = null

fun initializeAndroidEdgeAiRuntime(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createInferenceEngine(): InferenceEngine {
    val context = applicationContext
    return LiteRtInferenceEngine(
        modelLoader = if (context != null) {
            AndroidAssetLocalModelLoader(context = context)
        } else {
            UnavailableLocalModelLoader("Android context is not initialized yet.")
        },
        runtimeSession = AndroidLiteRtRuntimeSession(),
        fallbackEngine = MockInferenceEngine(),
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
