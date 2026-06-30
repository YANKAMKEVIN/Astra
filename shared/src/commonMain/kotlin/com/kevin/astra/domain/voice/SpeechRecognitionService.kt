package com.kevin.astra.domain.voice

import kotlinx.coroutines.flow.StateFlow

sealed interface SpeechRecognitionState {
    data object Idle : SpeechRecognitionState
    data object Listening : SpeechRecognitionState
    data class Partial(val text: String) : SpeechRecognitionState
    data class Result(val text: String) : SpeechRecognitionState
    data class Error(val message: String) : SpeechRecognitionState
}

interface SpeechRecognitionService {
    val state: StateFlow<SpeechRecognitionState>
    fun startListening()
    fun stopListening()
    fun destroy()
}

expect fun createSpeechRecognitionService(): SpeechRecognitionService
