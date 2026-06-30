package com.kevin.astra.domain.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kevin.astra.domain.vision.ImagenetLabels.LABELS
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

private lateinit var applicationContext: Context

fun initializeAndroidImageClassifier(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createImageClassifier(): ImageClassifier =
    if (::applicationContext.isInitialized) {
        val model = tryLoadModel(applicationContext)
        if (model != null) TfLiteImageClassifier(model)
        else MockImageClassifier
    } else MockImageClassifier

private fun tryLoadModel(context: Context): Interpreter? = runCatching {
    // Try filesDir first (downloaded via Model Manager), then assets
    val downloadedFile = context.filesDir
        .resolve("astra-models/efficientnet-lite0/model.tflite")
    val modelFile = when {
        downloadedFile.exists() -> downloadedFile
        else -> extractAssetToFile(context, "models/vision/model.tflite")
    } ?: return@runCatching null
    Interpreter(modelFile)
}.getOrNull()

// TFLite Interpreter(ByteBuffer) requires a direct/mapped buffer; heap ByteBuffer.wrap()
// silently produces wrong behavior. Extracting to a file lets the interpreter memory-map it.
private fun extractAssetToFile(context: Context, assetPath: String): File? = runCatching {
    val assetBytes = context.assets.open(assetPath).readBytes()
    val tmp = File(context.cacheDir, "vision_model.tflite")
    tmp.writeBytes(assetBytes)
    tmp
}.getOrNull()

private class TfLiteImageClassifier(private val interpreter: Interpreter) : ImageClassifier {

    override val isAvailable = true

    override fun classify(imageBytes: ByteArray): ImageClassificationResult {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return emptyResult()
        val input = preprocessBitmap(bitmap)
        val numClasses = interpreter.getOutputTensor(0).shape()[1]
        val output = Array(1) { FloatArray(numClasses) }
        interpreter.run(input, output)
        val probs = output[0]
        val top = probs.indices
            .sortedByDescending { probs[it] }
            .take(5)
            .map { ImageLabel(label = LABELS.getOrElse(it) { "unknown" }, confidence = probs[it]) }
            .filter { it.confidence > 0.05f }
        return ImageClassificationResult(labels = top, modelUsed = "EfficientNet-Lite0")
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val buffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            buffer.putFloat((pixel shr 16 and 0xFF) / 255f)
            buffer.putFloat((pixel shr 8 and 0xFF) / 255f)
            buffer.putFloat((pixel and 0xFF) / 255f)
        }
        return buffer
    }

    private fun emptyResult() = ImageClassificationResult(emptyList(), "TFLite")

    companion object {
        private const val INPUT_SIZE = 224
    }
}

// Used when the model file hasn't been downloaded yet
internal object MockImageClassifier : ImageClassifier {
    override val isAvailable = false
    override fun classify(imageBytes: ByteArray) = ImageClassificationResult(
        labels = listOf(
            ImageLabel("laptop computer", 0.91f),
            ImageLabel("keyboard", 0.85f),
            ImageLabel("coffee mug", 0.72f),
        ),
        modelUsed = "Mock (download EfficientNet-Lite0 to enable real vision)",
    )
}
