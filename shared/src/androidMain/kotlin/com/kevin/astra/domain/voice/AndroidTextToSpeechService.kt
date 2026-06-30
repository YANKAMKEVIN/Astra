package com.kevin.astra.domain.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private lateinit var applicationContext: Context

fun initializeAndroidTextToSpeechService(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createTextToSpeechService(): TextToSpeechService =
    if (::applicationContext.isInitialized) AndroidTextToSpeechService(applicationContext)
    else UnavailableTextToSpeechService

private class AndroidTextToSpeechService(context: Context) : TextToSpeechService {

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale.getDefault()
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.value = TtsState.Speaking
                    }
                    override fun onDone(utteranceId: String?) {
                        _state.value = TtsState.Done
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = TtsState.Error("TTS playback error")
                    }
                })
            } else {
                _state.value = TtsState.Error("TTS engine failed to initialize")
            }
        }
    }

    override fun speak(text: String) {
        _state.value = TtsState.Speaking
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun stop() {
        tts?.stop()
        _state.value = TtsState.Idle
    }

    override fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private companion object {
        const val UTTERANCE_ID = "astra-voice-response"
    }
}

private object UnavailableTextToSpeechService : TextToSpeechService {
    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    override val state: StateFlow<TtsState> = _state.asStateFlow()
    override fun speak(text: String) = Unit
    override fun stop() = Unit
    override fun destroy() = Unit
}
