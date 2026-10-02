package com.pixelbot.ears

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import com.pixelbot.ears.model.TranscriptionEvent
import com.pixelbot.ears.model.TranscriptionEventBus
import com.pixelbot.ears.model.TranscriptionListener
import com.pixelbot.ears.stt.SystemRecognizer
import com.pixelbot.ears.stt.VoskRecognizer
import com.pixelbot.ears.tts.TtsEngine
import com.pixelbot.ears.wakeword.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EarsModule(
    private val context: Context
) : TranscriptionListener {
    
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val eventBus = TranscriptionEventBus()
    private val voskRecognizer: VoskRecognizer
    private val systemRecognizer: SystemRecognizer
    private val wakeWordDetector: WakeWordDetector
    private val ttsEngine: TtsEngine
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Audio recording
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isWakeWordMode = true // true = listening for wake word, false = full transcription
    private val sampleRate = 16000
    private val bufferSize: Int
    
    // Timeouts
    private val silenceTimeoutMs = 3000L
    private val maxRecordingMs = 30000L
    private var lastVoiceTime = 0L
    private var recordingStartTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val silenceRunnable = Runnable { checkSilenceTimeout() }
    
    init {
        bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)
        
        voskRecognizer = VoskRecognizer(context, eventBus)
        systemRecognizer = SystemRecognizer(context, eventBus)
        wakeWordDetector = WakeWordDetector(context, eventBus, prefs)
        ttsEngine = TtsEngine(context)
    }
    
    override val events: ReceiveChannel<TranscriptionEvent> = eventBus.events
    
    suspend fun initialize(): Boolean {
        val voskReady = voskRecognizer.initialize()
        val systemReady = systemRecognizer.initialize()
        
        ttsEngine.initialize { success ->
            Log.d("EarsModule", "TTS inicializado: $success")
        }
        
        wakeWordDetector.start()
        
        return voskReady || systemReady
    }
    
    override fun startListening() {
        if (isRecording) return
        
        startAudioRecording()
        isWakeWordMode = true
        voskRecognizer.startListening()
        
        recordingStartTime = System.currentTimeMillis()
        lastVoiceTime = System.currentTimeMillis()
        scheduleSilenceCheck()
    }
    
    override fun stopListening() {
        stopAudioRecording()
        voskRecognizer.stopListening()
        systemRecognizer.stopListening()
        handler.removeCallbacks(silenceRunnable)
    }
    
    override fun onWakeWordDetected() {
        // Switch from wake word mode to full transcription mode
        isWakeWordMode = false
        handler.removeCallbacks(silenceRunnable)
        
        // Restart with longer timeout for actual command
        lastVoiceTime = System.currentTimeMillis()
        recordingStartTime = System.currentTimeMillis()
        scheduleSilenceCheck()
    }
    
    override fun onTapToTalk() {
        // Direct transcription mode (bypass wake word)
        isWakeWordMode = false
        startListening()
    }
    
    private fun startAudioRecording() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        ).also {
            it.startRecording()
            isRecording = true
            scope.launch { recordLoop() }
        }
    }
    
    private fun stopAudioRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    private suspend fun recordLoop() {
        val buffer = ByteArray(bufferSize)
        
        while (isRecording) {
            val read = withContext(Dispatchers.IO) {
                audioRecord?.read(buffer, 0, bufferSize) ?: -1
            }
            
            if (read > 0) {
                processAudio(buffer, read)
            } else if (read < 0) {
                Log.e("EarsModule", "Error leyendo audio: $read")
                break
            }
        }
    }
    
    private fun processAudio(buffer: ByteArray, length: Int) {
        val hasVoice = detectVoiceActivity(buffer, length)
        
        if (hasVoice) {
            lastVoiceTime = System.currentTimeMillis()
        }
        
        if (isWakeWordMode) {
            // Solo procesar parciales para wake word
            if (voskRecognizer.acceptWaveform(buffer, 0, length)) {
                val partial = voskRecognizer.getPartialResult()
                // Parse JSON para obtener texto parcial
                parsePartial(partial)?.let { text ->
                    eventBus.send(TranscriptionEvent.Partial(text))
                    wakeWordDetector.processPartial(text)
                }
            } else {
                val partial = voskRecognizer.getPartialResult()
                parsePartial(partial)?.let { text ->
                    wakeWordDetector.processPartial(text)
                }
            }
        } else {
            // Modo transcripción completa
            if (voskRecognizer.acceptWaveform(buffer, 0, length)) {
                val finalResult = voskRecognizer.getFinalResult()
                parseFinal(finalResult)?.let { text ->
                    eventBus.send(TranscriptionEvent.Final(text, 0.9f))
                    wakeWordDetector.processFinal(text)
                }
            } else {
                val partial = voskRecognizer.getPartialResult()
                parsePartial(partial)?.let { text ->
                    eventBus.send(TranscriptionEvent.Partial(text))
                }
            }
        }
    }
    
    private fun detectVoiceActivity(buffer: ByteArray, length: Int): Boolean {
        // Simple VAD basado en energía
        var sum = 0L
        for (i in 0 until length step 2) {
            val sample = buffer[i].toInt() + (buffer[i + 1].toInt() shl 8)
            sum += (sample * sample).toLong()
        }
        val avgEnergy = sum / (length / 2)
        return avgEnergy > 500000 // Threshold ajustable
    }
    
    private fun parsePartial(json: String): String? {
        return try {
            val start = json.indexOf("\"partial\":\"") + 11
            val end = json.indexOf("\"", start)
            if (start > 10 && end > start) json.substring(start, end) else null
        } catch (e: Exception) { null }
    }
    
    private fun parseFinal(json: String): String? {
        return try {
            val start = json.indexOf("\"text\":\"") + 8
            val end = json.indexOf("\"", start)
            if (start > 7 && end > start) json.substring(start, end) else null
        } catch (e: Exception) { null }
    }
    
    private fun checkSilenceTimeout() {
        val now = System.currentTimeMillis()
        val silenceDuration = now - lastVoiceTime
        val totalDuration = now - recordingStartTime
        
        if (silenceDuration > silenceTimeoutMs) {
            eventBus.send(TranscriptionEvent.SilenceDetected)
            stopListening()
        } else if (totalDuration > maxRecordingMs) {
            eventBus.send(TranscriptionEvent.Timeout)
            stopListening()
        } else {
            scheduleSilenceCheck()
        }
    }
    
    private fun scheduleSilenceCheck() {
        handler.postDelayed(silenceRunnable, 500)
    }
    
    fun speak(text: String): Boolean {
        return ttsEngine.speak(text)
    }
    
    fun stopSpeaking() {
        ttsEngine.stop()
    }
    
    fun isTtsSpeaking(): Boolean = ttsEngine.isSpeaking()
    
    fun setWakeWord(wakeWord: String) {
        wakeWordDetector.updateWakeWord(wakeWord)
    }
    
    fun getWakeWord(): String = wakeWordDetector.getWakeWord()
    
    fun useSystemRecognizer(fallback: Boolean = true) {
        // TODO: Implementar fallback automático
    }
    
    fun shutdown() {
        stopListening()
        voskRecognizer.release()
        systemRecognizer.release()
        ttsEngine.shutdown()
        wakeWordDetector.stop()
        eventBus.close()
        scope.cancel()
    }
}