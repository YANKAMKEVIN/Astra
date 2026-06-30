package com.kevin.astra.domain.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private lateinit var applicationContext: Context

fun initializeAndroidSpeechRecognitionService(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createSpeechRecognitionService(): SpeechRecognitionService =
    if (::applicationContext.isInitialized) AndroidSpeechRecognitionService(applicationContext)
    else UnavailableSpeechRecognitionService

private class AndroidSpeechRecognitionService(private val context: Context) : SpeechRecognitionService {

    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun startListening() {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.value = SpeechRecognitionState.Error("Speech recognition not available on this device")
                return@post
            }
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
                sr.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = SpeechRecognitionState.Listening
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: Bundle?) {
                        val text = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            _state.value = SpeechRecognitionState.Partial(text)
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val text = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                        _state.value = if (!text.isNullOrBlank()) SpeechRecognitionState.Result(text)
                        else SpeechRecognitionState.Error("No speech detected")
                    }
                    override fun onError(error: Int) {
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected — try speaking again"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out"
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network required — enable offline speech in Android Settings → Languages → Offline speech recognition"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy, please retry"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            else -> "Speech recognition error (code $error)"
                        }
                        _state.value = SpeechRecognitionState.Error(msg)
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }
                sr.startListening(intent)
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            recognizer?.stopListening()
            _state.value = SpeechRecognitionState.Idle
        }
    }

    override fun destroy() {
        mainHandler.post {
            recognizer?.destroy()
            recognizer = null
        }
    }
}

private object UnavailableSpeechRecognitionService : SpeechRecognitionService {
    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()
    override fun startListening() {
        _state.value = SpeechRecognitionState.Error("Speech recognition not initialized")
    }
    override fun stopListening() = Unit
    override fun destroy() = Unit
}
