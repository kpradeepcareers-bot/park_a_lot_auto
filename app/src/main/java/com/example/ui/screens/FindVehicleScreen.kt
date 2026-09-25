package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.ParkingSessionEntity
import com.example.location.LocationHelper
import com.example.ui.theme.ParkEvGreenTertiary
import com.example.ui.viewmodel.ParkALotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindVehicleScreen(
    viewModel: ParkALotViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeSession by viewModel.activeParkingSession.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()

    var distanceMeters by remember { mutableFloatStateOf(0f) }
    var showFullPhoto by remember { mutableStateOf(false) }

    // Calculate distance between user current GPS and parked spot
    LaunchedEffect(activeSession) {
        val session = activeSession
        if (session == null) {
            distanceMeters = 0f
            return@LaunchedEffect
        }
        val locationHelper = LocationHelper(context)
        if (locationHelper.hasLocationPermission()) {
            val userLoc = locationHelper.getCurrentLocation()
            if (userLoc != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLoc.latitude,
                    userLoc.longitude,
                    session.latitude,
                    session.longitude,
                    results
                )
                distanceMeters = results[0]
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Find My Vehicle", fontWeight = FontWeight.Bold)
                        selectedVehicle?.let {
                            Text(
                                text = "${it.name} (${it.registrationNumber})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Vehicle switcher chips for isolating & toggling between vehicles
            if (vehicles.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    vehicles.forEach { v ->
                        val isSelected = v.id == selectedVehicle?.id
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { viewModel.selectVehicle(v.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = v.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (activeSession == null) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Active Parking for ${selectedVehicle?.name ?: "Vehicle"}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "${selectedVehicle?.name ?: "This vehicle"} currently has no active parked spot recorded. Parking spots are strictly isolated per vehicle.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Return to Home")
                        }
                    }
                }
            } else {
                val session = activeSession!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Parking Radar / Map Visualizer Card
                ParkingRadarCard(
                    placeName = session.placeName,
                    distanceMeters = distanceMeters,
                    distanceUnit = distanceUnit,
                    latitude = session.latitude,
                    longitude = session.longitude
                )

                // Spot Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PARKING TELEMETRY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                            Text(
                                text = sdf.format(Date(session.parkedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = session.placeName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grid of Slot, Floor, Pillar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DetailChip(
                                title = "FLOOR",
                                value = session.floor?.takeIf { it.isNotBlank() } ?: "Ground",
                                modifier = Modifier.weight(1f)
                            )
                            DetailChip(
                                title = "SLOT",
                                value = session.slotNumber?.takeIf { it.isNotBlank() } ?: "Open",
                                modifier = Modifier.weight(1f)
                            )
                            DetailChip(
                                title = "TYPE",
                                value = session.placeType,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!session.building.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Pillar / Wing: ${session.building}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!session.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Notes: ${session.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Photo preview if attached
                        if (!session.imageUri.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showFullPhoto = true }
                            ) {
                                AsyncImage(
                                    model = session.imageUri,
                                    contentDescription = "Spot Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tap to Zoom", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Primary Actions
                Button(
                    onClick = {
                        launchNavigationIntent(context, session.latitude, session.longitude, session.placeName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate in Google Maps", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareParkingLocation(context, session)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.markVehicleAsRetrieved()
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retrieved")
                    }
                }
            }
        }
    }
}

    // Full photo modal dialog
    if (showFullPhoto && activeSession?.imageUri != null) {
        Dialog(onDismissRequest = { showFullPhoto = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.Black
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = activeSession?.imageUri,
                        contentDescription = "Full Parking Spot Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showFullPhoto = false }, shape = RoundedCornerShape(8.dp)) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailChip(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ParkingRadarCard(
    placeName: String,
    distanceMeters: Float,
    distanceUnit: String,
    latitude: Double,
    longitude: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B2E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Radar concentric rings canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val primaryColor = Color(0xFF00E5FF)

                // Grid background lines
                drawCircle(color = primaryColor.copy(alpha = 0.08f), radius = 120.dp.toPx())
                drawCircle(color = primaryColor.copy(alpha = 0.15f), radius = 80.dp.toPx(), style = Stroke(width = 1.5f))
                drawCircle(color = primaryColor.copy(alpha = 0.25f), radius = 45.dp.toPx(), style = Stroke(width = 1.5f))

                // Crosshair axes
                drawLine(
                    color = primaryColor.copy(alpha = 0.15f),
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, size.height),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.15f),
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                // User dot at center
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = center)
                drawCircle(color = Color(0x66FFFFFF), radius = 12.dp.toPx(), center = center)

                // Parked car target at offset
                val targetOffset = Offset(center.x + 45.dp.toPx(), center.y - 30.dp.toPx())
                drawCircle(color = Color(0xFFFF5252), radius = 8.dp.toPx(), center = targetOffset)
                drawCircle(color = Color(0x66FF5252), radius = 16.dp.toPx(), center = targetOffset)
            }

            // Overlay Distance Indicator
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = if (distanceMeters > 0) {
                        if (distanceUnit == "feet") "%.0f ft away".format(distanceMeters * 3.28084f)
                        else if (distanceMeters > 1000) "%.1f km away".format(distanceMeters / 1000f)
                        else "%.0f m away".format(distanceMeters)
                    } else "Coordinates logged",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "GPS: %.4f, %.4f".format(latitude, longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF80D8FF)
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0x99000000)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PinDrop, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Target Locked", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}

fun launchNavigationIntent(context: Context, latitude: Double, longitude: Double, placeName: String) {
    val uri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")
        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
    }
}

fun shareParkingLocation(context: Context, session: ParkingSessionEntity) {
    val text = buildString {
        append("🚗 Park a lot - Parking Location\n")
        append("Place: ${session.placeName}\n")
        if (!session.floor.isNullOrBlank()) append("Floor: ${session.floor}\n")
        if (!session.slotNumber.isNullOrBlank()) append("Slot: ${session.slotNumber}\n")
        if (!session.building.isNullOrBlank()) append("Wing/Pillar: ${session.building}\n")
        if (!session.notes.isNullOrBlank()) append("Notes: ${session.notes}\n")
        append("Google Maps: https://maps.google.com/?q=${session.latitude},${session.longitude}")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Parking Location"))
}
