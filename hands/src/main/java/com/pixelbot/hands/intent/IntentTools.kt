package com.pixelbot.hands.intent

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import com.pixelbot.hands.tool.Tool
import com.pixelbot.hands.tool.ToolExecutor
import com.pixelbot.hands.tool.ToolParameter
import com.pixelbot.hands.tool.ToolResult
import com.pixelbot.hands.tool.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenAppTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "abrir_app",
        description = "Abre una aplicación por nombre de paquete o nombre visible",
        parameters = listOf(
            ToolParameter("nombre", "string", "Nombre de la app o paquete (ej: 'whatsapp', 'chrome')", true),
            ToolParameter("paquete", "string", "Paquete exacto si se conoce (opcional)", false)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val nombre = args["nombre"] as? String ?: return ToolResult.Error("Falta parámetro 'nombre'")
        val paquete = args["paquete"] as? String
        
        val targetPackage = paquete ?: findPackageByName(nombre)
            ?: return ToolResult.Error("App no encontrada: $nombre")
        
        val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            ?: return ToolResult.Error("No se puede abrir: $targetPackage")
        
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        ToolResult.Success("Abriendo $nombre")
    }
    
    private fun findPackageByName(name: String): String? {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val lowerName = name.lowercase()
        return apps.firstOrNull { 
            it.loadLabel(pm).toString().lowercase().contains(lowerName) || 
            it.packageName.lowercase().contains(lowerName)
        }?.packageName
    }
}

class CallContactTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "llamar_contacto",
        description = "Llama a un contacto por nombre o número",
        parameters = listOf(
            ToolParameter("contacto", "string", "Nombre del contacto o número de teléfono", true),
            ToolParameter("confirmar", "boolean", "Pedir confirmación antes de llamar", false)
        ),
        riskLevel = RiskLevel.MEDIUM
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val contacto = args["contacto"] as? String ?: return ToolResult.Error("Falta 'contacto'")
        val confirmar = args["confirmar"] as? Boolean ?: true
        
        if (confirmar) {
            return ToolResult.ConfirmationRequired(
                "¿Llamar a $contacto?", "llamar_contacto", args
            )
        }
        
        val number = resolvePhoneNumber(contacto)
            ?: return ToolResult.Error("No se encontró número para: $contacto")
        
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            context.startActivity(intent)
            ToolResult.Success("Llamando a $contacto ($number)")
        } else {
            ToolResult.Error("Permiso CALL_PHONE no concedido")
        }
    }
    
    private fun resolvePhoneNumber(query: String): String? {
        // Simplificado: si parece número, usarlo directo
        if (query.replace(" ", "").replace("-", "").all { it.isDigit() || it == '+' }) {
            return query
        }
        // TODO: Buscar en contactos
        return null
    }
}

class SendWhatsAppTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "enviar_whatsapp",
        description = "Envía mensaje por WhatsApp a un contacto",
        parameters = listOf(
            ToolParameter("contacto", "string", "Nombre o número del contacto", true),
            ToolParameter("mensaje", "string", "Texto del mensaje", true),
            ToolParameter("confirmar", "boolean", "Pedir confirmación antes de enviar", false)
        ),
        riskLevel = RiskLevel.HIGH
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val contacto = args["contacto"] as? String ?: return ToolResult.Error("Falta 'contacto'")
        val mensaje = args["mensaje"] as? String ?: return ToolResult.Error("Falta 'mensaje'")
        val confirmar = args["confirmar"] as? Boolean ?: true
        
        if (confirmar) {
            return ToolResult.ConfirmationRequired(
                "¿Enviar WhatsApp a $contacto: \"$mensaje\"?", "enviar_whatsapp", args
            )
        }
        
        val encodedMsg = Uri.encode(mensaje)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$contacto?text=$encodedMsg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.whatsapp")
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ToolResult.Success("Abriendo WhatsApp para enviar a $contacto")
        } else {
            ToolResult.Error("WhatsApp no instalado")
        }
    }
}

class SendSmsTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "enviar_sms",
        description = "Envía SMS a un contacto",
        parameters = listOf(
            ToolParameter("contacto", "string", "Número de teléfono", true),
            ToolParameter("mensaje", "string", "Texto del mensaje", true),
            ToolParameter("confirmar", "boolean", "Pedir confirmación antes de enviar", false)
        ),
        riskLevel = RiskLevel.HIGH
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val contacto = args["contacto"] as? String ?: return ToolResult.Error("Falta 'contacto'")
        val mensaje = args["mensaje"] as? String ?: return ToolResult.Error("Falta 'mensaje'")
        val confirmar = args["confirmar"] as? Boolean ?: true
        
        if (confirmar) {
            return ToolResult.ConfirmationRequired(
                "¿Enviar SMS a $contacto: \"$mensaje\"?", "enviar_sms", args
            )
        }
        
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$contacto")).apply {
            putExtra("sms_body", mensaje)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ToolResult.Success("Abriendo app de SMS para $contacto")
        } else {
            ToolResult.Error("No hay app de SMS")
        }
    }
}

class SetAlarmTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "poner_alarma",
        description = "Crea una alarma a una hora específica",
        parameters = listOf(
            ToolParameter("hora", "string", "Hora en formato 24h (HH:mm)", true),
            ToolParameter("minutos", "number", "Minutos desde ahora (alternativo a hora)", false),
            ToolParameter("mensaje", "string", "Etiqueta de la alarma", false),
            ToolParameter("repetir", "boolean", "Repetir diariamente", false)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val horaStr = args["hora"] as? String
        val minutos = args["minutos"] as? Int
        val mensaje = args["mensaje"] as? String ?: "Alarma Pixel"
        val repetir = args["repetir"] as? Boolean ?: false
        
        val (hour, minute) = if (horaStr != null) {
            val parts = horaStr.split(":")
            parts[0].toInt() to parts[1].toInt()
        } else if (minutos != null) {
            val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, minutos) }
            cal.get(java.util.Calendar.HOUR_OF_DAY) to cal.get(java.util.Calendar.MINUTE)
        } else {
            return ToolResult.Error("Especifica 'hora' (HH:mm) o 'minutos'")
        }
        
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, mensaje)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            if (repetir) putExtra(AlarmClock.EXTRA_DAYS, java.util.BitSet().apply { (0..6).forEach { set(it) } })
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ToolResult.Success("Alarma puesta para ${hour.toString().padStart(2,'0')}:${minute.toString().padStart(2,'0')}")
        } else {
            ToolResult.Error("No hay app de alarmas")
        }
    }
}

class SetTimerTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "poner_temporizador",
        description = "Inicia un temporizador cuenta regresiva",
        parameters = listOf(
            ToolParameter("segundos", "number", "Duración en segundos", true),
            ToolParameter("mensaje", "string", "Etiqueta del temporizador", false)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val segundos = (args["segundos"] as? Number)?.toInt() ?: return ToolResult.Error("Falta 'segundos'")
        val mensaje = args["mensaje"] as? String ?: "Temporizador Pixel"
        
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, segundos)
            putExtra(AlarmClock.EXTRA_MESSAGE, mensaje)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ToolResult.Success("Temporizador de ${segundos}s iniciado")
        } else {
            ToolResult.Error("No hay app de temporizador")
        }
    }
}

class FlashlightTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "linterna",
        description = "Enciende o apaga la linterna",
        parameters = listOf(
            ToolParameter("accion", "string", "\"encender\" o \"apagar\"", true),
            ToolParameter("nivel", "number", "Nivel de brillo 0-100 (opcional)", false)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val accion = args["accion"] as? String ?: return ToolResult.Error("Falta 'accion'")
        
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull { 
            cameraManager.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return ToolResult.Error("No hay linterna disponible")
        
        try {
            when (accion.lowercase()) {
                "encender", "on", "true" -> {
                    val nivel = (args["nivel"] as? Number)?.toInt() ?: 100
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraManager.setTorchMode(cameraId, true)
                    }
                    ToolResult.Success("Linterna encendida")
                }
                "apagar", "off", "false" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraManager.setTorchMode(cameraId, false)
                    }
                    ToolResult.Success("Linterna apagada")
                }
                else -> ToolResult.Error("Acción inválida: $accion (usa 'encender' o 'apagar')")
            }
        } catch (e: Exception) {
            ToolResult.Error("Error con linterna: ${e.message}")
        }
    }
}

class VolumeBrightnessTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "ajustar_volumen_brillo",
        description = "Ajusta volumen o brillo de pantalla",
        parameters = listOf(
            ToolParameter("tipo", "string", "\"volumen\" o \"brillo\"", true),
            ToolParameter("nivel", "number", "Nivel 0-100", true),
            ToolParameter("stream", "string", "Stream de audio: media, ring, alarm, notification (solo volumen)", false)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val tipo = args["tipo"] as? String ?: return ToolResult.Error("Falta 'tipo'")
        val nivel = ((args["nivel"] as? Number)?.toInt() ?: -1).coerceIn(0, 100)
        if (nivel < 0) return ToolResult.Error("Nivel inválido")
        
        return when (tipo.lowercase()) {
            "volumen" -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val stream = when (args["stream"] as? String?.lowercase()) {
                    "ring" -> android.media.AudioManager.STREAM_RING
                    "alarm" -> android.media.AudioManager.STREAM_ALARM
                    "notification" -> android.media.AudioManager.STREAM_NOTIFICATION
                    else -> android.media.AudioManager.STREAM_MUSIC
                }
                val max = audioManager.getStreamMaxVolume(stream)
                val target = (max * nivel / 100).coerceIn(0, max)
                audioManager.setStreamVolume(stream, target, 0)
                ToolResult.Success("Volumen ${stream} ajustado a $nivel%")
            }
            "brillo" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val currentMode = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                    if (currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    }
                    val target = (255 * nivel / 100).coerceIn(0, 255)
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, target)
                }
                ToolResult.Success("Brillo ajustado a $nivel%")
            }
            else -> ToolResult.Error("Tipo inválido: $tipo (usa 'volumen' o 'brillo')")
        }
    }
}

class WifiBluetoothPanelTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "panel_wifi_bluetooth",
        description = "Abre el panel de Wi-Fi o Bluetooth",
        parameters = listOf(
            ToolParameter("tipo", "string", "\"wifi\" o \"bluetooth\"", true)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val tipo = args["tipo"] as? String ?: return ToolResult.Error("Falta 'tipo'")
        
        val intent = when (tipo.lowercase()) {
            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            else -> return ToolResult.Error("Tipo inválido: $tipo")
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        ToolResult.Success("Abriendo panel de $tipo")
    }
}

class NavigateTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "navegar",
        description = "Inicia navegación GPS a una dirección",
        parameters = listOf(
            ToolParameter("destino", "string", "Dirección o nombre del lugar", true),
            ToolParameter("modo", "string", "Modo: driving, walking, transit, bicycling", false)
        ),
        riskLevel = RiskLevel.MEDIUM
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val destino = args["destino"] as? String ?: return ToolResult.Error("Falta 'destino'")
        val modo = args["modo"] as? String ?: "driving"
        
        val uri = Uri.parse("google.navigation:q=${Uri.encode(destino)}&mode=$modo")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ToolResult.Success("Navegando a $destino en modo $modo")
        } else {
            ToolResult.Error("Google Maps no instalado")
        }
    }
}

class SearchWebTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "buscar_web",
        description = "Busca en el navegador",
        parameters = listOf(
            ToolParameter("consulta", "string", "Términos de búsqueda", true),
            ToolParameter("motor", "string", "Motor: google, duckduckgo, bing", false)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val consulta = args["consulta"] as? String ?: return ToolResult.Error("Falta 'consulta'")
        val motor = args["motor"] as? String ?: "google"
        
        val url = when (motor.lowercase()) {
            "duckduckgo" -> "https://duckduckgo.com/?q=${Uri.encode(consulta)}"
            "bing" -> "https://www.bing.com/search?q=${Uri.encode(consulta)}"
            else -> "https://www.google.com/search?q=${Uri.encode(consulta)}"
        }
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(intent)
        ToolResult.Success("Buscando '$consulta' en $motor")
    }
}

class MediaControlTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "control_musica",
        description = "Controla reproducción multimedia (play/pause/next/prev)",
        parameters = listOf(
            ToolParameter("accion", "string", "Acción: play, pause, next, prev, stop", true)
        ),
        riskLevel = RiskLevel.LOW
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val accion = args["accion"] as? String ?: return ToolResult.Error("Falta 'accion'")
        
        val intent = Intent("androidx.media.session.MediaController.ACTION_MEDIA_BUTTON")
        val keyEvent = when (accion.lowercase()) {
            "play" -> android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
            "pause" -> android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            "next", "siguiente" -> android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            "prev", "anterior", "previous" -> android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "stop" -> android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_STOP)
            else -> return ToolResult.Error("Acción inválida: $accion")
        }
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        context.sendBroadcast(intent)
        
        ToolResult.Success("Control multimedia: $accion")
    }
}

class TakePhotoTool(private val context: Context) : ToolExecutor {
    override val tool = Tool(
        name = "tomar_foto",
        description = "Abre la cámara para tomar una foto",
        parameters = listOf(
            ToolParameter("modo", "string", "\"foto\" o \"video\"", false)
        ),
        riskLevel = RiskLevel.MEDIUM
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val modo = args["modo"] as? String ?: "foto"
        
        val intent = when (modo.lowercase()) {
            "video" -> Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            else -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        if (intent.resolveActivity(context.packageManager) != null) {
            // Necesita Activity para startActivityForResult
            // Aquí solo lanzamos la intent básica
            context.startActivity(intent)
            ToolResult.Success("Abriendo cámara para $modo")
        } else {
            ToolResult.Error("No hay app de cámara")
        }
    }
}