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

class CloudProvider(config: ProviderConfig) : AiProvider {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val jsonParser = JsonParser()
    
    override val name: String = config.name
    override val isAvailable: Boolean = config.apiKey != null && config.apiKey!!.isNotBlank()
    override val supportsTools: Boolean = true
    
    private val baseUrl = config.baseUrl ?: getDefaultBaseUrl(config.name)
    private val model = config.model
    private val apiKey = config.apiKey!!
    
    private fun getDefaultBaseUrl(providerName: String): String {
        return when (providerName.lowercase()) {
            "claude", "anthropic" -> "https://api.anthropic.com/v1/messages"
            "gpt", "openai" -> "https://api.openai.com/v1/chat/completions"
            "gemini", "google" -> "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent"
            "openrouter" -> "https://openrouter.ai/api/v1/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }
    
    override suspend fun chat(messages: List<ChatMessage>, tools: List<Tool>): AiResponse {
        if (!isAvailable) {
            return AiResponse(
                content = "Proveedor en la nube no configurado (falta API key)",
                toolCalls = null,
                finishReason = "error"
            )
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = buildRequestBody(messages, tools)
                val request = buildRequest(requestBody)
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    return@withContext AiResponse(
                        content = "Error HTTP ${response.code}: $errorBody",
                        toolCalls = null,
                        finishReason = "error"
                    )
                }
                
                val responseBody = response.body?.string() ?: ""
                parseResponse(responseBody)
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
                emit("Proveedor no configurado")
                return@flow
            }
            
            // Streaming implementation would go here
            val response = chat(messages, tools)
            response.content?.let { emit(it) }
        }
    }
    
    private fun buildRequestBody(messages: List<ChatMessage>, tools: List<Tool>): JsonObject {
        val json = JsonObject()
        
        when {
            baseUrl.contains("anthropic") -> buildAnthropicBody(json, messages, tools)
            baseUrl.contains("gemini") -> buildGeminiBody(json, messages, tools)
            else -> buildOpenAIBody(json, messages, tools) // OpenAI, OpenRouter, etc.
        }
        
        return json
    }
    
