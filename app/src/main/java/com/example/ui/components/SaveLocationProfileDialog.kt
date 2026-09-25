package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.SavedLocationEntity
import com.example.location.LocationHelper
import com.example.location.PlaceResult
import com.example.ui.theme.JarvisArcReactorCyan
import com.example.ui.theme.ParkEvGreenTertiary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveLocationProfileDialog(
    initialLocation: SavedLocationEntity? = null,
    onDismiss: () -> Unit,
    onSave: (SavedLocationEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var isResolvingUrl by remember { mutableStateOf(false) }
    var urlResolveMessage by remember { mutableStateOf<String?>(null) }
    var isLocatingGps by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(initialLocation?.name ?: "") }
    var address by remember { mutableStateOf(initialLocation?.address ?: "") }
    var category by remember { mutableStateOf(initialLocation?.type ?: "Office") }
    var radiusMeters by remember { mutableFloatStateOf(initialLocation?.geofenceRadius ?: 200f) }

    var latitude by remember { mutableDoubleStateOf(initialLocation?.latitude ?: 17.4375) }
    var longitude by remember { mutableDoubleStateOf(initialLocation?.longitude ?: 78.3752) }

    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Office", "Work", "Home", "Mall", "Airport", "Hospital", "Shopping Complex", "Apartment", "Other")

    // If starting fresh without location, fetch current GPS position once
    LaunchedEffect(Unit) {
        if (initialLocation == null && locationHelper.hasLocationPermission()) {
            val cur = locationHelper.getCurrentLocation()
            if (cur != null) {
                latitude = cur.latitude
                longitude = cur.longitude
                val details = locationHelper.getDetailsFromCoordinates(cur.latitude, cur.longitude)
                name = details.name
                address = details.address
                if (details.suggestedType in categories) {
                    category = details.suggestedType
                }
            }
        }
    }

    fun updateCoordinatesAndAutoName(
        newLat: Double,
        newLng: Double,
        presetName: String? = null,
        presetAddress: String? = null,
        presetType: String? = null
    ) {
        latitude = newLat
        longitude = newLng

        scope.launch {
            if (presetName != null) {
                name = presetName
                address = presetAddress ?: ""
                if (presetType != null && presetType in categories) {
                    category = presetType
                }
            } else {
                val details = locationHelper.getDetailsFromCoordinates(newLat, newLng)
                name = details.name
                address = details.address
                if (details.suggestedType in categories) {
                    category = details.suggestedType
                }
            }
        }
    }

    // Function to resolve Google Maps link or query string
    fun processGoogleMapsLinkOrQuery(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        isResolvingUrl = true
        urlResolveMessage = null

        scope.launch {
            try {
                val resolved = locationHelper.resolveGoogleMapsUrlOrQuery(trimmed)
                if (resolved != null) {
                    latitude = resolved.latitude
                    longitude = resolved.longitude
                    name = resolved.name
                    address = resolved.address
                    if (resolved.suggestedType in categories) {
                        category = resolved.suggestedType
                    }
                    urlResolveMessage = "Extracted: ${resolved.name} (%.4f, %.4f)".format(resolved.latitude, resolved.longitude)
                    Toast.makeText(context, "Location resolved from Google Maps!", Toast.LENGTH_SHORT).show()
                } else {
                    urlResolveMessage = "Could not resolve coordinates. Check link format or search place name."
                }
            } catch (e: Exception) {
                urlResolveMessage = "Error resolving link: ${e.localizedMessage}"
            } finally {
                isResolvingUrl = false
            }
        }
    }

    // Function to fetch current GPS location
    fun fetchCurrentGpsLocation() {
        if (!locationHelper.hasLocationPermission()) {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }

        isLocatingGps = true
        scope.launch {
            try {
                val cur = locationHelper.getCurrentLocation()
                if (cur != null) {
                    updateCoordinatesAndAutoName(cur.latitude, cur.longitude)
                    Toast.makeText(context, "Current location loaded!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Unable to get GPS fix. Please retry.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLocatingGps = false
            }
        }
    }

    // Function to save location entity
    fun performSaveLocation() {
        if (name.isBlank()) {
            Toast.makeText(context, "Please enter a location name", Toast.LENGTH_SHORT).show()
            return
        }
        val isWork = category.equals("Office", ignoreCase = true) || category.equals("Work", ignoreCase = true)
        val entity = SavedLocationEntity(
            id = initialLocation?.id ?: 0,
            name = name.trim(),
            type = category,
            latitude = latitude,
            longitude = longitude,
            address = address.trim(),
            isWorkLocation = isWork,
            geofenceRadius = radiusMeters
        )
        onSave(entity)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ==========================================
                // 1. TOP HEADER: CANCEL, TITLE & SAVE ACTION
                // ==========================================
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel Option
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }

                        // Dialog Title
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (initialLocation == null) "Define Location" else "Edit Location",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Top Save Button
                        Button(
                            onClick = { performSaveLocation() },
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ==========================================
                // 2. SCROLLABLE FORM BODY
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(2.dp))

                    // ==========================================
                    // 3. GOOGLE MAPS ROUTE & URL EXTRACTOR CARD
                    // ==========================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Google Maps Quick Picker",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Route to Google Maps Button
                                Button(
                                    onClick = {
                                        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($name)")
                                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val webUri = Uri.parse("https://www.google.com/maps?q=$latitude,$longitude")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Google Maps", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Text(
                                text = "Pick a location in Google Maps, tap Share -> Copy Link, then paste it below to automatically extract the coordinates and place info.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Paste / Search URL Field with 1-Tap Clipboard Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { query ->
                                        searchQuery = query
                                        // Auto detect Google Maps URLs on paste or type
                                        if (query.contains("maps.app.goo.gl") || query.contains("goo.gl/maps") || query.contains("google.com/maps") || query.startsWith("aps/")) {
                                            processGoogleMapsLinkOrQuery(query)
                                        } else if (query.length >= 2) {
                                            scope.launch {
                                                searchResults = locationHelper.searchPlaces(query)
                                            }
                                        } else {
                                            searchResults = emptyList()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Paste Google Maps Link / Search") },
                                    placeholder = { Text("maps.app.goo.gl/..., aps/... or Place") },
                                    leadingIcon = {
                                        if (isResolvingUrl) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                searchQuery = ""
                                                searchResults = emptyList()
                                                urlResolveMessage = null
                                            }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                // Clipboard Paste Button
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clipItem = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clipItem.isNullOrBlank()) {
                                            searchQuery = clipItem
                                            processGoogleMapsLinkOrQuery(clipItem)
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paste", fontSize = 12.sp)
                                }
                            }

                            // Extraction status message if resolved
                            if (!urlResolveMessage.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = JarvisArcReactorCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = urlResolveMessage!!,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Dropdown search results if text search
                            if (searchResults.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(4.dp)
                                ) {
                                    searchResults.take(4).forEach { place ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    updateCoordinatesAndAutoName(
                                                        place.latitude,
                                                        place.longitude,
                                                        place.name,
                                                        place.address,
                                                        place.type
                                                    )
                                                    searchQuery = ""
                                                    searchResults = emptyList()
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(place.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Text(place.address, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 4. QUICK ACTIONS: GPS CURRENT LOCATION & SAVE BUTTON
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Get Current GPS Location Button
                        OutlinedButton(
                            onClick = { fetchCurrentGpsLocation() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            if (isLocatingGps) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Current GPS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Save Location Button
                        Button(
                            onClick = { performSaveLocation() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = name.isNotBlank(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Location", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // ==========================================
                    // 5. LOCATION NAME & CATEGORY
                    // ==========================================
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Location Name (Auto-selected, editable) *") },
                        placeholder = { Text("e.g. Headquarters Campus / Work Office") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Category Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (category) {
                                        "Office", "Work" -> Icons.Default.Apartment
                                        "Home" -> Icons.Default.Home
                                        "Mall", "Shopping Complex" -> Icons.Default.LocalMall
                                        "Airport" -> Icons.Default.Luggage
                                        "Hospital" -> Icons.Default.LocalHospital
                                        else -> Icons.Default.Place
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(cat)
                                            if (cat == "Office" || cat == "Work") {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "EV Auto Bays",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // EV Info banner for Office
                    if (category == "Office" || category == "Work") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Tagged as $category: Android Auto will automatically pop up EV parking bays when you arrive within ${radiusMeters.roundToInt()}m.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 7. PROXIMITY RADIUS (DYNAMIC SLIDER & PRESETS)
                    // ==========================================
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Proximity Radius (Dynamic Geofence):",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${radiusMeters.roundToInt()} m",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Preset Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(50f to "50m", 100f to "100m", 200f to "200m", 350f to "350m", 500f to "500m", 1000f to "1km").forEach { (meters, label) ->
                                val isSelected = radiusMeters.roundToInt() == meters.toInt()
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { radiusMeters = meters },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }

                        Slider(
                            value = radiusMeters,
                            onValueChange = { radiusMeters = it },
                            valueRange = 50f..1000f,
                            steps = 18
                        )
                    }

                    // ==========================================
                    // 8. ADDRESS / LANDMARK DETAILS
                    // ==========================================
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Landmark") },
                        placeholder = { Text("e.g. Hitec City, Madhapur, Hyderabad") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Big Final Save Location Button
                    Button(
                        onClick = { performSaveLocation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = name.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Location Profile", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
