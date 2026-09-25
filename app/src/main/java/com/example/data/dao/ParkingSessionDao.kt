package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.ParkingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingSessionDao {
    @Query("SELECT * FROM parking_sessions WHERE isActive = 1 ORDER BY parkedAt DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<ParkingSessionEntity?>

    @Query("SELECT * FROM parking_sessions WHERE isActive = 1 ORDER BY parkedAt DESC LIMIT 1")
    suspend fun getActiveSession(): ParkingSessionEntity?

    @Query("SELECT * FROM parking_sessions WHERE vehicleId = :vehicleId AND isActive = 1 LIMIT 1")
    fun getActiveSessionForVehicleFlow(vehicleId: Long): Flow<ParkingSessionEntity?>

    @Query("SELECT * FROM parking_sessions WHERE vehicleId = :vehicleId AND isActive = 1 LIMIT 1")
    suspend fun getActiveSessionForVehicle(vehicleId: Long): ParkingSessionEntity?

    @Query("SELECT * FROM parking_sessions ORDER BY parkedAt DESC")
    fun getAllHistoryFlow(): Flow<List<ParkingSessionEntity>>

    @Query("SELECT * FROM parking_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): ParkingSessionEntity?

    @Query("SELECT * FROM parking_sessions WHERE officeId = :officeId AND slotNumber IS NOT NULL ORDER BY parkedAt DESC LIMIT 1")
    suspend fun getLatestOfficeParking(officeId: Long): ParkingSessionEntity?

    @Query("SELECT * FROM parking_sessions WHERE officeId = :officeId AND slotNumber IS NOT NULL ORDER BY parkedAt DESC LIMIT 1")
    fun getLatestOfficeParkingFlow(officeId: Long): Flow<ParkingSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ParkingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ParkingSessionEntity)

    @Delete
    suspend fun deleteSession(session: ParkingSessionEntity)

    @Query("DELETE FROM parking_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM parking_sessions")
    suspend fun clearAllHistory()

    @Query("UPDATE parking_sessions SET isActive = 0, retrievedAt = :retrievedTime WHERE vehicleId = :vehicleId AND isActive = 1")
    suspend fun deactivateSessionsForVehicle(vehicleId: Long, retrievedTime: Long = System.currentTimeMillis())

    @Transaction
    suspend fun saveNewActiveSession(session: ParkingSessionEntity): Long {
        deactivateSessionsForVehicle(session.vehicleId)
        return insertSession(session.copy(isActive = true))
    }

    @Query("UPDATE parking_sessions SET isActive = 0, retrievedAt = :retrievedTime WHERE id = :sessionId")
    suspend fun markAsRetrieved(sessionId: Long, retrievedTime: Long = System.currentTimeMillis())

    @Query("SELECT * FROM parking_sessions WHERE isActive = 0 ORDER BY parkedAt DESC")
    suspend fun getRetrievedSessions(): List<ParkingSessionEntity>

    @Transaction
    suspend fun restoreSessionAsActive(sessionId: Long): ParkingSessionEntity? {
        val session = getSessionById(sessionId) ?: return null
        deactivateSessionsForVehicle(session.vehicleId)
        val restored = session.copy(
            isActive = true,
            retrievedAt = null
        )
        updateSession(restored)
        return restored
    }

    @Transaction
    suspend fun restoreAllRetrievedSessions(): Int {
        val retrieved = getRetrievedSessions()
        if (retrieved.isEmpty()) return 0
        for ((index, s) in retrieved.withIndex()) {
            val restored = s.copy(
                isActive = (index == 0),
                retrievedAt = null
            )
            updateSession(restored)
        }
        return retrieved.size
    }
}
