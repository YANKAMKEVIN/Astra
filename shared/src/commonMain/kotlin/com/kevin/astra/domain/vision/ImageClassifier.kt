package com.kevin.astra.domain.vision

interface ImageClassifier {
    fun classify(imageBytes: ByteArray): ImageClassificationResult
    val isAvailable: Boolean
}

expect fun createImageClassifier(): ImageClassifier
