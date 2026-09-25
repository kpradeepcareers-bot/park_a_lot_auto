package com.kspcr.parkalot.auto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver for explicit local broadcasts between the phone application
 * and the Android Auto renderer.
 */
class ParkALotAutoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        ParkALotAutoLogger.d("ParkALotAutoReceiver received action: $action")

        when (action) {
            ParkALotAutoDataBridge.ACTION_UPDATE_PARKING_DATA -> {
                val jsonStr = intent.getStringExtra(ParkALotAutoDataBridge.EXTRA_PARKING_DATA_JSON)
                if (!jsonStr.isNullOrBlank()) {
                    try {
                        val data = ParkingDisplayData.fromJson(jsonStr)
                        ParkALotAutoDataBridge.updateFromExternal(data)
                        ParkALotAutoLogger.logDataReceived(data, "BroadcastReceiver")
                    } catch (e: Exception) {
                        ParkALotAutoLogger.logCommunicationFailure("BroadcastParsing", e)
                    }
                } else {
                    ParkALotAutoDataBridge.refreshFromDatabase(context)
                }
            }
            ParkALotAutoDataBridge.ACTION_TRIGGER_TEST_MODE -> {
                ParkALotAutoDataBridge.setTestDataMode(context)
            }
        }
    }
}
