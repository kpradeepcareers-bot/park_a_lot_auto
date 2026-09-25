package com.kspcr.parkalot.auto

import android.content.Intent
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.media.MediaBrowserService

/**
 * Automotive Media Capability Service for Android Auto integration.
 * Enables Park A Lot Auto to be discovered and launched on the car screen
 * without being blocked by Car App Library validator restrictions.
 */
class ParkALotAutoMediaService : MediaBrowserService() {

    private var mediaSession: MediaSession? = null
    private val rootId = "parkalot_auto_root"

    override fun onCreate() {
        super.onCreate()
        ParkALotAutoLogger.logRendererStartup("ParkALotAutoMediaService")

        mediaSession = MediaSession(this, "ParkALotAutoSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    ParkALotAutoDataBridge.refreshFromDatabase(this@ParkALotAutoMediaService)
                    updateSessionState()
                }

                override fun onCustomAction(action: String, extras: Bundle?) {
                    when (action) {
                        "REFRESH_PARKING_DATA" -> {
                            ParkALotAutoDataBridge.refreshFromDatabase(this@ParkALotAutoMediaService)
                        }
                        "SET_TEST_MODE" -> {
                            ParkALotAutoDataBridge.setTestDataMode(this@ParkALotAutoMediaService)
                        }
                    }
                }
            })
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true
        }

        sessionToken = mediaSession?.sessionToken
        updateSessionState()
    }

    private fun updateSessionState() {
        val data = ParkALotAutoDataBridge.displayDataFlow.value
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, data.officeName.ifBlank { "Park A Lot" })
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Bays: ${data.slots.size} available")
            .putString(MediaMetadata.METADATA_KEY_ALBUM, if (data.arrivalStatus) "Arrived" else "Synced")

        mediaSession?.setMetadata(metadataBuilder.build())

        val stateBuilder = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP)
            .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)

        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        ParkALotAutoLogger.logConnection("MediaBrowserService", "Client: $clientPackageName (UID: $clientUid)")
        return BrowserRoot(rootId, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowser.MediaItem>>) {
        val data = ParkALotAutoDataBridge.displayDataFlow.value
        val items = mutableListOf<MediaBrowser.MediaItem>()

        val description = MediaDescription.Builder()
            .setMediaId("parkalot_current_parking")
            .setTitle(data.officeName.ifBlank { "Park A Lot" })
            .setSubtitle("${data.floors.size} Floors • ${data.slots.size} Bays")
            .setDescription(data.slots.joinToString(", "))
            .build()

        items.add(MediaBrowser.MediaItem(description, MediaBrowser.MediaItem.FLAG_PLAYABLE))
        result.sendResult(items)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
