package com.pixelbot.ears.wakeword

import android.content.Context
import android.content.SharedPreferences
import com.pixelbot.ears.model.TranscriptionEvent
import com.pixelbot.ears.model.TranscriptionEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WakeWordDetector(
    private val context: Context,
    private val eventBus: TranscriptionEventBus,
    private val prefs: SharedPreferences
) {
    private var wakeWord = prefs.getString("wake_word", "pixel") ?: "pixel"
    private var isActive = false
    private val partialBuffer = StringBuilder()
    
    suspend fun start() {
        isActive = true
    }
    
    fun stop() {
        isActive = false
        partialBuffer.clear()
    }
    
    fun updateWakeWord(newWakeWord: String) {
        wakeWord = newWakeWord.lowercase().trim()
        prefs.edit().putString("wake_word", wakeWord).apply()
    }
    
    fun getWakeWord(): String = wakeWord
    
    fun processPartial(text: String): Boolean {
        if (!isActive) return false
        
        partialBuffer.append(" ").append(text.lowercase())
        val buffer = partialBuffer.toString()
        
        if (buffer.contains(wakeWord)) {
            eventBus.send(TranscriptionEvent.WakeWordDetected)
            partialBuffer.clear()
            return true
        }
        
        if (buffer.length > 200) {
            partialBuffer.delete(0, buffer.length - 100)
        }
        
        return false
    }
    
    fun processFinal(text: String): Boolean {
        if (!isActive) return false
        
        val lowerText = text.lowercase().trim()
        if (lowerText.contains(wakeWord)) {
            eventBus.send(TranscriptionEvent.WakeWordDetected)
            return true
        }
        return false
    }
}