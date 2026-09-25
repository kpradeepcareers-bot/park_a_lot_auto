package com.example.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.ParkingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale

class CharlesWakeWordService : Service(), RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var repository: ParkingRepository
    private var isListening = false

    companion object {
        const val CHANNEL_ID = "charles_wake_service_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_LISTENING = "com.parkalot.ACTION_START_WAKE_LISTENING"
        const val ACTION_STOP_LISTENING = "com.parkalot.ACTION_STOP_WAKE_LISTENING"

        fun start(context: Context) {
            val intent = Intent(context, CharlesWakeWordService::class.java).apply {
                action = ACTION_START_LISTENING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CharlesWakeWordService::class.java).apply {
                action = ACTION_STOP_LISTENING
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = ParkingRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_LISTENING) {
            stopListening()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch {
            val isVoiceEnabled = repository.preferences.voiceAssistantEnabledFlow.firstOrNull() ?: true
            val isWakeEnabled = repository.preferences.wakeWordListeningFlow.firstOrNull() ?: false

            if (!isVoiceEnabled || !isWakeEnabled) {
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }

            startForeground(NOTIFICATION_ID, buildForegroundNotification())
            startListeningLoop()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Charles Wake Word Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background listener for Charles voice commands"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Charles Voice Assistant Active")
            .setContentText("Say \"Charles\" at any time to open and command")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startListeningLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        handler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(this@CharlesWakeWordService)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                isListening = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                scheduleRestart(1000)
            }
        }
    }

    private fun stopListening() {
        isListening = false
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun scheduleRestart(delayMs: Long = 1000) {
        if (!isListening) return
        handler.postDelayed({
            scope.launch {
                val isVoiceEnabled = repository.preferences.voiceAssistantEnabledFlow.firstOrNull() ?: true
                val isWakeEnabled = repository.preferences.wakeWordListeningFlow.firstOrNull() ?: false
                if (isVoiceEnabled && isWakeEnabled && isListening) {
                    startListeningLoop()
                } else {
                    stopListening()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }, delayMs)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        checkSpokenText(matches)
        scheduleRestart(200)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        checkSpokenText(matches)
    }

    private fun checkSpokenText(matches: List<String>?) {
        val fullText = matches?.joinToString(" ")?.lowercase() ?: return
        if (fullText.contains("charles")) {
            // Wake word detected! Bring app to foreground and activate assistant
            triggerAppLaunchWithVoice(fullText)
        }
    }

    private fun triggerAppLaunchWithVoice(spoken: String) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_TRIGGER_VOICE", true)
            putExtra("EXTRA_SPOKEN_WAKE", spoken)
        }
        startActivity(launchIntent)
    }

    override fun onError(error: Int) {
        // Continuous listening: on timeout or no match, quietly restart
        scheduleRestart(400)
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }
}
