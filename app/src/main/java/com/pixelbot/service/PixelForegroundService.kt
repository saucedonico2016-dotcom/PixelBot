package com.pixelbot.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pixelbot.R

class PixelForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "pixel_foreground_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.pixelbot.ACTION_STOP_SERVICE"
        const val ACTION_TAP_TO_TALK = "com.pixelbot.ACTION_TAP_TO_TALK"
    }

    private var notificationManager: NotificationManager? = null
    private var audioManager: AudioManager? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TAP_TO_TALK -> {
                // Notificar al módulo Ears para iniciar escucha directa
                // TODO: Enviar evento al EarsModule
            }
        }

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pixel - Servicio Principal",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene vivo el asistente para wake word y burbuja flotante"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, PixelForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this, 0, stopIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openAppIntent = Intent(this, com.pixelbot.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = android.app.PendingIntent.getActivity(
            this, 0, openAppIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pixel activo")
            .setContentText("Escuchando wake word... Toca para abrir")
            .setSmallIcon(R.drawable.ic_pixel_notification)
            .setIcon(android.R.drawable.ic_btn_speak_now)
            .setColor(androidx.core.content.ContextCompat.getColor(this, R.color.pixel_primary))
            .setColorized(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_pause,
                    "Detener",
                    stopPendingIntent
                ).build()
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Pixel está escuchando. Di \"Pixel\" para activar."
            ))
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }
}