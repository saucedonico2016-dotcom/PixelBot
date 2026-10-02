package com.pixelbot.brain.router

import com.pixelbot.brain.ai.AiProvider
import com.pixelbot.brain.ai.ProviderConfig
import com.pixelbot.brain.ai.ProviderType
import com.pixelbot.hands.tool.Tool
import com.pixelbot.hands.tool.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class Router(
    private val providers: List<AiProvider>,
    private val toolRegistry: ToolRegistry,
    private val rules: List<RouterRule> = defaultRules()
) {
    
    private val sortedProviders = providers
        .filter { it.isAvailable }
        .sortedBy { getProviderPriority(it) }
    
    private fun getProviderPriority(provider: AiProvider): Int {
        return when (provider) {
            is com.pixelbot.brain.ai.LocalProvider -> 1
            is com.pixelbot.brain.ai.OllamaProvider -> 2
            is com.pixelbot.brain.ai.CloudProvider -> 3
            else -> 10
        }
    }
    
    suspend fun route(input: String, context: Map<String, Any> = emptyMap()): RouteResult {
        // 1. Reglas regex para comandos simples
        for (rule in rules) {
            if (rule.pattern.matcher(input).matches()) {
                return RouteResult.RuleMatched(rule.action, rule.extractArgs(input))
            }
        }
        
        // 2. IA Local
        val localProvider = sortedProviders.firstOrNull { it is com.pixelbot.brain.ai.LocalProvider }
        if (localProvider != null) {
            return RouteResult.AiSelected(localProvider, ProviderType.LOCAL)
        }
        
        // 3. Ollama local
        val ollamaProvider = sortedProviders.firstOrNull { it is com.pixelbot.brain.ai.OllamaProvider }
        if (ollamaProvider != null && ollamaProvider.isAvailable) {
            return RouteResult.AiSelected(ollamaProvider, ProviderType.OLLAMA)
        }
        
        // 4. Nube
        val cloudProvider = sortedProviders.firstOrNull { it is com.pixelbot.brain.ai.CloudProvider }
        if (cloudProvider != null && cloudProvider.isAvailable) {
            return RouteResult.AiSelected(cloudProvider, ProviderType.CLOUD)
        }
        
        // 5. Fallback sin internet - solo reglas e intents básicos
        return RouteResult.FallbackOnly
    }
    
    companion object {
        fun defaultRules(): List<RouterRule> {
            return listOf(
                // Apps
                RouterRule(Pattern.compile("(?i)^abre\\s+(.+)$"), "abrir_app", mapOf("nombre" to 1)),
                RouterRule(Pattern.compile("(?i)^abrir\\s+(.+)$"), "abrir_app", mapOf("nombre" to 1)),
                RouterRule(Pattern.compile("(?i)^lanza\\s+(.+)$"), "abrir_app", mapOf("nombre" to 1)),
                
                // Llamadas
                RouterRule(Pattern.compile("(?i)^llama\\s+a?\\s*(.+)$"), "llamar_contacto", mapOf("contacto" to 1)),
                RouterRule(Pattern.compile("(?i)^llamar\\s+a?\\s*(.+)$"), "llamar_contacto", mapOf("contacto" to 1)),
                
                // WhatsApp/SMS
                RouterRule(Pattern.compile("(?i)^env[ií]a\\s+whatsapp\\s+a\\s+(.+)\\s*:\\s*(.+)$"), "enviar_whatsapp", mapOf("contacto" to 1, "mensaje" to 2)),
                RouterRule(Pattern.compile("(?i)^whatsapp\\s+a\\s+(.+)\\s*:\\s*(.+)$"), "enviar_whatsapp", mapOf("contacto" to 1, "mensaje" to 2)),
                RouterRule(Pattern.compile("(?i)^env[ií]a\\s+sms\\s+a\\s+(.+)\\s*:\\s*(.+)$"), "enviar_sms", mapOf("contacto" to 1, "mensaje" to 2)),
                
                // Alarmas/Temporizadores
                RouterRule(Pattern.compile("(?i)^alarma\\s+(?:a\\s+las?\\s+)?(\\d{1,2}):(\\d{2})$"), "poner_alarma", mapOf("hora" to 0, "minutos" to -1)),
                RouterRule(Pattern.compile("(?i)^pon(?:me)?\\s+alarma\\s+(?:a\\s+las?\\s+)?(\\d{1,2}):(\\d{2})$"), "poner_alarma", mapOf("hora" to 0, "minutos" to -1)),
                RouterRule(Pattern.compile("(?i)^temporizador\\s+(\\d+)\\s*(segundos?|minutos?)$"), "poner_temporizador", mapOf("segundos" to 1)),
                RouterRule(Pattern.compile("(?i)^pon(?:me)?\\s+temporizador\\s+(\\d+)\\s*(segundos?|minutos?)$"), "poner_temporizador", mapOf("segundos" to 1)),
                
                // Linterna
                RouterRule(Pattern.compile("(?i)^enciende\\s+la?\\s*linterna$"), "linterna", mapOf("accion" to "encender")),
                RouterRule(Pattern.compile("(?i)^apaga\\s+la?\\s*linterna$"), "linterna", mapOf("accion" to "apagar")),
                RouterRule(Pattern.compile("(?i)^linterna\\s+(on|off|encender|apagar)$"), "linterna", mapOf("accion" to 1)),
                
                // Volumen/Brillo
                RouterRule(Pattern.compile("(?i)^(?:sub[e|a]|baja|pon)\\s+el?\\s*volumen\\s+(?:al?\\s+)?(\\d+)$"), "ajustar_volumen_brillo", mapOf("tipo" to "volumen", "nivel" to 1)),
                RouterRule(Pattern.compile("(?i)^volumen\\s+(\\d+)$"), "ajustar_volumen_brillo", mapOf("tipo" to "volumen", "nivel" to 1)),
                RouterRule(Pattern.compile("(?i)^(?:sub[e|a]|baja|pon)\\s+el?\\s*brillo\\s+(?:al?\\s+)?(\\d+)$"), "ajustar_volumen_brillo", mapOf("tipo" to "brillo", "nivel" to 1)),
                RouterRule(Pattern.compile("(?i)^brillo\\s+(\\d+)$"), "ajustar_volumen_brillo", mapOf("tipo" to "brillo", "nivel" to 1)),
                
                // WiFi/Bluetooth
                RouterRule(Pattern.compile("(?i)^abre\\s+(?:ajustes?\\s+de?\\s+)?wifi$"), "panel_wifi_bluetooth", mapOf("tipo" to "wifi")),
                RouterRule(Pattern.compile("(?i)^abre\\s+(?:ajustes?\\s+de?\\s+)?bluetooth$"), "panel_wifi_bluetooth", mapOf("tipo" to "bluetooth")),
                
                // Navegación
                RouterRule(Pattern.compile("(?i)^navega\\s+a\\s+(.+)$"), "navegar", mapOf("destino" to 1)),
                RouterRule(Pattern.compile("(?i)^ll[eé]vame\\s+a\\s+(.+)$"), "navegar", mapOf("destino" to 1)),
                
                // Búsqueda
                RouterRule(Pattern.compile("(?i)^busca\\s+(.+)$"), "buscar_web", mapOf("consulta" to 1)),
                RouterRule(Pattern.compile("(?i)^buscar\\s+(.+)$"), "buscar_web", mapOf("consulta" to 1)),
                
                // Música
                RouterRule(Pattern.compile("(?i)^(?:pon|reproduce|play)\\s+m[uú]sica$"), "control_musica", mapOf("accion" to "play")),
                RouterRule(Pattern.compile("(?i)^pausa\\s+m[uú]sica$"), "control_musica", mapOf("accion" to "pause")),
                RouterRule(Pattern.compile("(?i)^siguiente\\s+canci[oó]n$"), "control_musica", mapOf("accion" to "next")),
                RouterRule(Pattern.compile("(?i)^canci[oó]n\\s+anterior$"), "control_musica", mapOf("accion" to "prev")),
                
                // Foto
                RouterRule(Pattern.compile("(?i)^toma\\s+foto$"), "tomar_foto", mapOf("modo" to "foto")),
                RouterRule(Pattern.compile("(?i)^haz\\s+foto$"), "tomar_foto", mapOf("modo" to "foto")),
                RouterRule(Pattern.compile("(?i)^graba\\s+video$"), "tomar_foto", mapOf("modo" to "video")),
                
                // Accesibilidad
                RouterRule(Pattern.compile("(?i)^qu[eé]\\s+hay\\s+en\\s+pantalla$"), "leer_pantalla", emptyMap()),
                RouterRule(Pattern.compile("(?i)^lee\\s+pantalla$"), "leer_pantalla", emptyMap()),
                RouterRule(Pattern.compile("(?i)^atrás$"), "volver", emptyMap()),
                RouterRule(Pattern.compile("(?i)^vuelve\\s+atrás$"), "volver", emptyMap()),
                RouterRule(Pattern.compile("(?i)^inicio$"), "ir_inicio", emptyMap()),
                RouterRule(Pattern.compile("(?i)^pantalla\\s+de\\s+inicio$"), "ir_inicio", emptyMap()),
                RouterRule(Pattern.compile("(?i)^recientes$"), "ir_recientes", emptyMap()),
                RouterRule(Pattern.compile("(?i)^notificaciones$"), "abrir_notificaciones", emptyMap()),
            )
        }
    }
}

data class RouterRule(
    val pattern: Pattern,
    val action: String,
    val argGroups: Map<String, Int> // nombre -> grupo regex (0 = match completo, -1 = calcular)
) {
    fun extractArgs(input: String): Map<String, Any> {
        val matcher = pattern.matcher(input)
        if (!matcher.matches()) return emptyMap()
        
        val args = mutableMapOf<String, Any>()
        for ((name, group) in argGroups) {
            when (group) {
                0 -> args[name] = matcher.group(0)
                -1 -> {
                    // Hora: calcular desde grupos 1 y 2
                    val h = matcher.group(1)?.toIntOrNull() ?: 0
                    val m = matcher.group(2)?.toIntOrNull() ?: 0
                    args[name] = String.format("%02d:%02d", h, m)
                }
                else -> args[name] = matcher.group(group)
            }
        }
        return args
    }
}

sealed class RouteResult {
    data class RuleMatched(val action: String, val args: Map<String, Any>) : RouteResult()
    data class AiSelected(val provider: AiProvider, val type: com.pixelbot.brain.ai.ProviderType) : RouteResult()
    object FallbackOnly : RouteResult()
}