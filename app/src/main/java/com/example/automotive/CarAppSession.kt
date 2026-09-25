package com.example.automotive

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.example.data.repository.ArrivalRepository
import kotlinx.coroutines.runBlocking

class CarAppSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        val arrivalRepository = ArrivalRepository.getInstance(carContext)
        val arrivalState = runBlocking { arrivalRepository.getArrivalState() }

        return if (arrivalState.arrivalActive) {
            OfficeCarScreen(
                carContext = carContext,
                targetOfficeId = arrivalState.arrivalLocationId
            )
        } else {
            MainCarScreen(carContext)
        }
    }
}
