package com.pixelbot.brain.ai

import android.content.Context
import com.google.ai.edge.llm.InferenceModel
import com.google.ai.edge.llm.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class LocalProvider(
    private val context: Context,
    config: ProviderConfig
) : AiProvider {
    
    private var llmInference: LlmInference? = null
    private val json = Json { ignoreUnknownKeys = true }
    
    override val name: String = config.name
    override val isAvailable: Boolean get() = llmInference != null
    override val supportsTools: Boolean = false // Modelos locales simples no suelen soportar function calling
    
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Usar Google AI Edge (MediaPipe LLM Inference) o llama.cpp
                val modelPath = File(context.filesDir, "models/${config.model}.task")
                if (!modelPath.exists()) {
                    // Copiar desde assets o descargar
                    copyModelFromAssets(modelPath)
                }
                
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath.absolutePath)
                    .setMaxTokens(config.maxTokens)
                    .setTemperature(config.temperature)
                    .build()
                
                llmInference = LlmInference.createFromOptions(context, options)
                true
            } catch (e: Exception) {
                android.util.Log.e("LocalProvider", "Error inicializando modelo local", e)
                false
            }
        }
    }
    
    private fun copyModelFromAssets(dest: File) {
        dest.parentFile.mkdirs()
        context.assets.open("models/${config.model}.task").use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    
    override suspend fun chat(messages: List<ChatMessage>, tools: List<Tool>): AiResponse {
        if (!isAvailable) {
            return AiResponse(
                content = "Modelo local no disponible",
                toolCalls = null,
                finishReason = "error"
            )
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(messages)
                val response = llmInference?.generateResponse(prompt) ?: ""
                
                AiResponse(
                    content = response,
                    toolCalls = null,
                    finishReason = "stop"
                )
            } catch (e: Exception) {
                AiResponse(
                    content = "Error: ${e.message}",
                    toolCalls = null,
                    finishReason = "error"
                )
            }
        }
    }
    
    override suspend fun chatStream(messages: List<ChatMessage>, tools: List<Tool>): kotlinx.coroutines.flow.Flow<String> {
        return kotlinx.coroutines.flow.flow {
            if (!isAvailable) {
                emit("Modelo local no disponible")
                return@flow
            }
            
            val prompt = buildPrompt(messages)
            val response = llmInference?.generateResponse(prompt) ?: ""
            
            // Simular streaming dividiendo en chunks
            response.chunked(50).forEach { chunk ->
                emit(chunk)
            }
        }
    }
    
    private fun buildPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        for (msg in messages) {
            when (msg.role) {
                ChatMessage.Role.SYSTEM -> sb.append("System: ${msg.content}\n\n")
                ChatMessage.Role.USER -> sb.append("User: ${msg.content}\n\n")
                ChatMessage.Role.ASSISTANT -> sb.append("Assistant: ${msg.content}\n\n")
                ChatMessage.Role.TOOL -> sb.append("Tool result: ${msg.content}\n\n")
            }
        }
        sb.append("Assistant: ")
        return sb.toString()
    }
    
    fun shutdown() {
        llmInference?.close()
        llmInference = null
    }
}

import java.io.File