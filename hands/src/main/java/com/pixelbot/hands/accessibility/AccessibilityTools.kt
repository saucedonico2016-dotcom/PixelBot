package com.pixelbot.hands.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import com.pixelbot.hands.tool.Tool
import com.pixelbot.hands.tool.ToolExecutor
import com.pixelbot.hands.tool.ToolParameter
import com.pixelbot.hands.tool.ToolResult
import com.pixelbot.hands.tool.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LeerPantallaTool(private val service: PixelAccessibilityService) : ToolExecutor {
    override val tool = Tool(
        name = AccessibilityToolExecutor.TOOL_LEER_PANTALLA,
        description = "Lee el árbol de la pantalla y devuelve un resumen compacto de elementos interactivos",
        parameters = listOf(
            ToolParameter("maxElementos", "number", "Máximo de elementos a devolver (default 50)", false)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val max = (args["maxElementos"] as? Number)?.toInt() ?: 50
        val summary = service.getScreenSummary(max)
        ToolResult.Success(summary)
    }
}

class TocarTool(private val service: PixelAccessibilityService) : ToolExecutor {
    override val tool = Tool(
        name = AccessibilityToolExecutor.TOOL_TOCAR,
        description = "Toca un elemento por texto, ID de recurso o coordenadas",
        parameters = listOf(
            ToolParameter("texto", "string", "Texto visible o contentDescription del elemento", false),
            ToolParameter("id", "string", "ID del recurso (sin paquete, ej: 'btn_enviar')", false),
            ToolParameter("x", "number", "Coordenada X en pantalla", false),
            ToolParameter("y", "number", "Coordenada Y en pantalla", false)
        ),
        riskLevel = RiskLevel.MEDIUM
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val texto = args["texto"] as? String
        val id = args["id"] as? String
        val x = (args["x"] as? Number)?.toInt()
        val y = (args["y"] as? Number)?.toInt()
        
        val success = when {
            texto != null && texto.isNotBlank() -> service.clickByText(texto)
            id != null && id.isNotBlank() -> service.clickById(id)
            x != null && y != null -> service.clickByCoordinates(x, y)
            else -> false
        }
        
        if (success) ToolResult.Success("Click realizado") else ToolResult.Error("Elemento no encontrado")
    }
}

class EscribirTool(private val service: PixelAccessibilityService) : ToolExecutor {
    override val tool = Tool(
        name = AccessibilityToolExecutor.TOOL_ESCRIBIR,
        description = "Escribe texto en el campo enfocado/editable",
        parameters = listOf(
            ToolParameter("texto", "string", "Texto a escribir", true)
        ),
        riskLevel = RiskLevel.MEDIUM
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val texto = args["texto"] as? String ?: return ToolResult.Error("Falta 'texto'")
        val success = service.typeText(texto)
        if (success) ToolResult.Success("Texto escrito") else ToolResult.Error("No hay campo editable enfocado")
    }
}

class DeslizarTool(private val service: PixelAccessibilityService) : ToolExecutor {
    override val tool = Tool(
        name = AccessibilityToolExecutor.TOOL_DESLIZAR,
        description = "Desliza/scroll en la dirección indicada",
        parameters = listOf(
            ToolParameter("direccion", "string", "Dirección: 'arriba', 'abajo', 'up', 'down'", true)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val dir = args["direccion"] as? String ?: return ToolResult.Error("Falta 'direccion'")
        val success = service.scroll(dir)
        if (success) ToolResult.Success("Deslizado $dir") else ToolResult.Error("No hay elemento deslizable")
    }
}

class VolverTool(private val service: PixelAccessibilityService) : ToolExecutor {
    override val tool = Tool(
        name = AccessibilityToolExecutor.TOOL_VOLVER,
        description = "Simula botón Atrás del sistema",
        parameters = emptyList(),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        service.goBack()
        ToolResult.Success("Atrás")
    }
}

class IrInicioTool(private val service: PixelAccessibilityService) : ToolExecutor {
    override val tool = Tool(
        name = AccessibilityToolExecutor.TOOL_HOME,
        description = "Va a la pantalla de inicio",
        parameters = emptyList(),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        service.goHome()
        ToolResult.Success("Inicio")
    }
}

class IrRecientesTool(private val service: PixelAccessibilityService) : ToolExecutor {
    override val tool = Tool(
        name = AccessibilityToolExecutor.TOOL_RECIENTS,
        description = "Abre vista de apps recientes",
        parameters = emptyList(),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        service.goRecents()
        ToolResult.Success("Recientes")
    }
}

class AbrirNotificacionesTool(private val service: PixelAccessibilityService) : ToolExecutor {
    override val tool = Tool(
        name = AccessibilityToolExecutor.TOOL_NOTIFICACIONES,
        description = "Abre el panel de notificaciones",
        parameters = emptyList(),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        service.openNotifications()
        ToolResult.Success("Notificaciones abiertas")
    }
}

class EsperarCambioPantallaTool(private val service: PixelAccessibilityService) : ToolExecutor {
    override val tool = Tool(
        name = AccessibilityToolExecutor.TOOL_ESPERAR_CAMBIO,
        description = "Espera a que la pantalla cambie (timeout configurable)",
        parameters = listOf(
            ToolParameter("timeoutMs", "number", "Timeout en milisegundos (default 5000)", false)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val timeout = (args["timeoutMs"] as? Number)?.toLong() ?: 5000L
        val result = service.waitForScreenChange(timeout)
        ToolResult.Success(result)
    }
}

class AccessibilityToolsProvider(private val context: Context) {
    private var service: PixelAccessibilityService? = null
    
    fun setService(service: PixelAccessibilityService) {
        this.service = service
    }
    
    fun getTools(): List<ToolExecutor> {
        val s = service ?: return emptyList()
        return listOf(
            LeerPantallaTool(s),
            TocarTool(s),
            EscribirTool(s),
            DeslizarTool(s),
            VolverTool(s),
            IrInicioTool(s),
            IrRecientesTool(s),
            AbrirNotificacionesTool(s),
            EsperarCambioPantallaTool(s)
        )
    }
    
    fun isServiceAvailable(): Boolean = service != null
    
    fun isSensitiveScreen(): Boolean = service?.isSensitiveScreen() ?: false
    
    fun isBlockedPackage(): Boolean = service?.isBlockedPackage() ?: false
}