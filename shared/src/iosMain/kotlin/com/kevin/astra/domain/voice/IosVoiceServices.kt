package com.kevin.astra.domain.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual fun createSpeechRecognitionService(): SpeechRecognitionService = IosStubSpeechRecognitionService
actual fun createTextToSpeechService(): TextToSpeechService = IosStubTextToSpeechService

private object IosStubSpeechRecognitionService : SpeechRecognitionService {
    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()
    override fun startListening() {
        _state.value = SpeechRecognitionState.Error("Voice assistant coming soon on iOS")
    }
    override fun stopListening() = Unit
    override fun destroy() = Unit
}

private object IosStubTextToSpeechService : TextToSpeechService {
    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    override val state: StateFlow<TtsState> = _state.asStateFlow()
    override fun speak(text: String) = Unit
    override fun stop() = Unit
    override fun destroy() = Unit
}
