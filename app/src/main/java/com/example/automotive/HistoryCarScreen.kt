package com.example.automotive

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.data.entity.ParkingSessionEntity
import com.example.data.repository.ParkingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryCarScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = ParkingRepository(carContext)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var historyList: List<ParkingSessionEntity> = emptyList()

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                scope.launch {
                    repository.historyFlow.collect { list ->
                        historyList = list
                        invalidate()
                    }
                }
            }
        })
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        if (historyList.isEmpty()) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("No History Found")
                    .addText("Previous parking spots will show here")
                    .build()
            )
        } else {
            historyList.take(6).forEach { session ->
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                val timeStr = sdf.format(Date(session.parkedAt))
                val subtitle = buildString {
                    if (!session.floor.isNullOrBlank()) append("Floor ${session.floor} ")
                    if (!session.slotNumber.isNullOrBlank()) append("Slot ${session.slotNumber} • ")
                    append(timeStr)
                    if (session.isActive) append(" (Active)")
                }

                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(session.placeName)
                        .addText(subtitle)
                        .setOnClickListener {
                            val navUri = Uri.parse("geo:${session.latitude},${session.longitude}?q=${session.latitude},${session.longitude}")
                            val navIntent = Intent(CarContext.ACTION_NAVIGATE, navUri)
                            try {
                                carContext.startCarApp(navIntent)
                            } catch (_: Exception) {
                                carContext.startActivity(Intent(Intent.ACTION_VIEW, navUri))
                            }
                        }
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle("Parking History")
            .setSingleList(listBuilder.build())
            .setHeaderAction(Action.BACK)
            .build()
    }
}
