package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.ParkingSessionEntity
import com.example.data.entity.VehicleEntity
import com.example.data.local.ParkALotDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: ParkALotDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ParkALotDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify app name resource is Park a lot`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Park a lot", appName)
    }

    @Test
    fun `insert vehicle and parking session in room database`() = runBlocking {
        val vehicle = VehicleEntity(
            id = 1,
            type = "Car",
            name = "Nexon EV",
            manufacturer = "Tata",
            model = "Nexon EV",
            registrationNumber = "TS09EV1234",
            year = 2025,
            color = "#00E5FF",
            isDefault = true,
            fuelType = "Electric"
        )
        val vehicleId = database.vehicleDao().insertVehicle(vehicle)
        assertTrue(vehicleId > 0)

        val session = ParkingSessionEntity(
            id = 101,
            vehicleId = vehicleId,
            placeName = "Deloitte Hyderabad",
            placeType = "Office",
            latitude = 17.4375,
            longitude = 78.3752,
            floor = "B1",
            slotNumber = "252",
            isActive = true
        )
        database.parkingSessionDao().insertSession(session)

        val active = database.parkingSessionDao().getActiveSession()
        assertNotNull(active)
        assertEquals("Deloitte Hyderabad", active?.placeName)
        assertEquals("252", active?.slotNumber)
        assertEquals("B1", active?.floor)
    }
}
