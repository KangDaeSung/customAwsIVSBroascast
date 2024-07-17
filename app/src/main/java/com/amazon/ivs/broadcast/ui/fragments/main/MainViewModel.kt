package com.amazon.ivs.broadcast.ui.fragments.main

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.amazon.ivs.broadcast.common.broadcast.BroadcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val broadcastManager: BroadcastManager) : ViewModel() {
    val isCameraOff get() = broadcastManager.isVideoMuted
    val isStreamMuted get() = broadcastManager.isAudioMuted
    val isScreenShareEnabled get() = broadcastManager.isScreenShareEnabled
    val isStreamOnline get() = broadcastManager.isStreamOnline
    val currentConfiguration get() = broadcastManager.currentConfiguration

    val onPreviewUpdated = broadcastManager.onPreviewUpdated
    val onAudioMuted = broadcastManager.onAudioMuted
    val onVideoMuted = broadcastManager.onVideoMuted
    val onError = broadcastManager.onError
    val onBroadcastState = broadcastManager.onBroadcastState
    val onStreamDataChanged = broadcastManager.onStreamDataChanged

    fun switchCameraDirection(context: Context) = broadcastManager.flipCameraDirection(context)

    fun toggleMute() = broadcastManager.toggleAudio()

    fun toggleCamera(bitmap: Bitmap) = broadcastManager.toggleVideo(bitmap)

    fun reloadDevices() = broadcastManager.reloadDevices()

    fun resetSession() = broadcastManager.resetSession()

    fun createSession(context: Context) = broadcastManager.createSession(context)

    fun startStream() = broadcastManager.startStream()

    fun reloadPreview() = broadcastManager.displayCameraOutput()
}
