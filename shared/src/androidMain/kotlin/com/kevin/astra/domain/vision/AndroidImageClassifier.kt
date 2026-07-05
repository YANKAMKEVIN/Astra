package com.kevin.astra.domain.vision

import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

actual fun createImageClassifier(): ImageClassifier = MlKitImageClassifier

/**
 * Real on-device image classification via ML Kit's image-labeling model, which is bundled with the
 * dependency (no download, works offline). Replaces the previous TFLite/EfficientNet path, which
 * never shipped a model file and silently fell back to hard-coded mock labels.
 */
private object MlKitImageClassifier : ImageClassifier {
    override val isAvailable = true

    private val labeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(MinConfidence)
                .build(),
        )
    }

    override fun classify(imageBytes: ByteArray): ImageClassificationResult {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return ImageClassificationResult(emptyList(), ModelUsed)

        // classify() is called off the main thread (VisionAssistantViewModel uses Dispatchers.Default),
        // so blocking on ML Kit's Task here is safe.
        val labels = runCatching { Tasks.await(labeler.process(InputImage.fromBitmap(bitmap, 0))) }
            .getOrNull()
            .orEmpty()
            .sortedByDescending { it.confidence }
            .take(5)
            .map { ImageLabel(label = it.text, confidence = it.confidence) }

        return ImageClassificationResult(labels = labels, modelUsed = ModelUsed)
    }

    private const val ModelUsed = "ML Kit Image Labeling"
    private const val MinConfidence = 0.5f
}
