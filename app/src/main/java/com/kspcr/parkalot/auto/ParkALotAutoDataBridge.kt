package com.kspcr.parkalot.auto

import android.content.Context
import android.content.Intent
import com.example.data.entity.OfficeEntity
import com.example.data.local.ParkALotDatabase
import com.example.data.repository.ArrivalRepository
import com.example.data.repository.ParkingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data bridge and local communication manager connecting the main Park A Lot database
 * and arrival tracking with the Android Auto renderer components.
 */
object ParkALotAutoDataBridge {

    const val ACTION_UPDATE_PARKING_DATA = "com.kspcr.parkalot.auto.ACTION_UPDATE_PARKING_DATA"
    const val ACTION_TRIGGER_TEST_MODE = "com.kspcr.parkalot.auto.ACTION_TRIGGER_TEST_MODE"
    const val EXTRA_PARKING_DATA_JSON = "extra_parking_data_json"
    const val EXTRA_IS_TEST_MODE = "extra_is_test_mode"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _displayDataFlow = MutableStateFlow(ParkingDisplayData())
    val displayDataFlow: StateFlow<ParkingDisplayData> = _displayDataFlow.asStateFlow()

    private var isTestModeActive = false

    init {
        ParkALotAutoLogger.d("ParkALotAutoDataBridge initialized")
    }

    fun isTestMode(): Boolean = isTestModeActive

