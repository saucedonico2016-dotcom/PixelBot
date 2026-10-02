package com.pixelbot.ears.stt

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.pixelbot.ears.model.TranscriptionEvent
import com.pixelbot.ears.model.TranscriptionEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemRecognizer(
    private val context: Context,
    private val eventBus: TranscriptionEventBus
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    fun initialize(): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("SystemRecognizer", "Reconocedor del sistema no disponible")
            return false
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            
            override fun onBeginningOfSpeech() {
                eventBus.send(TranscriptionEvent.ListeningStarted)
            }
            
            override fun onRmsChanged(rmsdB: Float) {}
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {}
            
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de red"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                    SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout de voz"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció voz"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sin permisos"
                    else -> "Error $error"
                }
                eventBus.send(TranscriptionEvent.Error(msg))
            }
            
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    eventBus.send(TranscriptionEvent.Final(text, 0.9f))
                }
            }
            
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    eventBus.send(TranscriptionEvent.Partial(text))
                }
            }
            
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        
        return true
    }
    
    suspend fun startListening(language: String = "es-AR") {
        return withContext(Dispatchers.IO) {
            if (isListening) return@withContext
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            
            speechRecognizer?.startListening(intent)
            isListening = true
        }
    }
    
    suspend fun stopListening() {
        return withContext(Dispatchers.IO) {
            speechRecognizer?.stopListening()
            isListening = false
            eventBus.send(TranscriptionEvent.ListeningStopped)
        }
    }
    
    fun isListening(): Boolean = isListening
    
    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}