package com.pixelbot.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

data class PermissionItem(
    val key: String,
    val title: String,
    val description: String,
    val icon: Int,
    val checkGranted: (Context) -> Boolean,
    val openSettings: (Context) -> Unit
) {
    companion object {
        fun microphone(context: Context): PermissionItem = PermissionItem(
            key = "microphone",
            title = "Micrófono",
            description = "Para escuchar tu voz y la wake word",
            icon = android.R.drawable.ic_btn_speak_now,
            checkGranted = { ctx ->
                ctx.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            },
            openSettings = { ctx ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", ctx.packageName, null)
                ctx.startActivity(intent)
            }
        )

        fun notifications(context: Context): PermissionItem = PermissionItem(
            key = "notifications",
            title = "Notificaciones",
            description = "Para la burbuja flotante y alertas del servicio",
            icon = android.R.drawable.stat_notify_chat,
            checkGranted = { ctx ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    return@checkGranted manager.areNotificationsEnabled()
                }
                return@checkGranted true
            },
            openSettings = { ctx ->
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                ctx.startActivity(intent)
            }
        )

        fun overlay(context: Context): PermissionItem = PermissionItem(
            key = "overlay",
            title = "Superposición sobre otras apps",
            description = "Para mostrar la burbuja flotante del personaje",
            icon = android.R.drawable.ic_menu_layers,
            checkGranted = { ctx ->
                Settings.canDrawOverlays(ctx)
            },
            openSettings = { ctx ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    .setData(Uri.parse("package:${ctx.packageName}"))
                ctx.startActivity(intent)
            }
        )

        fun accessibility(context: Context): PermissionItem = PermissionItem(
            key = "accessibility",
            title = "Servicio de accesibilidad",
            description = "Para leer la pantalla y ejecutar acciones táctiles (manos)",
            icon = android.R.drawable.ic_menu_accessibility,
            checkGranted = { ctx ->
                val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
                val enabledServices = am.getEnabledAccessibilityServiceList(android.view.accessibility.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                return@checkGranted enabledServices.any { it.id.contains("PixelAccessibilityService") || it.id.contains("pixelbot") }
            },
            openSettings = { ctx ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                ctx.startActivity(intent)
            }
        )

        fun batteryOptimization(context: Context): PermissionItem = PermissionItem(
            key = "battery",
            title = "Excluir de optimización de batería",
            description = "Para que el servicio en primer plano no sea detenido",
            icon = android.R.drawable.ic_menu_power_saving,
            checkGranted = { ctx ->
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                return@checkGranted pm.isIgnoringBatteryOptimizations(ctx.packageName)
            },
            openSettings = { ctx ->
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${ctx.packageName}"))
                ctx.startActivity(intent)
            }
        )

        fun all(context: Context): List<PermissionItem> = listOf(
            microphone(context),
            notifications(context),
            overlay(context),
            accessibility(context),
            batteryOptimization(context)
        )
    }
}