    private fun buildOpenAIBody(json: JsonObject, messages: List<ChatMessage>, tools: List<Tool>) {
        json.addProperty("model", model)
        json.addProperty("temperature", config.temperature)
        json.addProperty("max_tokens", config.maxTokens)
        
        val messagesArray = com.google.gson.JsonArray()
        for (msg in messages) {
            val m = JsonObject()
            m.addProperty("role", msg.role.name.lowercase())
            msg.content?.let { m.addProperty("content", it) }
            msg.toolCalls?.let { calls ->
                val callsArray = com.google.gson.JsonArray()
                for (call in calls) {
                    val c = JsonObject()
                    c.addProperty("id", call.id)
                    c.addProperty("type", "function")
                    val func = JsonObject()
                    func.addProperty("name", call.name)
                    func.addProperty("arguments", call.arguments)
                    c.add("function", func)
                    callsArray.add(c)
                }
                m.add("tool_calls", callsArray)
            }
            msg.toolCallId?.let { m.addProperty("tool_call_id", it) }
            msg.name?.let { m.addProperty("name", it) }
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
                    p.enumValues?.let { vals ->
                        val enumArr = com.google.gson.JsonArray()
                        vals.forEach { enumArr.add(it) }
                        prop.add("enum", enumArr)
                    }
                    properties.add(p.name, prop)
                    if (p.required) required.add(p.name)
                }
                func.add("parameters", params.apply { add("properties", properties); add("required", required) })
                t.add("function", func)
                toolsArray.add(t)
            }
            json.add("tools", toolsArray)
            json.addProperty("tool_choice", "auto")
        }
    }
    
    private fun buildAnthropicBody(json: JsonObject, messages: List<ChatMessage>, tools: List<Tool>) {
        json.addProperty("model", model)
        json.addProperty("max_tokens", config.maxTokens)
        json.addProperty("temperature", config.temperature)
        
        val systemPrompt = messages.firstOrNull { it.role == ChatMessage.Role.SYSTEM }?.content ?: ""
        if (systemPrompt.isNotBlank()) {
            json.addProperty("system", systemPrompt)
        }
        
        val messagesArray = com.google.gson.JsonArray()
        for (msg in messages.filter { it.role != ChatMessage.Role.SYSTEM }) {
            val m = JsonObject()
            m.addProperty("role", when (msg.role) {
                ChatMessage.Role.USER -> "user"
                ChatMessage.Role.ASSISTANT -> "assistant"
                ChatMessage.Role.TOOL -> "user" // Anthropic usa user para tool results
                else -> "user"
            })
            
            val contentArray = com.google.gson.JsonArray()
            val contentObj = JsonObject()
            contentObj.addProperty("type", "text")
            msg.content?.let { contentObj.addProperty("text", it) }
            contentArray.add(contentObj)
            m.add("content", contentArray)
            messagesArray.add(m)
        }
        json.add("messages", messagesArray)
        
        if (tools.isNotEmpty()) {
            val toolsArray = com.google.gson.JsonArray()
            for (tool in tools) {
                val t = JsonObject()
                t.addProperty("name", tool.name)
                t.addProperty("description", tool.description)
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
                params.add("properties", properties)
                params.add("required", required)
                t.add("input_schema", params)
                toolsArray.add(t)
            }
            json.add("tools", toolsArray)
        }
    }
    
    private fun buildGeminiBody(json: JsonObject, messages: List<ChatMessage>, tools: List<Tool>) {
        val contents = com.google.gson.JsonArray()
        for (msg in messages) {
            val c = JsonObject()
            c.addProperty("role", when (msg.role) {
                ChatMessage.Role.USER -> "user"
                ChatMessage.Role.ASSISTANT -> "model"
                else -> "user"
            })
            val parts = com.google.gson.JsonArray()
            val part = JsonObject()
            msg.content?.let { part.addProperty("text", it) }
            parts.add(part)
            c.add("parts", parts)
            contents.add(c)
        }
        json.add("contents", contents)
        
        val generationConfig = JsonObject()
        generationConfig.addProperty("temperature", config.temperature)
        generationConfig.addProperty("maxOutputTokens", config.maxTokens)
        json.add("generationConfig", generationConfig)
    }
    
    private fun buildRequest(body: JsonObject): Request {
        val requestBody = gson.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val builder = Request.Builder().url(baseUrl).post(requestBody)
        
        when {
            baseUrl.contains("anthropic") -> {
                builder.addHeader("x-api-key", apiKey)
                builder.addHeader("anthropic-version", "2023-06-01")
                builder.addHeader("content-type", "application/json")
            }
            baseUrl.contains("gemini") -> {
                builder.url("$baseUrl?key=$apiKey")
                builder.addHeader("content-type", "application/json")
            }
            else -> { // OpenAI, OpenRouter
                builder.addHeader("Authorization", "Bearer $apiKey")
                builder.addHeader("content-type", "application/json")
                if (baseUrl.contains("openrouter")) {
                    builder.addHeader("HTTP-Referer", "https://pixelbot.app")
                    builder.addHeader("X-Title", "PixelBot")
                }
            }
        }
        
        return builder.build()
    }
    
    private fun parseResponse(body: String): AiResponse {
        return try {
            val json = jsonParser.parse(body).asJsonObject
            
            when {
                baseUrl.contains("anthropic") -> parseAnthropic(json)
                baseUrl.contains("gemini") -> parseGemini(json)
                else -> parseOpenAI(json)
            }
        } catch (e: Exception) {
            AiResponse(
                content = "Error parseando respuesta: ${e.message}",
                toolCalls = null,
                finishReason = "error"
            )
        }
    }
    
    private fun parseOpenAI(json: com.google.gson.JsonObject): AiResponse {
        val choice = json.getAsJsonArray("choices")?.get(0)?.asJsonObject
        val message = choice?.getAsJsonObject("message")
        
        val content = message?.get("content")?.asString?.let { if (it.isNotBlank()) it } ?: null
        val toolCalls = message?.getAsJsonArray("tool_calls")?.let { arr ->
            val calls = mutableListOf<ToolCall>()
            for (tc in arr) {
                val obj = tc.asJsonObject
                val func = obj.getAsJsonObject("function")
                calls.add(ToolCall(
                    id = obj.get("id").asString,
                    name = func.get("name").asString,
                    arguments = func.get("arguments").asString
                ))
            }
            calls
        }
        
        val finishReason = choice?.get("finish_reason")?.asString ?: "stop"
        
        return AiResponse(
            content = content,
            toolCalls = toolCalls,
            finishReason = finishReason
        )
    }
    
    private fun parseAnthropic(json: com.google.gson.JsonObject): AiResponse {
        val content = json.getAsJsonArray("content")?.firstOrNull { it.asJsonObject.get("type").asString == "text" }
            ?.asJsonObject?.get("text")?.asString?.let { if (it.isNotBlank()) it }
        
        val toolCalls = json.getAsJsonArray("content")?.let { arr ->
            val calls = mutableListOf<ToolCall>()
            for (item in arr) {
                val obj = item.asJsonObject
                if (obj.get("type").asString == "tool_use") {
                    calls.add(ToolCall(
                        id = obj.get("id").asString,
                        name = obj.get("name").asString,
                        arguments = obj.get("input").toString()
                    ))
                }
            }
            calls
        }
        
        val finishReason = when (json.get("stop_reason")?.asString) {
            "tool_use" -> "tool_calls"
            "max_tokens" -> "length"
            else -> "stop"
        }
        
        return AiResponse(
            content = content,
            toolCalls = toolCalls,
            finishReason = finishReason
        )
    }
    
    private fun parseGemini(json: com.google.gson.JsonObject): AiResponse {
        val candidate = json.getAsJsonArray("candidates")?.get(0)?.asJsonObject
        val content = candidate?.getAsJsonObject("content")?.getAsJsonArray("parts")?.firstOrNull()
            ?.asJsonObject?.get("text")?.asString?.let { if (it.isNotBlank()) it }
        
        val finishReason = candidate?.get("finishReason")?.asString?.let {
            when (it) { "MAX_TOKENS" -> "length"; "TOOL_USE" -> "tool_calls"; else -> "stop" }
        } ?: "stop"
        
        // Gemini tool calls parsing would go here
        
        return AiResponse(
            content = content,
            toolCalls = null,
            finishReason = finishReason
        )
    }
    
    fun shutdown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}