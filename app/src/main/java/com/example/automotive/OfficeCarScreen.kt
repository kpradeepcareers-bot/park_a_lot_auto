package com.example.automotive

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
import com.example.data.entity.ParkingSlotEntity
import com.example.data.entity.VehicleEntity
import com.example.data.repository.ArrivalRepository
import com.example.data.repository.ParkingRepository
import com.example.location.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OfficeCarScreen(
    carContext: CarContext,
    private val targetOfficeId: Long? = null,
    private val arrivalDistanceMeters: Float? = null
) : Screen(carContext) {

    private val repository = ParkingRepository(carContext)
    private val arrivalRepository = ArrivalRepository.getInstance(carContext)
    private val scope = CoroutineScope(Dispatchers.Main)

    private var office: OfficeEntity? = null
    private var defaultVehicle: VehicleEntity? = null
    private var allOfficeSlots: List<ParkingSlotEntity> = emptyList()
    private var availableFloors: List<String> = emptyList()
    private var selectedFloor: String? = null
    private var isLoading = true

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                scope.launch {
                    val arrival = arrivalRepository.getArrivalState()
                    val resolvedId = targetOfficeId ?: arrival.arrivalLocationId

                    val resolvedOffice = if (resolvedId != null) {
                        repository.getOfficeById(resolvedId)
                            ?: repository.getAllOffices().find { it.id == resolvedId }
                    } else {
                        repository.getDefaultOffice() ?: repository.getAllOffices().firstOrNull()
                    }

                    office = resolvedOffice
                    defaultVehicle = repository.getDefaultVehicle()

                    if (resolvedOffice != null) {
                        val slots = repository.getSlotsByOffice(resolvedOffice.id)
                        allOfficeSlots = slots
                        val distinctFloors = slots.map { it.floor.trim() }.filter { it.isNotBlank() }.distinct()
                        availableFloors = if (distinctFloors.isNotEmpty()) distinctFloors else listOf("B1", "B2", "Floor 1")
                        if (distinctFloors.size == 1) {
                            selectedFloor = distinctFloors.first()
                        }
                    }
                    isLoading = false
                    invalidate()
                }
            }
        })
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val currentOffice = office

        val headerAction = if (screenManager.stackSize > 1) {
            Action.BACK
        } else {
            Action.APP_ICON
        }

        if (isLoading) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Loading Parking Bays...")
                    .addText("Connecting to parking directory...")
                    .build()
            )
            return ListTemplate.Builder()
                .setTitle("Select Parking Bay")
                .setHeaderAction(headerAction)
                .setSingleList(listBuilder.build())
                .build()
        }

        if (currentOffice == null) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("No Office Location Found")
                    .addText("Please configure a work location in 'My Locations' on your phone")
                    .build()
            )
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Go to Dashboard")
                    .setOnClickListener { navigateToDashboard() }
                    .build()
            )
            return ListTemplate.Builder()
                .setTitle("Select Parking Bay")
                .setHeaderAction(headerAction)
                .setSingleList(listBuilder.build())
                .build()
        }

        // STEP 1: Floor selection if multiple floors and no floor selected yet
        val currentFloor = selectedFloor
        if (currentFloor == null && availableFloors.size > 1) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Select Parking Floor")
                    .addText("Choose the level where you are parking at ${currentOffice.name}")
                    .build()
            )

            for (fl in availableFloors) {
                val count = allOfficeSlots.count { it.floor.equals(fl, ignoreCase = true) }
                val evCount = allOfficeSlots.count { it.floor.equals(fl, ignoreCase = true) && it.isEV }
                val desc = if (count > 0) {
                    "$count bays available${if (evCount > 0) " • $evCount EV chargers" else ""}"
                } else {
                    "Open floor • Tap to select"
                }

                listBuilder.addItem(
                    Row.Builder()
                        .setTitle("Floor $fl")
                        .addText(desc)
                        .setOnClickListener {
                            selectedFloor = fl
                            invalidate()
                        }
                        .build()
                )
            }

            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Return to Dashboard")
                    .setOnClickListener { navigateToDashboard() }
                    .build()
            )

            return ListTemplate.Builder()
                .setTitle("${currentOffice.name} - Select Floor")
                .setHeaderAction(headerAction)
                .setSingleList(listBuilder.build())
                .build()
        }

        // STEP 2: Slots for the selected floor (or only floor)
        val activeFloor = currentFloor ?: availableFloors.firstOrNull() ?: "B1"
        val floorSlots = allOfficeSlots.filter { it.floor.equals(activeFloor, ignoreCase = true) }

        // Switch floor header row if multiple floors exist
        if (availableFloors.size > 1) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Floor $activeFloor (Tap to change floor)")
                    .addText("Switch to a different parking level")
                    .setOnClickListener {
                        selectedFloor = null
                        invalidate()
                    }
                    .build()
            )
        }

        if (floorSlots.isEmpty()) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Park on Floor $activeFloor")
                    .addText("Confirm parking on Floor $activeFloor")
                    .setOnClickListener {
                        confirmParking(currentOffice, activeFloor, "Open Bay")
                    }
                    .build()
            )
        } else {
            floorSlots.take(10).forEach { slot ->
                val evIcon = if (slot.isEV) "⚡ " else "🅿️ "
                val subtitle = buildString {
                    append("Floor ${slot.floor}")
                    if (!slot.zone.isNullOrBlank()) append(" • ${slot.zone}")
                    if (slot.isEV) append(" • EV Charger")
                    if (!slot.notes.isNullOrBlank()) append(" • ${slot.notes}")
                }

                listBuilder.addItem(
                    Row.Builder()
                        .setTitle("${evIcon}Bay ${slot.slotNumber}")
                        .addText(subtitle)
                        .setOnClickListener {
                            confirmParking(currentOffice, slot.floor, slot.slotNumber, slot.zone, slot.notes)
                        }
                        .build()
                )
            }
        }

        // Minimise / Return action
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Return to Dashboard")
                .setOnClickListener {
                    navigateToDashboard()
                }
                .build()
        )

        val screenTitle = "${currentOffice.name} • Floor $activeFloor"

        return ListTemplate.Builder()
            .setTitle(screenTitle)
            .setHeaderAction(headerAction)
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun navigateToDashboard() {
        if (screenManager.stackSize > 1) {
            screenManager.pop()
        } else {
            screenManager.push(MainCarScreen(carContext))
        }
    }

    private fun confirmParking(
        currentOffice: OfficeEntity,
        floor: String,
        slotNumber: String,
        zone: String? = null,
        notes: String? = null
    ) {
        scope.launch {
            val vehicle = defaultVehicle ?: repository.getDefaultVehicle()
            val newSession = ParkingSessionEntity(
                vehicleId = vehicle?.id ?: 0,
                officeId = currentOffice.id,
                placeName = currentOffice.name,
                placeType = "Office",
                latitude = currentOffice.latitude,
                longitude = currentOffice.longitude,
                floor = floor,
                slotNumber = slotNumber,
                building = currentOffice.name,
                wing = zone,
                notes = notes,
                parkedAt = System.currentTimeMillis(),
                isActive = true
            )
            repository.saveNewParkingSession(newSession)
            // Clear arrival in shared repository
            arrivalRepository.clearArrival()
            repository.preferences.setIsAtOffice(false, "")
            NotificationHelper.cancelOfficeArrivalNotification(carContext)

            CarToast.makeText(
                carContext,
                "Parked at ${currentOffice.name}: Floor $floor, Bay $slotNumber",
                CarToast.LENGTH_LONG
            ).show()

            navigateToDashboard()
        }
    }
}
