package com.example.data.export

import com.example.data.entity.OfficeEntity
import com.example.data.entity.ParkingSlotEntity
import com.example.data.entity.SavedLocationEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.VehicleEntity
import com.example.data.repository.ParkingRepository
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject

data class ConfigurationImportResult(
    val success: Boolean,
    val message: String,
    val vehiclesCount: Int = 0,
    val locationsCount: Int = 0,
    val officesCount: Int = 0,
    val slotsCount: Int = 0
)

object ConfigurationManager {

    suspend fun exportConfiguration(repository: ParkingRepository): String {
        val root = JSONObject()
        root.put("app", "Park a lot")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        // User Profile
        val user = repository.getUserProfile()
        if (user != null) {
            val userObj = JSONObject().apply {
                put("firstName", user.firstName)
                put("lastName", user.lastName)
                put("dateOfBirth", user.dateOfBirth)
                put("gender", user.gender)
            }
            root.put("user", userObj)
        }

        // Vehicles
        val vehicles = repository.getAllVehicles()
        val vehiclesArray = JSONArray()
        for (v in vehicles) {
            val vObj = JSONObject().apply {
                put("name", v.name)
                put("type", v.type)
                put("manufacturer", v.manufacturer)
                put("model", v.model)
                put("registrationNumber", v.registrationNumber)
                put("fuelType", v.fuelType)
                put("isDefault", v.isDefault)
                if (v.year != null) put("year", v.year)
                if (v.color != null) put("color", v.color)
            }
            vehiclesArray.put(vObj)
        }
        root.put("vehicles", vehiclesArray)

        // Saved Locations
        val locations = repository.savedLocationsFlow.firstOrNull() ?: emptyList()
        val locationsArray = JSONArray()
        for (loc in locations) {
            val locObj = JSONObject().apply {
                put("id", loc.id)
                put("name", loc.name)
                put("type", loc.type)
                put("latitude", loc.latitude)
                put("longitude", loc.longitude)
                put("address", loc.address)
                put("isWorkLocation", loc.isWorkLocation)
                put("geofenceRadius", loc.geofenceRadius.toDouble())
                if (loc.notes != null) put("notes", loc.notes)
            }
            locationsArray.put(locObj)
        }
        root.put("savedLocations", locationsArray)

        // Office Profiles (including default Deloitte and custom work locations)
        val offices = repository.getAllOffices()
        val officesArray = JSONArray()
        for (office in offices) {
            val offObj = JSONObject().apply {
                put("id", office.id)
                put("name", office.name)
                put("latitude", office.latitude)
                put("longitude", office.longitude)
                put("address", office.address)
                put("geofenceRadius", office.geofenceRadius.toDouble())
                put("isDefault", office.isDefault)
                if (office.savedLocationId != null) put("savedLocationId", office.savedLocationId)
            }
            officesArray.put(offObj)
        }
        root.put("offices", officesArray)

        // Parking Slots (floors, slot numbers, zone/pillar, charger notes for each office profile)
        val allSlots = repository.getAllSlots()
        val slotsArray = JSONArray()
        for (slot in allSlots) {
            val slotObj = JSONObject().apply {
                put("officeId", slot.officeId)
                put("floor", slot.floor)
                put("slotNumber", slot.slotNumber)
                if (slot.zone != null) put("zone", slot.zone)
                put("isEV", slot.isEV)
                if (slot.notes != null) put("notes", slot.notes)
            }
            slotsArray.put(slotObj)
        }
        root.put("parkingSlots", slotsArray)

        // Preferences
        val prefs = repository.preferences
        val prefObj = JSONObject().apply {
            put("distanceUnit", prefs.distanceUnitFlow.firstOrNull() ?: "meters")
            put("notificationsEnabled", prefs.notificationsEnabledFlow.firstOrNull() ?: true)
            put("geofencingEnabled", prefs.geofencingEnabledFlow.firstOrNull() ?: true)
            put("voiceAssistantEnabled", prefs.voiceAssistantEnabledFlow.firstOrNull() ?: true)
            put("wakeWordListening", prefs.wakeWordListeningFlow.firstOrNull() ?: false)
        }
        root.put("preferences", prefObj)

        return root.toString(2)
    }

