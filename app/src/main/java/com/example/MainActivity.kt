package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.JarvisWaveformOverlay
import com.example.ui.components.OfficeArrivalOverlay
import com.example.ui.components.ParkALotAnimatedSplash
import com.example.ui.navigation.NavRoutes
import com.example.ui.screens.FindVehicleScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyVehiclesScreen
import com.example.ui.screens.OfficeParkingScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ParkingHistoryScreen
import com.example.ui.screens.ParkVehicleScreen
import com.example.ui.screens.SavedLocationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.Vehicle360Screen
import com.example.ui.screens.launchNavigationIntent
import com.example.ui.theme.ParkALotTheme
import com.example.ui.viewmodel.ParkALotViewModel
import com.example.voice.CharlesVoiceAssistant
import com.example.voice.CharlesWakeWordService

class MainActivity : ComponentActivity() {

    private val viewModel: ParkALotViewModel by viewModels()
    private var voiceAssistant: CharlesVoiceAssistant? = null
    private var onWakeWordTriggered: (() -> Unit)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_TRIGGER_VOICE", false)) {
            onWakeWordTriggered?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialNavigateTo = intent?.getStringExtra("EXTRA_NAVIGATE_TO")
        val initialTriggerVoice = intent?.getBooleanExtra("EXTRA_TRIGGER_VOICE", false) == true

        setContent {
            ParkALotTheme {
                val navController = rememberNavController()
                val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
                val voiceAssistantEnabled by viewModel.voiceAssistantEnabled.collectAsStateWithLifecycle()
                val wakeWordListening by viewModel.wakeWordListening.collectAsStateWithLifecycle()

                var showSplashAnimation by remember { mutableStateOf(true) }
                var showVoiceOverlay by remember { mutableStateOf(initialTriggerVoice && voiceAssistantEnabled) }
                var showPermissionDialog by remember { mutableStateOf(false) }

                // Check and request runtime permissions on app start
                val startupPermissions = remember {
                    val list = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    list.toTypedArray()
                }

                val startupPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissionsResult ->
                    val fineLocation = permissionsResult[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    val coarseLocation = permissionsResult[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    val hasLocation = fineLocation || coarseLocation

                    if (!hasLocation) {
                        showPermissionDialog = true
                    } else {
                        showPermissionDialog = false
                        com.example.location.BackgroundLocationManager.startLocationMonitoring(this@MainActivity)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            com.example.location.GeofenceManager(this@MainActivity).registerAllOfficeGeofences()
                        }
                    }

                    if (permissionsResult[Manifest.permission.RECORD_AUDIO] == true && voiceAssistantEnabled && wakeWordListening) {
                        CharlesWakeWordService.start(this@MainActivity)
                    }
                }

                LaunchedEffect(Unit) {
                    val missingPermissions = startupPermissions.filter { perm ->
                        ContextCompat.checkSelfPermission(this@MainActivity, perm) != PackageManager.PERMISSION_GRANTED
                    }
                    if (missingPermissions.isNotEmpty()) {
                        startupPermissionLauncher.launch(missingPermissions.toTypedArray())
                    }
                }

                // Charles Voice Assistant
                val assistant = remember {
                    CharlesVoiceAssistant(
                        context = this@MainActivity,
                        repository = viewModel.repository,
                        onNavigateRequested = { lat, lng ->
                            launchNavigationIntent(this@MainActivity, lat, lng, "Parked Spot")
                        },
                        onScreenRequested = { route ->
                            showVoiceOverlay = false
                            when (route) {
                                "park_vehicle" -> navController.navigate(NavRoutes.ParkVehicle.route)
                                "office_parking" -> navController.navigate(NavRoutes.OfficeParking.route)
                                "vehicle_360" -> navController.navigate(NavRoutes.Vehicle360.route)
                                "find_vehicle" -> navController.navigate(NavRoutes.FindVehicle.route)
                            }
                        }
                    )
                }

                voiceAssistant = assistant

                val audioPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted && voiceAssistantEnabled) {
                        CharlesWakeWordService.stop(this@MainActivity)
                        assistant.startListening()
                    }
                }

