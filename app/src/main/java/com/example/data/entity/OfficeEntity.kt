package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offices")
data class OfficeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val geofenceRadius: Float = 150f,
    val imageUri: String? = null,
    val isDefault: Boolean = false,
    val savedLocationId: Long? = null
)
