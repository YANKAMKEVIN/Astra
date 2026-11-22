package com.kevin.astra.domain.voice

import kotlin.test.Test
import kotlin.test.assertEquals

class IosTextToSpeechServiceTest {

    // stop() used to cast 0L to the AVSpeechBoundary enum, which threw as soon as it ran:
    // leaving the Voice screen (destroy) or interrupting a spoken answer crashed the app.
    @Test
    fun stoppingAndDestroyingTheVoiceGoBackToIdleWithoutCrashing() {
        val tts = createTextToSpeechService()

        tts.stop()
        assertEquals(TtsState.Idle, tts.state.value)

        tts.destroy()
        assertEquals(TtsState.Idle, tts.state.value)
    }
}