    suspend fun importConfiguration(
        jsonString: String,
        repository: ParkingRepository
    ): ConfigurationImportResult {
        return try {
            val root = JSONObject(jsonString.trim())

            var importedVehicles = 0
            var importedLocations = 0
            var importedOffices = 0
            var importedSlots = 0

            // Import User
            if (root.has("user")) {
                val userObj = root.getJSONObject("user")
                val existing = repository.getUserProfile()
                val updated = UserEntity(
                    id = existing?.id ?: 0L,
                    firstName = userObj.optString("firstName", existing?.firstName ?: "Driver"),
                    lastName = userObj.optString("lastName", existing?.lastName ?: ""),
                    dateOfBirth = userObj.optLong("dateOfBirth", existing?.dateOfBirth ?: 0L),
                    gender = userObj.optString("gender", existing?.gender ?: "Male")
                )
                repository.saveUser(updated)
            }

            // Import Vehicles
            if (root.has("vehicles")) {
                val vehiclesArray = root.getJSONArray("vehicles")
                for (i in 0 until vehiclesArray.length()) {
                    val vObj = vehiclesArray.getJSONObject(i)
                    val vehicle = VehicleEntity(
                        name = vObj.optString("name", "Vehicle"),
                        type = vObj.optString("type", "Car"),
                        manufacturer = vObj.optString("manufacturer", "Standard"),
                        model = vObj.optString("model", "Standard"),
                        registrationNumber = vObj.optString("registrationNumber", ""),
                        fuelType = vObj.optString("fuelType", "Petrol"),
                        isDefault = vObj.optBoolean("isDefault", false),
                        year = if (vObj.has("year")) vObj.getInt("year") else null,
                        color = if (vObj.has("color")) vObj.getString("color") else null
                    )
                    repository.saveVehicle(vehicle)
                    importedVehicles++
                }
            }

            // Import Saved Locations
            val oldToNewLocIdMap = mutableMapOf<Long, Long>()
            if (root.has("savedLocations")) {
                val locationsArray = root.getJSONArray("savedLocations")
                for (i in 0 until locationsArray.length()) {
                    val locObj = locationsArray.getJSONObject(i)
                    val originalId = locObj.optLong("id", 0L)
                    val location = SavedLocationEntity(
                        name = locObj.optString("name", "Location"),
                        type = locObj.optString("type", "Other"),
                        latitude = locObj.optDouble("latitude", 0.0),
                        longitude = locObj.optDouble("longitude", 0.0),
                        address = locObj.optString("address", ""),
                        notes = if (locObj.has("notes")) locObj.getString("notes") else null,
                        isWorkLocation = locObj.optBoolean("isWorkLocation", false),
                        geofenceRadius = locObj.optDouble("geofenceRadius", 150.0).toFloat()
                    )
                    val newLocId = repository.saveLocation(location)
                    if (originalId != 0L) {
                        oldToNewLocIdMap[originalId] = newLocId
                    }
                    importedLocations++
                }
            }

            // Import Office Profiles
            val oldToNewOfficeIdMap = mutableMapOf<Long, Long>()
            if (root.has("offices")) {
                val officesArray = root.getJSONArray("offices")
                for (i in 0 until officesArray.length()) {
                    val offObj = officesArray.getJSONObject(i)
                    val originalOfficeId = offObj.optLong("id", 0L)
                    val origSavedLocId = if (offObj.has("savedLocationId")) offObj.getLong("savedLocationId") else null
                    val mappedSavedLocId = origSavedLocId?.let { oldToNewLocIdMap[it] ?: it }

                    val office = OfficeEntity(
                        name = offObj.optString("name", "Office Campus"),
                        latitude = offObj.optDouble("latitude", 0.0),
                        longitude = offObj.optDouble("longitude", 0.0),
                        address = offObj.optString("address", ""),
                        geofenceRadius = offObj.optDouble("geofenceRadius", 150.0).toFloat(),
                        isDefault = offObj.optBoolean("isDefault", false),
                        savedLocationId = mappedSavedLocId
                    )
                    val newOfficeId = repository.saveOffice(office)
                    if (originalOfficeId != 0L) {
                        oldToNewOfficeIdMap[originalOfficeId] = newOfficeId
                    }
                    importedOffices++
                }
            }

            // Import Parking Slots (floors, slot numbers, zone/pillar, charger notes)
            if (root.has("parkingSlots")) {
                val slotsArray = root.getJSONArray("parkingSlots")
                for (i in 0 until slotsArray.length()) {
                    val slotObj = slotsArray.getJSONObject(i)
                    val origOfficeId = slotObj.optLong("officeId", 1L)
                    val targetOfficeId = oldToNewOfficeIdMap[origOfficeId] ?: origOfficeId

                    val slot = ParkingSlotEntity(
                        officeId = targetOfficeId,
                        floor = slotObj.optString("floor", "B1"),
                        slotNumber = slotObj.optString("slotNumber", "1"),
                        zone = if (slotObj.has("zone")) slotObj.getString("zone") else null,
                        isEV = slotObj.optBoolean("isEV", false),
                        notes = if (slotObj.has("notes")) slotObj.getString("notes") else null
                    )
                    repository.insertSlot(slot)
                    importedSlots++
                }
            }

            // Import Preferences
            if (root.has("preferences")) {
                val prefObj = root.getJSONObject("preferences")
                val prefs = repository.preferences

                if (prefObj.has("distanceUnit")) {
                    prefs.setDistanceUnit(prefObj.getString("distanceUnit"))
                }
                if (prefObj.has("notificationsEnabled")) {
                    prefs.setNotificationsEnabled(prefObj.getBoolean("notificationsEnabled"))
                }
                if (prefObj.has("geofencingEnabled")) {
                    prefs.setGeofencingEnabled(prefObj.getBoolean("geofencingEnabled"))
                }
                if (prefObj.has("voiceAssistantEnabled")) {
                    prefs.setVoiceAssistantEnabled(prefObj.getBoolean("voiceAssistantEnabled"))
                }
                if (prefObj.has("wakeWordListening")) {
                    prefs.setWakeWordListening(prefObj.getBoolean("wakeWordListening"))
                }
            }

            ConfigurationImportResult(
                success = true,
                message = "Configuration restored successfully! Imported $importedVehicles vehicle(s), $importedLocations location(s), $importedOffices office(s), and $importedSlots parking slot(s).",
                vehiclesCount = importedVehicles,
                locationsCount = importedLocations,
                officesCount = importedOffices,
                slotsCount = importedSlots
            )
        } catch (e: Exception) {
            ConfigurationImportResult(
                success = false,
                message = "Failed to import configuration: ${e.localizedMessage ?: "Invalid format"}"
            )
        }
    }
}
