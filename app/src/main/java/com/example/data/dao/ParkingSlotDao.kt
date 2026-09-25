package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ParkingSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingSlotDao {
    @Query("SELECT * FROM parking_slots WHERE officeId = :officeId ORDER BY floor ASC, slotNumber ASC")
    fun getSlotsByOfficeFlow(officeId: Long): Flow<List<ParkingSlotEntity>>

    @Query("SELECT * FROM parking_slots WHERE officeId = :officeId ORDER BY floor ASC, slotNumber ASC")
    suspend fun getSlotsByOffice(officeId: Long): List<ParkingSlotEntity>

    @Query("SELECT DISTINCT floor FROM parking_slots WHERE officeId = :officeId ORDER BY floor ASC")
    fun getFloorsByOfficeFlow(officeId: Long): Flow<List<String>>

    @Query("SELECT DISTINCT floor FROM parking_slots WHERE officeId = :officeId ORDER BY floor ASC")
    suspend fun getFloorsByOffice(officeId: Long): List<String>

    @Query("SELECT * FROM parking_slots WHERE officeId = :officeId AND floor = :floor ORDER BY slotNumber ASC")
    fun getSlotsByOfficeAndFloorFlow(officeId: Long, floor: String): Flow<List<ParkingSlotEntity>>

    @Query("SELECT * FROM parking_slots WHERE officeId = :officeId AND floor = :floor ORDER BY slotNumber ASC")
    suspend fun getSlotsByOfficeAndFloor(officeId: Long, floor: String): List<ParkingSlotEntity>

    @Query("SELECT * FROM parking_slots WHERE officeId = :officeId AND floor = :floor AND isEV = :isEV ORDER BY slotNumber ASC")
    fun getSlotsByOfficeFloorAndEvFlow(officeId: Long, floor: String, isEV: Boolean): Flow<List<ParkingSlotEntity>>

    @Query("SELECT * FROM parking_slots WHERE officeId = :officeId AND isEV = :isEV ORDER BY floor ASC, slotNumber ASC")
    fun getSlotsByOfficeAndEvFlow(officeId: Long, isEV: Boolean): Flow<List<ParkingSlotEntity>>

    @Query("SELECT * FROM parking_slots ORDER BY officeId ASC, floor ASC, slotNumber ASC")
    suspend fun getAllSlots(): List<ParkingSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: ParkingSlotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<ParkingSlotEntity>)

    @Update
    suspend fun updateSlot(slot: ParkingSlotEntity)

    @Delete
    suspend fun deleteSlot(slot: ParkingSlotEntity)

    @Delete
    suspend fun deleteSlots(slots: List<ParkingSlotEntity>)

    @Query("DELETE FROM parking_slots WHERE id IN (:slotIds)")
    suspend fun deleteSlotsByIds(slotIds: List<Long>)

    @Query("DELETE FROM parking_slots WHERE officeId = :officeId")
    suspend fun clearSlotsForOffice(officeId: Long)
}
