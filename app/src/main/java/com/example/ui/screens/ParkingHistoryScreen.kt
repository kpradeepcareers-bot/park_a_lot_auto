package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.ParkingSessionEntity
import com.example.ui.theme.ParkEvGreenTertiary
import com.example.ui.viewmodel.ParkALotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingHistoryScreen(
    viewModel: ParkALotViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val history by viewModel.parkingHistory.collectAsStateWithLifecycle()
    val retrievedSessions = remember(history) { history.filter { !it.isActive } }

    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showRestoreAllConfirm by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<ParkingSessionEntity?>(null) }
    var sessionToRestore by remember { mutableStateOf<ParkingSessionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parking History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (retrievedSessions.isNotEmpty()) {
                        IconButton(
                            onClick = { showRestoreAllConfirm = true }
                        ) {
                            Icon(
                                Icons.Default.SettingsBackupRestore,
                                contentDescription = "Restore All Retrieved",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearAllConfirm = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All History")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Parking History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Your completed and active parking sessions will appear here chronologically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (retrievedSessions.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Restore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${retrievedSessions.size} retrieved record(s)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Button(
                                    onClick = { showRestoreAllConfirm = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                items(history, key = { it.id }) { session ->
                    val sdf = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault())
                    val parkedDateStr = sdf.format(Date(session.parkedAt))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (session.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (session.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (session.isActive) ParkEvGreenTertiary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (session.isActive) "ACTIVE SPOT" else "RETRIEVED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (session.isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!session.isActive) {
                                        IconButton(
                                            onClick = { sessionToRestore = session },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Restore,
                                                contentDescription = "Restore Session",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { sessionToDelete = session },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = session.placeName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val locDetail = buildString {
                                if (!session.floor.isNullOrBlank()) append("Floor ${session.floor}")
                                if (!session.slotNumber.isNullOrBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append("Slot ${session.slotNumber}")
                                }
                                if (!session.building.isNullOrBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(session.building)
                                }
                            }

                            if (locDetail.isNotBlank()) {
                                Text(
                                    text = locDetail,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            Text(
                                text = "Parked: $parkedDateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            session.retrievedAt?.let { retTime ->
                                val retStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(retTime))
                                val durationMins = ((retTime - session.parkedAt) / (1000 * 60)).coerceAtLeast(1)
                                val durationText = if (durationMins >= 60) "${durationMins / 60}h ${durationMins % 60}m" else "${durationMins}m"
                                Text(
                                    text = "Retrieved at $retStr (Parked for $durationText)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Photo thumbnail if exists
                            if (!session.imageUri.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(
                                    model = session.imageUri,
                                    contentDescription = "Spot Photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Restore button for retrieved session
                            if (!session.isActive) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        viewModel.restoreParkingSession(session)
                                        Toast.makeText(context, "Spot at ${session.placeName} restored as active parking!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore Spot to Active", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Restore single item confirmation dialog
    sessionToRestore?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToRestore = null },
            title = { Text("Restore Parking Spot", fontWeight = FontWeight.Bold) },
            text = { Text("Reactivate this parking session for ${session.placeName} as your current active parking spot?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreParkingSession(session)
                        Toast.makeText(context, "Restored active parking at ${session.placeName}", Toast.LENGTH_SHORT).show()
                        sessionToRestore = null
                    }
                ) {
                    Text("Restore Active Spot")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore all items confirmation dialog
    if (showRestoreAllConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreAllConfirm = false },
            title = { Text("Restore All Retrieved Sessions", fontWeight = FontWeight.Bold) },
            text = { Text("Restore all ${retrievedSessions.size} retrieved records and reactivate the most recent one as your current active parking spot?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreAllRetrievedSessions()
                        Toast.makeText(context, "Restored all retrieved sessions!", Toast.LENGTH_SHORT).show()
                        showRestoreAllConfirm = false
                    }
                ) {
                    Text("Restore All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete single item dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete History Record") },
            text = { Text("Are you sure you want to remove this record for ${session.placeName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteParkingSession(session)
                        sessionToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear all confirm dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear All History") },
            text = { Text("This will remove all saved and historical parking sessions from the database.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearParkingHistory()
                        showClearAllConfirm = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
