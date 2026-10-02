package com.pixelbot.ears.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

sealed interface TranscriptionEvent {
    data class Partial(val text: String) : TranscriptionEvent
    data class Final(val text: String, val confidence: Float) : TranscriptionEvent
    data class Error(val message: String) : TranscriptionEvent
    data class Timeout : TranscriptionEvent
    object SilenceDetected : TranscriptionEvent
    object WakeWordDetected : TranscriptionEvent
    object ListeningStarted : TranscriptionEvent
    object ListeningStopped : TranscriptionEvent
}

interface TranscriptionListener {
    val events: ReceiveChannel<TranscriptionEvent>
    fun startListening()
    fun stopListening()
    fun onWakeWordDetected()
    fun onTapToTalk()
}

class TranscriptionEventBus {
    private val _channel = Channel<TranscriptionEvent>(Channel.UNLIMITED)
    val events: ReceiveChannel<TranscriptionEvent> = _channel
    
    fun send(event: TranscriptionEvent) {
        _channel.trySend(event)
    }
    
    fun close() {
        _channel.close()
    }
}