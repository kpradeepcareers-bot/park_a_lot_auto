package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.OfficeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeDao {
    @Query("SELECT * FROM offices ORDER BY isDefault DESC, name ASC")
    fun getAllOfficesFlow(): Flow<List<OfficeEntity>>

    @Query("SELECT * FROM offices ORDER BY isDefault DESC, name ASC")
    suspend fun getAllOffices(): List<OfficeEntity>

    @Query("SELECT * FROM offices WHERE id = :id LIMIT 1")
    fun getOfficeByIdFlow(id: Long): Flow<OfficeEntity?>

    @Query("SELECT * FROM offices WHERE id = :id LIMIT 1")
    suspend fun getOfficeById(id: Long): OfficeEntity?

    @Query("SELECT * FROM offices WHERE isDefault = 1 LIMIT 1")
    fun getDefaultOfficeFlow(): Flow<OfficeEntity?>

    @Query("SELECT * FROM offices WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultOffice(): OfficeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffice(office: OfficeEntity): Long

    @Update
    suspend fun updateOffice(office: OfficeEntity)

    @Delete
    suspend fun deleteOffice(office: OfficeEntity)

    @Query("UPDATE offices SET isDefault = 0")
    suspend fun clearAllDefaults()

    @Query("UPDATE offices SET isDefault = 1 WHERE id = :officeId")
    suspend fun setOfficeDefault(officeId: Long)

    @Query("SELECT * FROM offices WHERE savedLocationId = :savedLocationId LIMIT 1")
    suspend fun getOfficeBySavedLocationId(savedLocationId: Long): OfficeEntity?

    @Query("DELETE FROM offices WHERE savedLocationId = :savedLocationId")
    suspend fun deleteOfficeBySavedLocationId(savedLocationId: Long)

    @Transaction
    suspend fun setDefaultOffice(officeId: Long) {
        clearAllDefaults()
        setOfficeDefault(officeId)
    }
}
