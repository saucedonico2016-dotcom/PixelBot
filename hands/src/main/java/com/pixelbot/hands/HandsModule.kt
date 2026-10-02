package com.pixelbot.hands

import android.content.Context
import com.pixelbot.hands.intent.*
import com.pixelbot.hands.tool.Tool
import com.pixelbot.hands.tool.ToolExecutor
import com.pixelbot.hands.tool.ToolRegistry
import com.pixelbot.hands.tool.ToolResult

class HandsModule(private val context: Context) {
    private val registry = ToolRegistry()
    
    init {
        registerIntentTools()
    }
    
    private fun registerIntentTools() {
        val tools: List<ToolExecutor> = listOf(
            OpenAppTool(context),
            CallContactTool(context),
            SendWhatsAppTool(context),
            SendSmsTool(context),
            SetAlarmTool(context),
            SetTimerTool(context),
            FlashlightTool(context),
            VolumeBrightnessTool(context),
            WifiBluetoothPanelTool(context),
            NavigateTool(context),
            SearchWebTool(context),
            MediaControlTool(context),
            TakePhotoTool(context)
        )
        
        tools.forEach { registry.register(it) }
    }
    
    fun getRegistry(): ToolRegistry = registry
    
    fun getToolsSchema(): List<Tool> = registry.getToolsSchema()
    
    suspend fun executeTool(name: String, args: Map<String, Any>): ToolResult {
        return registry.execute(name, args)
    }
    
    fun getTool(name: String): ToolExecutor? = registry.get(name)
}