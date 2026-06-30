package com.kevin.astra.domain.documents

import android.content.Context
import com.kevin.astra.data.documents.BowEmbeddingEngine
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

private lateinit var applicationContext: Context

fun initializeAndroidEmbeddingEngine(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createEmbeddingEngine(): EmbeddingEngine =
    if (::applicationContext.isInitialized) {
        tryLoadTfLiteEngine(applicationContext) ?: BowEmbeddingEngine()
    } else BowEmbeddingEngine()

private fun tryLoadTfLiteEngine(context: Context): TfLiteEmbeddingEngine? = runCatching {
    val downloadedFile = context.filesDir.resolve("astra-models/minilm-l6/model.tflite")
    val modelFile = when {
        downloadedFile.exists() -> downloadedFile
        else -> extractAssetToFile(context, "models/embedding/minilm-l6.tflite")
    } ?: return@runCatching null
    TfLiteEmbeddingEngine(Interpreter(modelFile))
}.getOrNull()

private fun extractAssetToFile(context: Context, assetPath: String): File? = runCatching {
    val bytes = context.assets.open(assetPath).readBytes()
    val tmp = File(context.cacheDir, "embedding_model.tflite")
    tmp.writeBytes(bytes)
    tmp
}.getOrNull()

/**
 * MiniLM-L6-v2 TFLite embedding engine.
 *
 * Expected model I/O (standard sentence-transformers export):
 *   Input 0 : int32[1, 128]  — input_ids   (token IDs, WordPiece)
 *   Input 1 : int32[1, 128]  — attention_mask
 *   Output 0: float[1, 384]  — mean-pooled + L2-normalized sentence embedding
 *
 * When the model is absent, the factory returns BowEmbeddingEngine instead.
 */
private class TfLiteEmbeddingEngine(private val interpreter: Interpreter) : EmbeddingEngine {
    override val isNeural = true

    // Simple whitespace tokenizer + hash trick to produce token IDs without a vocab file.
    // A real deployment would ship the WordPiece vocab alongside the model.
    override fun embed(text: String): FloatArray {
        val inputIds = tokenize(text)

        val idBuf = ByteBuffer.allocateDirect(4 * SEQ_LEN).order(ByteOrder.nativeOrder())
        val maskBuf = ByteBuffer.allocateDirect(4 * SEQ_LEN).order(ByteOrder.nativeOrder())
        for (i in 0 until SEQ_LEN) {
            idBuf.putInt(inputIds.getOrElse(i) { 0 })
            maskBuf.putInt(if (i < inputIds.size) 1 else 0)
        }
        idBuf.rewind(); maskBuf.rewind()

        val outputBuf = ByteBuffer.allocateDirect(4 * EMBEDDING_DIM).order(ByteOrder.nativeOrder())
        interpreter.runForMultipleInputsOutputs(
            arrayOf(idBuf, maskBuf),
            mapOf(0 to outputBuf),
        )
        outputBuf.rewind()
        val embedding = FloatArray(EMBEDDING_DIM) { outputBuf.float }
        return l2Normalize(embedding)
    }

    private fun tokenize(text: String): List<Int> {
        val tokens = text.lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { hashToken(it) }
        return listOf(101) + tokens.take(SEQ_LEN - 2) + listOf(102) // [CLS] … [SEP]
    }

    /** Maps a token to a stable int ID in [0, 30_522) via unsigned hash. */
    private fun hashToken(token: String): Int =
        (token.hashCode().toLong() and 0xFFFFFFFFL % 30_522L).toInt() + 1

    private fun l2Normalize(vec: FloatArray): FloatArray {
        val norm = sqrt(vec.fold(0f) { acc, v -> acc + v * v })
        if (norm == 0f) return vec
        return FloatArray(vec.size) { vec[it] / norm }
    }

    private companion object {
        const val SEQ_LEN = 128
        const val EMBEDDING_DIM = 384
    }
}
