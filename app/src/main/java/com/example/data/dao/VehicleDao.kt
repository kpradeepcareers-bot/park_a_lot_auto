package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY isDefault DESC, createdAt ASC")
    fun getAllVehiclesFlow(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY isDefault DESC, createdAt ASC")
    suspend fun getAllVehicles(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun getVehicleByIdFlow(id: Long): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleById(id: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE isDefault = 1 LIMIT 1")
    fun getDefaultVehicleFlow(): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultVehicle(): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    @Query("UPDATE vehicles SET isDefault = 0 WHERE id != :defaultVehicleId")
    suspend fun clearOtherDefaults(defaultVehicleId: Long)

    @Query("UPDATE vehicles SET isDefault = 0")
    suspend fun clearAllDefaults()

    @Transaction
    suspend fun setDefaultVehicle(vehicleId: Long) {
        clearAllDefaults()
        setVehicleDefault(vehicleId)
    }

    @Query("UPDATE vehicles SET isDefault = 1 WHERE id = :vehicleId")
    suspend fun setVehicleDefault(vehicleId: Long)

    @Query("DELETE FROM vehicles")
    suspend fun clearAllVehicles()
}
