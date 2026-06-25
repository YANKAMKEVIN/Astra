package com.kevin.astra.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface AstraState

interface AstraIntent

interface AstraEffect

abstract class AstraViewModel<State : AstraState, Intent : AstraIntent, Effect : AstraEffect>(
    initialState: State,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<State> = mutableState.asStateFlow()

    private val effectChannel = Channel<Effect>(capacity = Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun dispatch(intent: Intent) {
        handleIntent(intent)
    }

    protected abstract fun handleIntent(intent: Intent)

    protected fun updateState(reducer: State.() -> State) {
        mutableState.update(reducer)
    }

    protected fun emitEffect(effect: Effect) {
        viewModelScope.launch {
            effectChannel.send(effect)
        }
    }
}
