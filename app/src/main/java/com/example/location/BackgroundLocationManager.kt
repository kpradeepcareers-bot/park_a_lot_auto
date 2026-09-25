package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.entity.OfficeEntity
import com.example.data.local.ParkALotDatabase
import com.example.data.repository.ArrivalRepository
import com.example.data.repository.ParkingRepository
import com.kspcr.parkalot.auto.ParkALotAutoDataBridge
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult as GmsLocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State representing live auto-detected location and office proximity.
 */
data class AutoLocationState(
    val isChecking: Boolean = false,
    val lastLocation: LocationResult? = null,
    val locationDetails: LocationHelper.LocationDetails? = null,
    val nearestOffice: OfficeEntity? = null,
    val nearestOfficeDistanceMeters: Float? = null,
    val isInsideOfficeGeofence: Boolean = false,
    val detectedOfficeName: String? = null,
    val lastCheckTimestamp: Long = 0L,
    val statusMessage: String = "Location auto-detection ready",
    val isSimulated: Boolean = false
)

/**
 * Robust Singleton Manager for auto-detecting location in real-time
 * and automatically monitoring work/office arrival in both foreground and background.
 */
object BackgroundLocationManager {

    private const val TAG = "ParkALotLocation"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var isMonitoring = false

    private val _locationState = MutableStateFlow(AutoLocationState())
    val locationState: StateFlow<AutoLocationState> = _locationState.asStateFlow()

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    /**
     * Initializes and starts background / foreground continuous location tracking.
     */
    @SuppressLint("MissingPermission")
    fun startLocationMonitoring(context: Context) {
        val appContext = context.applicationContext
        if (!hasLocationPermission(appContext)) {
            Log.w(TAG, "Cannot start location monitoring: permissions missing")
            _locationState.value = _locationState.value.copy(
                statusMessage = "Location permission needed"
            )
            return
        }

        if (isMonitoring) {
            Log.d(TAG, "Location monitoring already active")
            return
        }

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000L)
                .setMinUpdateIntervalMillis(8000L)
                .setMinUpdateDistanceMeters(10f)
                .setWaitForAccurateLocation(false)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: GmsLocationResult) {
                    val lastLoc = result.lastLocation ?: return
                    scope.launch {
                        processNewLocation(appContext, lastLoc, isSimulated = false)
                    }
                }
            }

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )

            isMonitoring = true
            Log.i(TAG, "Continuous auto-location monitoring started successfully")

            // Trigger immediate single check
            checkLocationNow(appContext)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates: ${e.message}", e)
        }
    }

    /**
     * Stops background location updates.
     */
    fun stopLocationMonitoring() {
        try {
            locationCallback?.let { callback ->
                fusedLocationClient?.removeLocationUpdates(callback)
            }
            isMonitoring = false
            Log.i(TAG, "Location monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location monitoring: ${e.message}", e)
        }
    }

    /**
     * Performs an immediate one-shot auto-detect location check and evaluates
     * proximity to all defined offices and work locations.
     */
    fun checkLocationNow(context: Context, onComplete: ((AutoLocationState) -> Unit)? = null) {
        val appContext = context.applicationContext
        _locationState.value = _locationState.value.copy(isChecking = true, statusMessage = "Detecting current location...")

        scope.launch {
            try {
                if (!hasLocationPermission(appContext)) {
                    _locationState.value = _locationState.value.copy(
                        isChecking = false,
                        statusMessage = "Location permission required"
                    )
                    withContext(Dispatchers.Main) { onComplete?.invoke(_locationState.value) }
                    return@launch
                }

                val helper = LocationHelper(appContext)
                val locResult = helper.getCurrentLocation()

                if (locResult != null) {
                    val loc = Location("FusedProvider").apply {
                        latitude = locResult.latitude
                        longitude = locResult.longitude
                        accuracy = locResult.accuracy
                    }
                    val updatedState = processNewLocation(appContext, loc, isSimulated = false)
                    withContext(Dispatchers.Main) { onComplete?.invoke(updatedState) }
                } else {
                    _locationState.value = _locationState.value.copy(
                        isChecking = false,
                        statusMessage = "Unable to get GPS fix. Check device location settings."
                    )
                    withContext(Dispatchers.Main) { onComplete?.invoke(_locationState.value) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking location: ${e.message}", e)
                _locationState.value = _locationState.value.copy(
                    isChecking = false,
                    statusMessage = "Location check error: ${e.message}"
                )
                withContext(Dispatchers.Main) { onComplete?.invoke(_locationState.value) }
            }
        }
    }

    /**
     * Simulates arriving at a specific office (for testing without moving).
     */
    fun simulateOfficeArrival(context: Context, office: OfficeEntity) {
        val appContext = context.applicationContext
        scope.launch {
            val loc = Location("Simulation").apply {
                latitude = office.latitude
                longitude = office.longitude
                accuracy = 5f
            }
            processNewLocation(appContext, loc, isSimulated = true, forcedOffice = office)
        }
    }

    /**
     * Resets simulated location and restores live GPS detection.
     */
    fun resetSimulation(context: Context) {
        checkLocationNow(context)
    }

    /**
     * Core processing engine: resolves place details, calculates distance to all
     * defined offices / work locations, and triggers arrival/exit events.
     */
    private suspend fun processNewLocation(
        context: Context,
        location: Location,
        isSimulated: Boolean = false,
        forcedOffice: OfficeEntity? = null
    ): AutoLocationState {
        val helper = LocationHelper(context)
        val details = helper.getDetailsFromCoordinates(location.latitude, location.longitude)
        val locationResult = LocationResult(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            address = details.address
        )

        val database = ParkALotDatabase.getDatabase(context)
        val officeDao = database.officeDao()
        val savedLocationDao = database.savedLocationDao()
        val arrivalRepo = ArrivalRepository.getInstance(context)
        val parkingRepo = ParkingRepository(context)

        val allOffices = officeDao.getAllOffices()
        val workSavedLocations = savedLocationDao.getWorkLocations()

        // Build list of candidate office/work places
        val candidateOffices = mutableListOf<OfficeEntity>()
        candidateOffices.addAll(allOffices)

        for (workLoc in workSavedLocations) {
            if (candidateOffices.none { it.name.equals(workLoc.name, ignoreCase = true) || it.savedLocationId == workLoc.id }) {
                candidateOffices.add(
                    OfficeEntity(
                        id = workLoc.id,
                        name = workLoc.name,
                        latitude = workLoc.latitude,
                        longitude = workLoc.longitude,
                        address = workLoc.address,
                        geofenceRadius = workLoc.geofenceRadius,
                        savedLocationId = workLoc.id
                    )
                )
            }
        }

        var nearestOffice: OfficeEntity? = forcedOffice
        var minDistance = Float.MAX_VALUE
        var isInsideGeofence = false

        if (forcedOffice != null) {
            minDistance = 0f
            isInsideGeofence = true
        } else if (candidateOffices.isNotEmpty()) {
            for (office in candidateOffices) {
                val distanceArr = FloatArray(1)
                Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    office.latitude,
                    office.longitude,
                    distanceArr
                )
                val distance = distanceArr[0]
                if (distance < minDistance) {
                    minDistance = distance
                    nearestOffice = office
                }
            }

            if (nearestOffice != null) {
                val radius = if (nearestOffice.geofenceRadius > 0f) nearestOffice.geofenceRadius else 200f
                isInsideGeofence = minDistance <= radius
            }
        }

        val officeName = nearestOffice?.name ?: ""

        val statusMsg = when {
            isInsideGeofence && nearestOffice != null -> {
                "📍 Inside ${nearestOffice.name} (${minDistance.toInt()}m away)"
            }
            nearestOffice != null -> {
                if (minDistance < 1000) "${minDistance.toInt()}m from ${nearestOffice.name}"
                else "%.1f km from ${nearestOffice.name}".format(minDistance / 1000f)
            }
            else -> "No offices configured"
        }

        val newState = AutoLocationState(
            isChecking = false,
            lastLocation = locationResult,
            locationDetails = details,
            nearestOffice = nearestOffice,
            nearestOfficeDistanceMeters = if (minDistance == Float.MAX_VALUE) null else minDistance,
            isInsideOfficeGeofence = isInsideGeofence,
            detectedOfficeName = if (isInsideGeofence) officeName else null,
            lastCheckTimestamp = System.currentTimeMillis(),
            statusMessage = statusMsg,
            isSimulated = isSimulated
        )

        _locationState.value = newState

        // Auto trigger arrival / exit state across app
        val currentArrival = arrivalRepo.getArrivalState()
        if (isInsideGeofence && nearestOffice != null) {
            // Update preferences & arrival repository
            parkingRepo.preferences.setIsAtOffice(true, nearestOffice.name)
            arrivalRepo.setArrival(
                active = true,
                locationId = nearestOffice.id,
                locationName = nearestOffice.name,
                timestamp = System.currentTimeMillis(),
                latitude = nearestOffice.latitude,
                longitude = nearestOffice.longitude
            )

            // Enforce high-priority office arrival notification on the phone
            val notifEnabled = parkingRepo.preferences.notificationsEnabledFlow.firstOrNull() ?: true
            val alertType = parkingRepo.preferences.arrivalAlertTypeFlow.firstOrNull() ?: "until_dismissed"
            if (notifEnabled) {
                NotificationHelper.showOfficeArrivalNotification(
                    context = context,
                    officeName = nearestOffice.name,
                    arrivalTimestamp = System.currentTimeMillis(),
                    alertType = alertType,
                    enforce = true
                )
            }

            // Sync to Android Auto Renderer and automatically open on car screen if connected
            ParkALotAutoDataBridge.onGeofenceArrival(
                context = context,
                arrived = true,
                officeName = nearestOffice.name,
                locationId = nearestOffice.id
            )

            if (ParkALotAutoDataBridge.isCarConnected(context)) {
                withContext(Dispatchers.Main) {
                    ParkALotAutoDataBridge.openAutoRenderer(context, nearestOffice.name)
                }
            }

        } else if (!isInsideGeofence && currentArrival.arrivalActive) {
            // Exit detected (with 50m margin)
            val threshold = (nearestOffice?.geofenceRadius ?: 200f) + 50f
            if (minDistance > threshold) {
                parkingRepo.preferences.setIsAtOffice(false, "")
                arrivalRepo.clearArrival()

                ParkALotAutoDataBridge.onGeofenceArrival(
                    context = context,
                    arrived = false,
                    officeName = "",
                    locationId = null
                )
            }
        }

        Log.d(TAG, "Location processed: ${details.name} (Lat: ${location.latitude}, Lng: ${location.longitude}) -> Inside: $isInsideGeofence, Nearest: ${nearestOffice?.name} (${minDistance}m)")
        return newState
    }
}