    /**
     * Checks if the device is currently connected to Android Auto or running in car mode.
     */
    fun isCarConnected(context: Context): Boolean {
        return try {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager
            val isCarMode = uiModeManager?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR
            val isCarAppConnected = try {
                androidx.car.app.connection.CarConnection(context).type.value == androidx.car.app.connection.CarConnection.CONNECTION_TYPE_PROJECTION
            } catch (_: Throwable) {
                false
            }
            isCarMode || isCarAppConnected
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Automatically launches the Park A Lot Auto renderer activity if Android Auto is connected.
     */
    fun openAutoRenderer(context: Context, officeName: String? = null) {
        try {
            val intent = Intent(context, ParkALotAutoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                addCategory("com.google.android.gms.car.category.CATEGORY_PROJECTION")
                putExtra("EXTRA_OFFICE_NAME", officeName)
                putExtra("EXTRA_AUTO_OPENED", true)
            }
            context.startActivity(intent)
            ParkALotAutoLogger.i("Automatically opened Park A Lot Auto renderer for: $officeName")
        } catch (e: Exception) {
            ParkALotAutoLogger.e("Failed to automatically launch Auto renderer: ${e.message}", e)
        }
    }

    /**
     * Pulls the real-time dynamic parking configuration from the existing Room database
     * and ArrivalRepository, without any hardcoding.
     */
    fun refreshFromDatabase(context: Context, onComplete: ((ParkingDisplayData) -> Unit)? = null) {
        if (isTestModeActive) {
            ParkALotAutoLogger.i("Test mode is active; skipping DB refresh to keep sample test data.")
            onComplete?.invoke(_displayDataFlow.value)
            return
        }

        scope.launch {
            try {
                val data = loadLiveParkingDataFromDb(context)
                _displayDataFlow.value = data
                ParkALotAutoLogger.logDataReceived(data, "RoomDatabase")
                broadcastData(context, data)

                // If arrived and car is connected, open auto renderer
                if (data.arrivalStatus && isCarConnected(context)) {
                    withContext(Dispatchers.Main) {
                        openAutoRenderer(context, data.officeName)
                    }
                }

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(data)
                }
            } catch (e: Exception) {
                ParkALotAutoLogger.logCommunicationFailure("RoomDatabaseFetch", e)
            }
        }
    }

    /**
     * Activates Developer / Test Mode with explicit test data (Deloitte Tower B1, B3, B4).
     * Does NOT touch or overwrite the real database.
     */
    fun setTestDataMode(context: Context, customTestData: ParkingDisplayData? = null) {
        val testData = customTestData ?: ParkingDisplayData.createSampleTestData()
        isTestModeActive = true
        _displayDataFlow.value = testData
        ParkALotAutoLogger.logDataReceived(testData, "DeveloperTestMode")
        broadcastData(context, testData)
    }

    /**
     * Deactivates Test Mode and restores real live dynamic data from Room database.
     */
    fun restoreLiveDatabaseMode(context: Context, onComplete: ((ParkingDisplayData) -> Unit)? = null) {
        isTestModeActive = false
        refreshFromDatabase(context, onComplete)
    }

    /**
     * Updates display data directly (e.g., received via IPC Binder or Broadcast).
     */
    fun updateFromExternal(data: ParkingDisplayData) {
        _displayDataFlow.value = data
        ParkALotAutoLogger.logDataReceived(data, "ExternalIPC")
    }

    /**
     * Notification from Geofence receiver that arrival state changed.
     */
    fun onGeofenceArrival(context: Context, arrived: Boolean, officeName: String, locationId: Long?) {
        ParkALotAutoLogger.i("Geofence event received -> Arrived: $arrived, Office: '$officeName', LocId: $locationId")
        if (!isTestModeActive) {
            refreshFromDatabase(context)
        }
    }

    private suspend fun loadLiveParkingDataFromDb(context: Context): ParkingDisplayData {
        val appContext = context.applicationContext
        val database = ParkALotDatabase.getDatabase(appContext)
        val officeDao = database.officeDao()
        val parkingSlotDao = database.parkingSlotDao()
        val arrivalRepo = ArrivalRepository.getInstance(appContext)

        val arrivalState = arrivalRepo.getArrivalState()
        val offices = officeDao.getAllOffices()

        // Match arrived office or default office or first available office
        var targetOffice: OfficeEntity? = null
        if (arrivalState.arrivalActive && arrivalState.arrivalLocationId != null) {
            targetOffice = officeDao.getOfficeById(arrivalState.arrivalLocationId)
                ?: offices.find { it.savedLocationId == arrivalState.arrivalLocationId || it.name.equals(arrivalState.arrivalLocationName, ignoreCase = true) }
        }

        if (targetOffice == null) {
            targetOffice = officeDao.getDefaultOffice() ?: offices.firstOrNull()
        }

        if (targetOffice == null) {
            // No office configured yet
            return ParkingDisplayData(
                officeName = "No Office Configured",
                officeId = null,
                arrivalStatus = arrivalState.arrivalActive,
                lastUpdated = System.currentTimeMillis(),
                isTestData = false
            )
        }

        // Fetch slots and group by floor
        val slots = parkingSlotDao.getSlotsByOffice(targetOffice.id)
        val floors = parkingSlotDao.getFloorsByOffice(targetOffice.id)

        val floorDisplayList = mutableListOf<FloorDisplayData>()
        if (floors.isNotEmpty()) {
            floors.forEach { floorName ->
                val floorSlots = slots.filter { it.floor.equals(floorName, ignoreCase = true) }
                    .map { it.slotNumber }
                if (floorSlots.isNotEmpty()) {
                    floorDisplayList.add(FloorDisplayData(floorName, floorSlots))
                }
            }
        } else if (slots.isNotEmpty()) {
            val grouped = slots.groupBy { it.floor }
            grouped.forEach { (fl, sList) ->
                floorDisplayList.add(FloorDisplayData(fl, sList.map { it.slotNumber }))
            }
        }

        val allSlotNumbers = slots.map { it.slotNumber }

        return ParkingDisplayData(
            officeName = targetOffice.name,
            officeId = targetOffice.id,
            officeLatitude = targetOffice.latitude,
            officeLongitude = targetOffice.longitude,
            floors = floorDisplayList,
            slots = allSlotNumbers,
            lastUpdated = System.currentTimeMillis(),
            arrivalStatus = arrivalState.arrivalActive,
            isTestData = false
        )
    }

    private fun broadcastData(context: Context, data: ParkingDisplayData) {
        try {
            val json = data.toJson()
            val intent = Intent(ACTION_UPDATE_PARKING_DATA).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_PARKING_DATA_JSON, json)
                putExtra(EXTRA_IS_TEST_MODE, data.isTestData)
            }
            context.sendBroadcast(intent)
            ParkALotAutoLogger.d("Broadcasted parking display data to ${context.packageName}")
        } catch (e: Exception) {
            ParkALotAutoLogger.logCommunicationFailure("LocalBroadcast", e)
        }
    }
}
