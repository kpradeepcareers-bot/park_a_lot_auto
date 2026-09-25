package com.example

import android.app.Application
import com.example.data.repository.ParkingRepository
import com.example.location.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ParkALotApp : Application() {

    lateinit var repository: ParkingRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = ParkingRepository(this)

        NotificationHelper.createNotificationChannel(this)

        applicationScope.launch {
            repository.seedInitialDataIfEmpty()
            com.example.location.GeofenceManager(this@ParkALotApp).registerAllOfficeGeofences()
            com.example.location.BackgroundLocationManager.startLocationMonitoring(this@ParkALotApp)
        }
    }

    companion object {
        lateinit var instance: ParkALotApp
            private set
    }
}
