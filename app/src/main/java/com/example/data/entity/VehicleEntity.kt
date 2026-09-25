package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "Car" or "Bike"
    val name: String, // nickname/display name
    val manufacturer: String,
    val model: String,
    val registrationNumber: String,
    val year: Int? = null,
    val color: String? = null,
    val imageUri: String? = null,
    val isDefault: Boolean = false,
    val fuelType: String = "Electric", // Electric, Petrol, Diesel, Hybrid
    val createdAt: Long = System.currentTimeMillis()
)
