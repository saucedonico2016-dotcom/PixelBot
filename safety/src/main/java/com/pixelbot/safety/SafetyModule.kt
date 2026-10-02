package com.pixelbot.safety

import com.pixelbot.hands.tool.RiskLevel
import com.pixelbot.hands.tool.Tool
import com.pixelbot.memory.MemoryModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafetyModule(private val memory: MemoryModule) {
    
    // Apps en lista blanca (siempre permitidas sin confirmación extra)
    private val whitelistedApps = mutableSetOf<String>(
        "com.android.settings",
        "com.android.calculator2",
        "com.google.android.calendar"
    )
    
    // Apps en lista negra (nunca permitidas)
    private val blacklistedApps = mutableSetOf<String>(
        "com.android.packageinstaller",
        "com.android.vending"
    )
    
    // Configuración de riesgo por herramienta (override del default)
    private val toolRiskOverrides = mutableMapOf<String, RiskLevel>()
    
    // Modo ensayo (solo dice qué harían, no ejecutan)
    var dryRunMode = false
    
    // Log de acciones para auditoría
    private val actionLog = mutableListOf<ActionLogEntry>()
    private val maxLogSize = 1000
    
    data class ActionLogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val toolName: String,
        val args: Map<String, Any>,
        val riskLevel: RiskLevel,
        val allowed: Boolean,
        val reason: String?,
        val result: String?
    )
    
    data class SafetyCheckResult(
        val allowed: Boolean,
        val message: String?,
        val requiresVoiceConfirm: Boolean = false,
        val requiresTouchConfirm: Boolean = false
    )
    
    fun validateToolExecution(
        toolName: String,
        args: Map<String, Any>,
        defaultRisk: RiskLevel
    ): SafetyCheckResult {
        val riskLevel = toolRiskOverrides[toolName] ?: defaultRisk
        
        // Verificar lista negra de apps
        if (toolName == "abrir_app") {
            val pkg = args["paquete"] as? String ?: args["nombre"] as? String ?: ""
            if (blacklistedApps.any { pkg.contains(it, ignoreCase = true) }) {
                return SafetyCheckResult(false, "App en lista negra: $pkg")
            }
            if (whitelistedApps.any { pkg.contains(it, ignoreCase = true) }) {
                // Apps en whitelist: riesgo bajo automático
                return SafetyCheckResult(true, null)
            }
        }
        
        // Verificar pantalla sensible (contraseñas, pagos)
        // TODO: Integrar con AccessibilityService
        
        // Modo ensayo
        if (dryRunMode) {
            return SafetyCheckResult(
                allowed = false,
                message = "[ENSUEÑO] Habría ejecutado: $toolName con $args",
                requiresVoiceConfirm = false,
                requiresTouchConfirm = false
            )
        }
        
        // Determinar confirmaciones necesarias según riesgo
        return when (riskLevel) {
            RiskLevel.LOW -> SafetyCheckResult(true, null)
            RiskLevel.MEDIUM -> SafetyCheckResult(
                allowed = true,
                message = "Confirmar: $toolName",
                requiresVoiceConfirm = false,
                requiresTouchConfirm = true
            )
            RiskLevel.HIGH -> SafetyCheckResult(
                allowed = true,
                message = "CONFIRMACIÓN REQUERIDA (voz + toque): $toolName",
                requiresVoiceConfirm = true,
                requiresTouchConfirm = true
            )
        }
    }
    
    fun logAction(
        toolName: String,
        args: Map<String, Any>,
        riskLevel: RiskLevel,
        allowed: Boolean,
        reason: String?,
        result: String?
    ) {
        val entry = ActionLogEntry(
            toolName = toolName,
            args = args,
            riskLevel = riskLevel,
            allowed = allowed,
            reason = reason,
            result = result
        )
        
        actionLog.add(entry)
        if (actionLog.size > maxLogSize) {
            actionLog.removeAt(0)
        }
    }
    
    fun getActionLog(limit: Int = 50): List<ActionLogEntry> {
        return actionLog.reversed().take(limit)
    }
    
    fun clearLog() {
        actionLog.clear()
    }
    
    fun exportLog(): String {
        return kotlinx.serialization.json.Json { prettyPrint = true }
            .encodeToString(actionLog)
    }
    
    // Panic button - detiene todo y silencia micrófono
    fun panicButton(): PanicResult {
        // 1. Detener todos los servicios
        // 2. Silenciar micrófono
        // 3. Ocultar burbuja
        // 4. Limpiar estado
        
        return PanicResult(
            stoppedServices = true,
            micMuted = true,
            bubbleHidden = true,
            message = "🛑 BOTÓN DE PÁNICO ACTIVADO - Todo detenido"
        )
    }
    
    data class PanicResult(
        val stoppedServices: Boolean,
        val micMuted: Boolean,
        val bubbleHidden: Boolean,
        val message: String
    )
    
    // Gestión de listas
    fun addToWhitelist(packageName: String) { whitelistedApps.add(packageName) }
    fun removeFromWhitelist(packageName: String) { whitelistedApps.remove(packageName) }
    fun getWhitelist(): Set<String> = whitelistedApps.toSet()
    
    fun addToBlacklist(packageName: String) { blacklistedApps.add(packageName) }
    fun removeFromBlacklist(packageName: String) { blacklistedApps.remove(packageName) }
    fun getBlacklist(): Set<String> = blacklistedApps.toSet()
    
    fun setToolRisk(toolName: String, risk: RiskLevel) { toolRiskOverrides[toolName] = risk }
    fun getToolRisk(toolName: String): RiskLevel? = toolRiskOverrides[toolName]
    
    fun setDryRun(enabled: Boolean) { dryRunMode = enabled }
    fun isDryRun(): Boolean = dryRunMode
    
    // Verificar si una app tiene acceso a datos sensibles
    fun isSensitiveApp(packageName: String): Boolean {
        val sensitivePackages = setOf(
            "com.android.settings",
            "com.android.packageinstaller",
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gm",
            "com.whatsapp",
            "com.telegram",
            "com.signal"
        )
        return sensitivePackages.contains(packageName)
    }
}