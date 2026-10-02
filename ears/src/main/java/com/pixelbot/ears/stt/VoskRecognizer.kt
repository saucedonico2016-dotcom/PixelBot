package com.pixelbot.ears.stt

import android.content.Context
import android.util.Log
import com.alphacephei.vosk.Model
import com.alphacephei.vosk.Recognizer
import com.pixelbot.ears.model.TranscriptionEvent
import com.pixelbot.ears.model.TranscriptionEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class VoskRecognizer(
    private val context: Context,
    private val eventBus: TranscriptionEventBus,
    private val modelPath: String = "vosk-model-es-0.42",
    private val sampleRate: Float = 16000f
) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var isListening = false
    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = File(context.filesDir, modelPath)
                if (!modelFile.exists()) {
                    extractModelFromAssets(modelFile)
                }
                
                model = Model(modelFile.absolutePath)
                recognizer = Recognizer(model!!, sampleRate)
                Log.d("VoskRecognizer", "Modelo Vosk cargado correctamente")
                true
            } catch (e: Exception) {
                Log.e("VoskRecognizer", "Error inicializando Vosk", e)
                eventBus.send(TranscriptionEvent.Error("Error cargando modelo Vosk: ${e.message}"))
                false
            }
        }
    }
    
    private fun extractModelFromAssets(modelDir: File) {
        modelDir.mkdirs()
        val assets = context.assets
        val files = assets.list("vosk-model") ?: return
        
        for (file in files) {
            val input = assets.open("vosk-model/$file")
            val output = FileOutputStream(File(modelDir, file))
            input.copyTo(output)
            input.close()
            output.close()
        }
    }
    
    fun acceptWaveform(audioData: ByteArray, offset: Int, length: Int): Boolean {
        return recognizer?.acceptWaveform(audioData, offset, length) ?: false
    }
    
    fun getPartialResult(): String {
        return recognizer?.partialResult() ?: "{}"
    }
    
    fun getFinalResult(): String {
        return recognizer?.finalResult() ?: "{}"
    }
    
    fun startListening() {
        isListening = true
        eventBus.send(TranscriptionEvent.ListeningStarted)
    }
    
    fun stopListening() {
        isListening = false
        eventBus.send(TranscriptionEvent.ListeningStopped)
    }
    
    fun isInitialized(): Boolean = model != null && recognizer != null
    
    fun release() {
        recognizer?.close()
        model?.close()
        recognizer = null
        model = null
        scope.cancel()
    }
}