                onWakeWordTriggered = {
                    if (voiceAssistantEnabled) {
                        showVoiceOverlay = true
                        CharlesWakeWordService.stop(this@MainActivity)
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            assistant.handleWakeWordDetected()
                            assistant.startListening()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }

                DisposableEffect(voiceAssistantEnabled, wakeWordListening) {
                    // Only start background wake-word service if BOTH voice assistant and wake word are explicitly enabled
                    if (voiceAssistantEnabled && wakeWordListening && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        CharlesWakeWordService.start(this@MainActivity)
                    } else {
                        CharlesWakeWordService.stop(this@MainActivity)
                    }
                    onDispose {
                        assistant.shutdown()
                        CharlesWakeWordService.stop(this@MainActivity)
                    }
                }

                // If launched via background wake word
                LaunchedEffect(initialTriggerVoice) {
                    if (initialTriggerVoice) {
                        onWakeWordTriggered?.invoke()
                    }
                }

                val assistantState by assistant.assistantState.collectAsStateWithLifecycle()
                val spokenText by assistant.spokenText.collectAsStateWithLifecycle()
                val charlesResponse by assistant.charlesResponse.collectAsStateWithLifecycle()
                val audioRmsDb by assistant.audioRmsDb.collectAsStateWithLifecycle()

                // Deep link navigation from Notifications
                LaunchedEffect(initialNavigateTo) {
                    if (!initialNavigateTo.isNullOrBlank()) {
                        when (initialNavigateTo) {
                            "office_parking" -> navController.navigate(NavRoutes.OfficeParking.route)
                            "find_vehicle" -> navController.navigate(NavRoutes.FindVehicle.route)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val startDestination = if (onboardingCompleted) NavRoutes.Home.route else NavRoutes.Onboarding.route

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(NavRoutes.Onboarding.route) {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onOnboardingComplete = {
                                    navController.navigate(NavRoutes.Home.route) {
                                        popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(NavRoutes.Home.route) {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateTo = { route ->
                                    navController.navigate(route)
                                },
                                onOpenVoiceAssistant = {
                                    if (!voiceAssistantEnabled) {
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            "Charles Voice Assistant is disabled in Settings",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        showVoiceOverlay = true
                                        CharlesWakeWordService.stop(this@MainActivity)
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            assistant.startListening()
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                            )
                        }

                        composable(NavRoutes.ParkVehicle.route) {
                            ParkVehicleScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoutes.FindVehicle.route) {
                            FindVehicleScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoutes.OfficeParking.route) {
                            OfficeParkingScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToLocations = { navController.navigate(NavRoutes.SavedLocations.route) }
                            )
                        }

                        composable(NavRoutes.SavedLocations.route) {
                            SavedLocationsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onParkAtLocation = { loc ->
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(NavRoutes.MyVehicles.route) {
                            MyVehiclesScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpen360 = { navController.navigate(NavRoutes.Vehicle360.route) }
                            )
                        }

                        composable(NavRoutes.Vehicle360.route) {
                            Vehicle360Screen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoutes.ParkingHistory.route) {
                            ParkingHistoryScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavRoutes.Settings.route) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Charles Voice Assistant Hologram Arc Reactor
                    JarvisWaveformOverlay(
                        visible = showVoiceOverlay,
                        state = assistantState,
                        spokenText = spokenText,
                        responseText = charlesResponse,
                        audioLevel = audioRmsDb,
                        onDismiss = {
                            assistant.stopListening()
                            showVoiceOverlay = false
                            if (voiceAssistantEnabled && wakeWordListening && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                CharlesWakeWordService.start(this@MainActivity)
                            } else {
                                CharlesWakeWordService.stop(this@MainActivity)
                            }
                        },
                        onMicTap = {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                assistant.activateListeningFromTap()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onReplayVoice = {
                            assistant.repeatLastSpeech()
                        },
                        onCommandSelected = { cmd ->
                            assistant.processCommand(cmd)
                        }
                    )

                    // Office Arrival Full Screen Pop-up & Floating Minimized Banner
                    OfficeArrivalOverlay(
                        viewModel = viewModel
                    )

                    // Stylish Animated Opening Logo & Splash (displayed on every app open)
                    if (showSplashAnimation) {
                        ParkALotAnimatedSplash(
                            onAnimationFinished = { showSplashAnimation = false }
                        )
                    }

                    // App Startup Permissions Rationale Dialog
                    if (showPermissionDialog && !showSplashAnimation) {
                        AlertDialog(
                            onDismissRequest = { showPermissionDialog = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = {
                                Text(
                                    text = "Permissions Required",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Park a lot needs key permissions to deliver its full car & smart parking experience:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Precise Location: Auto office proximity & Android Auto navigation", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Notifications: Arrival reminders & parked slot alert", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showPermissionDialog = false
                                        startupPermissionLauncher.launch(startupPermissions)
                                    }
                                ) {
                                    Text("Grant Permissions", fontWeight = FontWeight.SemiBold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPermissionDialog = false }) {
                                    Text("Later")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
