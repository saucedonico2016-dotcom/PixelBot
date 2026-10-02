package com.pixelbot.ears.tts

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.Locale

sealed interface TtsEvent {
    data class Started(val utteranceId: String) : TtsEvent
    data class Done(val utteranceId: String) : TtsEvent
    data class Error(val utteranceId: String, val errorCode: Int) : TtsEvent
}

class TtsEngine(
    private val context: Context,
    private val language: Locale = Locale("es", "AR")
) {
    private var tts: TextToSpeech? = null
    private val _events = Channel<TtsEvent>(Channel.UNLIMITED)
    val events: ReceiveChannel<TtsEvent> = _events
    private var isInitialized = false
    private var initCallback: ((Boolean) -> Unit)? = null
    
    fun initialize(callback: (Boolean) -> Unit) {
        initCallback = callback
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(language)
                if (result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                    isInitialized = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) {
                            _events.trySend(TtsEvent.Started(utteranceId))
                        }
                        override fun onDone(utteranceId: String) {
                            _events.trySend(TtsEvent.Done(utteranceId))
                        }
                        override fun onError(utteranceId: String, error: Int) {
                            _events.trySend(TtsEvent.Error(utteranceId, error))
                        }
                    })
                    callback(true)
                } else {
                    Log.w("TtsEngine", "Idioma no soportado: $language")
                    callback(false)
                }
            } else {
                Log.e("TtsEngine", "Error inicializando TTS")
                callback(false)
            }
        }
    }
    
    fun speak(text: String, utteranceId: String = "tts_${System.currentTimeMillis()}"): Boolean {
        if (!isInitialized || tts == null) return false
        
        val params = android.os.Bundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
        }
        
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        }
        
        return result == TextToSpeech.SUCCESS
    }
    
    fun stop() {
        tts?.stop()
    }
    
    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
        _events.close()
    }
    
    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false
    
    fun setLanguage(locale: Locale): Boolean {
        return tts?.setLanguage(locale) == TextToSpeech.SUCCESS
    }
    
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.1f, 2.0f))
    }
    
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.1f, 2.0f))
    }
}