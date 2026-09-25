package com.example.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.ArrivalRepository
import com.example.data.repository.ParkingRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == "com.parkalot.app.ACTION_DISMISS_ARRIVAL_NOTIF") {
                    NotificationHelper.onArrivalNotificationDismissed()
                    return@launch
                }

                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    // Restore geofences on device reboot
                    GeofenceManager(context).registerAllOfficeGeofences()
                    return@launch
                }

                val geofencingEvent = GeofencingEvent.fromIntent(intent)
                if (geofencingEvent == null || geofencingEvent.hasError()) {
                    return@launch
                }

                val transition = geofencingEvent.geofenceTransition
                val repository = ParkingRepository(context)
                val arrivalRepo = ArrivalRepository.getInstance(context)

                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
                    val officeRequestId = triggeringGeofences.firstOrNull()?.requestId

                    val notificationsEnabled = repository.preferences.notificationsEnabledFlow.firstOrNull() ?: true
                    val alertType = repository.preferences.arrivalAlertTypeFlow.firstOrNull() ?: "until_dismissed"

                    var resolvedLocationId: Long? = null
                    var resolvedName = "Office"
                    var resolvedLat = 0.0
                    var resolvedLng = 0.0

                    if (!officeRequestId.isNullOrEmpty()) {
                        if (officeRequestId.startsWith("loc_")) {
                            val locId = officeRequestId.removePrefix("loc_").toLongOrNull()
                            if (locId != null) {
                                val loc = repository.getLocationById(locId)
                                if (loc != null) {
                                    resolvedName = loc.name
                                    resolvedLat = loc.latitude
                                    resolvedLng = loc.longitude
                                    val linkedOffice = repository.getAllOffices().find {
                                        it.savedLocationId == loc.id || it.name.equals(loc.name, ignoreCase = true)
                                    }
                                    resolvedLocationId = linkedOffice?.id ?: loc.id
                                }
                            }
                        } else if (officeRequestId.startsWith("office_")) {
                            val officeId = officeRequestId.removePrefix("office_").toLongOrNull()
                            if (officeId != null) {
                                val office = repository.getOfficeById(officeId)
                                if (office != null) {
                                    resolvedLocationId = office.id
                                    resolvedName = office.name
                                    resolvedLat = office.latitude
                                    resolvedLng = office.longitude
                                }
                            }
                        }
                    }

                    val now = System.currentTimeMillis()
                    NotificationHelper.onNewArrivalEvent(now)

                    // 1. Persist shared arrival state in ArrivalRepository
                    arrivalRepo.setArrival(
                        active = true,
                        locationId = resolvedLocationId,
                        locationName = resolvedName,
                        timestamp = now,
                        latitude = resolvedLat,
                        longitude = resolvedLng
                    )

                    // 2. Update preferences state
                    repository.preferences.setIsAtOffice(true, resolvedName)

                    // 3. Notify user safely if enabled
                    if (notificationsEnabled) {
                        NotificationHelper.showOfficeArrivalNotification(
                            context = context,
                            officeName = resolvedName,
                            arrivalTimestamp = now,
                            alertType = alertType,
                            enforce = true
                        )
                    }

                    // 4. Update Android Auto Renderer dynamically and open on car if connected
                    com.kspcr.parkalot.auto.ParkALotAutoDataBridge.onGeofenceArrival(
                        context = context,
                        arrived = true,
                        officeName = resolvedName,
                        locationId = resolvedLocationId
                    )

                    if (com.kspcr.parkalot.auto.ParkALotAutoDataBridge.isCarConnected(context)) {
                        com.kspcr.parkalot.auto.ParkALotAutoDataBridge.openAutoRenderer(context, resolvedName)
                    }
                } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    arrivalRepo.clearArrival()
                    repository.preferences.setIsAtOffice(false, "")
                    NotificationHelper.cancelOfficeArrivalNotification(context)

                    // Update Android Auto Renderer
                    com.kspcr.parkalot.auto.ParkALotAutoDataBridge.onGeofenceArrival(
                        context = context,
                        arrived = false,
                        officeName = "",
                        locationId = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
