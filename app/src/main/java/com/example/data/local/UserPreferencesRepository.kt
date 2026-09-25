package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val GEOFENCING_ENABLED = booleanPreferencesKey("geofencing_enabled")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit") // "meters" or "feet"
        val SELECTED_VEHICLE_ID = longPreferencesKey("selected_vehicle_id")
        val SELECTED_OFFICE_ID = longPreferencesKey("selected_office_id")
        val IS_AT_OFFICE = booleanPreferencesKey("is_at_office")
        val CURRENT_OFFICE_NAME = stringPreferencesKey("current_office_name")
        val WAKE_WORD_LISTENING = booleanPreferencesKey("wake_word_listening")
        val VOICE_ASSISTANT_ENABLED = booleanPreferencesKey("voice_assistant_enabled")
        val ANDROID_AUTO_ENABLED = booleanPreferencesKey("android_auto_enabled")
        val LOCATION_CHECK_INTERVAL_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("location_check_interval_seconds")
        val ARRIVAL_ALERT_TYPE = stringPreferencesKey("arrival_alert_type") // "until_dismissed" or "temporary"
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }
    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }
    val geofencingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.GEOFENCING_ENABLED] ?: true }
    val distanceUnitFlow: Flow<String> = context.dataStore.data.map { it[Keys.DISTANCE_UNIT] ?: "meters" }
    val selectedVehicleIdFlow: Flow<Long?> = context.dataStore.data.map { it[Keys.SELECTED_VEHICLE_ID] }
    val selectedOfficeIdFlow: Flow<Long?> = context.dataStore.data.map { it[Keys.SELECTED_OFFICE_ID] }
    val isAtOfficeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_AT_OFFICE] ?: false }
    val currentOfficeNameFlow: Flow<String> = context.dataStore.data.map { it[Keys.CURRENT_OFFICE_NAME] ?: "" }
    val wakeWordListeningFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.WAKE_WORD_LISTENING] ?: false }
    val voiceAssistantEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.VOICE_ASSISTANT_ENABLED] ?: true }
    val androidAutoEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.ANDROID_AUTO_ENABLED] ?: true }
    val locationCheckIntervalSecondsFlow: Flow<Int> = context.dataStore.data.map { it[Keys.LOCATION_CHECK_INTERVAL_SECONDS] ?: 15 }
    val arrivalAlertTypeFlow: Flow<String> = context.dataStore.data.map { it[Keys.ARRIVAL_ALERT_TYPE] ?: "until_dismissed" }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setArrivalAlertType(type: String) {
        context.dataStore.edit { it[Keys.ARRIVAL_ALERT_TYPE] = type }
    }

    suspend fun setGeofencingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GEOFENCING_ENABLED] = enabled }
    }

    suspend fun setDistanceUnit(unit: String) {
        context.dataStore.edit { it[Keys.DISTANCE_UNIT] = unit }
    }

    suspend fun setSelectedVehicleId(id: Long) {
        context.dataStore.edit { it[Keys.SELECTED_VEHICLE_ID] = id }
    }

    suspend fun setSelectedOfficeId(id: Long) {
        context.dataStore.edit { it[Keys.SELECTED_OFFICE_ID] = id }
    }

    suspend fun setIsAtOffice(isAtOffice: Boolean, officeName: String = "") {
        context.dataStore.edit {
            it[Keys.IS_AT_OFFICE] = isAtOffice
            it[Keys.CURRENT_OFFICE_NAME] = officeName
        }
    }

    suspend fun setWakeWordListening(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WAKE_WORD_LISTENING] = enabled }
    }

    suspend fun setVoiceAssistantEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_ASSISTANT_ENABLED] = enabled }
    }

    suspend fun setAndroidAutoEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANDROID_AUTO_ENABLED] = enabled }
    }

    suspend fun setLocationCheckIntervalSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.LOCATION_CHECK_INTERVAL_SECONDS] = seconds.coerceIn(3, 300) }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
