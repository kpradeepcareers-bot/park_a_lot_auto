package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parking_slots",
    foreignKeys = [
        ForeignKey(
            entity = OfficeEntity::class,
            parentColumns = ["id"],
            childColumns = ["officeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["officeId"]), Index(value = ["officeId", "floor", "slotNumber"], unique = true)]
)
data class ParkingSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val officeId: Long,
    val floor: String,
    val slotNumber: String,
    val zone: String? = null,
    val isEV: Boolean = true,
    val notes: String? = null
)
