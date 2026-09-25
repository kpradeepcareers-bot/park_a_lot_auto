package com.example.automotive

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.data.entity.OfficeEntity
import com.example.data.entity.ParkingSessionEntity
import com.example.data.entity.VehicleEntity
import com.example.data.repository.ArrivalRepository
import com.example.data.repository.ArrivalState
import com.example.data.repository.ParkingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainCarScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = ParkingRepository(carContext)
    private val arrivalRepository = ArrivalRepository.getInstance(carContext)
    private val scope = CoroutineScope(Dispatchers.Main)

    private var activeSession: ParkingSessionEntity? = null
    private var defaultVehicle: VehicleEntity? = null
    private var workOffices: List<OfficeEntity> = emptyList()
    private var currentArrival: ArrivalState = ArrivalState()
    private var hasAutoPopped = false
    private var isAndroidAutoEnabled = true
    private var flowJob: Job? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                flowJob = scope.launch {
                    repository.preferences.androidAutoEnabledFlow.collect { enabled ->
                        isAndroidAutoEnabled = enabled
                        invalidate()
                    }
                }

                scope.launch {
                    repository.activeSessionFlow.collect { session ->
                        activeSession = session
                        invalidate()
                    }
                }

                scope.launch {
                    defaultVehicle = repository.getDefaultVehicle()
                    workOffices = repository.getAllOffices()
                    invalidate()
                }

                // Shared Arrival State observation:
                // When geofence ENTER persists arrivalActive = true, immediately refresh car UI
                scope.launch {
                    arrivalRepository.arrivalStateFlow.collect { arrival ->
                        currentArrival = arrival
                        invalidate()

                        if (arrival.arrivalActive && !hasAutoPopped && activeSession == null) {
                            hasAutoPopped = true
                            screenManager.push(
                                OfficeCarScreen(
                                    carContext = carContext,
                                    targetOfficeId = arrival.arrivalLocationId
                                )
                            )
                        }
                    }
                }

                // Initial read on screen opening (if Android Auto connected after geofence triggered)
                scope.launch {
                    val initial = arrivalRepository.getArrivalState()
                    currentArrival = initial
                    if (initial.arrivalActive && !hasAutoPopped && activeSession == null) {
                        hasAutoPopped = true
                        screenManager.push(
                            OfficeCarScreen(
                                carContext = carContext,
                                targetOfficeId = initial.arrivalLocationId
                            )
                        )
                    }
                    invalidate()
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                flowJob?.cancel()
            }
        })
    }

    override fun onGetTemplate(): Template {
        if (!isAndroidAutoEnabled) {
            val disabledList = ItemList.Builder()
                .addItem(
                    Row.Builder()
                        .setTitle("Android Auto Integration Paused")
                        .addText("Enable Android Auto in Park A Lot > Settings on your phone")
                        .build()
                )
                .build()

            return ListTemplate.Builder()
                .setTitle("Park A Lot")
                .setSingleList(disabledList)
                .setHeaderAction(Action.APP_ICON)
                .build()
        }

        val listBuilder = ItemList.Builder()

        // 0. Active Geofence Arrival Row (Priority 1 if user has arrived at work/office)
        if (currentArrival.arrivalActive) {
            val locName = currentArrival.arrivalLocationName.ifBlank { "Office" }
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("⚡ ARRIVED: $locName")
                    .addText("Geofence detected arrival. Tap to select floor & parking bay.")
                    .setOnClickListener {
                        screenManager.push(
                            OfficeCarScreen(
                                carContext = carContext,
                                targetOfficeId = currentArrival.arrivalLocationId
                            )
                        )
                    }
                    .build()
            )
        }

        // 1. Current Active Parking Status Row
        val session = activeSession
        if (session != null) {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeStr = sdf.format(Date(session.parkedAt))
            val detail = buildString {
                if (!session.floor.isNullOrBlank()) append("Floor ${session.floor}")
                if (!session.slotNumber.isNullOrBlank()) {
                    if (isNotEmpty()) append(", ")
                    append("Slot ${session.slotNumber}")
                }
                append(" • Parked at $timeStr")
            }

            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Parked: ${session.placeName}")
                    .addText(detail)
                    .setOnClickListener {
                        val navUri = Uri.parse("geo:${session.latitude},${session.longitude}?q=${session.latitude},${session.longitude}")
                        val navIntent = Intent(CarContext.ACTION_NAVIGATE, navUri)
                        try {
                            carContext.startCarApp(navIntent)
                        } catch (_: Exception) {
                            carContext.startActivity(Intent(Intent.ACTION_VIEW, navUri))
                        }
                    }
                    .build()
            )

            // Mark vehicle as retrieved action row
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Mark Vehicle as Retrieved")
                    .addText("Clear active spot and log departure time")
                    .setOnClickListener {
                        scope.launch {
                            repository.markSessionAsRetrieved(session.id)
                            arrivalRepository.clearArrival()
                            invalidate()
                        }
                    }
                    .build()
            )
        } else if (!currentArrival.arrivalActive) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("No Active Parking Recorded")
                    .addText("Vehicle is currently on the move")
                    .build()
            )
        }

        // 2. Office EV Parking Quick Access
        if (workOffices.isNotEmpty()) {
            for (office in workOffices) {
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle("Office Parking: ${office.name}")
                        .addText("Select floor & available parking bays")
                        .setOnClickListener {
                            screenManager.push(
                                OfficeCarScreen(
                                    carContext = carContext,
                                    targetOfficeId = office.id
                                )
                            )
                        }
                        .build()
                )
            }
        } else {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Office Parking")
                    .addText("Define a Work/Office location in 'My Locations' on phone")
                    .setOnClickListener {
                        CarToast.makeText(
                            carContext,
                            "Please set up an Office location in My Locations on your phone",
                            CarToast.LENGTH_SHORT
                        ).show()
                    }
                    .build()
            )
        }

        // 3. Quick Park at Current Location
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Quick Park Here")
                .addText("Save current coordinates as parking spot")
                .setOnClickListener {
                    scope.launch {
                        val vehicle = defaultVehicle ?: repository.getDefaultVehicle()
                        if (vehicle != null) {
                            val newSession = ParkingSessionEntity(
                                vehicleId = vehicle.id,
                                placeName = "Current Location",
                                placeType = "Other",
                                latitude = currentArrival.arrivalLatitude.takeIf { it != 0.0 } ?: 17.4375,
                                longitude = currentArrival.arrivalLongitude.takeIf { it != 0.0 } ?: 78.3752,
                                parkedAt = System.currentTimeMillis(),
                                isActive = true
                            )
                            repository.saveNewParkingSession(newSession)
                            arrivalRepository.clearArrival()
                            com.example.location.NotificationHelper.cancelOfficeArrivalNotification(carContext)
                            CarToast.makeText(carContext, "Current location saved!", CarToast.LENGTH_SHORT).show()
                            invalidate()
                        }
                    }
                }
                .build()
        )

        // 4. Select Parking Bay
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Select Parking Bay")
                .addText("Browse & select available parking bays")
                .setOnClickListener {
                    screenManager.push(ParkingBaySelectionScreen(carContext))
                }
                .build()
        )

        // 5. Parking History Row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Parking History")
                .addText("View recent parking locations")
                .setOnClickListener {
                    screenManager.push(HistoryCarScreen(carContext))
                }
                .build()
        )

        val vehicleTitle = defaultVehicle?.let { "${it.name} (${it.registrationNumber})" } ?: "Park a lot"

        return ListTemplate.Builder()
            .setTitle(vehicleTitle)
            .setSingleList(listBuilder.build())
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
