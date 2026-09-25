package com.kspcr.parkalot.auto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Android Auto Automotive Renderer Activity.
 * Minimal, read-only parking information dashboard with high contrast,
 * large readable typography, and distraction-optimized layout.
 */
class ParkALotAutoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ParkALotAutoLogger.logRendererStartup("ParkALotAutoActivity")

        // Initial sync from local Room database
        ParkALotAutoDataBridge.refreshFromDatabase(this)

        setContent {
            val displayData by ParkALotAutoDataBridge.displayDataFlow.collectAsStateWithLifecycle()

            LaunchedEffect(displayData) {
                ParkALotAutoLogger.logUiRefresh(
                    rendererType = "NativeComposeAutoRenderer",
                    officeName = displayData.officeName,
                    floorCount = displayData.floors.size,
                    slotCount = displayData.slots.size
                )
            }

            ParkALotAutoScreen(data = displayData)
        }
    }

    override fun onResume() {
        super.onResume()
        ParkALotAutoDataBridge.refreshFromDatabase(this)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParkALotAutoScreen(data: ParkingDisplayData) {
    val scrollState = rememberScrollState()
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(data.lastUpdated))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Test Mode Notice Banner
            if (data.isTestData) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ DEVELOPER TEST DATA MODE (DELOITTE SAMPLE)",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp)
                    )
                }
            }

            // App Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2563EB),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("P", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "PARK A LOT",
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Arrival status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (data.arrivalStatus) Color(0x3310B981) else Color(0x333B82F6),
                    border = BorderStroke(1.dp, if (data.arrivalStatus) Color(0xFF10B981) else Color(0xFF3B82F6))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (data.arrivalStatus) Color(0xFF34D399) else Color(0xFF60A5FA),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (data.arrivalStatus) "ARRIVED" else "SYNCED",
                            color = if (data.arrivalStatus) Color(0xFF34D399) else Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Office Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = data.officeName.ifBlank { "No Office Configured" },
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (data.arrivalStatus) "Arrived at destination" else "Configured Office bays",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            }

            // Floor & Parking Bays Display
            if (data.floors.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF131B2A),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🅿️", fontSize = 40.sp)
                        Text(
                            text = "No Parking Bays Configured",
                            color = Color(0xFFE2E8F0),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure parking bays for ${data.officeName} in the Park A Lot phone app.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                for (floor in data.floors) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = BorderStroke(1.dp, Color(0xFF1F2937))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Floor Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF2563EB)
                                ) {
                                    Text(
                                        text = floor.name,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "${floor.slots.size} bays",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Large high-contrast slot badges
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (slot in floor.slots) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.width(76.dp)
                                    ) {
                                        Text(
                                            text = slot,
                                            color = Color(0xFFF8FAFC),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer
            Text(
                text = "Park A Lot • Last updated: $formattedTime • Read-only Driver Display",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
        }
    }
}
