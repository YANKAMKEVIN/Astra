package com.kevin.astra.domain.assistant

import com.kevin.astra.core.ai.GenerationResult

sealed interface StreamEvent {
    data class Token(val text: String) : StreamEvent
    data class Complete(val result: GenerationResult) : StreamEvent
    data class Error(val message: String) : StreamEvent
}
