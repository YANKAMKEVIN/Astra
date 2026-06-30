package com.kevin.astra.domain.vision

actual fun createImageClassifier(): ImageClassifier = IosStubImageClassifier

private object IosStubImageClassifier : ImageClassifier {
    override val isAvailable = false
    override fun classify(imageBytes: ByteArray) = ImageClassificationResult(
        labels = emptyList(),
        modelUsed = "Not available on iOS yet",
    )
}
