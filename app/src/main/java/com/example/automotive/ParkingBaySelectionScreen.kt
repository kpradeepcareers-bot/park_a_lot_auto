package com.example.automotive

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.data.entity.OfficeEntity
import com.example.data.entity.ParkingSessionEntity
import com.example.data.entity.ParkingSlotEntity
import com.example.data.repository.ArrivalRepository
import com.example.data.repository.ParkingRepository
import com.example.location.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android Auto screen for selecting an available parking bay from the user's configured office parking slots.
 * Complies strictly with Android for Cars App Library rules and Driver Distraction Guidelines.
 */
class ParkingBaySelectionScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = ParkingRepository(carContext)
    private val arrivalRepository = ArrivalRepository.getInstance(carContext)
    private val scope = CoroutineScope(Dispatchers.Main)

    private var offices: List<OfficeEntity> = emptyList()
    private var allSlots: List<ParkingSlotEntity> = emptyList()
    private var isLoading = true

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                loadData()
            }
        })
    }

    private fun loadData() {
        scope.launch {
            offices = repository.getAllOffices()
            allSlots = repository.getAllSlots()
            isLoading = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()

        if (isLoading) {
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle("Loading Parking Bays...")
                    .addText("Fetching configured parking spaces...")
                    .build()
            )
            return ListTemplate.Builder()
                .setTitle("Select Parking Bay")
                .setHeaderAction(Action.BACK)
                .setSingleList(itemListBuilder.build())
                .build()
        }

        if (allSlots.isEmpty() && offices.isEmpty()) {
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle("No Configured Bays Found")
                    .addText("Add offices and parking bays in Park A Lot on your phone")
                    .build()
            )
        } else if (allSlots.isEmpty()) {
            val firstOffice = offices.firstOrNull()
            if (firstOffice != null) {
                itemListBuilder.addItem(
                    Row.Builder()
                        .setTitle("Park at ${firstOffice.name}")
                        .addText("Tap to park at ${firstOffice.name}")
                        .setOnClickListener {
                            handleDirectPark(firstOffice, "Floor 1", "Bay 1")
                        }
                        .build()
                )
            }
        } else {
            // Group slots by office if available, display dynamically
            allSlots.take(10).forEach { slot ->
                val officeName = offices.find { it.id == slot.officeId }?.name ?: "Office"
                val evIcon = if (slot.isEV) "⚡ " else "🅿️ "
                val subtitle = buildString {
                    append(officeName)
                    append(" • Floor ${slot.floor}")
                    if (!slot.zone.isNullOrBlank()) append(" • ${slot.zone}")
                    if (slot.isEV) append(" • EV Charger")
                    if (!slot.notes.isNullOrBlank()) append(" • ${slot.notes}")
                }

                itemListBuilder.addItem(
                    Row.Builder()
                        .setTitle("${evIcon}Bay ${slot.slotNumber}")
                        .addText(subtitle)
                        .setOnClickListener {
                            val targetOffice = offices.find { it.id == slot.officeId }
                            handleSlotSelected(slot, targetOffice)
                        }
                        .build()
                )
            }
        }

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Refresh")
                    .setOnClickListener {
                        isLoading = true
                        invalidate()
                        loadData()
                    }
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("Select Parking Bay")
            .setHeaderAction(Action.BACK)
            .setSingleList(itemListBuilder.build())
            .setActionStrip(actionStrip)
            .build()
    }

    private fun handleSlotSelected(slot: ParkingSlotEntity, office: OfficeEntity?) {
        scope.launch {
            val vehicle = repository.getDefaultVehicle()
            val arrival = arrivalRepository.getArrivalState()
            val officeName = office?.name ?: arrival.arrivalLocationName.ifBlank { "Office Parking" }
            val lat = office?.latitude ?: arrival.arrivalLatitude.takeIf { it != 0.0 } ?: 17.4375
            val lng = office?.longitude ?: arrival.arrivalLongitude.takeIf { it != 0.0 } ?: 78.3752

            val newSession = ParkingSessionEntity(
                vehicleId = vehicle?.id ?: 0,
                officeId = office?.id,
                placeName = officeName,
                placeType = "Office",
                latitude = lat,
                longitude = lng,
                floor = slot.floor,
                slotNumber = slot.slotNumber,
                building = officeName,
                wing = slot.zone,
                notes = slot.notes,
                parkedAt = System.currentTimeMillis(),
                isActive = true
            )
            repository.saveNewParkingSession(newSession)
            arrivalRepository.clearArrival()
            repository.preferences.setIsAtOffice(false, "")
            NotificationHelper.cancelOfficeArrivalNotification(carContext)

            CarToast.makeText(
                carContext,
                "Parked: $officeName (Floor ${slot.floor}, Bay ${slot.slotNumber})",
                CarToast.LENGTH_LONG
            ).show()

            screenManager.pop()
        }
    }

    private fun handleDirectPark(office: OfficeEntity, floor: String, slotNumber: String) {
        scope.launch {
            val vehicle = repository.getDefaultVehicle()
            val newSession = ParkingSessionEntity(
                vehicleId = vehicle?.id ?: 0,
                officeId = office.id,
                placeName = office.name,
                placeType = "Office",
                latitude = office.latitude,
                longitude = office.longitude,
                floor = floor,
                slotNumber = slotNumber,
                building = office.name,
                parkedAt = System.currentTimeMillis(),
                isActive = true
            )
            repository.saveNewParkingSession(newSession)
            arrivalRepository.clearArrival()
            repository.preferences.setIsAtOffice(false, "")
            NotificationHelper.cancelOfficeArrivalNotification(carContext)

            CarToast.makeText(
                carContext,
                "Parked at ${office.name}: Floor $floor, Bay $slotNumber",
                CarToast.LENGTH_LONG
            ).show()

            screenManager.pop()
        }
    }
}
