package com.kevin.astra.presentation.vision

import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.vision.ImageClassificationResult

enum class VisionPhase(val label: String) {
    Idle("Take a photo"),
    Classifying("Analyzing image…"),
    Thinking("Asking ASTRA…"),
    Done("Analysis complete"),
}

data class VisionAssistantState(
    val phase: VisionPhase = VisionPhase.Idle,
    val capturedImageBytes: ByteArray? = null,
    val classificationResult: ImageClassificationResult? = null,
    val userQuestion: String = "What is this? Describe what you see.",
    val response: String = "",
    val error: String? = null,
    val classifierAvailable: Boolean = false,
) : AstraState

sealed interface VisionAssistantIntent : AstraIntent {
    data class PhotoCaptured(val bytes: ByteArray) : VisionAssistantIntent
    data class UpdateQuestion(val question: String) : VisionAssistantIntent
    data object Reset : VisionAssistantIntent
    data object ClearError : VisionAssistantIntent
}

sealed interface VisionAssistantEffect : AstraEffect
