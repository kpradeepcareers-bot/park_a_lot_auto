package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.VehicleEntity
import com.example.data.local.PreloadedData
import com.example.ui.viewmodel.ParkALotViewModel
import com.example.util.LicensePlateOcrHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVehiclesScreen(
    viewModel: ParkALotViewModel,
    onNavigateBack: () -> Unit,
    onOpen360: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var vehicleToEdit by remember { mutableStateOf<VehicleEntity?>(null) }
    var vehicleToDelete by remember { mutableStateOf<VehicleEntity?>(null) }
    var activeVehicleForPhoto by remember { mutableStateOf<VehicleEntity?>(null) }

    // Camera launcher for vehicle photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        val v = activeVehicleForPhoto
        if (bitmap != null && v != null) {
            scope.launch(Dispatchers.IO) {
                val savedUri = saveBitmapToInternalStorage(context, bitmap, "vehicle_${v.id}")
                if (savedUri != null) {
                    withContext(Dispatchers.Main) {
                        viewModel.updateVehicle(v.copy(imageUri = savedUri.toString()))
                    }
                }
            }
        }
    }

    // Gallery Photo Picker launcher for vehicle image upload
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val v = activeVehicleForPhoto
        if (uri != null && v != null) {
            scope.launch(Dispatchers.IO) {
                val savedUri = copyUriToInternalStorage(context, uri, "vehicle_${v.id}")
                if (savedUri != null) {
                    withContext(Dispatchers.Main) {
                        viewModel.updateVehicle(v.copy(imageUri = savedUri.toString()))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Vehicles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(vehicles, key = { it.id }) { vehicle ->
                val isDefault = vehicle.isDefault
                val isSelected = selectedVehicle?.id == vehicle.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectVehicle(vehicle.id) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.8.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (vehicle.type.equals("Bike", ignoreCase = true)) Icons.Default.TwoWheeler else Icons.Default.ElectricCar,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = vehicle.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${vehicle.manufacturer} ${vehicle.model} • ${vehicle.registrationNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!vehicle.fuelType.isNullOrBlank() || !vehicle.color.isNullOrBlank()) {
                                        Text(
                                            text = "${vehicle.fuelType ?: "EV"} • ${vehicle.color ?: "White"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isDefault) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "DEFAULT",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                IconButton(
                                    onClick = { vehicleToEdit = vehicle },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Vehicle",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Vehicle Photo Banner (If attached)
                        if (!vehicle.imageUri.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = vehicle.imageUri,
                                    contentDescription = "Vehicle Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Remove photo chip
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(30.dp)
                                    ) {
                                    IconButton(
                                        onClick = {
                                            viewModel.updateVehicle(vehicle.copy(imageUri = null))
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions: Edit, Camera & Gallery Upload, Set Default, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { vehicleToEdit = vehicle },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        activeVehicleForPhoto = vehicle
                                        cameraLauncher.launch(null)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (vehicle.imageUri.isNullOrBlank()) "Photo" else "Retake", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        activeVehicleForPhoto = vehicle
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Upload", fontSize = 12.sp)
                                }

                                if (!isDefault) {
                                    TextButton(
                                        onClick = { viewModel.setDefaultVehicle(vehicle.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.StarBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Default", fontSize = 12.sp)
                                    }
                                }
                            }

                            if (vehicles.size > 1) {
                                IconButton(onClick = { vehicleToDelete = vehicle }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Vehicle Dialog
    if (showAddDialog) {
        val allManufacturers = remember { PreloadedData.getAllManufacturers() }
        var selectedMfg by remember { mutableStateOf(allManufacturers.firstOrNull() ?: "Maruti Suzuki") }
        var mfgExpanded by remember { mutableStateOf(false) }

        val isOthers = selectedMfg.equals("Others", ignoreCase = true)
        val models = remember(selectedMfg) { if (isOthers) emptyList() else PreloadedData.getModelsByManufacturer(selectedMfg) }
        var selectedModelIdx by remember(selectedMfg) { mutableStateOf(0) }
        var modelExpanded by remember { mutableStateOf(false) }

        val chosenModel = models.getOrNull(selectedModelIdx) ?: models.firstOrNull()

        // Custom fields
        var customMfgName by remember { mutableStateOf("") }
        var customModelName by remember { mutableStateOf("") }
        var customType by remember { mutableStateOf("Car") }
        var customFuel by remember { mutableStateOf("Electric") }
        var customColor by remember { mutableStateOf("White") }

        var customNickname by remember { mutableStateOf("") }
        var customReg by remember { mutableStateOf("") }
        var newVehicleImageUri by remember { mutableStateOf<String?>(null) }
        var isScanningPlate by remember { mutableStateOf(false) }

        // Camera launcher for plate number OCR
        val plateCameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            if (bitmap != null) {
                isScanningPlate = true
                scope.launch {
                    val extracted = LicensePlateOcrHelper.recognizePlateFromBitmap(bitmap)
                    isScanningPlate = false
                    if (!extracted.isNullOrBlank()) {
                        customReg = extracted.uppercase().trim()
                        Toast.makeText(context, "Plate Detected: $customReg (Editable)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not detect plate clearly. Please type or retake photo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Gallery picker launcher for plate number OCR
        val plateGalleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                isScanningPlate = true
                scope.launch {
                    val extracted = LicensePlateOcrHelper.recognizePlateFromUri(context, uri)
                    isScanningPlate = false
                    if (!extracted.isNullOrBlank()) {
                        customReg = extracted.uppercase().trim()
                        Toast.makeText(context, "Plate Detected: $customReg (Editable)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not detect plate clearly. Please type or select another photo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val addDialogCameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            if (bitmap != null) {
                scope.launch(Dispatchers.IO) {
                    val saved = saveBitmapToInternalStorage(context, bitmap, "new_vehicle_${System.currentTimeMillis()}")
                    if (saved != null) {
                        withContext(Dispatchers.Main) {
                            newVehicleImageUri = saved.toString()
                        }
                    }
                }
            }
        }

        val addDialogGalleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    val saved = copyUriToInternalStorage(context, uri, "new_vehicle_${System.currentTimeMillis()}")
                    if (saved != null) {
                        withContext(Dispatchers.Main) {
                            newVehicleImageUri = saved.toString()
                        }
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Vehicle", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Select Manufacturer:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    ExposedDropdownMenuBox(
                        expanded = mfgExpanded,
                        onExpandedChange = { mfgExpanded = !mfgExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedMfg,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Manufacturer") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mfgExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = mfgExpanded,
                            onDismissRequest = { mfgExpanded = false }
                        ) {
                            allManufacturers.forEach { mfg ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mfg,
                                            fontWeight = if (mfg == "Others") FontWeight.Bold else FontWeight.Normal,
                                            color = if (mfg == "Others") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedMfg = mfg
                                        selectedModelIdx = 0
                                        customNickname = ""
                                        mfgExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (isOthers) {
                        Text(
                            text = "2. Enter Custom Vehicle Details:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = customMfgName,
                            onValueChange = { customMfgName = it },
                            label = { Text("Manufacturer / Make *") },
                            placeholder = { Text("e.g. Porsche, Hero, Volvo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customModelName,
                            onValueChange = { customModelName = it },
                            label = { Text("Model Name *") },
                            placeholder = { Text("e.g. Taycan, Splendor, XC90") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Car", "Bike").forEach { type ->
                                val isSelected = customType.equals(type, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { customType = type },
                                    label = { Text(if (type == "Car") "🚗 Car" else "🛵 Bike / 2W") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = "Powertrain:",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Electric", "Petrol", "Diesel", "Hybrid").forEach { fuel ->
                                FilterChip(
                                    selected = customFuel == fuel,
                                    onClick = { customFuel = fuel },
                                    label = { Text(fuel) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customColor,
                            onValueChange = { customColor = it },
                            label = { Text("Color") },
                            placeholder = { Text("e.g. White, Black, Red") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = "2. Select Vehicle Model:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        ExposedDropdownMenuBox(
                            expanded = modelExpanded,
                            onExpandedChange = { modelExpanded = !modelExpanded }
                        ) {
                            OutlinedTextField(
                                value = "${chosenModel?.model ?: ""} (${chosenModel?.fuelType ?: ""})",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Model & Powertrain") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = modelExpanded,
                                onDismissRequest = { modelExpanded = false }
                            ) {
                                models.forEachIndexed { idx, item ->
                                    DropdownMenuItem(
                                        text = { Text("${item.model} • ${item.fuelType} (${item.type})") },
                                        onClick = {
                                            selectedModelIdx = idx
                                            customNickname = "${item.manufacturer} ${item.model}"
                                            modelExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = if (customNickname.isEmpty() && !isOthers) "${chosenModel?.manufacturer ?: ""} ${chosenModel?.model ?: ""}" else customNickname,
                        onValueChange = { customNickname = it },
                        label = { Text("Display Nickname") },
                        placeholder = { Text("e.g. Daily Commuter") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Registration Plate with OCR Scan feature
                    Text(
                        text = "Registration Plate Number (Editable):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = customReg,
                        onValueChange = { customReg = it.uppercase() },
                        label = { Text("Registration Plate *") },
                        placeholder = { Text("e.g. TS09AB1234") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (isScanningPlate) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { plateCameraLauncher.launch(null) }) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Scan Plate with Camera",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )

                    // Plate Photo OCR Quick Actions
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { plateCameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Snap Plate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    plateGalleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload Plate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Optional Vehicle Image
                    Text(
                        text = "Vehicle Photo (Optional):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!newVehicleImageUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = newVehicleImageUri,
                                contentDescription = "Vehicle Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { newVehicleImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { addDialogCameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    addDialogGalleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val canSubmit = if (isOthers) {
                    customReg.isNotBlank() && customMfgName.isNotBlank() && customModelName.isNotBlank()
                } else {
                    customReg.isNotBlank() && chosenModel != null
                }

                Button(
                    onClick = {
                        if (isOthers) {
                            val mfg = customMfgName.trim()
                            val mdl = customModelName.trim()
                            val nick = customNickname.trim().ifEmpty { "$mfg $mdl" }
                            val newVehicle = VehicleEntity(
                                type = customType,
                                name = nick,
                                manufacturer = mfg,
                                model = mdl,
                                registrationNumber = customReg.trim(),
                                year = 2025,
                                color = customColor.trim().ifEmpty { "White" },
                                imageUri = newVehicleImageUri,
                                isDefault = vehicles.isEmpty(),
                                fuelType = customFuel
                            )
                            viewModel.addVehicle(newVehicle)
                            showAddDialog = false
                        } else if (chosenModel != null) {
                            val nick = customNickname.trim().ifEmpty { "${chosenModel.manufacturer} ${chosenModel.model}" }
                            val newVehicle = VehicleEntity(
                                type = chosenModel.type,
                                name = nick,
                                manufacturer = chosenModel.manufacturer,
                                model = chosenModel.model,
                                registrationNumber = customReg.trim(),
                                year = 2025,
                                color = chosenModel.defaultColor,
                                imageUri = newVehicleImageUri,
                                isDefault = vehicles.isEmpty(),
                                fuelType = chosenModel.fuelType
                            )
                            viewModel.addVehicle(newVehicle)
                            showAddDialog = false
                        }
                    },
                    enabled = canSubmit
                ) {
                    Text("Add Vehicle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Vehicle Dialog
    vehicleToEdit?.let { vehicle ->
        var editNickname by remember(vehicle) { mutableStateOf(vehicle.name) }
        var editManufacturer by remember(vehicle) { mutableStateOf(vehicle.manufacturer) }
        var editModel by remember(vehicle) { mutableStateOf(vehicle.model) }
        var editRegNumber by remember(vehicle) { mutableStateOf(vehicle.registrationNumber) }
        var editType by remember(vehicle) { mutableStateOf(vehicle.type) }
        var editFuelType by remember(vehicle) { mutableStateOf(vehicle.fuelType ?: "Electric") }
        var editColor by remember(vehicle) { mutableStateOf(vehicle.color ?: "White") }
        var editImageUri by remember(vehicle) { mutableStateOf(vehicle.imageUri) }
        var editIsDefault by remember(vehicle) { mutableStateOf(vehicle.isDefault) }
        var isEditScanningPlate by remember { mutableStateOf(false) }

        // Camera launcher for plate number OCR in Edit Dialog
        val editPlateCameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            if (bitmap != null) {
                isEditScanningPlate = true
                scope.launch {
                    val extracted = LicensePlateOcrHelper.recognizePlateFromBitmap(bitmap)
                    isEditScanningPlate = false
                    if (!extracted.isNullOrBlank()) {
                        editRegNumber = extracted.uppercase().trim()
                        Toast.makeText(context, "Plate Detected: $editRegNumber (Editable)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not detect plate clearly. Please type or retake photo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Gallery picker launcher for plate number OCR in Edit Dialog
        val editPlateGalleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                isEditScanningPlate = true
                scope.launch {
                    val extracted = LicensePlateOcrHelper.recognizePlateFromUri(context, uri)
                    isEditScanningPlate = false
                    if (!extracted.isNullOrBlank()) {
                        editRegNumber = extracted.uppercase().trim()
                        Toast.makeText(context, "Plate Detected: $editRegNumber (Editable)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not detect plate clearly. Please type or select another photo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val editVehicleCameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            if (bitmap != null) {
                scope.launch(Dispatchers.IO) {
                    val saved = saveBitmapToInternalStorage(context, bitmap, "vehicle_${vehicle.id}")
                    if (saved != null) {
                        withContext(Dispatchers.Main) {
                            editImageUri = saved.toString()
                        }
                    }
                }
            }
        }

        val editVehicleGalleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    val saved = copyUriToInternalStorage(context, uri, "vehicle_${vehicle.id}")
                    if (saved != null) {
                        withContext(Dispatchers.Main) {
                            editImageUri = saved.toString()
                        }
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { vehicleToEdit = null },
            title = { Text("Edit Vehicle Information", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editNickname,
                        onValueChange = { editNickname = it },
                        label = { Text("Display Nickname *") },
                        placeholder = { Text("e.g. Daily Commuter") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editManufacturer,
                            onValueChange = { editManufacturer = it },
                            label = { Text("Make / Brand *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editModel,
                            onValueChange = { editModel = it },
                            label = { Text("Model *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Vehicle Type
                    Text(text = "Vehicle Type:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Car", "Bike").forEach { type ->
                            FilterChip(
                                selected = editType.equals(type, ignoreCase = true),
                                onClick = { editType = type },
                                label = { Text(if (type == "Car") "🚗 Car" else "🛵 Bike / 2W") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Powertrain / Fuel
                    Text(text = "Powertrain:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Electric", "Petrol", "Diesel", "Hybrid").forEach { fuel ->
                            FilterChip(
                                selected = editFuelType.equals(fuel, ignoreCase = true),
                                onClick = { editFuelType = fuel },
                                label = { Text(fuel) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editColor,
                        onValueChange = { editColor = it },
                        label = { Text("Color") },
                        placeholder = { Text("e.g. Black, White, Red") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Registration Plate with OCR feature
                    Text(
                        text = "Registration Plate (Photo OCR & Editable):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = editRegNumber,
                        onValueChange = { editRegNumber = it.uppercase() },
                        label = { Text("Registration Plate *") },
                        placeholder = { Text("e.g. TS09AB1234") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (isEditScanningPlate) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { editPlateCameraLauncher.launch(null) }) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Scan Plate with Camera",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )

                    // Plate Photo Scan Quick Actions
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { editPlateCameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Snap Plate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    editPlateGalleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload Plate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Vehicle Photo
                    Text(text = "Vehicle Photo:", style = MaterialTheme.typography.labelSmall)
                    if (!editImageUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = editImageUri,
                                contentDescription = "Vehicle Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { editImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { editVehicleCameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    editVehicleGalleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload", fontSize = 12.sp)
                            }
                        }
                    }

                    // Default Vehicle Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editIsDefault = !editIsDefault }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (editIsDefault) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (editIsDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Set as Default Vehicle",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            },
            confirmButton = {
                val canSave = editNickname.isNotBlank() && editManufacturer.isNotBlank() && editModel.isNotBlank() && editRegNumber.isNotBlank()

                Button(
                    onClick = {
                        val updated = vehicle.copy(
                            name = editNickname.trim(),
                            manufacturer = editManufacturer.trim(),
                            model = editModel.trim(),
                            registrationNumber = editRegNumber.trim().uppercase(),
                            type = editType,
                            fuelType = editFuelType,
                            color = editColor.trim(),
                            imageUri = editImageUri,
                            isDefault = editIsDefault
                        )
                        viewModel.updateVehicle(updated)
                        if (editIsDefault) {
                            viewModel.setDefaultVehicle(vehicle.id)
                        }
                        Toast.makeText(context, "Vehicle information updated successfully!", Toast.LENGTH_SHORT).show()
                        vehicleToEdit = null
                    },
                    enabled = canSave
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    vehicleToDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            title = { Text("Delete Vehicle") },
            text = { Text("Are you sure you want to remove ${vehicle.name} (${vehicle.registrationNumber})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVehicle(vehicle)
                        vehicleToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Saves a Bitmap from camera capture into app's persistent internal storage
 */
private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, prefix: String): Uri? {
    return try {
        val filename = "${prefix}_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Copies a Uri from Android photo picker into app's persistent internal storage
 */
private fun copyUriToInternalStorage(context: Context, sourceUri: Uri, prefix: String): Uri? {
    return try {
        val filename = "${prefix}_${System.currentTimeMillis()}.jpg"
        val destFile = File(context.filesDir, filename)
        val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
        inputStream?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(destFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
