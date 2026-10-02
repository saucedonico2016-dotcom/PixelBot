package com.pixelbot.brain.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OllamaProvider(config: ProviderConfig) : AiProvider {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // LLM local puede tardar
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val jsonParser = JsonParser()
    
    override val name: String = config.name
    override val isAvailable: Boolean = checkAvailability()
    override val supportsTools: Boolean = true // Ollama soporta function calling en modelos nuevos
    
    private val baseUrl = config.baseUrl ?: "http://192.168.1.100:11434"
    private val model = config.model
    
    private fun checkAvailability(): Boolean {
        try {
            val url = "$baseUrl/api/tags"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            return response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun chat(messages: List<ChatMessage>, tools: List<Tool>): AiResponse {
        if (!isAvailable) {
            return AiResponse(
                content = "Ollama no disponible en $baseUrl",
                toolCalls = null,
                finishReason = "error"
            )
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = buildOllamaBody(messages, tools)
                val request = Request.Builder()
                    .url("$baseUrl/api/chat")
                    .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    val error = response.body?.string() ?: "Unknown error"
                    return@withContext AiResponse(
                        content = "Error Ollama: $error",
                        toolCalls = null,
                        finishReason = "error"
                    )
                }
                
                val body = response.body?.string() ?: ""
                parseOllamaResponse(body)
            } catch (e: Exception) {
                AiResponse(
                    content = "Error Ollama: ${e.message}",
                    toolCalls = null,
                    finishReason = "error"
                )
            }
        }
    }
    
    override suspend fun chatStream(messages: List<ChatMessage>, tools: List<Tool>): kotlinx.coroutines.flow.Flow<String> {
        return kotlinx.coroutines.flow.flow {
            if (!isAvailable) {
                emit("Ollama no disponible")
                return@flow
            }
            
            val requestBody = buildOllamaBody(messages, tools).toBuilder().set("stream", true).build()
            val request = Request.Builder()
                .url("$baseUrl/api/chat")
                .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val source = response.body?.source()
            
            source?.use { src ->
                while (!src.exhausted()) {
                    val line = src.readUtf8LineStrict()
                    if (line.isNotBlank()) {
                        try {
                            val json = jsonParser.parse(line).asJsonObject
                            val message = json.getAsJsonObject("message")
                            val content = message?.get("content")?.asString
                            if (content != null && content.isNotBlank()) {
                                emit(content)
                            }
                            val done = json.get("done")?.asBoolean ?: false
                            if (done) break
                        } catch (e: Exception) {
                            // Ignorar líneas malformadas
                        }
                    }
                }
            }
        }
    }
    
    private fun buildOllamaBody(messages: List<ChatMessage>, tools: List<Tool>): JsonObject {
        val json = JsonObject()
        json.addProperty("model", model)
        json.addProperty("stream", false)
        
        val options = JsonObject()
        options.addProperty("temperature", config.temperature)
        options.addProperty("num_predict", config.maxTokens)
        json.add("options", options)
        
        val messagesArray = com.google.gson.JsonArray()
        for (msg in messages) {
            val m = JsonObject()
            m.addProperty("role", when (msg.role) {
                ChatMessage.Role.SYSTEM -> "system"
                ChatMessage.Role.USER -> "user"
                ChatMessage.Role.ASSISTANT -> "assistant"
                ChatMessage.Role.TOOL -> "tool"
            })
            msg.content?.let { m.addProperty("content", it) }
            msg.toolCalls?.let { calls ->
                val toolCallsArray = com.google.gson.JsonArray()
                for (call in calls) {
                    val tc = JsonObject()
                    tc.addProperty("name", call.name)
                    val args = JsonParser.parseString(call.arguments).asJsonObject
                    tc.add("arguments", args)
                    toolCallsArray.add(tc)
                }
                m.add("tool_calls", toolCallsArray)
            }
            messagesArray.add(m)
        }
        json.add("messages", messagesArray)
        
        if (tools.isNotEmpty() && supportsTools) {
            val toolsArray = com.google.gson.JsonArray()
            for (tool in tools) {
                val t = JsonObject()
                t.addProperty("type", "function")
                val func = JsonObject()
                func.addProperty("name", tool.name)
                func.addProperty("description", tool.description)
                val params = JsonObject()
                params.addProperty("type", "object")
                val properties = JsonObject()
                val required = com.google.gson.JsonArray()
                for (p in tool.parameters) {
                    val prop = JsonObject()
                    prop.addProperty("type", p.type)
                    prop.addProperty("description", p.description)
                    properties.add(p.name, prop)
                    if (p.required) required.add(p.name)
                }
                func.add("parameters", params.apply { add("properties", properties); add("required", required) })
                t.add("function", func)
                toolsArray.add(t)
            }
            json.add("tools", toolsArray)
        }
        
        return json
    }
    
    private fun parseOllamaResponse(body: String): AiResponse {
        return try {
            val json = jsonParser.parse(body).asJsonObject
            val message = json.getAsJsonObject("message")
            
            val content = message?.get("content")?.asString?.let { if (it.isNotBlank()) it }
            
            val toolCalls = message?.getAsJsonArray("tool_calls")?.let { arr ->
                val calls = mutableListOf<ToolCall>()
                for (tc in arr) {
                    val obj = tc.asJsonObject
                    val func = obj.getAsJsonObject("function")
                    calls.add(ToolCall(
                        id = "call_${System.currentTimeMillis()}",
                        name = func.get("name").asString,
                        arguments = func.get("arguments").toString()
                    ))
                }
                calls
            }
            
            val finishReason = if (json.get("done")?.asBoolean == true) {
                if (toolCalls != null && toolCalls.isNotEmpty()) "tool_calls" else "stop"
            } else "length"
            
            AiResponse(
                content = content,
                toolCalls = toolCalls,
                finishReason = finishReason
            )
        } catch (e: Exception) {
            AiResponse(
                content = "Error parseando Ollama: ${e.message}",
                toolCalls = null,
                finishReason = "error"
            )
        }
    }
    
    fun shutdown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}