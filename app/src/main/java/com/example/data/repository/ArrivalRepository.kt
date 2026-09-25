package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.arrivalDataStore: DataStore<Preferences> by preferencesDataStore(name = "parkalot_arrival_state")

/**
 * Shared persistent arrival state used across:
 * - GeofenceManager & GeofenceBroadcastReceiver
 * - Phone UI (ViewModel & Screens)
 * - CarAppService & Android Auto Screens (MainCarScreen, OfficeCarScreen)
 * - System notification engine
 */
data class ArrivalState(
    val arrivalActive: Boolean = false,
    val arrivalLocationId: Long? = null,
    val arrivalLocationName: String = "",
    val arrivalTimestamp: Long = 0L,
    val arrivalLatitude: Double = 0.0,
    val arrivalLongitude: Double = 0.0
)

class ArrivalRepository private constructor(private val context: Context) {

    private object Keys {
        val ARRIVAL_ACTIVE = booleanPreferencesKey("arrival_active")
        val ARRIVAL_LOCATION_ID = longPreferencesKey("arrival_location_id")
        val ARRIVAL_LOCATION_NAME = stringPreferencesKey("arrival_location_name")
        val ARRIVAL_TIMESTAMP = longPreferencesKey("arrival_timestamp")
        val ARRIVAL_LATITUDE = doublePreferencesKey("arrival_latitude")
        val ARRIVAL_LONGITUDE = doublePreferencesKey("arrival_longitude")
    }

    val arrivalStateFlow: Flow<ArrivalState> = context.arrivalDataStore.data.map { prefs ->
        val active = prefs[Keys.ARRIVAL_ACTIVE] ?: false
        val locId = prefs[Keys.ARRIVAL_LOCATION_ID]?.takeIf { it != -1L }
        val name = prefs[Keys.ARRIVAL_LOCATION_NAME] ?: ""
        val timestamp = prefs[Keys.ARRIVAL_TIMESTAMP] ?: 0L
        val lat = prefs[Keys.ARRIVAL_LATITUDE] ?: 0.0
        val lng = prefs[Keys.ARRIVAL_LONGITUDE] ?: 0.0
        ArrivalState(
            arrivalActive = active,
            arrivalLocationId = locId,
            arrivalLocationName = name,
            arrivalTimestamp = timestamp,
            arrivalLatitude = lat,
            arrivalLongitude = lng
        )
    }

    private val _invalidationEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val invalidationEvents: SharedFlow<Unit> = _invalidationEvents.asSharedFlow()

    suspend fun getArrivalState(): ArrivalState {
        val prefs = context.arrivalDataStore.data.first()
        val active = prefs[Keys.ARRIVAL_ACTIVE] ?: false
        val locId = prefs[Keys.ARRIVAL_LOCATION_ID]?.takeIf { it != -1L }
        val name = prefs[Keys.ARRIVAL_LOCATION_NAME] ?: ""
        val timestamp = prefs[Keys.ARRIVAL_TIMESTAMP] ?: 0L
        val lat = prefs[Keys.ARRIVAL_LATITUDE] ?: 0.0
        val lng = prefs[Keys.ARRIVAL_LONGITUDE] ?: 0.0
        return ArrivalState(
            arrivalActive = active,
            arrivalLocationId = locId,
            arrivalLocationName = name,
            arrivalTimestamp = timestamp,
            arrivalLatitude = lat,
            arrivalLongitude = lng
        )
    }

    suspend fun setArrival(
        active: Boolean,
        locationId: Long?,
        locationName: String,
        timestamp: Long = System.currentTimeMillis(),
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ) {
        context.arrivalDataStore.edit { prefs ->
            prefs[Keys.ARRIVAL_ACTIVE] = active
            if (locationId != null) {
                prefs[Keys.ARRIVAL_LOCATION_ID] = locationId
            } else {
                prefs.remove(Keys.ARRIVAL_LOCATION_ID)
            }
            prefs[Keys.ARRIVAL_LOCATION_NAME] = locationName
            prefs[Keys.ARRIVAL_TIMESTAMP] = timestamp
            prefs[Keys.ARRIVAL_LATITUDE] = latitude
            prefs[Keys.ARRIVAL_LONGITUDE] = longitude
        }
        _invalidationEvents.tryEmit(Unit)
    }

    suspend fun clearArrival() {
        context.arrivalDataStore.edit { prefs ->
            prefs[Keys.ARRIVAL_ACTIVE] = false
            prefs.remove(Keys.ARRIVAL_LOCATION_ID)
            prefs[Keys.ARRIVAL_LOCATION_NAME] = ""
            prefs[Keys.ARRIVAL_TIMESTAMP] = 0L
            prefs[Keys.ARRIVAL_LATITUDE] = 0.0
            prefs[Keys.ARRIVAL_LONGITUDE] = 0.0
        }
        _invalidationEvents.tryEmit(Unit)
    }

    companion object {
        @Volatile
        private var INSTANCE: ArrivalRepository? = null

        fun getInstance(context: Context): ArrivalRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ArrivalRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
