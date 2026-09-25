package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.data.entity.OfficeEntity
import com.example.data.entity.SavedLocationEntity
import com.example.data.repository.ParkingRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = "com.parkalot.app.ACTION_GEOFENCE_EVENT"
        }
        PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation
    }

    @SuppressLint("MissingPermission")
    fun addLocationGeofence(location: SavedLocationEntity, onComplete: (Boolean) -> Unit = {}) {
        if (!hasLocationPermission()) {
            onComplete(false)
            return
        }

        val geofenceId = "loc_${location.id}"
        val radius = if (location.geofenceRadius > 0f) location.geofenceRadius else 200f

        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(location.latitude, location.longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    @SuppressLint("MissingPermission")
    fun addOfficeGeofence(office: OfficeEntity, onComplete: (Boolean) -> Unit = {}) {
        if (!hasLocationPermission()) {
            onComplete(false)
            return
        }

        val geofenceId = "office_${office.id}"
        val radius = if (office.geofenceRadius > 0f) office.geofenceRadius else 150f

        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(office.latitude, office.longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun removeOfficeGeofence(officeId: Long, onComplete: (Boolean) -> Unit = {}) {
        val geofenceId = "office_$officeId"
        geofencingClient.removeGeofences(listOf(geofenceId))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun removeLocationGeofence(locationId: Long, onComplete: (Boolean) -> Unit = {}) {
        val geofenceId = "loc_$locationId"
        geofencingClient.removeGeofences(listOf(geofenceId))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun removeAllGeofences(onComplete: (Boolean) -> Unit = {}) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    suspend fun registerAllOfficeGeofences() {
        if (!hasLocationPermission()) return
        val repository = ParkingRepository(context)

        // Only register geofences for location profiles tagged as office/work
        val workLocations = repository.getWorkLocations()
        for (loc in workLocations) {
            addLocationGeofence(loc)
        }

        // Also register for offices that correspond to work locations
        val offices = repository.getAllOffices()
        for (office in offices) {
            // Check if office is linked to a work location or tagged
            val isWorkRelated = workLocations.any { it.name.equals(office.name, ignoreCase = true) }
            if (isWorkRelated || office.isDefault) {
                addOfficeGeofence(office)
            }
        }
    }
}
