package com.example.data.repository

import android.content.Context
import com.example.data.entity.OfficeEntity
import com.example.data.entity.ParkingSessionEntity
import com.example.data.entity.ParkingSlotEntity
import com.example.data.entity.SavedLocationEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.VehicleEntity
import com.example.data.local.ParkALotDatabase
import com.example.data.local.PreloadedData
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ParkingRepository(context: Context) {

    private val database = ParkALotDatabase.getDatabase(context)
    val preferences = UserPreferencesRepository(context)
    val arrivalRepository = ArrivalRepository.getInstance(context)

    private val userDao = database.userDao()
    private val vehicleDao = database.vehicleDao()
    private val savedLocationDao = database.savedLocationDao()
    private val officeDao = database.officeDao()
    private val parkingSlotDao = database.parkingSlotDao()
    private val parkingSessionDao = database.parkingSessionDao()

    // User Profile
    val userFlow: Flow<UserEntity?> = userDao.getUserFlow()
    suspend fun getUser(): UserEntity? = userDao.getUser()
    suspend fun getUserProfile(): UserEntity? = userDao.getUser()
    suspend fun saveUser(user: UserEntity): Long = userDao.insertUser(user)
    suspend fun clearUser() = userDao.clearUser()

    // Vehicles
    val allVehiclesFlow: Flow<List<VehicleEntity>> = vehicleDao.getAllVehiclesFlow()
    val defaultVehicleFlow: Flow<VehicleEntity?> = vehicleDao.getDefaultVehicleFlow()
    suspend fun getAllVehicles(): List<VehicleEntity> = vehicleDao.getAllVehicles()
    suspend fun getVehicleById(id: Long): VehicleEntity? = vehicleDao.getVehicleById(id)
    suspend fun getDefaultVehicle(): VehicleEntity? = vehicleDao.getDefaultVehicle()

    suspend fun saveVehicle(vehicle: VehicleEntity): Long {
        val id = vehicleDao.insertVehicle(vehicle)
        if (vehicle.isDefault) {
            vehicleDao.clearOtherDefaults(id)
            preferences.setSelectedVehicleId(id)
        }
        return id
    }

    suspend fun updateVehicle(vehicle: VehicleEntity) {
        vehicleDao.updateVehicle(vehicle)
        if (vehicle.isDefault) {
            vehicleDao.clearOtherDefaults(vehicle.id)
            preferences.setSelectedVehicleId(vehicle.id)
        }
    }

    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        vehicleDao.deleteVehicle(vehicle)
        val remaining = vehicleDao.getAllVehicles()
        if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
            vehicleDao.setDefaultVehicle(remaining.first().id)
            preferences.setSelectedVehicleId(remaining.first().id)
        }
    }

    suspend fun setDefaultVehicle(vehicleId: Long) {
        vehicleDao.setDefaultVehicle(vehicleId)
        preferences.setSelectedVehicleId(vehicleId)
    }

    // Saved Locations
    val savedLocationsFlow: Flow<List<SavedLocationEntity>> = savedLocationDao.getAllLocationsFlow()
    suspend fun saveLocation(location: SavedLocationEntity): Long = savedLocationDao.insertLocation(location)
    suspend fun updateLocation(location: SavedLocationEntity) = savedLocationDao.updateLocation(location)
    suspend fun deleteLocation(location: SavedLocationEntity) = savedLocationDao.deleteLocation(location)
    suspend fun deleteLocationById(id: Long) = savedLocationDao.deleteLocationById(id)
    suspend fun getWorkLocations(): List<SavedLocationEntity> = savedLocationDao.getWorkLocations()
    suspend fun getLocationById(id: Long): SavedLocationEntity? = savedLocationDao.getLocationById(id)

    // Offices
    val allOfficesFlow: Flow<List<OfficeEntity>> = officeDao.getAllOfficesFlow()
    val defaultOfficeFlow: Flow<OfficeEntity?> = officeDao.getDefaultOfficeFlow()
    suspend fun getAllOffices(): List<OfficeEntity> = officeDao.getAllOffices()
    suspend fun getOfficeById(id: Long): OfficeEntity? = officeDao.getOfficeById(id)
    suspend fun getDefaultOffice(): OfficeEntity? = officeDao.getDefaultOffice()

    suspend fun saveOffice(office: OfficeEntity): Long {
        val id = officeDao.insertOffice(office)
        if (office.isDefault) {
            officeDao.setDefaultOffice(id)
            preferences.setSelectedOfficeId(id)
        }
        return id
    }

    suspend fun updateOffice(office: OfficeEntity) {
        officeDao.updateOffice(office)
        if (office.isDefault) {
            officeDao.setDefaultOffice(office.id)
            preferences.setSelectedOfficeId(office.id)
        }
    }

    suspend fun deleteOffice(office: OfficeEntity) = officeDao.deleteOffice(office)
    suspend fun setDefaultOffice(officeId: Long) {
        officeDao.setDefaultOffice(officeId)
        preferences.setSelectedOfficeId(officeId)
    }

    // Parking Slots
    fun getSlotsByOfficeFlow(officeId: Long): Flow<List<ParkingSlotEntity>> =
        parkingSlotDao.getSlotsByOfficeFlow(officeId)

    suspend fun getSlotsByOffice(officeId: Long): List<ParkingSlotEntity> =
        parkingSlotDao.getSlotsByOffice(officeId)

    fun getFloorsByOfficeFlow(officeId: Long): Flow<List<String>> =
        parkingSlotDao.getFloorsByOfficeFlow(officeId)

    suspend fun getFloorsByOffice(officeId: Long): List<String> =
        parkingSlotDao.getFloorsByOffice(officeId)

    fun getSlotsByOfficeAndFloorFlow(officeId: Long, floor: String): Flow<List<ParkingSlotEntity>> =
        parkingSlotDao.getSlotsByOfficeAndFloorFlow(officeId, floor)

    fun getSlotsByOfficeFloorAndEvFlow(officeId: Long, floor: String, isEV: Boolean): Flow<List<ParkingSlotEntity>> =
        parkingSlotDao.getSlotsByOfficeFloorAndEvFlow(officeId, floor, isEV)

    suspend fun insertSlots(slots: List<ParkingSlotEntity>) =
        parkingSlotDao.insertSlots(slots)

    suspend fun insertSlot(slot: ParkingSlotEntity): Long =
        parkingSlotDao.insertSlot(slot)

    suspend fun updateSlot(slot: ParkingSlotEntity) =
        parkingSlotDao.updateSlot(slot)

    suspend fun deleteSlot(slot: ParkingSlotEntity) =
        parkingSlotDao.deleteSlot(slot)

    suspend fun deleteSlots(slots: List<ParkingSlotEntity>) =
        parkingSlotDao.deleteSlots(slots)

    suspend fun deleteSlotsByIds(slotIds: List<Long>) =
        parkingSlotDao.deleteSlotsByIds(slotIds)

    suspend fun clearSlotsForOffice(officeId: Long) =
        parkingSlotDao.clearSlotsForOffice(officeId)

    suspend fun getAllSlots(): List<ParkingSlotEntity> =
        parkingSlotDao.getAllSlots()

    suspend fun syncOfficeFromSavedLocation(location: SavedLocationEntity): Long? {
        val isWork = location.isWorkLocation ||
                location.type.equals("Office", ignoreCase = true) ||
                location.type.equals("Work", ignoreCase = true)

        if (isWork) {
            val existing = officeDao.getOfficeBySavedLocationId(location.id)
                ?: officeDao.getAllOffices().firstOrNull { it.name.equals(location.name, ignoreCase = true) }

            return if (existing != null) {
                val updated = existing.copy(
                    name = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = location.address,
                    geofenceRadius = location.geofenceRadius,
                    savedLocationId = location.id
                )
                officeDao.updateOffice(updated)
                existing.id
            } else {
                val newOffice = OfficeEntity(
                    name = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = location.address,
                    geofenceRadius = location.geofenceRadius,
                    savedLocationId = location.id,
                    isDefault = officeDao.getAllOffices().isEmpty()
                )
                val newId = officeDao.insertOffice(newOffice)
                newId
            }
        } else {
            officeDao.deleteOfficeBySavedLocationId(location.id)
            return null
        }
    }

    suspend fun removeOfficeForSavedLocation(savedLocationId: Long) {
        officeDao.deleteOfficeBySavedLocationId(savedLocationId)
    }

    // Parking Sessions
    val activeSessionFlow: Flow<ParkingSessionEntity?> = parkingSessionDao.getActiveSessionFlow()
    val historyFlow: Flow<List<ParkingSessionEntity>> = parkingSessionDao.getAllHistoryFlow()

    fun getActiveSessionForVehicleFlow(vehicleId: Long): Flow<ParkingSessionEntity?> =
        parkingSessionDao.getActiveSessionForVehicleFlow(vehicleId)

    suspend fun getActiveSession(): ParkingSessionEntity? =
        parkingSessionDao.getActiveSession()

    suspend fun getActiveSessionForVehicle(vehicleId: Long): ParkingSessionEntity? =
        parkingSessionDao.getActiveSessionForVehicle(vehicleId)

    fun getLatestOfficeParkingFlow(officeId: Long): Flow<ParkingSessionEntity?> =
        parkingSessionDao.getLatestOfficeParkingFlow(officeId)

    suspend fun getLatestOfficeParking(officeId: Long): ParkingSessionEntity? =
        parkingSessionDao.getLatestOfficeParking(officeId)

    suspend fun saveNewParkingSession(session: ParkingSessionEntity): Long {
        return parkingSessionDao.saveNewActiveSession(session)
    }

    suspend fun markSessionAsRetrieved(sessionId: Long) {
        parkingSessionDao.markAsRetrieved(sessionId)
    }

    suspend fun restoreSession(sessionId: Long): ParkingSessionEntity? {
        return parkingSessionDao.restoreSessionAsActive(sessionId)
    }

    suspend fun restoreAllRetrievedSessions(): Int {
        return parkingSessionDao.restoreAllRetrievedSessions()
    }

    suspend fun deleteSession(session: ParkingSessionEntity) {
        parkingSessionDao.deleteSession(session)
    }

    suspend fun deleteSessionById(id: Long) {
        parkingSessionDao.deleteSessionById(id)
    }

    suspend fun clearAllHistory() {
        parkingSessionDao.clearAllHistory()
    }

    suspend fun resetEntireDatabase() {
        database.clearAllTables()
        preferences.clearPreferences()
        arrivalRepository.clearArrival()
    }

    // Preload sample data if database is brand new
    suspend fun seedInitialDataIfEmpty() {
        // Clean initial state: No default Deloitte location or slots injected.
        // User configures all their own locations, vehicles, and parking bays.
    }
}
