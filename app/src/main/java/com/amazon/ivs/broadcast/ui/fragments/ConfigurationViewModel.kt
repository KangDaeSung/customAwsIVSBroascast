package com.amazon.ivs.broadcast.ui.fragments

import androidx.lifecycle.ViewModel
import com.amazon.ivs.broadcast.CLog
import com.amazon.ivs.broadcast.common.broadcast.BroadcastManager
import com.amazon.ivs.broadcast.common.launch
import com.amazon.ivs.broadcast.models.Orientation
import com.amazon.ivs.broadcast.models.ResolutionModel
import com.amazon.ivs.broadcast.models.ui.DeviceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
class ConfigurationViewModel @Inject constructor(private val broadcastManager: BroadcastManager) : ViewModel() {
    var isConfigurationChanged = true
    var camerasList: List<DeviceItem>? = null

    var orientationId by Delegates.observable(Orientation.PORTRAIT.id) { _, oldValue, newValue ->
        isConfigurationChanged = oldValue != newValue
    }
    var isLandscape by Delegates.observable(false) { _, _, newValue ->
        resolution.isLandscape = newValue
    }
    var resolution by Delegates.observable(ResolutionModel(720f, 1280f, orientation = Orientation.PORTRAIT.id)) { _, oldValue, newValue ->
        orientationId = when {
            newValue.initialWidth > newValue.initialHeight -> Orientation.LANDSCAPE.id
            newValue.initialWidth < newValue.initialHeight -> Orientation.PORTRAIT.id
            else -> Orientation.SQUARE.id
        }
        newValue.orientation = orientationId
        isConfigurationChanged = oldValue != newValue
    }

    init {
        broadcastManager.init(this)
        launch {
            broadcastManager.onDevicesListed.collect { devices ->
                camerasList = devices
            }
        }
    }

    fun onConfigurationChanged(isLandscapeOrientation: Boolean) {
        CLog.d("Configuration changed: $isLandscapeOrientation")
        isLandscape = isLandscapeOrientation
        broadcastManager.reloadDevices()

        orientationId = 1
    }
}
