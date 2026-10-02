package com.pixelbot.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.glance.ColorProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideGlance
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.size.dp
import androidx.glance.size.px
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import com.pixelbot.body.model.BodyState
import com.pixelbot.memory.PixelDatabase
import com.pixelbot.memory.dao.SettingsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PixelWidgetContent(private val context: Context) : GlanceAppWidget() {
    
    @Composable
    override fun Content() {
        var currentState by remember { mutableStateOf(BodyState.DORMIDO) }
        var showBubble by remember { mutableStateOf(true) }
        
        // Leer estado guardado
        val prefs = getGlancePreferences(context)
        val savedState = prefs.getString("widget_state", BodyState.DORMIDO.name)
        currentState = BodyState.valueOf(savedState)
        
        val colors = ColorProvider(
            background = if (currentState == BodyState.DORMIDO) 0xFF1A1A2E else 0xFF16213E,
            primary = 0xFF6C5CE7,
            secondary = 0xFF00CEC9,
            onBackground = 0xFFEEEEEE,
            onPrimary = 0xFFFFFFFF
        )
        
        provideGlance(
            modifier = GlanceModifier.fillMaxSize().background(colors.background),
            content = {
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = androidx.glance.layout.Arrangement.spacedBy(12.dp),
                    horizontalAlignment = androidx.glance.layout.Alignment.CenterHorizontally
                ) {
                    // Personaje - frame estático según estado
                    Box(
                        modifier = GlanceModifier
                            .size(120.dp)
                            .background(getStateColor(currentState), androidx.glance.layout.RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        // Aquí iría una imagen del frame idle del estado actual
                        // Por ahora usamos texto representativo
                        androidx.glance.text.Text(
                            text = getStateEmoji(currentState),
                            fontSize = 48.sp,
                            textAlign = TextAlign.Center,
                            modifier = GlanceModifier.fillMaxSize().wrapContent()
                        )
                    }
                    
                    // Nombre del estado
                    androidx.glance.text.Text(
                        text = getStateLabel(currentState),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center
                    )
                    
                    // Botón grande HABLAR
                    androidx.glance.layout.Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(colors.primary, androidx.glance.layout.RoundedCornerShape(12.dp))
                            .fillMaxWidth()
                            .clickable(
                                actionStartActivity(
                                    Intent(context, com.pixelbot.service.PixelForegroundService::class.java).apply {
                                        action = com.pixelbot.service.PixelForegroundService.ACTION_TAP_TO_TALK
                                    }
                                )
                            )
                    ) {
                        androidx.glance.text.Text(
                            text = "🎤  HABLAR",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary,
                            textAlign = TextAlign.Center,
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .wrapContent()
                        )
                    }
                    
                    // Botón toggle burbuja
                    androidx.glance.layout.Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(if (showBubble) colors.secondary else 0xFF3D3D5C, androidx.glance.layout.RoundedCornerShape(12.dp))
                            .fillMaxWidth()
                            .clickable(
                                actionStartService(
                                    Intent(context, com.pixelbot.body.service.FloatingBubbleService::class.java).apply {
                                        action = com.pixelbot.body.service.FloatingBubbleService.ACTION_TOGGLE
                                    }
                                )
                            )
                    ) {
                        androidx.glance.text.Text(
                            text = if (showBubble) "👁️ Ocultar burbuja" else "👁️ Mostrar burbuja",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .wrapContent()
                        )
                    }
                }
            }
        )
    }
    
    private fun getStateColor(state: BodyState): Int {
        return when (state) {
            BodyState.DORMIDO -> 0xFF6C5CE7
            BodyState.ESCUCHANDO -> 0xFF00CEC9
            BodyState.PENSANDO -> 0xFFFFD93D
            BodyState.EJECUTANDO -> 0xFF00B5B0
            BodyState.HABLANDO -> 0xFFFF6B6B
            BodyState.FELIZ -> 0xFF00E676
            BodyState.ERROR -> 0xFFFF1744
            BodyState.CONFUNDIDO -> 0xFFAA00FF
        }
    }
    
    private fun getStateEmoji(state: BodyState): String {
        return when (state) {
            BodyState.DORMIDO -> "😴"
            BodyState.ESCUCHANDO -> "👂"
            BodyState.PENSANDO -> "🤔"
            BodyState.EJECUTANDO -> "⚡"
            BodyState.HABLANDO -> "💬"
            BodyState.FELIZ -> "😊"
            BodyState.ERROR -> "❌"
            BodyState.CONFUNDIDO -> "😕"
        }
    }
    
    private fun getStateLabel(state: BodyState): String {
        return when (state) {
            BodyState.DORMIDO -> "Durmiendo"
            BodyState.ESCUCHANDO -> "Escuchando"
            BodyState.PENSANDO -> "Pensando"
            BodyState.EJECUTANDO -> "Ejecutando"
            BodyState.HABLANDO -> "Hablando"
            BodyState.FELIZ -> "Feliz"
            BodyState.ERROR -> "Error"
            BodyState.CONFUNDIDO -> "Confundido"
        }
    }
    
    companion object {
        fun updateState(context: Context, state: BodyState) {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, PixelWidgetReceiver::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            
            ids.forEach { id ->
                val prefs = context.getSharedPreferences("GlanceAppWidget_$id", Context.MODE_PRIVATE)
                prefs.edit().putString("widget_state", state.name).apply()
                // Trigger update
                val intent = Intent(context, PixelWidgetReceiver::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
                context.sendBroadcast(intent)
            }
        }
    }
}