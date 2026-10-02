package com.pixelbot.hands.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.pixelbot.hands.accessibility.AccessibilityToolExecutor.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PixelAccessibilityService : AccessibilityService() {
    
    companion object {
        var instance: PixelAccessibilityService? = null
        private val eventChannel = Channel<AccessibilityEvent>(Channel.UNLIMITED)
        val events = eventChannel
    }
    
    private val pendingActions = ConcurrentHashMap<String, PendingAction>()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        eventChannel.trySend(event)
    }
    
    override fun onInterrupt() {}
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            notificationTimeout = 100
            packageNames = null // todas las apps
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
        }
        setServiceInfo(info)
    }
    
    // === ACCIONES EXPUESTAS COMO HERRAMIENTAS ===
    
    fun getScreenSummary(maxNodes: Int = 50): String {
        val root = rootInActiveWindow ?: return "Sin ventana activa"
        val sb = StringBuilder()
        collectNodes(root, sb, 0, maxNodes)
        return sb.toString()
    }
    
    private fun collectNodes(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int, maxNodes: Int) {
        if (sb.length > 2000 || depth > 10) return
        
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val className = node.className?.toString()?.substringAfterLast(".") ?: ""
        val id = node.viewIdResourceName?.substringAfterLast("/") ?: ""
        val clickable = node.isClickable
        val editable = node.isEditable
        val checkable = node.isCheckable
        val checked = node.isChecked
        
        val label = text ?: desc ?: ""
        if (label.isNotBlank() || clickable || editable || checkable) {
            val indent = "  ".repeat(depth)
            val flags = buildString {
                if (clickable) append("[click]")
                if (editable) append("[edit]")
                if (checkable) append("[check${if (checked) "✓" else ""}]")
            }
            val idStr = if (id.isNotBlank()) " @$id" else ""
            sb.appendLine("$indent$className$idStr: $label $flags")
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectNodes(it, sb, depth + 1, maxNodes) }
        }
    }
    
    fun clickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val targets = mutableListOf<AccessibilityNodeInfo>()
        findByText(root, text, targets)
        return targets.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }
    
    fun clickById(id: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val targets = mutableListOf<AccessibilityNodeInfo>()
        findById(root, id, targets)
        return targets.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }
    
    fun clickByCoordinates(x: Int, y: Int): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = findByCoordinates(root, x, y)
        return target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }
    
    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = findFocusedEditable(root)
        return focused?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, 
            android.os.Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }) ?: false
    }
    
    fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findScrollable(root)
        val action = when (direction.lowercase()) {
            "abajo", "down", "forward" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "arriba", "up", "backward" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            else -> return false
        }
        return scrollable?.performAction(action) ?: false
    }
    
    fun goBack(): Boolean {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        return true
    }
    
    fun goHome(): Boolean {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        return true
    }
    
    fun goRecents(): Boolean {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        return true
    }
    
    fun openNotifications(): Boolean {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        return true
    }
    
    fun openQuickSettings(): Boolean {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
        return true
    }
    
    suspend fun waitForScreenChange(timeoutMs: Long = 5000): String {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var lastSummary = getScreenSummary()
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                kotlinx.coroutines.delay(200)
                val currentSummary = getScreenSummary()
                if (currentSummary != lastSummary) {
                    return@withContext currentSummary
                }
            }
            "Timeout: la pantalla no cambió"
        }
    }
    
    private fun findByText(node: AccessibilityNodeInfo, text: String, results: MutableList<AccessibilityNodeInfo>) {
        val nodeText = node.text?.toString()?.lowercase() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (nodeText.contains(text.lowercase()) || nodeDesc.contains(text.lowercase())) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findByText(it, text, results) }
        }
    }
    
    private fun findById(node: AccessibilityNodeInfo, id: String, results: MutableList<AccessibilityNodeInfo>) {
        val nodeId = node.viewIdResourceName?.substringAfterLast("/") ?: ""
        if (nodeId == id) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findById(it, id, results) }
        }
    }
    
    private fun findByCoordinates(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.contains(x, y)) {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                val found = child?.let { findByCoordinates(it, x, y) }
                if (found != null) return found
            }
            return node
        }
        return null
    }
    
    private fun findFocusedEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused && node.isEditable) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let { findFocusedEditable(it) }
            if (found != null) return found
        }
        return null
    }
    
    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let { findScrollable(it) }
            if (found != null) return found
        }
        return null
    }
    
    // Verificar si es pantalla sensible (contraseña, pago, etc.)
    fun isSensitiveScreen(): Boolean {
        val root = rootInActiveWindow ?: return false
        return checkSensitive(root)
    }
    
    private fun checkSensitive(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        val sensitiveKeywords = listOf("password", "contraseña", "pin", "cvv", "tarjeta", "card", "payment", "pago", "banco", "bank")
        val allText = "$className $text $desc".lowercase()
        
        if (sensitiveKeywords.any { allText.contains(it) }) return true
        
        for (i in 0 until node.childCount) {
            if (node.getChild(i)?.let { checkSensitive(it) } == true) return true
        }
        return false
    }
    
    // Lista negra de apps
    private val blockedPackages = setOf("com.android.settings", "com.android.systemui")
    
    fun isBlockedPackage(): Boolean {
        val root = rootInActiveWindow
        val pkg = root?.packageName ?: ""
        return blockedPackages.contains(pkg)
    }
    
    data class PendingAction(
        val id: String,
        val action: String,
        val args: Map<String, Any>,
        val callback: (Boolean) -> Unit
    )
}

object AccessibilityToolExecutor {
    const val TOOL_LEER_PANTALLA = "leer_pantalla"
    const val TOOL_TOCAR = "tocar"
    const val TOOL_ESCRIBIR = "escribir"
    const val TOOL_DESLIZAR = "deslizar"
    const val TOOL_VOLVER = "volver"
    const val TOOL_HOME = "ir_inicio"
    const val TOOL_RECIENTS = "ir_recientes"
    const val TOOL_NOTIFICACIONES = "abrir_notificaciones"
    const val TOOL_ESPERAR_CAMBIO = "esperar_cambio_pantalla"
}