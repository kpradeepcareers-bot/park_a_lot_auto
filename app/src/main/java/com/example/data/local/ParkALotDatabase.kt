package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.OfficeDao
import com.example.data.dao.ParkingSessionDao
import com.example.data.dao.ParkingSlotDao
import com.example.data.dao.SavedLocationDao
import com.example.data.dao.UserDao
import com.example.data.dao.VehicleDao
import com.example.data.entity.OfficeEntity
import com.example.data.entity.ParkingSessionEntity
import com.example.data.entity.ParkingSlotEntity
import com.example.data.entity.SavedLocationEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.VehicleEntity

@Database(
    entities = [
        UserEntity::class,
        VehicleEntity::class,
        SavedLocationEntity::class,
        OfficeEntity::class,
        ParkingSlotEntity::class,
        ParkingSessionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ParkALotDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun officeDao(): OfficeDao
    abstract fun parkingSlotDao(): ParkingSlotDao
    abstract fun parkingSessionDao(): ParkingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: ParkALotDatabase? = null

        fun getDatabase(context: Context): ParkALotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ParkALotDatabase::class.java,
                    "park_a_lot.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
