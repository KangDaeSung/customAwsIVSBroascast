package com.amazon.ivs.broadcast.ui.fragments

import androidx.lifecycle.ViewModel
import com.amazon.ivs.broadcast.CLog
import com.amazon.ivs.broadcast.cache.PreferenceProvider
import com.amazon.ivs.broadcast.common.SLOT_DEFAULT
import com.amazon.ivs.broadcast.common.broadcast.BroadcastManager
import com.amazon.ivs.broadcast.common.launch
import com.amazon.ivs.broadcast.models.Orientation
import com.amazon.ivs.broadcast.models.ResolutionModel
import com.amazon.ivs.broadcast.models.ui.DeviceItem
import com.amazonaws.ivs.broadcast.BroadcastConfiguration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
class ConfigurationViewModel @Inject constructor(
    private val broadcastManager: BroadcastManager,
    private val preferences: PreferenceProvider,
) : ViewModel() {

    var isConfigurationChanged = true
    var developerMode = false
    var camerasList: List<DeviceItem>? = null
    var orientationId by Delegates.observable(preferences.orientation) { _, oldValue, newValue ->
        preferences.orientation = newValue
        isConfigurationChanged = oldValue != newValue
    }
    var isLandscape by Delegates.observable(false) { _, _, newValue ->
        resolution.isLandscape = newValue
    }
    var resolution by Delegates.observable(
        ResolutionModel(
            preferences.width,
            preferences.height,
            orientation = preferences.orientation
        )
    ) { _, oldValue, newValue ->
        preferences.width = newValue.width
        preferences.height = newValue.height
        orientationId = when {
            newValue.initialWidth > newValue.initialHeight -> Orientation.LANDSCAPE.id
            newValue.initialWidth < newValue.initialHeight -> Orientation.PORTRAIT.id
            else -> Orientation.SQUARE.id
        }
        newValue.orientation = orientationId
        isConfigurationChanged = oldValue != newValue
    }
    var defaultCameraId by Delegates.observable(preferences.defaultCameraId) { _, _, newValue ->
        preferences.defaultCameraId = newValue
        camerasList?.onEach {
            it.isSelected = false
        }?.firstOrNull {
            it.deviceId == newValue
        }?.apply { isSelected = true }
        camerasList?.forEach {
            CLog.d("${it.isSelected}")
        }
    }
    var defaultCameraPosition by Delegates.observable(preferences.defaultCameraPosition) { _, _, newValue ->
        preferences.defaultCameraPosition = newValue
    }

    val defaultDeviceItem get() = camerasList?.firstOrNull { it.isSelected }
    val newestConfiguration get() = BroadcastConfiguration().apply {
        video.initialBitrate = 1500000
        video.maxBitrate = 1500000
        video.minBitrate = 1500000
        video.size = BroadcastConfiguration.Vec2(720f, 1280f)
        video.targetFramerate = 30
        video.isUseAutoBitrate = true
        audio.channels = 1
        mixer.slots = arrayOf(defaultSlot)
    }

    val defaultSlot: BroadcastConfiguration.Mixer.Slot get() = BroadcastConfiguration.Mixer.Slot.with { slot ->
        slot.name = SLOT_DEFAULT
        if (orientationId != Orientation.AUTO.id) {
            slot.aspect = BroadcastConfiguration.AspectMode.FILL
        }
        slot
    }

    init {
        broadcastManager.init(this)
        launch {
            broadcastManager.onDevicesListed.collect { devices ->
                camerasList = devices
            }
        }
    }

    fun resetDefaultCamera() {
        defaultCameraId = preferences.defaultCameraId
    }

    fun onConfigurationChanged(isLandscapeOrientation: Boolean) {
        CLog.d("Configuration changed: $isLandscapeOrientation")
        isLandscape = isLandscapeOrientation
        broadcastManager.reloadDevices()
    }
}
