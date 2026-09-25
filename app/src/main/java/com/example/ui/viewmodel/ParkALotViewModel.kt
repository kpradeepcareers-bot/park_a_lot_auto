package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ParkALotApp
import com.example.data.local.PreloadedData
import com.example.data.entity.OfficeEntity
import com.example.data.entity.ParkingSessionEntity
import com.example.data.entity.ParkingSlotEntity
import com.example.data.entity.SavedLocationEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.VehicleEntity
import com.example.data.repository.ParkingRepository
import com.example.location.GeofenceManager
import com.example.location.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ParkALotViewModel(application: Application) : AndroidViewModel(application) {

    val repository: ParkingRepository = (application as ParkALotApp).repository
    private val geofenceManager = GeofenceManager(application)

    // Office Arrival Auto Pop-Up State
    val isArrivalPopUpVisible = MutableStateFlow(false)
    val isArrivalPopUpMinimized = MutableStateFlow(false)
    val arrivalOffice = MutableStateFlow<OfficeEntity?>(null)
    val arrivalDistanceMeters = MutableStateFlow<Float?>(null)
    val arrivalSlots = MutableStateFlow<List<ParkingSlotEntity>>(emptyList())
    private var lastDismissedOfficeId: Long? = null

    // Preferences & Flags
    val onboardingCompleted: StateFlow<Boolean> = repository.preferences.onboardingCompletedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notificationsEnabled: StateFlow<Boolean> = repository.preferences.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val geofencingEnabled: StateFlow<Boolean> = repository.preferences.geofencingEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val distanceUnit: StateFlow<String> = repository.preferences.distanceUnitFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "meters")

    val isAtOffice: StateFlow<Boolean> = repository.preferences.isAtOfficeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentOfficeName: StateFlow<String> = repository.preferences.currentOfficeNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val wakeWordListening: StateFlow<Boolean> = repository.preferences.wakeWordListeningFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val voiceAssistantEnabled: StateFlow<Boolean> = repository.preferences.voiceAssistantEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val androidAutoEnabled: StateFlow<Boolean> = repository.preferences.androidAutoEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val locationCheckIntervalSeconds: StateFlow<Int> = repository.preferences.locationCheckIntervalSecondsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val arrivalAlertType: StateFlow<String> = repository.preferences.arrivalAlertTypeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "until_dismissed")

    // User Profile
    val userProfile: StateFlow<UserEntity?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Vehicles
    val vehicles: StateFlow<List<VehicleEntity>> = repository.allVehiclesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultVehicle: StateFlow<VehicleEntity?> = repository.defaultVehicleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedVehicleId = MutableStateFlow<Long?>(null)
    val selectedVehicleId: StateFlow<Long?> = _selectedVehicleId.asStateFlow()

    val selectedVehicle: StateFlow<VehicleEntity?> = combine(vehicles, selectedVehicleId, defaultVehicle) { list, selId, def ->
        if (selId != null) list.find { it.id == selId } ?: def ?: list.firstOrNull()
        else def ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Parking Sessions - Strictly isolated to the selected vehicle
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeParkingSession: StateFlow<ParkingSessionEntity?> = selectedVehicle
        .flatMapLatest { vehicle ->
            if (vehicle != null) {
                repository.getActiveSessionForVehicleFlow(vehicle.id)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val parkingHistory: StateFlow<List<ParkingSessionEntity>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Locations
    val savedLocations: StateFlow<List<SavedLocationEntity>> = repository.savedLocationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Offices & Slots - Restrict to locations tagged as Office or Work
    val offices: StateFlow<List<OfficeEntity>> = repository.allOfficesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workOffices: StateFlow<List<OfficeEntity>> = combine(offices, savedLocations) { officeList, locList ->
        val workLocIds = locList.filter {
            it.isWorkLocation ||
            it.type.equals("Office", ignoreCase = true) ||
            it.type.equals("Work", ignoreCase = true)
        }.map { it.id }.toSet()

        officeList.filter { office ->
            office.savedLocationId != null && workLocIds.contains(office.savedLocationId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultOffice: StateFlow<OfficeEntity?> = workOffices.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedOfficeId = MutableStateFlow<Long?>(null)
    val selectedOfficeId: StateFlow<Long?> = _selectedOfficeId.asStateFlow()

    fun selectOfficeId(id: Long) {
        _selectedOfficeId.value = id
        loadOfficeSlotsAndHistory(id)
    }

    val selectedOffice: StateFlow<OfficeEntity?> = combine(workOffices, selectedOfficeId, defaultOffice) { list, selId, def ->
        if (selId != null) list.find { it.id == selId } ?: def ?: list.firstOrNull()
        else def ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedFloor = MutableStateFlow<String>("B1")
    val selectedFloor: StateFlow<String> = _selectedFloor.asStateFlow()

    private val _onlyEvFilter = MutableStateFlow(true)
    val onlyEvFilter: StateFlow<Boolean> = _onlyEvFilter.asStateFlow()

    val availableFloors = MutableStateFlow<List<String>>(listOf("B1", "B3", "B4"))
    val currentFloorSlots = MutableStateFlow<List<ParkingSlotEntity>>(emptyList())
    val previousOfficeParking = MutableStateFlow<ParkingSessionEntity?>(null)

    val isCurrentVehicleEV: StateFlow<Boolean> = selectedVehicle.map { vehicle ->
        vehicle?.fuelType?.equals("Electric", ignoreCase = true) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Auto-detection of nearest office on tab open
    val isAutoDetectingOffice = MutableStateFlow(false)
    val autoDetectedOfficeDistanceMeters = MutableStateFlow<Float?>(null)
    val autoDetectedOfficeName = MutableStateFlow<String?>(null)

    fun autoDetectAndSelectNearestOffice() {
        viewModelScope.launch {
            try {
                isAutoDetectingOffice.value = true
                val locHelper = com.example.location.LocationHelper(getApplication())
                val currentLoc = locHelper.getCurrentLocation()
                
                val allOfficesList = repository.getAllOffices()
                val workLocs = repository.getWorkLocations()
                
                val candidateOffices = if (allOfficesList.isNotEmpty()) {
                    allOfficesList
                } else if (workLocs.isNotEmpty()) {
                    workLocs.map { loc ->
                        OfficeEntity(
                            id = loc.id,
                            name = loc.name,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            address = loc.address,
                            geofenceRadius = loc.geofenceRadius,
                            savedLocationId = loc.id
                        )
                    }
                } else {
                    emptyList()
                }

                if (currentLoc != null && candidateOffices.isNotEmpty()) {
                    var closestOffice: OfficeEntity? = null
                    var minDistance = Float.MAX_VALUE

                    for (office in candidateOffices) {
                        val distanceArr = FloatArray(1)
                        android.location.Location.distanceBetween(
                            currentLoc.latitude,
                            currentLoc.longitude,
                            office.latitude,
                            office.longitude,
                            distanceArr
                        )
                        val dist = distanceArr[0]
                        if (dist < minDistance) {
                            minDistance = dist
                            closestOffice = office
                        }
                    }

                    if (closestOffice != null) {
                        autoDetectedOfficeDistanceMeters.value = minDistance
                        autoDetectedOfficeName.value = closestOffice.name
                        _selectedOfficeId.value = closestOffice.id
                        loadOfficeSlotsAndHistory(closestOffice.id)
                    }
                } else if (candidateOffices.isNotEmpty() && _selectedOfficeId.value == null) {
                    val firstOffice = defaultOffice.value ?: candidateOffices.firstOrNull()
                    if (firstOffice != null) {
                        _selectedOfficeId.value = firstOffice.id
                        loadOfficeSlotsAndHistory(firstOffice.id)
                    }
                }
            } catch (_: Exception) {
                // Graceful fallback
            } finally {
                isAutoDetectingOffice.value = false
            }
        }
    }

    // Auto Location & Background Monitoring State
    val autoLocationState: StateFlow<com.example.location.AutoLocationState> =
        com.example.location.BackgroundLocationManager.locationState

    fun refreshCurrentLocation() {
        com.example.location.BackgroundLocationManager.checkLocationNow(getApplication())
    }

    fun simulateOfficeArrival(office: OfficeEntity) {
        com.example.location.BackgroundLocationManager.simulateOfficeArrival(getApplication(), office)
    }

    fun resetLocationSimulation() {
        com.example.location.BackgroundLocationManager.resetSimulation(getApplication())
    }

    init {
        // Start live location monitoring
        com.example.location.BackgroundLocationManager.startLocationMonitoring(getApplication())

        viewModelScope.launch {
            combine(selectedOffice, selectedVehicle) { office, vehicle ->
                Pair(office, vehicle)
            }.collect { (office, _) ->
                if (office != null) {
                    loadOfficeSlotsAndHistory(office.id)
                }
            }
        }

        // Auto-pop arrival screen when geofence triggers via shared ArrivalRepository
        viewModelScope.launch {
            repository.arrivalRepository.arrivalStateFlow.collect { arrivalState ->
                if (arrivalState.arrivalActive) {
                    val officesList = repository.getAllOffices()
                    val targetOffice = if (arrivalState.arrivalLocationId != null) {
                        repository.getOfficeById(arrivalState.arrivalLocationId)
                            ?: officesList.find { it.name.equals(arrivalState.arrivalLocationName, ignoreCase = true) }
                            ?: officesList.firstOrNull()
                    } else {
                        officesList.find { it.name.equals(arrivalState.arrivalLocationName, ignoreCase = true) }
                            ?: officesList.firstOrNull()
                    }
                    if (targetOffice != null && lastDismissedOfficeId != targetOffice.id) {
                        showArrivalPopUp(targetOffice)
                    }
                } else {
                    if (isArrivalPopUpVisible.value) {
                        isArrivalPopUpVisible.value = false
                        isArrivalPopUpMinimized.value = false
                    }
                }
            }
        }
    }

    fun selectVehicle(vehicleId: Long) {
        _selectedVehicleId.value = vehicleId
        selectedOffice.value?.let { loadOfficeSlotsAndHistory(it.id) }
    }

    fun selectOffice(officeId: Long) {
        _selectedOfficeId.value = officeId
        loadOfficeSlotsAndHistory(officeId)
    }

    fun toggleEvFilter(onlyEv: Boolean = !_onlyEvFilter.value) {
        _onlyEvFilter.value = onlyEv
        refreshCurrentSlots()
    }

    fun selectFloor(floor: String) {
        _selectedFloor.value = floor
        refreshCurrentSlots()
    }

    fun refreshCurrentSlots() {
        val office = selectedOffice.value ?: return
        val floor = _selectedFloor.value
        viewModelScope.launch {
            val allSlots = repository.getSlotsByOffice(office.id).filter { it.floor == floor }
            currentFloorSlots.value = if (_onlyEvFilter.value) {
                val evs = allSlots.filter { it.isEV }
                if (evs.isNotEmpty()) evs else allSlots
            } else {
                allSlots
            }
        }
    }

    private fun loadOfficeSlotsAndHistory(officeId: Long) {
        viewModelScope.launch {
            val floors = repository.getFloorsByOffice(officeId)
            availableFloors.value = floors

            if (floors.isNotEmpty()) {
                val currentSelected = _selectedFloor.value
                val targetFloor = if (floors.contains(currentSelected)) currentSelected else floors.first()
                _selectedFloor.value = targetFloor

                val allSlots = repository.getSlotsByOffice(officeId).filter { it.floor == targetFloor }
                currentFloorSlots.value = if (_onlyEvFilter.value) {
                    val evs = allSlots.filter { it.isEV }
                    if (evs.isNotEmpty()) evs else allSlots
                } else {
                    allSlots
                }
            } else {
                _selectedFloor.value = ""
                currentFloorSlots.value = emptyList()
            }

            previousOfficeParking.value = repository.getLatestOfficeParking(officeId)
        }
    }

    fun addBay(slot: ParkingSlotEntity) {
        viewModelScope.launch {
            repository.insertSlot(slot)
            _selectedFloor.value = slot.floor
            selectedOffice.value?.let { loadOfficeSlotsAndHistory(it.id) }
        }
    }

    fun addBay(
        officeId: Long,
        floor: String,
        slotNumber: String,
        zone: String? = null,
        isEV: Boolean = true,
        notes: String? = null
    ) {
        val slot = ParkingSlotEntity(
            officeId = officeId,
            floor = floor,
            slotNumber = slotNumber,
            zone = zone,
            isEV = isEV,
            notes = notes
        )
        addBay(slot)
    }

    fun updateBay(slot: ParkingSlotEntity) {
        viewModelScope.launch {
            repository.updateSlot(slot)
            selectedOffice.value?.let { loadOfficeSlotsAndHistory(it.id) }
        }
    }

    fun deleteBay(slot: ParkingSlotEntity) {
        viewModelScope.launch {
            repository.deleteSlot(slot)
            selectedOffice.value?.let { loadOfficeSlotsAndHistory(it.id) }
        }
    }

    fun deleteBay(slotId: Long) {
        viewModelScope.launch {
            val slot = repository.getAllSlots().find { it.id == slotId }
            if (slot != null) {
                repository.deleteSlot(slot)
            }
            selectedOffice.value?.let { loadOfficeSlotsAndHistory(it.id) }
        }
    }

    fun deleteBays(slots: List<ParkingSlotEntity>) {
        viewModelScope.launch {
            repository.deleteSlots(slots)
            selectedOffice.value?.let { loadOfficeSlotsAndHistory(it.id) }
        }
    }

    fun deleteBaysByIds(slotIds: List<Long>) {
        viewModelScope.launch {
            repository.deleteSlotsByIds(slotIds)
            selectedOffice.value?.let { loadOfficeSlotsAndHistory(it.id) }
        }
    }

    fun deleteAllBaysForCurrentOffice() {
        val office = selectedOffice.value ?: return
        viewModelScope.launch {
            repository.clearSlotsForOffice(office.id)
            loadOfficeSlotsAndHistory(office.id)
        }
    }

    fun createAndSelectOfficeLocation(location: SavedLocationEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveLocation(location)
            val saved = location.copy(id = id)
            val officeId = repository.syncOfficeFromSavedLocation(saved)
            if (officeId != null) {
                _selectedOfficeId.value = officeId
                loadOfficeSlotsAndHistory(officeId)
                onComplete(officeId)
            }
        }
    }

    // Onboarding Save
    fun saveUserProfile(firstName: String, lastName: String, dobMillis: Long, gender: String = "Male") {
        viewModelScope.launch {
            val user = UserEntity(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                dateOfBirth = dobMillis,
                gender = gender
            )
            repository.saveUser(user)
            repository.preferences.setOnboardingCompleted(true)
        }
    }

    fun updateUserGender(gender: String) {
        viewModelScope.launch {
            val user = repository.getUserProfile() ?: return@launch
            val updated = user.copy(gender = gender)
            repository.saveUser(updated)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            repository.preferences.setOnboardingCompleted(completed)
        }
    }

    // Vehicles CRUD
    fun addVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            val id = repository.saveVehicle(vehicle)
            _selectedVehicleId.value = id
        }
    }

    fun updateVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            repository.updateVehicle(vehicle)
        }
    }

    fun deleteVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
            if (_selectedVehicleId.value == vehicle.id) {
                _selectedVehicleId.value = null
            }
        }
    }

    fun setDefaultVehicle(vehicleId: Long) {
        viewModelScope.launch {
            repository.setDefaultVehicle(vehicleId)
            _selectedVehicleId.value = vehicleId
        }
    }

    // Manual Parking
    fun saveManualParking(
        placeName: String,
        placeType: String,
        latitude: Double,
        longitude: Double,
        floor: String? = null,
        slotNumber: String? = null,
        building: String? = null,
        wing: String? = null,
        notes: String? = null,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val vehicle = selectedVehicle.value ?: repository.getDefaultVehicle() ?: return@launch
            val session = ParkingSessionEntity(
                vehicleId = vehicle.id,
                placeName = placeName.trim(),
                placeType = placeType,
                latitude = latitude,
                longitude = longitude,
                floor = floor?.takeIf { it.isNotBlank() },
                slotNumber = slotNumber?.takeIf { it.isNotBlank() },
                building = building?.takeIf { it.isNotBlank() },
                wing = wing?.takeIf { it.isNotBlank() },
                notes = notes?.takeIf { it.isNotBlank() },
                imageUri = imageUri,
                parkedAt = System.currentTimeMillis(),
                isActive = true
            )
            repository.saveNewParkingSession(session)

            // Show notification if enabled
            if (notificationsEnabled.value) {
                val locDesc = buildString {
                    append(placeName)
                    if (!floor.isNullOrBlank()) append(" - Floor $floor")
                    if (!slotNumber.isNullOrBlank()) append(", Slot $slotNumber")
                }
                NotificationHelper.showParkingSavedNotification(getApplication(), locDesc)
            }
        }
    }

    // Park at Office Slot
    fun parkAtOfficeSlot(
        slot: ParkingSlotEntity,
        currentLat: Double,
        currentLng: Double
    ) {
        viewModelScope.launch {
            val office = selectedOffice.value ?: repository.getDefaultOffice() ?: return@launch
            val vehicle = selectedVehicle.value ?: repository.getDefaultVehicle() ?: return@launch

            val session = ParkingSessionEntity(
                vehicleId = vehicle.id,
                officeId = office.id,
                placeName = office.name,
                placeType = "Office",
                latitude = if (currentLat != 0.0) currentLat else office.latitude,
                longitude = if (currentLng != 0.0) currentLng else office.longitude,
                floor = slot.floor,
                slotNumber = slot.slotNumber,
                building = office.name,
                wing = slot.zone,
                notes = slot.notes,
                parkedAt = System.currentTimeMillis(),
                isActive = true
            )
            repository.saveNewParkingSession(session)
            previousOfficeParking.value = session

            if (notificationsEnabled.value) {
                NotificationHelper.showParkingSavedNotification(
                    getApplication(),
                    "${office.name} (Floor ${slot.floor} - Slot ${slot.slotNumber})"
                )
            }
        }
    }

    // Retrieve active parked vehicle
    fun markVehicleAsRetrieved() {
        viewModelScope.launch {
            val active = activeParkingSession.value ?: return@launch
            repository.markSessionAsRetrieved(active.id)
        }
    }

    // Parking History
    fun restoreParkingSession(session: ParkingSessionEntity) {
        viewModelScope.launch {
            repository.restoreSession(session.id)
            if (notificationsEnabled.value) {
                val locDesc = buildString {
                    append(session.placeName)
                    if (!session.floor.isNullOrBlank()) append(" - Floor ${session.floor}")
                    if (!session.slotNumber.isNullOrBlank()) append(", Slot ${session.slotNumber}")
                }
                NotificationHelper.showParkingSavedNotification(getApplication(), locDesc)
            }
        }
    }

    fun restoreAllRetrievedSessions() {
        viewModelScope.launch {
            repository.restoreAllRetrievedSessions()
        }
    }

    fun deleteParkingSession(session: ParkingSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun clearParkingHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    // Saved Locations
    fun addSavedLocation(location: SavedLocationEntity) {
        viewModelScope.launch {
            val locId = repository.saveLocation(location)
            val saved = location.copy(id = locId)
            val officeId = repository.syncOfficeFromSavedLocation(saved)
            if (officeId != null) {
                val office = repository.getOfficeById(officeId)
                if (office != null && geofencingEnabled.value) {
                    geofenceManager.addOfficeGeofence(office)
                }
            }
        }
    }

    fun updateSavedLocation(location: SavedLocationEntity) {
        viewModelScope.launch {
            repository.updateLocation(location)
            val officeId = repository.syncOfficeFromSavedLocation(location)
            if (officeId != null) {
                val office = repository.getOfficeById(officeId)
                if (office != null && geofencingEnabled.value) {
                    geofenceManager.addOfficeGeofence(office)
                }
            } else {
                val existingOffice = repository.getAllOffices().find { it.savedLocationId == location.id }
                if (existingOffice != null) {
                    geofenceManager.removeOfficeGeofence(existingOffice.id)
                    repository.deleteOffice(existingOffice)
                }
            }
        }
    }

    fun deleteSavedLocation(location: SavedLocationEntity) {
        viewModelScope.launch {
            repository.deleteLocation(location)
            val matchingOffice = repository.getAllOffices().find {
                it.savedLocationId == location.id ||
                (location.isWorkLocation && it.name.equals(location.name, ignoreCase = true))
            }
            if (matchingOffice != null) {
                geofenceManager.removeOfficeGeofence(matchingOffice.id)
                repository.deleteOffice(matchingOffice)
            }
        }
    }

    // Office Slots CRUD
    fun addParkingSlot(
        officeId: Long,
        floor: String,
        slotNumber: String,
        zone: String?,
        isEV: Boolean,
        notes: String?
    ) {
        viewModelScope.launch {
            val cleanFloor = floor.trim().ifBlank { "Floor 1" }
            val slot = ParkingSlotEntity(
                officeId = officeId,
                floor = cleanFloor,
                slotNumber = slotNumber.trim(),
                zone = zone?.trim()?.takeIf { it.isNotBlank() },
                isEV = isEV,
                notes = notes?.trim()?.takeIf { it.isNotBlank() }
            )
            repository.insertSlot(slot)
            loadOfficeSlotsAndHistory(officeId)
            selectFloor(cleanFloor)
        }
    }

    fun deleteParkingSlot(slot: ParkingSlotEntity) {
        viewModelScope.launch {
            repository.deleteSlot(slot)
            loadOfficeSlotsAndHistory(slot.officeId)
        }
    }

    // Office CRUD & Geofence
    fun addOffice(office: OfficeEntity) {
        viewModelScope.launch {
            val id = repository.saveOffice(office)
            val updatedOffice = office.copy(id = id)
            if (geofencingEnabled.value) {
                geofenceManager.addOfficeGeofence(updatedOffice)
            }
        }
    }

    fun deleteOffice(office: OfficeEntity) {
        viewModelScope.launch {
            repository.deleteOffice(office)
            geofenceManager.removeOfficeGeofence(office.id)
        }
    }

    // Settings
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setNotificationsEnabled(enabled)
        }
    }

    fun setGeofencingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setGeofencingEnabled(enabled)
            if (enabled) {
                geofenceManager.registerAllOfficeGeofences()
            }
        }
    }

    fun setWakeWordListening(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setWakeWordListening(enabled)
        }
    }

    fun setVoiceAssistantEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setVoiceAssistantEnabled(enabled)
            if (!enabled) {
                repository.preferences.setWakeWordListening(false)
            }
        }
    }

    fun setAndroidAutoEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setAndroidAutoEnabled(enabled)
        }
    }

    fun setArrivalAlertType(type: String) {
        viewModelScope.launch {
            repository.preferences.setArrivalAlertType(type)
        }
    }

    fun setLocationCheckIntervalSeconds(seconds: Int) {
        viewModelScope.launch {
            repository.preferences.setLocationCheckIntervalSeconds(seconds)
        }
    }

    fun increaseLocationCheckInterval() {
        viewModelScope.launch {
            val current = locationCheckIntervalSeconds.value
            val next = when {
                current < 10 -> current + 2
                current < 30 -> current + 5
                current < 60 -> current + 15
                current < 120 -> current + 30
                else -> (current + 60).coerceAtMost(300)
            }
            repository.preferences.setLocationCheckIntervalSeconds(next)
        }
    }

    fun decreaseLocationCheckInterval() {
        viewModelScope.launch {
            val current = locationCheckIntervalSeconds.value
            val prev = when {
                current <= 5 -> 3
                current <= 10 -> current - 2
                current <= 30 -> current - 5
                current <= 60 -> current - 15
                current <= 120 -> current - 30
                else -> current - 60
            }.coerceAtLeast(3)
            repository.preferences.setLocationCheckIntervalSeconds(prev)
        }
    }

    // Office Arrival Pop-Up Management
    fun showArrivalPopUp(office: OfficeEntity, distance: Float? = null) {
        viewModelScope.launch {
            arrivalOffice.value = office
            arrivalDistanceMeters.value = distance
            val slotsList = repository.getSlotsByOffice(office.id)
            val evSlots = slotsList.filter { it.isEV }
            arrivalSlots.value = if (evSlots.isNotEmpty()) evSlots else slotsList
            isArrivalPopUpVisible.value = true
            isArrivalPopUpMinimized.value = false
        }
    }

    fun minimizeArrivalPopUp() {
        isArrivalPopUpMinimized.value = true
    }

    fun expandArrivalPopUp() {
        isArrivalPopUpMinimized.value = false
        isArrivalPopUpVisible.value = true
    }

    fun dismissArrivalPopUp() {
        isArrivalPopUpVisible.value = false
        isArrivalPopUpMinimized.value = false
        lastDismissedOfficeId = arrivalOffice.value?.id
    }

    fun testTriggerArrivalPopUp() {
        viewModelScope.launch {
            lastDismissedOfficeId = null
            val officesList = repository.getAllOffices()
            val targetOffice = officesList.firstOrNull() ?: run {
                val sampleOffice = OfficeEntity(
                    name = "Headquarters Campus",
                    latitude = 17.4375,
                    longitude = 78.3752,
                    geofenceRadius = 200f
                )
                val newId = repository.saveOffice(sampleOffice)
                repository.insertSlot(ParkingSlotEntity(officeId = newId, floor = "B1", slotNumber = "101", zone = "Zone A", isEV = true, notes = "Fast AC 7.4kW"))
                repository.insertSlot(ParkingSlotEntity(officeId = newId, floor = "B1", slotNumber = "102", zone = "Zone A", isEV = true, notes = "DC Fast 22kW"))
                repository.insertSlot(ParkingSlotEntity(officeId = newId, floor = "B2", slotNumber = "205", zone = "Zone B", isEV = true, notes = "EV Charger"))
                sampleOffice.copy(id = newId)
            }
            // Update shared ArrivalRepository so BOTH Phone and Android Auto immediately reflect arrival
            repository.arrivalRepository.setArrival(
                active = true,
                locationId = targetOffice.id,
                locationName = targetOffice.name,
                timestamp = System.currentTimeMillis(),
                latitude = targetOffice.latitude,
                longitude = targetOffice.longitude
            )
            repository.preferences.setIsAtOffice(true, targetOffice.name)
            showArrivalPopUp(targetOffice, 75f)
        }
    }

    fun parkFromArrivalPopUp(
        slot: ParkingSlotEntity? = null,
        customSlotNumber: String? = null,
        customFloor: String? = null,
        notes: String? = null
    ) {
        val targetOffice = arrivalOffice.value ?: return
        val currentVehicle = selectedVehicle.value
        val floorToSave = slot?.floor ?: customFloor ?: "B1"
        val slotNumToSave = slot?.slotNumber ?: customSlotNumber ?: "101"
        val zoneToSave = slot?.zone ?: ""
        val notesToSave = slot?.notes ?: notes

        viewModelScope.launch {
            val newSession = ParkingSessionEntity(
                vehicleId = currentVehicle?.id ?: 0,
                officeId = targetOffice.id,
                placeName = targetOffice.name,
                placeType = "Office",
                latitude = targetOffice.latitude,
                longitude = targetOffice.longitude,
                floor = floorToSave,
                slotNumber = slotNumToSave,
                building = targetOffice.name,
                wing = zoneToSave,
                notes = notesToSave,
                parkedAt = System.currentTimeMillis(),
                isActive = true
            )
            repository.saveNewParkingSession(newSession)
            // Clear arrival state in persistent ArrivalRepository
            repository.arrivalRepository.clearArrival()
            repository.preferences.setIsAtOffice(false, "")
            NotificationHelper.cancelOfficeArrivalNotification(getApplication())
            if (notificationsEnabled.value) {
                NotificationHelper.showParkingSavedNotification(
                    getApplication(),
                    targetOffice.name,
                    floorToSave,
                    slotNumToSave
                )
            }
            dismissArrivalPopUp()
        }
    }

    suspend fun exportConfiguration(): String {
        return com.example.data.export.ConfigurationManager.exportConfiguration(repository)
    }

    suspend fun importConfiguration(json: String): com.example.data.export.ConfigurationImportResult {
        return com.example.data.export.ConfigurationManager.importConfiguration(json, repository)
    }

    fun resetAppData() {
        viewModelScope.launch {
            // 1. Remove all active geofences from Google Location Services
            try {
                geofenceManager.removeAllGeofences()
            } catch (_: Exception) {}

            // 2. Dismiss and clear all posted notifications
            try {
                NotificationHelper.cancelAll(getApplication())
            } catch (_: Exception) {}

            // 3. Clear all database tables and DataStore preferences
            repository.resetEntireDatabase()

            // 4. Reset in-memory state flows
            _selectedVehicleId.value = null
            _selectedOfficeId.value = null
            _selectedFloor.value = "B1"
            isArrivalPopUpVisible.value = false
            isArrivalPopUpMinimized.value = false
            arrivalOffice.value = null
            lastDismissedOfficeId = null
            currentFloorSlots.value = emptyList()
            previousOfficeParking.value = null

            // 5. Clean start initialization
            repository.seedInitialDataIfEmpty()
            com.kspcr.parkalot.auto.ParkALotAutoDataBridge.refreshFromDatabase(getApplication())
        }
    }

    // Android Auto Renderer (AABrowser Architecture) Data & Test Controls
    val autoDisplayData: StateFlow<com.kspcr.parkalot.auto.ParkingDisplayData> =
        com.kspcr.parkalot.auto.ParkALotAutoDataBridge.displayDataFlow

    fun sendTestParkingDataToAuto() {
        com.kspcr.parkalot.auto.ParkALotAutoDataBridge.setTestDataMode(getApplication())
    }

    fun restoreLiveDatabaseModeToAuto() {
        com.kspcr.parkalot.auto.ParkALotAutoDataBridge.restoreLiveDatabaseMode(getApplication())
    }

    fun refreshAutoRendererData() {
        com.kspcr.parkalot.auto.ParkALotAutoDataBridge.refreshFromDatabase(getApplication())
    }
}
