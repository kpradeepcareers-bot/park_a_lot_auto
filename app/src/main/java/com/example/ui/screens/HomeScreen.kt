package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import com.example.ui.components.ParkALotMiniLogo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.ParkingSessionEntity
import com.example.data.entity.VehicleEntity
import com.example.ui.theme.JarvisArcReactorCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.ParkEvGreenTertiary
import com.example.ui.viewmodel.ParkALotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ParkALotViewModel,
    onNavigateTo: (String) -> Unit,
    onOpenVoiceAssistant: () -> Unit
) {
    val user by viewModel.userProfile.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeParkingSession.collectAsStateWithLifecycle()
    val isAtOffice by viewModel.isAtOffice.collectAsStateWithLifecycle()
    val currentOfficeName by viewModel.currentOfficeName.collectAsStateWithLifecycle()
    val voiceAssistantEnabled by viewModel.voiceAssistantEnabled.collectAsStateWithLifecycle()
    val autoLocationState by viewModel.autoLocationState.collectAsStateWithLifecycle()
    val workOffices by viewModel.workOffices.collectAsStateWithLifecycle()
    val allOffices by viewModel.offices.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshCurrentLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ParkALotMiniLogo(size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Park a lot",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (user != null) "Welcome back, ${user?.firstName?.ifBlank { "Driver" }}" else "Welcome",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateTo("parking_history") }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Parking History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onNavigateTo("settings") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (voiceAssistantEnabled) {
                FloatingActionButton(
                    onClick = onOpenVoiceAssistant,
                    containerColor = Color(0xFF0A192F),
                    contentColor = JarvisArcReactorCyan,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(60.dp)
                        .border(1.5.dp, JarvisArcReactorCyan.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Charles Voice Assistant",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Live Auto-Detect Location Card with Office/Work Parking Access
            AutoDetectLocationCard(
                locationState = autoLocationState,
                isAtOffice = isAtOffice,
                currentOfficeName = currentOfficeName,
                candidateOffices = if (workOffices.isNotEmpty()) workOffices else allOffices,
                onNavigateToParking = { officeId ->
                    if (officeId != null) {
                        viewModel.selectOfficeId(officeId)
                    }
                    onNavigateTo("office_parking")
                },
                onRefreshLocation = { viewModel.refreshCurrentLocation() },
                onSimulateArrival = { office -> viewModel.simulateOfficeArrival(office) },
                onResetSimulation = { viewModel.resetLocationSimulation() }
            )

            // Selected Vehicle Showcase Card or Empty State
            if (selectedVehicle != null) {
                VehicleProfileCard(
                    vehicle = selectedVehicle!!,
                    allVehicles = vehicles,
                    onSelectVehicle = { viewModel.selectVehicle(it) },
                    onOpen360View = { onNavigateTo("vehicle_360") },
                    onManageVehicles = { onNavigateTo("my_vehicles") }
                )
            } else {
                NoVehicleAssignedCard(
                    onAddVehicle = { onNavigateTo("my_vehicles") }
                )
            }

            // Active Parking Status Card (If Parked)
            activeSession?.let { session ->
                ActiveParkingCard(
                    session = session,
                    onFindVehicle = { onNavigateTo("find_vehicle") },
                    onMarkRetrieved = { viewModel.markVehicleAsRetrieved() }
                )
            }

            // Charles Voice Assistant Bar
            CharlesVoiceCard(onOpenVoiceAssistant = onOpenVoiceAssistant)

            // Primary Navigation Grid
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionTile(
                    title = "Park Vehicle",
                    subtitle = "Save location & photo",
                    icon = Icons.Default.LocalParking,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("park_vehicle") }
                )

                ActionTile(
                    title = "Find Vehicle",
                    subtitle = if (activeSession != null) "Active spot saved" else "No active spot",
                    icon = Icons.Default.NearMe,
                    containerColor = if (activeSession != null) ParkEvGreenTertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = if (activeSession != null) ParkEvGreenTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("find_vehicle") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionTile(
                    title = "Office Parking",
                    subtitle = "EV slot grid & memory",
                    icon = Icons.Default.Apartment,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    iconColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("office_parking") }
                )

                ActionTile(
                    title = "My Locations",
                    subtitle = "Home, mall, office",
                    icon = Icons.Default.AddLocationAlt,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("saved_locations") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionTile(
                    title = "My Vehicles",
                    subtitle = "${vehicles.size} saved • Photos",
                    icon = Icons.Default.DirectionsCar,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("my_vehicles") }
                )

                ActionTile(
                    title = "Settings",
                    subtitle = "Android Auto & Voice",
                    icon = Icons.Default.Settings,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("settings") }
                )
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun AutoDetectLocationCard(
    locationState: com.example.location.AutoLocationState,
    isAtOffice: Boolean,
    currentOfficeName: String,
    candidateOffices: List<com.example.data.entity.OfficeEntity>,
    onNavigateToParking: (Long?) -> Unit,
    onRefreshLocation: () -> Unit,
    onSimulateArrival: (com.example.data.entity.OfficeEntity) -> Unit,
    onResetSimulation: () -> Unit
) {
    val isInside = isAtOffice || locationState.isInsideOfficeGeofence
    val officeName = if (locationState.nearestOffice != null && isInside) {
        locationState.nearestOffice.name
    } else if (currentOfficeName.isNotBlank() && isInside) {
        currentOfficeName
    } else {
        locationState.nearestOffice?.name ?: "Office"
    }

    // Minimized by default as requested
    var isExpanded by remember { mutableStateOf(false) }

    val placeName = locationState.locationDetails?.name
        ?: locationState.lastLocation?.address
        ?: if (isInside) officeName else "Acquiring GPS position..."

    val fullAddress = locationState.locationDetails?.address
        ?: locationState.lastLocation?.let { "Lat: %.4f, Lng: %.4f (±%.0fm)".format(it.latitude, it.longitude, it.accuracy) }
        ?: "Checking nearby work & office landmarks..."

    val targetOfficeId = locationState.nearestOffice?.id ?: candidateOffices.firstOrNull()?.id

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInside) {
                ParkEvGreenTertiary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isInside) 1.5.dp else 1.dp,
            color = if (isInside) ParkEvGreenTertiary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Minimized Header Bar (Always visible, tap anywhere to toggle maximize/minimize)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isInside) ParkEvGreenTertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isInside) Icons.Default.Apartment else Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isInside) "Arrived at $officeName" else placeName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (locationState.isSimulated) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF59E0B)
                                ) {
                                    Text(
                                        text = "TEST",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        val subText = if (isInside) {
                            "Inside work zone • Tap to view parking"
                        } else if (locationState.nearestOfficeDistanceMeters != null && locationState.nearestOffice != null) {
                            val dist = if (locationState.nearestOfficeDistanceMeters < 1000) {
                                "${locationState.nearestOfficeDistanceMeters.toInt()}m"
                            } else {
                                "%.1f km".format(locationState.nearestOfficeDistanceMeters / 1000f)
                            }
                            "$dist from ${locationState.nearestOffice.name}"
                        } else {
                            locationState.statusMessage
                        }

                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isInside) ParkEvGreenTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Header Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Quick "View Bays" button in minimized state if inside office
                    if (isInside && !isExpanded) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ParkEvGreenTertiary,
                            modifier = Modifier.clickable { onNavigateToParking(targetOfficeId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.LocalParking, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "View Bays",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Refresh Button with Spinner
                    IconButton(
                        onClick = onRefreshLocation,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (locationState.isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Location",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Maximize / Minimize Toggle Chevron
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Minimize" else "Maximize",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Maximized / Expanded Details Section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Full Address & Coordinates
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Detected Location Details",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = fullAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Distance & Background Monitoring Status Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isInside) ParkEvGreenTertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isInside) Icons.Default.CheckCircle else Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = if (isInside) ParkEvGreenTertiary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = locationState.statusMessage,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isInside) ParkEvGreenTertiary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (locationState.nearestOfficeDistanceMeters != null && !isInside) {
                                val distStr = if (locationState.nearestOfficeDistanceMeters < 1000) {
                                    "${locationState.nearestOfficeDistanceMeters.toInt()} m"
                                } else {
                                    "%.1f km".format(locationState.nearestOfficeDistanceMeters / 1000f)
                                }
                                Text(
                                    text = distStr,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Prominent Primary Button: TAP TO VIEW PARKING LOTS & BAYS
                    Button(
                        onClick = { onNavigateToParking(targetOfficeId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInside) ParkEvGreenTertiary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalParking,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isInside) "Tap to View Parking Lots & Bays" else "View Office Parking Lots",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Quick Simulation Bar for Developer / User Testing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (locationState.isSimulated) "Simulated location active" else "Automatic background detection",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        if (locationState.isSimulated) {
                            TextButton(
                                onClick = onResetSimulation,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Reset to Live GPS", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (candidateOffices.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    val office = locationState.nearestOffice ?: candidateOffices.first()
                                    onSimulateArrival(office)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Test Office Arrival", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfficeArrivalBanner(officeName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ParkEvGreenTertiary.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ParkEvGreenTertiary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = ParkEvGreenTertiary,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Arrived at $officeName",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap to choose your EV parking slot",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.NearMe,
                contentDescription = null,
                tint = ParkEvGreenTertiary
            )
        }
    }
}

@Composable
fun VehicleProfileCard(
    vehicle: VehicleEntity,
    allVehicles: List<VehicleEntity>,
    onSelectVehicle: (Long) -> Unit,
    onOpen360View: () -> Unit,
    onManageVehicles: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onManageVehicles),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (vehicle.type.equals("Bike", ignoreCase = true)) Icons.Default.TwoWheeler else Icons.Default.ElectricCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${vehicle.manufacturer} ${vehicle.model} • ${vehicle.registrationNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable(onClick = onManageVehicles)
                ) {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Vehicle Photo Display if available
            if (!vehicle.imageUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = vehicle.imageUri,
                    contentDescription = vehicle.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // Vehicle Quick Selector Carousel if user has multiple vehicles
            if (allVehicles.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(allVehicles) { v ->
                        val isSelected = v.id == vehicle.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onSelectVehicle(v.id) }
                        ) {
                            Text(
                                text = v.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveParkingCard(
    session: ParkingSessionEntity,
    onFindVehicle: () -> Unit,
    onMarkRetrieved: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFindVehicle),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PinDrop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Currently parked at",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timeStr = sdf.format(Date(session.parkedAt))
                Text(
                    text = timeStr,
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

            val details = buildString {
                if (!session.floor.isNullOrBlank()) append("Floor ${session.floor}")
                if (!session.slotNumber.isNullOrBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append("Slot ${session.slotNumber}")
                }
                if (!session.building.isNullOrBlank() && session.building != session.placeName) {
                    if (isNotEmpty()) append(" • ")
                    append(session.building)
                }
            }

            if (details.isNotBlank()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            // Thumbnail photo if available
            if (!session.imageUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = session.imageUri,
                    contentDescription = "Parking Spot Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onFindVehicle,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Navigate")
                }

                OutlinedButton(
                    onClick = onMarkRetrieved,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retrieved")
                }
            }
        }
    }
}

@Composable
fun CharlesVoiceCard(onOpenVoiceAssistant: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenVoiceAssistant),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF091424)),
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisArcReactorCyan.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = JarvisArcReactorCyan.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = JarvisArcReactorCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Charles Assistant",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = JarvisArcReactorCyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "ALWAYS LISTENING",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = JarvisArcReactorCyan,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "Say \"Charles\" anytime to wake • Tap to talk",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisArcReactorCyan
                )
            }

            Icon(
                imageVector = Icons.Default.NearMe,
                contentDescription = null,
                tint = JarvisGold,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun NoVehicleAssignedCard(
    onAddVehicle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No Vehicle Assigned",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Add your car or bike from all top manufacturers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAddVehicle,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
