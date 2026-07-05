@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.kevin.astra.domain.vision

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.Vision.VNClassificationObservation
import platform.Vision.VNClassifyImageRequest
import platform.Vision.VNImageRequestHandler

actual fun createImageClassifier(): ImageClassifier = IosVisionImageClassifier

/**
 * Real on-device image classification via Apple's Vision framework
 * ([VNClassifyImageRequest]) — no model file to bundle or download, runs entirely on-device.
 * Replaces the previous "not available on iOS" stub.
 */
private object IosVisionImageClassifier : ImageClassifier {
    override val isAvailable = true

    override fun classify(imageBytes: ByteArray): ImageClassificationResult {
        if (imageBytes.isEmpty()) return ImageClassificationResult(emptyList(), ModelName)

        val data = imageBytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = imageBytes.size.toULong())
        }
        val request = VNClassifyImageRequest()
        val handler = VNImageRequestHandler(data = data, options = emptyMap<Any?, Any?>())

        val ok = memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            handler.performRequests(listOf(request), error = errorPtr.ptr)
        }
        if (!ok) return ImageClassificationResult(emptyList(), "$ModelName (failed)")

        @Suppress("UNCHECKED_CAST")
        val observations = request.results as? List<VNClassificationObservation> ?: emptyList()

        // VNClassifyImageRequest returns hundreds of low-confidence taxonomy entries; keep only the
        // confident, human-meaningful ones.
        val labels = observations
            .filter { it.confidence >= MinConfidence }
            .sortedByDescending { it.confidence }
            .take(5)
            .map { ImageLabel(label = it.identifier, confidence = it.confidence) }

        return ImageClassificationResult(labels = labels, modelUsed = ModelName)
    }

    private const val ModelName = "Apple Vision"
    private const val MinConfidence = 0.10f
}
