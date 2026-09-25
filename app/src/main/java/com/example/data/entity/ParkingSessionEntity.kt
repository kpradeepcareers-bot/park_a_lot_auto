package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parking_sessions",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["officeId"]),
        Index(value = ["savedLocationId"]),
        Index(value = ["isActive"])
    ]
)
data class ParkingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val savedLocationId: Long? = null,
    val officeId: Long? = null,
    val placeName: String,
    val placeType: String = "Other",
    val latitude: Double,
    val longitude: Double,
    val floor: String? = null,
    val slotNumber: String? = null,
    val building: String? = null,
    val wing: String? = null,
    val notes: String? = null,
    val imageUri: String? = null,
    val parkedAt: Long = System.currentTimeMillis(),
    val retrievedAt: Long? = null,
    val isActive: Boolean = true
)
