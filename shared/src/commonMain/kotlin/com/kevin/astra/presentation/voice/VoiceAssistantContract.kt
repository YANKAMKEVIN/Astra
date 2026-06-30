package com.kevin.astra.presentation.voice

import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState

enum class VoicePhase(val label: String) {
    Idle("Tap to speak"),
    Listening("Listening…"),
    Processing("Thinking…"),
    Speaking("Speaking…"),
}

data class VoiceAssistantState(
    val phase: VoicePhase = VoicePhase.Idle,
    val transcript: String = "",
    val response: String = "",
    val error: String? = null,
) : AstraState

sealed interface VoiceAssistantIntent : AstraIntent {
    data object ToggleMic : VoiceAssistantIntent
    data object StopSpeaking : VoiceAssistantIntent
    data object ClearError : VoiceAssistantIntent
}

sealed interface VoiceAssistantEffect : AstraEffect
