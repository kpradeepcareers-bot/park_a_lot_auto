package com.example.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "parking_channel"
    const val CHANNEL_NAME = "Parking & Geofence Alerts"
    const val OFFICE_ARRIVAL_NOTIF_ID = 1001
    const val PARKING_SAVED_NOTIF_ID = 1002

    private var lastNotifiedArrivalTimestamp: Long = 0L
    private var isNotificationDismissedForCurrentArrival: Boolean = false

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications for office arrival and parking bay selection"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun onArrivalNotificationDismissed() {
        isNotificationDismissedForCurrentArrival = true
    }

    fun onNewArrivalEvent(timestamp: Long) {
        if (timestamp != lastNotifiedArrivalTimestamp) {
            lastNotifiedArrivalTimestamp = timestamp
            isNotificationDismissedForCurrentArrival = false
        }
    }

    fun showOfficeArrivalNotification(
        context: Context,
        officeName: String,
        arrivalTimestamp: Long = System.currentTimeMillis(),
        alertType: String = "until_dismissed",
        enforce: Boolean = true
    ) {
        if (!enforce && isNotificationDismissedForCurrentArrival && arrivalTimestamp == lastNotifiedArrivalTimestamp) {
            // User explicitly dismissed notification for this arrival event
            return
        }

        lastNotifiedArrivalTimestamp = arrivalTimestamp
        isNotificationDismissedForCurrentArrival = false

        // 1. Phone App Intent (Office Parking Screen)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "office_parking")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Car Auto Display Intent
        val carIntent = Intent(context, com.kspcr.parkalot.auto.ParkALotAutoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val carPendingIntent = PendingIntent.getActivity(
            context,
            2,
            carIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = PendingIntent.getBroadcast(
            context,
            999,
            Intent(context, GeofenceBroadcastReceiver::class.java).apply {
                action = "com.parkalot.app.ACTION_DISMISS_ARRIVAL_NOTIF"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isUntilDismissed = alertType.equals("until_dismissed", ignoreCase = true)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("📍 Arrived at $officeName")
            .setContentText("Tap to view available EV parking slots & bays.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You have arrived at $officeName. Tap to choose your floor and EV parking slot, or open Park A Lot Auto on your car screen.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deleteIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_compass,
                "🅿️ View Bays",
                pendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_directions,
                "🚗 Car Screen",
                carPendingIntent
            )

        if (!isUntilDismissed) {
            // Temporary alert
            notificationBuilder.setTimeoutAfter(90000L) // 90 seconds
        }

        try {
            NotificationManagerCompat.from(context).notify(OFFICE_ARRIVAL_NOTIF_ID, notificationBuilder.build())
        } catch (_: SecurityException) {
            // Handled when notification permission is not granted
        }
    }

    fun cancelOfficeArrivalNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(OFFICE_ARRIVAL_NOTIF_ID)
            isNotificationDismissedForCurrentArrival = false
        } catch (_: Exception) {}
    }

    fun showParkingSavedNotification(
        context: Context,
        placeName: String,
        floor: String? = null,
        slot: String? = null
    ) {
        val locationDesc = buildString {
            append(placeName)
            if (!floor.isNullOrBlank()) append(" - Floor $floor")
            if (!slot.isNullOrBlank()) append(", Slot $slot")
        }
        showParkingSavedNotification(context, locationDesc)
    }

    fun showParkingSavedNotification(context: Context, locationDesc: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "find_vehicle")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Vehicle Parking Saved")
            .setContentText("Vehicle saved at $locationDesc.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(PARKING_SAVED_NOTIF_ID, notification)
        } catch (_: SecurityException) {
            // Handled
        }
    }

    fun cancelAll(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (_: Exception) {
            // Handled
        }
    }
}
