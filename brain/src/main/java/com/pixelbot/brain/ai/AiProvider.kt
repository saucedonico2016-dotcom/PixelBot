package com.pixelbot.brain.ai

import com.pixelbot.hands.tool.Tool
import kotlinx.coroutines.flow.Flow

interface AiProvider {
    val name: String
    val isAvailable: Boolean
    val supportsTools: Boolean
    
    suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<Tool>
    ): AiResponse
    
    suspend fun chatStream(
        messages: List<ChatMessage>,
        tools: List<Tool>
    ): Flow<String>
}

@Serializable
data class ChatMessage(
    val role: Role,
    val content: String?,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val name: String? = null
) {
    enum class Role { SYSTEM, USER, ASSISTANT, TOOL }
}

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String // JSON string
)

@Serializable
data class AiResponse(
    val content: String?,           // Texto de respuesta
    val toolCalls: List<ToolCall>?, // Llamadas a herramientas
    val finishReason: String?,      // "stop", "tool_calls", "length", "error"
    val usage: Usage? = null
)

@Serializable
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

enum class ProviderType {
    LOCAL,      // Google AI Edge / llama.cpp
    CLOUD,      // Claude / GPT / Gemini / OpenRouter
    OLLAMA      // Servidor Ollama local
}

@Serializable
data class ProviderConfig(
    val type: ProviderType,
    val name: String,
    val model: String,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val enabled: Boolean = true,
    val priority: Int = 0, // menor = mayor prioridad
    val maxTokens: Int = 2048,
    val temperature: Float = 0.7f
)