package com.kevin.astra.domain.vision

data class ImageLabel(val label: String, val confidence: Float)

data class ImageClassificationResult(
    val labels: List<ImageLabel>,
    val modelUsed: String,
) {
    fun toPromptDescription(): String {
        val top = labels.take(5).joinToString(", ") { "${it.label} (${(it.confidence * 100).toInt()}%)" }
        return "Objects and elements detected in the image: $top."
    }
}
