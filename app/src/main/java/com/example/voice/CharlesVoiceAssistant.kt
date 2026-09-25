package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.entity.UserEntity
import com.example.data.repository.ParkingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class AssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

class CharlesVoiceAssistant(
    private val context: Context,
    private val repository: ParkingRepository,
    private val onNavigateRequested: (Double, Double) -> Unit = { _, _ -> },
    private val onScreenRequested: (String) -> Unit = {}
) : RecognitionListener {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val handler = Handler(Looper.getMainLooper())
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    private var pendingSpeech: String? = null
    private var isContinuousListeningEnabled = true

    // Alexa-like wake word state
    var isAwaitingWakeWord = true
        private set

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _spokenText = MutableStateFlow("Say \"Charles\" to wake up...")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private val _charlesResponse = MutableStateFlow("Charles is standing by. Say \"Charles\" to activate.")
    val charlesResponse: StateFlow<String> = _charlesResponse.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    private var currentUser: UserEntity? = null
    private var lastSpokenUtterance: String = ""

    init {
        initTts()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        scope.launch {
            currentUser = repository.getUserProfile()
        }
    }

    private fun getHonorific(): String {
        val user = currentUser
        return if (user != null) {
            user.getHonorific()
        } else {
            "Sir"
        }
    }

    private fun playWakeChime() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 130)
            handler.postDelayed({
                try {
                    toneGen.release()
                } catch (_: Exception) {}
            }, 300)
        } catch (_: Exception) {}
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Ensure English language
                val ukLocale = Locale.UK
                val available = textToSpeech?.isLanguageAvailable(ukLocale)
                if (available == TextToSpeech.LANG_AVAILABLE || available == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                    textToSpeech?.language = ukLocale
                } else {
                    textToSpeech?.language = Locale.US
                }

                // Male voice selection: find installed offline male voice or configure deep male pitch
                try {
                    val voices = textToSpeech?.voices
                    val installedMaleVoice = voices?.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        !voice.isNetworkConnectionRequired &&
                                (voice.features == null || !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) &&
                                (name.contains("male") || name.contains("en-gb-x-rjs") || name.contains("en-us-x-sfg") || name.contains("#male") || name.contains("man")) &&
                                !name.contains("female")
                    } ?: voices?.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        !voice.isNetworkConnectionRequired &&
                                (voice.features == null || !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) &&
                                !name.contains("female") && !name.contains("woman") &&
                                voice.locale.language == "en"
                    }

                    if (installedMaleVoice != null) {
                        textToSpeech?.voice = installedMaleVoice
                    }
                } catch (_: Exception) {}

                // Pitch 0.80f creates a deep, distinguished masculine voice (Jarvis / Charles butler tone)
                textToSpeech?.setPitch(0.80f)
                textToSpeech?.setSpeechRate(0.96f)

                // Route through STREAM_MUSIC
                try {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    textToSpeech?.setAudioAttributes(audioAttributes)
                } catch (_: Exception) {}

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _assistantState.value = AssistantState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        _assistantState.value = AssistantState.IDLE
                        // If wake word was detected and Charles responded, resume listening
                        if (isContinuousListeningEnabled) {
                            handler.postDelayed({
                                startListening()
                            }, 500)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _assistantState.value = AssistantState.IDLE
                        if (isContinuousListeningEnabled) {
                            handler.postDelayed({ startListening() }, 500)
                        }
                    }
                })
                isTtsReady = true

                pendingSpeech?.let { queued ->
                    pendingSpeech = null
                    handler.post { speak(queued) }
                }
            }
        }
    }

    fun speak(text: String) {
        lastSpokenUtterance = text
        _charlesResponse.value = text

        // Ensure volume is adequate so user hears the male voice loud and clear
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (currentVol < (maxVol * 0.45f).toInt()) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.75f).toInt(), 0)
            }
        } catch (_: Exception) {}

        if (!isTtsReady || textToSpeech == null) {
            pendingSpeech = text
            return
        }

        pauseListeningForTts()
        _assistantState.value = AssistantState.SPEAKING

        val utteranceId = "charles_speech_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun repeatLastSpeech() {
        if (lastSpokenUtterance.isNotBlank()) {
            speak(lastSpokenUtterance)
        }
    }

    private fun pauseListeningForTts() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _audioRmsDb.value = 0f
    }

    /**
     * Starts listening. By default, it operates in Wake Word mode where
     * it waits for the keyword "Charles".
     */
    fun startListening(requireWakeWord: Boolean = isAwaitingWakeWord) {
        isContinuousListeningEnabled = true
        isAwaitingWakeWord = requireWakeWord

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val h = getHonorific()
            _spokenText.value = "Speech recognition unavailable"
            speak("Speech recognition is not available on this device, $h.")
            return
        }

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@CharlesVoiceAssistant)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            if (isAwaitingWakeWord) {
                _assistantState.value = AssistantState.IDLE
                _spokenText.value = "Say \"Charles\" to wake up..."
            } else {
                _assistantState.value = AssistantState.LISTENING
                _spokenText.value = "Listening for your query, ${getHonorific()}..."
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    /**
     * Alexa manual tap: User taps the mic button to immediately speak without needing the wake word.
     */
    fun activateListeningFromTap() {
        playWakeChime()
        isAwaitingWakeWord = false
        _assistantState.value = AssistantState.LISTENING
        _spokenText.value = "Listening for your query, ${getHonorific()}..."
        startListening(requireWakeWord = false)
    }

    fun stopListening() {
        isContinuousListeningEnabled = false
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _audioRmsDb.value = 0f
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        if (!isAwaitingWakeWord) {
            _assistantState.value = AssistantState.LISTENING
        }
    }

    override fun onBeginningOfSpeech() {
        if (!isAwaitingWakeWord) {
            _assistantState.value = AssistantState.LISTENING
        }
    }

    override fun onRmsChanged(rmsdB: Float) {
        _audioRmsDb.value = (rmsdB.coerceIn(0f, 10f) / 10f)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        if (!isAwaitingWakeWord) {
            _assistantState.value = AssistantState.PROCESSING
        }
    }

    override fun onError(error: Int) {
        _audioRmsDb.value = 0f
        if (isContinuousListeningEnabled && _assistantState.value != AssistantState.SPEAKING) {
            _assistantState.value = AssistantState.IDLE
            val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                1200L
            } else {
                400L
            }
            handler.postDelayed({
                if (isContinuousListeningEnabled && _assistantState.value != AssistantState.SPEAKING) {
                    startListening(requireWakeWord = isAwaitingWakeWord)
                }
            }, delay)
        } else {
            _assistantState.value = AssistantState.IDLE
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
        handleRecognizedMatches(matches)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
        val text = matches.firstOrNull() ?: ""
        if (!isAwaitingWakeWord) {
            _spokenText.value = text
        } else {
            // Check if user said "Charles" during partial results
            val lower = text.lowercase()
            if (containsWakeWord(lower)) {
                handleWakeWordDetected(text)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun containsWakeWord(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("charles") ||
                lower.contains("charlie") ||
                lower.contains("hey charles") ||
                lower.contains("ok charles") ||
                lower.contains("assistant")
    }

    private fun handleRecognizedMatches(matches: List<String>) {
        val rawInput = matches.firstOrNull() ?: return
        val lower = rawInput.lowercase().trim()

        if (isAwaitingWakeWord) {
            // Check all candidates for the wake word
            val matchWithWakeWord = matches.firstOrNull { containsWakeWord(it) }
            if (matchWithWakeWord != null) {
                handleWakeWordDetected(matchWithWakeWord)
            } else {
                // Ignore ambient speech completely when awaiting wake word!
                _assistantState.value = AssistantState.IDLE
                _spokenText.value = "Say \"Charles\" to wake up..."
                if (isContinuousListeningEnabled) {
                    handler.postDelayed({
                        startListening(requireWakeWord = true)
                    }, 400)
                }
            }
        } else {
            // Already awake: process command!
            _spokenText.value = rawInput
            processCommand(rawInput)
        }
    }

    fun handleWakeWordDetected(rawInput: String = "charles") {
        playWakeChime()
        val lower = rawInput.lowercase()

        // Extract command if user spoke "Charles, where is my car?"
        val afterKeyword = when {
            lower.contains("charles") -> lower.substringAfter("charles")
            lower.contains("charlie") -> lower.substringAfter("charlie")
            lower.contains("assistant") -> lower.substringAfter("assistant")
            else -> ""
        }.trim().trim(',', '.', '!', '?')

        if (afterKeyword.isNotBlank()) {
            // Direct query combined with wake word!
            isAwaitingWakeWord = false
            _spokenText.value = "Charles, $afterKeyword"
            processCommand(afterKeyword)
        } else {
            // Only wake word spoken: greet and listen for command
            isAwaitingWakeWord = false
            _spokenText.value = "Charles"
            val h = getHonorific()
            speak("Yes, $h? How may I assist you?")
            // After speaking finishes, it will start command listening
        }
    }

    fun processCommand(rawInput: String) {
        val input = rawInput.lowercase().trim()
        val cleaned = input.removePrefix("charles")
            .removePrefix("hey charles")
            .removePrefix("ok charles")
            .removePrefix("charlie")
            .removePrefix("assistant")
            .trim()

        // After answering, return to wake word listening
        isAwaitingWakeWord = true

        scope.launch {
            if (currentUser == null) {
                currentUser = repository.getUserProfile()
            }
            val honorific = getHonorific()
            val activeSession = repository.getActiveSession()
            val defaultVehicle = repository.getDefaultVehicle()

            when {
                // Where is my car / parking inquiry
                cleaned.contains("where") || cleaned.contains("find") || cleaned.contains("parked") || cleaned.contains("locate") || cleaned.contains("car") || cleaned.contains("vehicle") -> {
                    if (activeSession != null) {
                        val place = activeSession.placeName
                        val floorSlot = if (!activeSession.floor.isNullOrBlank() && !activeSession.slotNumber.isNullOrBlank()) {
                            ", on Floor ${activeSession.floor}, Slot ${activeSession.slotNumber}"
                        } else if (!activeSession.floor.isNullOrBlank()) {
                            ", on Floor ${activeSession.floor}"
                        } else ""
                        speak("I believe you left your vehicle at $place$floorSlot, $honorific. Would you like me to plot navigational guidance?")
                    } else {
                        speak("There are currently no active parking sessions recorded, $honorific. All vehicles are accounted for.")
                    }
                }

                // Navigate to car
                cleaned.contains("navigate") || cleaned.contains("take me") || cleaned.contains("directions") || cleaned.contains("route") -> {
                    if (activeSession != null) {
                        speak("Plotting navigational vectors to your parked vehicle now, $honorific.")
                        onNavigateRequested(activeSession.latitude, activeSession.longitude)
                    } else {
                        speak("I cannot initiate navigational vectors, $honorific, as no active parking coordinate is on file.")
                    }
                }

                // Status report (Charles style)
                cleaned.contains("status") || cleaned.contains("report") || cleaned.contains("telemetry") || cleaned.contains("diagnostics") -> {
                    val office = repository.getDefaultOffice()
                    val vehicleName = defaultVehicle?.let { "${it.name} (${it.registrationNumber})" } ?: "No active vehicle assigned"
                    val parkStatus = if (activeSession != null) "Parked at ${activeSession.placeName}" else "Currently in transit"
                    speak("System diagnostics nominal, $honorific. Vehicle assigned is $vehicleName. Parking status: $parkStatus. Office campus calibrated to ${office?.name ?: "No office configured"}.")
                }

                // Park vehicle command
                cleaned.contains("park") && !cleaned.contains("where") -> {
                    speak("Accessing parking telemetry controls now, $honorific.")
                    onScreenRequested("park_vehicle")
                }

                // Mark as retrieved
                cleaned.contains("retrieved") || cleaned.contains("found") || cleaned.contains("leaving") || cleaned.contains("departing") -> {
                    if (activeSession != null) {
                        repository.markSessionAsRetrieved(activeSession.id)
                        speak("Understood, $honorific. Active parking session closed. Wishing you a pleasant journey.")
                    } else {
                        speak("All parking sessions are already resolved, $honorific.")
                    }
                }

                // Office parking
                cleaned.contains("office") || cleaned.contains("work") -> {
                    speak("Displaying office parking bays now, $honorific.")
                    onScreenRequested("office_parking")
                }

                // 360 view
                cleaned.contains("360") || cleaned.contains("view") || cleaned.contains("model") || cleaned.contains("studio") -> {
                    speak("Initializing vehicle 360-degree holographic inspection, $honorific.")
                    onScreenRequested("vehicle_360")
                }

                // Wake word / Greeting
                cleaned.isEmpty() || cleaned == "hello" || cleaned == "hi" -> {
                    speak("Good day, $honorific. Charles at your service. All systems green.")
                }

                else -> {
                    speak("Command received, $honorific. You may ask: 'Where did I park my car?', 'Navigate to car', or 'Open office parking'.")
                }
            }
        }
    }

    fun shutdown() {
        isContinuousListeningEnabled = false
        stopListening()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}

