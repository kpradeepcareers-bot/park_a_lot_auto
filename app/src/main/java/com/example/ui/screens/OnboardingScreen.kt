package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.launch
import com.example.data.entity.VehicleEntity
import com.example.data.local.PreloadedData
import com.example.ui.theme.ParkEvGreenTertiary
import com.example.ui.viewmodel.ParkALotViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: ParkALotViewModel,
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var showImportDialog by remember { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val json = stream.bufferedReader().use { it.readText() }
                    importInputText = json
                    importStatusMessage = null
                }
            } catch (e: Exception) {
                importStatusMessage = "Failed to read selected file: ${e.localizedMessage}"
            }
        }
    }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dobMillis by remember { mutableLongStateOf(0L) }
    var dobString by remember { mutableStateOf("") }
    var calculatedAge by remember { mutableStateOf<Int?>(null) }
    var isUnderAge by remember { mutableStateOf(false) }

    // Gender selection: "Male", "Female", "Others"
    var selectedGender by remember { mutableStateOf("Male") }

    // Vehicle addition toggle: Default is NO vehicle set!
    var shouldAddVehicleNow by remember { mutableStateOf(false) }

    // Multi-manufacturer vehicle catalog & dropdown
    val allManufacturers = remember { PreloadedData.getAllManufacturers() }
    var selectedManufacturer by remember { mutableStateOf(allManufacturers.firstOrNull() ?: "Maruti Suzuki") }
    var mfgDropdownExpanded by remember { mutableStateOf(false) }

    // Custom vehicle fields when "Others" is selected
    var customManufacturerName by remember { mutableStateOf("") }
    var customModelName by remember { mutableStateOf("") }
    var customVehicleType by remember { mutableStateOf("Car") }
    var customFuelType by remember { mutableStateOf("Electric") }
    var customColor by remember { mutableStateOf("White") }

    val isOthersSelected = selectedManufacturer.equals("Others", ignoreCase = true)
    val modelsForSelectedManufacturer = remember(selectedManufacturer) {
        if (isOthersSelected) emptyList() else PreloadedData.getModelsByManufacturer(selectedManufacturer)
    }
    var selectedModelIndex by remember(selectedManufacturer) { mutableIntStateOf(0) }
    var customRegNumber by remember { mutableStateOf("") }
    var customVehicleNickname by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val chosenMillis = selectedCal.timeInMillis
                dobMillis = chosenMillis

                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                dobString = sdf.format(selectedCal.time)

                // Age Calculation
                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - year
                if (today.get(Calendar.DAY_OF_YEAR) < selectedCal.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }
                calculatedAge = age
                isUnderAge = age < 18
            },
            calendar.get(Calendar.YEAR) - 20,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val canContinue = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            dobMillis > 0L &&
            !isUnderAge

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalParking,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to Park a lot",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Your AI parking companion powered by Charles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Setup: Import Existing Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        importStatusMessage = null
                        showImportDialog = true
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Already have a backup file?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Import profile, vehicles & locations with 1 tap",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "IMPORT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step 1: Personal Details & Age Verification
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "1. Driver Registration & Verification",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        placeholder = { Text("e.g., Tony") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        placeholder = { Text("e.g., Stark") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gender Selection (Male, Female, Others)
                    Text(
                        text = "Gender",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Male option
                        GenderSelectionCard(
                            title = "Male",
                            isSelected = selectedGender == "Male",
                            onClick = { selectedGender = "Male" },
                            modifier = Modifier.weight(1f)
                        )

                        // Female option
                        GenderSelectionCard(
                            title = "Female",
                            isSelected = selectedGender == "Female",
                            onClick = { selectedGender = "Female" },
                            modifier = Modifier.weight(1f)
                        )

                        // Others option
                        GenderSelectionCard(
                            title = "Others",
                            isSelected = selectedGender == "Others",
                            onClick = { selectedGender = "Others" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date of Birth & 18+ Verification
                    Text(
                        text = "Date of Birth (Minimum Driving Age: 18 Years)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (dobString.isEmpty()) "Select Date of Birth" else dobString,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    calculatedAge?.let { age ->
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isUnderAge) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Age: $age years old. You must be 18+ to use Park a lot.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ParkEvGreenTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Age verified: $age years old (Eligible)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ParkEvGreenTertiary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step 2: Vehicle Setup
            // User requested: "by default there shall be no vehicle set to the user , not only the electric vehicle"
            // "user shall be able to add any vehicle at the start so show all the manufactures and their respective vehicles"
            AnimatedVisibility(visible = calculatedAge != null && !isUnderAge) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "2. Vehicle Setup (Optional)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (shouldAddVehicleNow) "Select manufacturer and model below" else "By default, no vehicle is set. You can add one now or later.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = shouldAddVehicleNow,
                                onCheckedChange = { shouldAddVehicleNow = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        if (shouldAddVehicleNow) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Manufacturer Dropdown Option
                            Text(
                                text = "Select Manufacturer",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            ExposedDropdownMenuBox(
                                expanded = mfgDropdownExpanded,
                                onExpandedChange = { mfgDropdownExpanded = !mfgDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedManufacturer,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Vehicle Manufacturer") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mfgDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = mfgDropdownExpanded,
                                    onDismissRequest = { mfgDropdownExpanded = false }
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
                                                selectedManufacturer = mfg
                                                selectedModelIndex = 0
                                                mfgDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isOthersSelected) {
                                // Manual entry for Custom Manufacturer & Vehicle Details
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Custom Manufacturer & Vehicle Details",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        OutlinedTextField(
                                            value = customManufacturerName,
                                            onValueChange = { customManufacturerName = it },
                                            label = { Text("Manufacturer / Brand Name *") },
                                            placeholder = { Text("e.g. Porsche, Volkswagen, Hero, etc.") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = customModelName,
                                            onValueChange = { customModelName = it },
                                            label = { Text("Model Name *") },
                                            placeholder = { Text("e.g. Taycan, Virtus, Splendor, etc.") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        // Vehicle Type (Car / Bike)
                                        Text(
                                            text = "Vehicle Type",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("Car", "Bike").forEach { type ->
                                                val isSelected = customVehicleType.equals(type, ignoreCase = true)
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { customVehicleType = type },
                                                    label = { Text(if (type == "Car") "🚗 Car" else "🛵 Bike / 2W") },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        // Fuel / Powertrain Type
                                        Text(
                                            text = "Powertrain / Fuel Type",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(listOf("Electric", "Petrol", "Diesel", "Hybrid", "CNG")) { fuel ->
                                                FilterChip(
                                                    selected = customFuelType == fuel,
                                                    onClick = { customFuelType = fuel },
                                                    label = { Text(fuel) }
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = customColor,
                                            onValueChange = { customColor = it },
                                            label = { Text("Vehicle Color") },
                                            placeholder = { Text("e.g. Blue, Grey, Red") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = customRegNumber,
                                            onValueChange = { customRegNumber = it.uppercase() },
                                            label = { Text("Registration Plate Number") },
                                            placeholder = { Text("e.g. TS09AB1234") },
                                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = customVehicleNickname,
                                            onValueChange = { customVehicleNickname = it },
                                            label = { Text("Vehicle Nickname (Optional)") },
                                            placeholder = { Text("e.g. My Daily Ride") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }
                            } else {
                                // Models for chosen manufacturer
                                val currentModel = modelsForSelectedManufacturer.getOrNull(selectedModelIndex)
                                    ?: modelsForSelectedManufacturer.firstOrNull()

                                if (currentModel != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (currentModel.type == "Bike") Icons.Default.DirectionsBike else Icons.Default.DirectionsCar,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "${currentModel.manufacturer} ${currentModel.model}",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (currentModel.isEV) ParkEvGreenTertiary else MaterialTheme.colorScheme.secondary
                                                ) {
                                                    Text(
                                                        text = currentModel.fuelType,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = "Specs: ${currentModel.batterySpec} • Power: ${currentModel.powerBhp} bhp • Range: ${currentModel.rangeKm} km",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (modelsForSelectedManufacturer.size > 1) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    selectedModelIndex = if (selectedModelIndex > 0) {
                                                        selectedModelIndex - 1
                                                    } else {
                                                        modelsForSelectedManufacturer.size - 1
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Previous")
                                            }

                                            Text(
                                                text = "${selectedModelIndex + 1} of ${modelsForSelectedManufacturer.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.align(Alignment.CenterVertically)
                                            )

                                            OutlinedButton(
                                                onClick = {
                                                    selectedModelIndex = (selectedModelIndex + 1) % modelsForSelectedManufacturer.size
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Next Model")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = customRegNumber,
                                        onValueChange = { customRegNumber = it.uppercase() },
                                        label = { Text("Registration Plate Number") },
                                        placeholder = { Text("e.g. TS09AB1234") },
                                        leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = customVehicleNickname,
                                        onValueChange = { customVehicleNickname = it },
                                        label = { Text("Vehicle Nickname (Optional)") },
                                        placeholder = { Text("e.g. Daily Commuter") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Submit Button
            Button(
                onClick = {
                    if (canContinue) {
                        viewModel.saveUserProfile(firstName, lastName, dobMillis, selectedGender)

                        if (shouldAddVehicleNow) {
                            if (isOthersSelected) {
                                val mfgName = customManufacturerName.trim().ifEmpty { "Custom Brand" }
                                val mdlName = customModelName.trim().ifEmpty { "Custom Model" }
                                val vName = customVehicleNickname.trim().ifEmpty { "$mfgName $mdlName" }
                                val vReg = customRegNumber.trim().ifEmpty { "TS09AB0001" }

                                val vehicle = VehicleEntity(
                                    type = customVehicleType,
                                    name = vName,
                                    manufacturer = mfgName,
                                    model = mdlName,
                                    registrationNumber = vReg,
                                    year = 2025,
                                    color = customColor.trim().ifEmpty { "White" },
                                    imageUri = null,
                                    isDefault = true,
                                    fuelType = customFuelType
                                )
                                viewModel.addVehicle(vehicle)
                            } else {
                                val currentModel = modelsForSelectedManufacturer.getOrNull(selectedModelIndex)
                                    ?: modelsForSelectedManufacturer.firstOrNull()

                                if (currentModel != null) {
                                    val vName = customVehicleNickname.trim().ifEmpty {
                                        "${currentModel.manufacturer} ${currentModel.model}"
                                    }
                                    val vReg = customRegNumber.trim().ifEmpty { "TS09AB0001" }

                                    val vehicle = VehicleEntity(
                                        type = currentModel.type,
                                        name = vName,
                                        manufacturer = currentModel.manufacturer,
                                        model = currentModel.model,
                                        registrationNumber = vReg,
                                        year = 2025,
                                        color = currentModel.defaultColor,
                                        imageUri = null,
                                        isDefault = true,
                                        fuelType = currentModel.fuelType
                                    )
                                    viewModel.addVehicle(vehicle)
                                }
                            }
                        }
                        onOnboardingComplete()
                    }
                },
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Get Started with Park a lot",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Import Configuration Dialog on Welcome Screen
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Configuration", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Upload a backup JSON file or paste your exported configuration to restore all settings and bypass manual setup:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    importFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload File", maxLines = 1, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val clip = clipboardManager.getText()
                                    if (clip != null && clip.text.isNotBlank()) {
                                        importInputText = clip.text
                                        importStatusMessage = null
                                    } else {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste JSON", maxLines = 1, fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = importInputText,
                            onValueChange = {
                                importInputText = it
                                importStatusMessage = null
                            },
                            label = { Text("Configuration JSON") },
                            placeholder = { Text("{\n  \"version\": 1,\n  ...\n}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            maxLines = 10
                        )

                        importStatusMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (msg.startsWith("Success", ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importInputText.isBlank()) {
                                importStatusMessage = "Please paste JSON or choose a backup file first"
                                return@Button
                            }
                            isImporting = true
                            scope.launch {
                                try {
                                    val result = viewModel.importConfiguration(importInputText)
                                    if (result.success) {
                                        Toast.makeText(
                                            context,
                                            "Configuration imported! Welcome back.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        showImportDialog = false
                                        viewModel.setOnboardingCompleted(true)
                                        onOnboardingComplete()
                                    } else {
                                        importStatusMessage = "Import failed: ${result.message}"
                                    }
                                } catch (e: Exception) {
                                    importStatusMessage = "Error importing: ${e.localizedMessage}"
                                } finally {
                                    isImporting = false
                                }
                            }
                        },
                        enabled = !isImporting && importInputText.isNotBlank()
                    ) {
                        Text(if (isImporting) "Importing..." else "Restore & Start")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun GenderSelectionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
