package com.kspcr.parkalot.auto

import org.json.JSONArray
import org.json.JSONObject

/**
 * Floor display data model for Android Auto renderer.
 */
data class FloorDisplayData(
    val name: String,
    val slots: List<String>
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("name", name)
        val arr = JSONArray()
        slots.forEach { arr.put(it) }
        obj.put("slots", arr)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): FloorDisplayData {
            val name = obj.optString("name", "Floor")
            val slotsArray = obj.optJSONArray("slots")
            val slots = mutableListOf<String>()
            if (slotsArray != null) {
                for (i in 0 until slotsArray.length()) {
                    slots.add(slotsArray.optString(i))
                }
            }
            return FloorDisplayData(name, slots)
        }
    }
}

/**
 * Dynamic parking display data model shared between Park A Lot phone application
 * and the Android Auto renderer components.
 */
data class ParkingDisplayData(
    val officeName: String = "",
    val officeId: Long? = null,
    val officeLatitude: Double = 0.0,
    val officeLongitude: Double = 0.0,
    val floors: List<FloorDisplayData> = emptyList(),
    val slots: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val arrivalStatus: Boolean = false,
    val isTestData: Boolean = false
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("officeName", officeName)
        if (officeId != null) {
            obj.put("officeId", officeId)
        }
        obj.put("officeLatitude", officeLatitude)
        obj.put("officeLongitude", officeLongitude)
        obj.put("arrival", arrivalStatus)
        obj.put("lastUpdated", lastUpdated)
        obj.put("isTestData", isTestData)

        val floorsArray = JSONArray()
        floors.forEach { floorsArray.put(it.toJsonObject()) }
        obj.put("floors", floorsArray)

        val slotsArray = JSONArray()
        slots.forEach { slotsArray.put(it) }
        obj.put("slots", slotsArray)

        return obj.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): ParkingDisplayData {
            val obj = JSONObject(jsonStr)
            val officeName = obj.optString("officeName", "Office")
            val officeId = if (obj.has("officeId")) obj.optLong("officeId") else null
            val officeLatitude = obj.optDouble("officeLatitude", 0.0)
            val officeLongitude = obj.optDouble("officeLongitude", 0.0)
            val arrival = obj.optBoolean("arrival", false)
            val lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
            val isTestData = obj.optBoolean("isTestData", false)

            val floorsList = mutableListOf<FloorDisplayData>()
            val floorsArray = obj.optJSONArray("floors")
            if (floorsArray != null) {
                for (i in 0 until floorsArray.length()) {
                    val floorObj = floorsArray.optJSONObject(i)
                    if (floorObj != null) {
                        floorsList.add(FloorDisplayData.fromJsonObject(floorObj))
                    }
                }
            }

            val slotsList = mutableListOf<String>()
            val slotsArray = obj.optJSONArray("slots")
            if (slotsArray != null) {
                for (i in 0 until slotsArray.length()) {
                    slotsList.add(slotsArray.optString(i))
                }
            } else {
                floorsList.forEach { fl -> slotsList.addAll(fl.slots) }
            }

            return ParkingDisplayData(
                officeName = officeName,
                officeId = officeId,
                officeLatitude = officeLatitude,
                officeLongitude = officeLongitude,
                floors = floorsList,
                slots = slotsList,
                lastUpdated = lastUpdated,
                arrivalStatus = arrival,
                isTestData = isTestData
            )
        }

        /**
         * Developer / Test Mode Sample Data:
         * Office: Deloitte Tower
         * B1: 252, 251, 216, 217, 160, 161, 134, 132, 133
         * B3: 24, 25, 41, 42, 47, 63
         * B4: 46, 47, 24, 25, 41, 42, 134, 144
         */
        fun createSampleTestData(): ParkingDisplayData {
            val b1 = FloorDisplayData("B1", listOf("252", "251", "216", "217", "160", "161", "134", "132", "133"))
            val b3 = FloorDisplayData("B3", listOf("24", "25", "41", "42", "47", "63"))
            val b4 = FloorDisplayData("B4", listOf("46", "47", "24", "25", "41", "42", "134", "144"))
            val allSlots = b1.slots + b3.slots + b4.slots

            return ParkingDisplayData(
                officeName = "Deloitte Tower",
                officeId = 999999L,
                officeLatitude = 45.4965,
                officeLongitude = -73.5694,
                floors = listOf(b1, b3, b4),
                slots = allSlots,
                lastUpdated = System.currentTimeMillis(),
                arrivalStatus = true,
                isTestData = true
            )
        }
    }
}
