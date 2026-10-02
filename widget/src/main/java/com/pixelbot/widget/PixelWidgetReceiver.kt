package com.pixelbot.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAppWidgetState
import com.pixelbot.body.model.BodyState
import com.pixelbot.memory.PixelDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PixelWidgetReceiver : GlanceAppWidget() {
    
    override fun provideGlance(context: Context, glanceId: String): PixelWidgetContent {
        return PixelWidgetContent(context)
    }
    
    companion object {
        fun updateAllWidgets(context: Context, state: BodyState) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, PixelWidgetReceiver::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            
            ids.forEach { id ->
                updateAppWidgetState(context, id) { preferences ->
                    preferences["widget_state"] = state.name
                }
            }
        }
        
        fun toggleBubble(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            intent.action = FloatingBubbleService.ACTION_TOGGLE
            context.startService(intent)
        }
        
        fun speakAction(context: Context) {
            val intent = Intent(context, PixelForegroundService::class.java)
            intent.action = PixelForegroundService.ACTION_TAP_TO_TALK
            context.startForegroundService(intent)
        }
    }
}

import com.pixelbot.body.service.FloatingBubbleService
import com.pixelbot.service.PixelForegroundService