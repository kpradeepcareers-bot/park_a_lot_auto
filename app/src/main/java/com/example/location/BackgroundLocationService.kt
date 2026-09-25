package com.example.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/**
 * Background service for continuous auto-detect location monitoring
 * and office arrival detection.
 */
class BackgroundLocationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createMonitoringNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_SERVICE) {
            BackgroundLocationManager.stopLocationMonitoring()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start foreground with quiet notification if needed
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        BackgroundLocationManager.startLocationMonitoring(this)

        return START_STICKY
    }

    override fun onDestroy() {
        BackgroundLocationManager.stopLocationMonitoring()
        super.onDestroy()
    }

    private fun createMonitoringNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Auto-Check",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors vehicle arrival and parking bay detection in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Park a lot Auto Location")
            .setContentText("Monitoring office arrival & parking zones")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START_SERVICE = "com.parkalot.app.ACTION_START_LOCATION_SERVICE"
        const val ACTION_STOP_SERVICE = "com.parkalot.app.ACTION_STOP_LOCATION_SERVICE"
        private const val CHANNEL_ID = "parkalot_location_monitoring_channel"
        private const val NOTIFICATION_ID = 2005

        fun start(context: Context) {
            val intent = Intent(context, BackgroundLocationService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Background start restriction fallback
                BackgroundLocationManager.startLocationMonitoring(context)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BackgroundLocationService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
                BackgroundLocationManager.stopLocationMonitoring()
            }
        }
    }
}
