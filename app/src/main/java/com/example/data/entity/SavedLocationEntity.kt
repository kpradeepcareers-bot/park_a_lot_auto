package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // Home, Office, Apartment, Mall, Airport, Hospital, Shopping Complex, Other
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val notes: String? = null,
    val isWorkLocation: Boolean = false,
    val geofenceRadius: Float = 150f
)
