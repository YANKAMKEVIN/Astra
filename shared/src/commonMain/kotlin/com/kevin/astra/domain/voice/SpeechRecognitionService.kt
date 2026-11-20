package com.kevin.astra.domain.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SpeechRecognitionState {
    data object Idle : SpeechRecognitionState
    data object Listening : SpeechRecognitionState
    data class Partial(val text: String) : SpeechRecognitionState
    data class Result(val text: String) : SpeechRecognitionState
    data class Error(val message: String) : SpeechRecognitionState
}

interface SpeechRecognitionService {
    val state: StateFlow<SpeechRecognitionState>

    /** Microphone level while listening, from 0 (silence) to 1 (loud speech). */
    val inputLevel: StateFlow<Float> get() = NoInputLevel

    /** False when the platform can't measure [inputLevel]; the UI then reacts to transcribed words. */
    val reportsInputLevel: Boolean get() = false

    fun startListening()
    fun stopListening()
    fun destroy()
}

expect fun createSpeechRecognitionService(): SpeechRecognitionService

private val NoInputLevel: StateFlow<Float> = MutableStateFlow(0f).asStateFlow()

/**
 * Maps Android's RecognitionListener.onRmsChanged value (about −2 dB in silence to 10 dB for
 * loud speech) to 0..1.
 */
fun normalizeRmsDb(rmsDb: Float): Float = ((rmsDb - SilenceRmsDb) / (LoudRmsDb - SilenceRmsDb)).coerceIn(0f, 1f)

private const val SilenceRmsDb = -2f
private const val LoudRmsDb = 10f
