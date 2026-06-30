package com.kevin.astra.domain.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
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
    private val audioEngine = AVAudioEngine()
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

        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryRecord, error = null)
        session.setMode(AVAudioSessionModeMeasurement, error = null)
        session.setActive(true, error = null)

        val req = SFSpeechAudioBufferRecognitionRequest()
        req.shouldReportPartialResults = true
        request = req

        val inputNode = audioEngine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0u)
        inputNode.installTapOnBus(
            bus = 0u,
            bufferSize = 1024u,
            format = recordingFormat,
        ) { buffer, _ ->
            buffer?.let { req.appendAudioPCMBuffer(it) }
        }

        audioEngine.prepare()
        audioEngine.startAndReturnError(null)

        recognizer?.recognitionTaskWithRequest(req) { result, error ->
            when {
                error != null -> {
                    stopListening()
                    _state.value = SpeechRecognitionState.Error(error.localizedDescription)
                }
                result != null -> {
                    val text = result.bestTranscription.formattedString
                    if (result.isFinal()) {
                        stopListening()
                        _state.value = SpeechRecognitionState.Result(text)
                    } else {
                        _state.value = SpeechRecognitionState.Partial(text)
                    }
                }
            }
        }
    }

    override fun stopListening() {
        audioEngine.inputNode.removeTapOnBus(0u)
        audioEngine.stop()
        request?.endAudio()
        request = null
        isListening = false
        AVAudioSession.sharedInstance().setActive(false, error = null)
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
