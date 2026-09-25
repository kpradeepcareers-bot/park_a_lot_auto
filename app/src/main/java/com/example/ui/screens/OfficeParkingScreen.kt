package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.OfficeEntity
import com.example.data.entity.ParkingSlotEntity
import com.example.ui.components.SaveLocationProfileDialog
import com.example.ui.theme.JarvisArcReactorCyan
import com.example.ui.theme.ParkEvGreenTertiary
import com.example.ui.viewmodel.ParkALotViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OfficeParkingScreen(
    viewModel: ParkALotViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLocations: () -> Unit = {}
) {
    val offices by viewModel.workOffices.collectAsStateWithLifecycle()
    val selectedOffice by viewModel.selectedOffice.collectAsStateWithLifecycle()
    val floors by viewModel.availableFloors.collectAsStateWithLifecycle()
    val selectedFloor by viewModel.selectedFloor.collectAsStateWithLifecycle()
    val slots by viewModel.currentFloorSlots.collectAsStateWithLifecycle()
    val previousParking by viewModel.previousOfficeParking.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()
    val isCurrentVehicleEV by viewModel.isCurrentVehicleEV.collectAsStateWithLifecycle()
    val evOnlyFilter by viewModel.onlyEvFilter.collectAsStateWithLifecycle()

    val isAutoDetectingOffice by viewModel.isAutoDetectingOffice.collectAsStateWithLifecycle()
    val autoDetectedOfficeName by viewModel.autoDetectedOfficeName.collectAsStateWithLifecycle()
    val autoDetectedOfficeDistanceMeters by viewModel.autoDetectedOfficeDistanceMeters.collectAsStateWithLifecycle()

    // Automatically detect GPS location on tab open and select nearest office
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.autoDetectAndSelectNearestOffice()
    }

    val scope = rememberCoroutineScope()
    var officeDropdownExpanded by remember { mutableStateOf(false) }
    var slotToConfirm by remember { mutableStateOf<ParkingSlotEntity?>(null) }
    var slotToEdit by remember { mutableStateOf<ParkingSlotEntity?>(null) }
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var showCreateLocationDialog by remember { mutableStateOf(false) }

    // Multi-selection & Bulk Delete State
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedSlotIds = remember { mutableStateListOf<Long>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedSlotIds.size} Selected",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedSlotIds.clear()
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Selection",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    actions = {
                        // Select All / Deselect All
                        val allSelected = slots.isNotEmpty() && selectedSlotIds.size == slots.size
                        IconButton(onClick = {
                            if (allSelected) {
                                selectedSlotIds.clear()
                            } else {
                                selectedSlotIds.clear()
                                selectedSlotIds.addAll(slots.map { it.id })
                            }
                        }) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deselect All" else "Select All",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Delete Selected
                        IconButton(
                            onClick = { showBulkDeleteConfirm = true },
                            enabled = selectedSlotIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Selected Bays",
                                tint = if (selectedSlotIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Office EV Parking", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Create a new location in this section
                        IconButton(onClick = { showCreateLocationDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AddLocationAlt,
                                contentDescription = "Create New Location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Add bay to current office
                        if (selectedOffice != null) {
                            IconButton(onClick = { showAddSlotDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Bay")
                            }

                            if (slots.isNotEmpty()) {
                                IconButton(onClick = {
                                    isSelectionMode = true
                                }) {
                                    Icon(Icons.Default.Checklist, contentDescription = "Select Bays")
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (offices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Apartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Text(
                        text = "No Office / Work Locations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Create an office or work location to configure custom parking bays and proximity radar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Button(
                        onClick = { showCreateLocationDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Location Here")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Auto-detection of Current Location Status Card
                if (isAutoDetectingOffice) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Detecting current GPS location & workplace...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (autoDetectedOfficeName != null && autoDetectedOfficeDistanceMeters != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ParkEvGreenTertiary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ParkEvGreenTertiary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = ParkEvGreenTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Auto-detected: $autoDetectedOfficeName (${autoDetectedOfficeDistanceMeters!!.roundToInt()}m away)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = { viewModel.autoDetectAndSelectNearestOffice() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("Re-detect", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // 1. Office Selector Dropdown + Create New Location Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = officeDropdownExpanded,
                            onExpandedChange = { officeDropdownExpanded = !officeDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedOffice?.name ?: offices.firstOrNull()?.name ?: "Select Office",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Active Office Location") },
                                leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = officeDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = officeDropdownExpanded,
                                onDismissRequest = { officeDropdownExpanded = false }
                            ) {
                                offices.forEach { office ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(office.name, fontWeight = if (office.id == selectedOffice?.id) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectOffice(office.id)
                                            officeDropdownExpanded = false
                                            isSelectionMode = false
                                            selectedSlotIds.clear()
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("+ Create New Location", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        officeDropdownExpanded = false
                                        showCreateLocationDialog = true
                                    }
                                )
                            }
                        }
                    }

                    // Quick "+ New Location" button
                    Button(
                        onClick = { showCreateLocationDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", fontSize = 13.sp)
                    }
                }

                // Proximity Auto Pop-up Test & Info Action Banner
                OutlinedButton(
                    onClick = {
                        selectedOffice?.let {
                            viewModel.showArrivalPopUp(it, it.geofenceRadius)
                        } ?: viewModel.testTriggerArrivalPopUp()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.EvStation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚡ Preview Office Arrival Full-Screen Pop-Up", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Previous Parking Slot Memory Banner
                previousParking?.let { prev ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Last Parked at this Office",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Floor ${prev.floor} • Slot ${prev.slotNumber}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Button(
                                onClick = {
                                    val currentOffice = selectedOffice ?: return@Button
                                    val slot = ParkingSlotEntity(
                                        officeId = currentOffice.id,
                                        floor = prev.floor ?: "B1",
                                        slotNumber = prev.slotNumber ?: "252",
                                        isEV = true
                                    )
                                    viewModel.parkAtOfficeSlot(slot, currentOffice.latitude, currentOffice.longitude)
                                    onNavigateBack()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Park Here", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Floor Selector Tabs
                if (floors.isNotEmpty()) {
                    val selectedIndex = floors.indexOf(selectedFloor).coerceAtLeast(0)
                    PrimaryTabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        floors.forEachIndexed { index, floorName ->
                            Tab(
                                selected = selectedIndex == index,
                                onClick = {
                                    viewModel.selectFloor(floorName)
                                    selectedSlotIds.clear()
                                },
                                text = {
                                    Text(
                                        text = "Floor $floorName",
                                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }

                // Header with Isolated Bays Count, Filter, Select / Add Bay button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EvStation,
                            contentDescription = null,
                            tint = ParkEvGreenTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedOffice?.name ?: "Office"} Bays (${slots.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // EV Filter Chip
                        FilterChip(
                            selected = evOnlyFilter,
                            onClick = { viewModel.toggleEvFilter() },
                            label = { Text("EV Only", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.ElectricCar, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ParkEvGreenTertiary.copy(alpha = 0.2f),
                                selectedLabelColor = ParkEvGreenTertiary
                            )
                        )

                        // Multi-Select Mode Toggle
                        if (slots.isNotEmpty() && !isSelectionMode) {
                            OutlinedButton(
                                onClick = { isSelectionMode = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Select", fontSize = 12.sp)
                            }
                        }

                        // Add Bay button
                        OutlinedButton(
                            onClick = { showAddSlotDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Bay", fontSize = 12.sp)
                        }
                    }
                }

                // Selection Mode Action Strip (when active)
                if (isSelectionMode && slots.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedSlotIds.size} of ${slots.size} bays selected",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        if (selectedSlotIds.size == slots.size) {
                                            selectedSlotIds.clear()
                                        } else {
                                            selectedSlotIds.clear()
                                            selectedSlotIds.addAll(slots.map { it.id })
                                        }
                                    }
                                ) {
                                    Text(if (selectedSlotIds.size == slots.size) "Deselect All" else "Select All")
                                }

                                if (selectedSlotIds.isNotEmpty()) {
                                    Button(
                                        onClick = { showBulkDeleteConfirm = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete (${selectedSlotIds.size})", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Parking Slots Grid
                if (slots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.EvStation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Text(
                                text = "No Bays in ${selectedOffice?.name ?: "this Location"}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Create and configure custom EV or regular parking bays isolated to this office.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddSlotDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Parking Bay")
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(slots, key = { it.id }) { slot ->
                            val isPreviousSlot = previousParking?.floor == slot.floor && previousParking?.slotNumber == slot.slotNumber
                            val isSelected = selectedSlotIds.contains(slot.id)

                            SlotCardWithSelection(
                                slot = slot,
                                isPreviousSlot = isPreviousSlot,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedSlotIds.remove(slot.id) else selectedSlotIds.add(slot.id)
                                    } else {
                                        slotToConfirm = slot
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedSlotIds.add(slot.id)
                                    }
                                },
                                onEditClick = { slotToEdit = slot }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation to Park Dialog
    slotToConfirm?.let { slot ->
        AlertDialog(
            onDismissRequest = { slotToConfirm = null },
            title = {
                Text("Park at ${selectedOffice?.name ?: "Office"}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Record parking for your ${selectedVehicle?.name ?: "vehicle"} at:")
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Floor ${slot.floor} • Bay ${slot.slotNumber}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!slot.zone.isNullOrBlank()) {
                                Text(
                                    text = "Zone: ${slot.zone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!slot.notes.isNullOrBlank()) {
                                Text(
                                    text = slot.notes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val office = selectedOffice ?: return@Button
                        viewModel.parkAtOfficeSlot(slot, office.latitude, office.longitude)
                        slotToConfirm = null
                        onNavigateBack()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm & Park")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val s = slotToConfirm
                        slotToConfirm = null
                        slotToEdit = s
                    }) {
                        Text("Edit Bay")
                    }
                    TextButton(onClick = { slotToConfirm = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Bulk Delete Selected Bays Confirmation Dialog
    if (showBulkDeleteConfirm && selectedSlotIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete ${selectedSlotIds.size} Parking Bays?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete ${selectedSlotIds.size} selected parking bays from ${selectedOffice?.name ?: "this location"}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBaysByIds(selectedSlotIds.toList())
                        selectedSlotIds.clear()
                        isSelectionMode = false
                        showBulkDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete (${selectedSlotIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add New Slot Dialog
    if (showAddSlotDialog) {
        val currentOffice = selectedOffice
        if (currentOffice != null) {
            AddOrEditBayDialog(
                officeName = currentOffice.name,
                currentFloor = if (selectedFloor.isNotBlank()) selectedFloor else "B1",
                slot = null,
                onDismiss = { showAddSlotDialog = false },
                onSave = { floor, slotNum, zone, isEV, notes ->
                    viewModel.addBay(
                        officeId = currentOffice.id,
                        floor = floor,
                        slotNumber = slotNum,
                        zone = zone,
                        isEV = isEV,
                        notes = notes
                    )
                    showAddSlotDialog = false
                }
            )
        }
    }

    // Edit Existing Slot Dialog
    slotToEdit?.let { slot ->
        val currentOffice = selectedOffice
        AddOrEditBayDialog(
            officeName = currentOffice?.name ?: "Office",
            currentFloor = slot.floor,
            slot = slot,
            onDismiss = { slotToEdit = null },
            onSave = { floor, slotNum, zone, isEV, notes ->
                val updated = slot.copy(
                    floor = floor,
                    slotNumber = slotNum,
                    zone = zone,
                    isEV = isEV,
                    notes = notes
                )
                viewModel.updateBay(updated)
                slotToEdit = null
            },
            onDelete = {
                viewModel.deleteBay(slot)
                slotToEdit = null
            }
        )
    }

    // Create New Location Dialog (integrated directly inside Office EV Parking!)
    if (showCreateLocationDialog) {
        SaveLocationProfileDialog(
            initialLocation = com.example.data.entity.SavedLocationEntity(
                name = "",
                type = "Office",
                latitude = selectedOffice?.latitude ?: 17.4375,
                longitude = selectedOffice?.longitude ?: 78.3752,
                address = "",
                isWorkLocation = true,
                geofenceRadius = 200f
            ),
            onDismiss = { showCreateLocationDialog = false },
            onSave = { loc ->
                // Ensure marked as work location so office entity is created
                val workLoc = loc.copy(isWorkLocation = true, type = "Office")
                viewModel.createAndSelectOfficeLocation(workLoc) {
                    showCreateLocationDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlotCardWithSelection(
    slot: ParkingSlotEntity,
    isPreviousSlot: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isPreviousSlot -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected || isPreviousSlot) 2.dp else 1.dp,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isPreviousSlot -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected || isPreviousSlot) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: Type indicator + Checkbox or Edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (slot.isEV) ParkEvGreenTertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (slot.isEV) Icons.Default.EvStation else Icons.Default.DirectionsCar,
                            contentDescription = if (slot.isEV) "EV Slot" else "Parking Slot",
                            tint = if (slot.isEV) ParkEvGreenTertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    // Edit bay icon button
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Bay",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Bay info
            Column {
                Text(
                    text = slot.slotNumber,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = slot.zone ?: "Floor ${slot.floor}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (isPreviousSlot) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "LAST",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddOrEditBayDialog(
    officeName: String,
    currentFloor: String,
    slot: ParkingSlotEntity?,
    onDismiss: () -> Unit,
    onSave: (floor: String, slotNumber: String, zone: String?, isEV: Boolean, notes: String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEdit = slot != null
    var floor by remember { mutableStateOf(slot?.floor ?: currentFloor) }
    var slotNumber by remember { mutableStateOf(slot?.slotNumber ?: "") }
    var zone by remember { mutableStateOf(slot?.zone ?: "") }
    var isEV by remember { mutableStateOf(slot?.isEV ?: true) }
    var notes by remember { mutableStateOf(slot?.notes ?: if (isEV) "AC Fast Charger 22kW" else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isEdit) "Edit Bay" else "Add Parking Bay",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Location: $officeName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Floor Input
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text("Floor (e.g. B1, B2, Ground, 1)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Slot / Bay Number
                OutlinedTextField(
                    value = slotNumber,
                    onValueChange = { slotNumber = it },
                    label = { Text("Bay / Slot Number (e.g. 204, EV-12)") },
                    placeholder = { Text("e.g. 252") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Zone or Pillar
                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    label = { Text("Zone / Pillar (e.g. Green Zone, Pillar C-14)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // EV Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("EV Charging Bay", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isEV) "Equipped with EV charger" else "General parking bay",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEV,
                        onCheckedChange = { checked ->
                            isEV = checked
                            if (checked && notes.isBlank()) {
                                notes = "AC Fast Charger 22kW"
                            }
                        }
                    )
                }

                // Charger / Bay Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Charger / Bay Notes") },
                    placeholder = { Text("e.g. Type 2, Wallbox, Fast Charger") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (slotNumber.isNotBlank() && floor.isNotBlank()) {
                        onSave(
                            floor.trim(),
                            slotNumber.trim(),
                            zone.trim().takeIf { it.isNotBlank() },
                            isEV,
                            notes.trim().takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = slotNumber.isNotBlank() && floor.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isEdit) "Save Changes" else "Add Bay")
            }
        },
        dismissButton = {
            Row {
                if (isEdit && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
