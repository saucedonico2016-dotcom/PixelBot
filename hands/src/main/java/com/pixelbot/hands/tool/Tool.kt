package com.pixelbot.hands.tool

import kotlinx.serialization.Serializable

@Serializable
data class ToolParameter(
    val name: String,
    val type: String, // "string", "number", "boolean", "array", "object"
    val description: String,
    val required: Boolean = true,
    val enumValues: List<String>? = null
)

@Serializable
data class Tool(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

@Serializable
sealed interface ToolResult {
    data class Success(val message: String, val data: Map<String, Any>? = null) : ToolResult
    data class Error(val message: String, val code: String? = null) : ToolResult
    data class ConfirmationRequired(val message: String, val toolName: String, val args: Map<String, Any>) : ToolResult
}

interface ToolExecutor {
    val tool: Tool
    suspend fun execute(args: Map<String, Any>): ToolResult
}

class ToolRegistry {
    private val tools = mutableMapOf<String, ToolExecutor>()
    
    fun register(executor: ToolExecutor) {
        tools[executor.tool.name] = executor
    }
    
    fun unregister(name: String) {
        tools.remove(name)
    }
    
    fun get(name: String): ToolExecutor? = tools[name]
    
    fun getAll(): List<ToolExecutor> = tools.values.toList()
    
    fun getToolsSchema(): List<Tool> = tools.values.map { it.tool }
    
    fun execute(name: String, args: Map<String, Any>): ToolResult {
        val executor = tools[name] ?: return ToolResult.Error("Herramienta no encontrada: $name")
        return try {
            executor.execute(args)
        } catch (e: Exception) {
            ToolResult.Error("Error ejecutando $name: ${e.message}")
        }
    }
}