package com.kspcr.parkalot.auto

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Bound service for direct inter-process communication between Park A Lot phone app
 * and the Android Auto renderer.
 */
class ParkALotAutoService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ParkALotAutoService = this@ParkALotAutoService

        fun getParkingDisplayData(): ParkingDisplayData {
            return ParkALotAutoDataBridge.displayDataFlow.value
        }

        fun updateParkingData(data: ParkingDisplayData) {
            ParkALotAutoDataBridge.updateFromExternal(data)
        }

        fun triggerDatabaseRefresh() {
            ParkALotAutoDataBridge.refreshFromDatabase(this@ParkALotAutoService)
        }

        fun setTestDataMode(testData: ParkingDisplayData? = null) {
            ParkALotAutoDataBridge.setTestDataMode(this@ParkALotAutoService, testData)
        }

        fun restoreLiveMode() {
            ParkALotAutoDataBridge.restoreLiveDatabaseMode(this@ParkALotAutoService)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ParkALotAutoLogger.logRendererStartup("ParkALotAutoService")
        ParkALotAutoDataBridge.refreshFromDatabase(this)
    }

    override fun onBind(intent: Intent?): IBinder {
        val callingPackage = packageManager.getNameForUid(Binder.getCallingUid()) ?: "unknown"
        ParkALotAutoLogger.logConnection("ServiceBinding", "Caller UID package: $callingPackage")
        return binder
    }

    override fun onDestroy() {
        ParkALotAutoLogger.d("ParkALotAutoService destroyed")
        super.onDestroy()
    }
}
