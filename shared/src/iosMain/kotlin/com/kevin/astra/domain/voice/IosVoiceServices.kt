package com.kevin.astra.domain.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceMinimumSpeechRate
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus

actual fun createSpeechRecognitionService(): SpeechRecognitionService = IosSpeechRecognitionService()
actual fun createTextToSpeechService(): TextToSpeechService = IosTextToSpeechService()

// ── STT ─────────────────────────────────────────────────────────────────────

private class IosSpeechRecognitionService : SpeechRecognitionService {
    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    private val recognizer = SFSpeechRecognizer(locale = NSLocale.currentLocale)
    private var request: SFSpeechAudioBufferRecognitionRequest? = null
    private var isListening = false

    override fun startListening() {
        SFSpeechRecognizer.requestAuthorization { status ->
            when (status) {
                SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized ->
                    beginRecognition()
                else ->
                    _state.value = SpeechRecognitionState.Error(
                        "Microphone permission denied. Enable Speech Recognition in Settings."
                    )
            }
        }
    }

    private fun beginRecognition() {
        if (isListening) return
        isListening = true
        _state.value = SpeechRecognitionState.Listening

        val req = SFSpeechAudioBufferRecognitionRequest()
        req.shouldReportPartialResults = true
        request = req

        recognizer?.recognitionTaskWithRequest(req) { result, error ->
            when {
                error != null -> {
                    isListening = false
                    _state.value = SpeechRecognitionState.Error(error.localizedDescription)
                }
                result != null -> {
                    val text = result.bestTranscription.formattedString
                    if (result.isFinal()) {
                        isListening = false
                        _state.value = SpeechRecognitionState.Result(text)
                    } else {
                        _state.value = SpeechRecognitionState.Partial(text)
                    }
                }
            }
        }
    }

    override fun stopListening() {
        request?.endAudio()
        request = null
        isListening = false
        _state.value = SpeechRecognitionState.Idle
    }

    override fun destroy() = stopListening()
}

// ── TTS ─────────────────────────────────────────────────────────────────────

private class IosTextToSpeechService : TextToSpeechService {
    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private val synthesizer = AVSpeechSynthesizer()

    override fun speak(text: String) {
        if (text.isBlank()) return
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.rate = AVSpeechUtteranceMinimumSpeechRate * 2.5f
        _state.value = TtsState.Speaking
        synthesizer.speakUtterance(utterance)
        // Emit Done immediately so the VM returns to Idle phase
        _state.value = TtsState.Done
    }

    override fun stop() {
        // 0L = AVSpeechBoundaryImmediate
        @Suppress("UNCHECKED_CAST")
        synthesizer.stopSpeakingAtBoundary(0L as platform.AVFAudio.AVSpeechBoundary)
        _state.value = TtsState.Idle
    }

    override fun destroy() = stop()
}
