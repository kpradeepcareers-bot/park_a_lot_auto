package com.kspcr.parkalot.auto

import android.util.Log

/**
 * Standardized logger for the Park A Lot Android Auto Renderer component.
 * Uses the required tag 'ParkALotAuto'.
 */
object ParkALotAutoLogger {
    const val TAG = "ParkALotAuto"

    fun logRendererStartup(component: String, version: String = "1.0") {
        Log.i(TAG, "[$component] Renderer startup initiated. Version: $version")
    }

    fun logConnection(source: String, details: String = "") {
        Log.i(TAG, "Connection established from: $source. Details: $details")
    }

    fun logDataReceived(data: ParkingDisplayData, channel: String = "LocalIPC") {
        Log.i(
            TAG,
            "Received dynamic parking data via $channel -> Office: '${data.officeName}', " +
                    "Floors: ${data.floors.size}, Slots: ${data.slots.size}, " +
                    "Timestamp: ${data.lastUpdated}, Arrived: ${data.arrivalStatus}, TestMode: ${data.isTestData}"
        )
    }

    fun logUiRefresh(rendererType: String, officeName: String, floorCount: Int, slotCount: Int) {
        Log.i(
            TAG,
            "[$rendererType] UI refresh executed -> Office: '$officeName', Floors: $floorCount, Total Slots: $slotCount"
        )
    }

    fun logCommunicationFailure(channel: String, error: Throwable?, contextInfo: String = "") {
        Log.e(TAG, "Communication failure on channel [$channel] ($contextInfo): ${error?.message}", error)
    }

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
