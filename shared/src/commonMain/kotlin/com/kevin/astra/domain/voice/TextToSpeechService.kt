package com.kevin.astra.domain.voice

import kotlinx.coroutines.flow.StateFlow

sealed interface TtsState {
    data object Idle : TtsState
    data object Speaking : TtsState
    data object Done : TtsState
    data class Error(val message: String) : TtsState
}

interface TextToSpeechService {
    val state: StateFlow<TtsState>
    fun speak(text: String)
    fun stop()
    fun destroy()
}

expect fun createTextToSpeechService(): TextToSpeechService
