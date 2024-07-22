package com.amazon.ivs.broadcast.common.broadcast

import android.content.Context
import android.content.res.Configuration
import android.net.TrafficStats
import android.os.Handler
import android.os.Looper
import android.view.TextureView
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.ViewModel
import com.amazon.ivs.broadcast.CLog
import com.amazon.ivs.broadcast.R
import com.amazon.ivs.broadcast.SharePref
import com.amazon.ivs.broadcast.common.ConsumableSharedFlow
import com.amazon.ivs.broadcast.common.LiveConfig.BYTES_TO_MEGABYTES_FACTOR
import com.amazon.ivs.broadcast.common.LiveConfig.SLOT_DEFAULT
import com.amazon.ivs.broadcast.common.asString
import com.amazon.ivs.broadcast.common.emitNew
import com.amazon.ivs.broadcast.common.getSessionUsedBytes
import com.amazon.ivs.broadcast.common.launchMain
import com.amazon.ivs.broadcast.models.ui.DeviceItem
import com.amazon.ivs.broadcast.models.ui.StreamTopBarModel
import com.amazonaws.ivs.broadcast.BroadcastConfiguration
import com.amazonaws.ivs.broadcast.BroadcastException
import com.amazonaws.ivs.broadcast.BroadcastSession
import com.amazonaws.ivs.broadcast.Device
import com.amazonaws.ivs.broadcast.ErrorType
import com.amazonaws.ivs.broadcast.ImageDevice
import com.amazonaws.ivs.broadcast.SurfaceSource
import kotlinx.coroutines.flow.asSharedFlow

const val SHARE_IVS_DEFAULT_CAMERA = "SHARE_IVS_DEFAULT_CAMERA"
const val SHARE_IVS_IS_BACK_CAMERA = "SHARE_IVS_IS_BACK_CAMERA"
const val DEFAULT_VIDEO_WIDTH = 720f
const val DEFAULT_VIDEO_HEIGHT = 1280f
class BroadcastManager : ViewModel() {
    private var isBackCamera = true
    private val cameraDirection get() = if (isBackCamera) Device.Descriptor.Position.BACK else Device.Descriptor.Position.FRONT

    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable = object : Runnable {
        override fun run() {
            try {
                _onStreamDataChanged.tryEmit(StreamTopBarModel())
            } finally {
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    private var startBytes = 0f
    private var currentState = BroadcastSession.State.DISCONNECTED
    private var _onError = ConsumableSharedFlow<ErrorType>()
    private var _onBroadcastState = ConsumableSharedFlow<BroadcastSession.State>()
    private var _onStreamDataChanged = ConsumableSharedFlow<StreamTopBarModel>()
    private var _onPreviewUpdated = ConsumableSharedFlow<TextureView?>()
    private var _onAudioMuted = ConsumableSharedFlow<Boolean>(canReplay = true)
    private var _onVideoMuted = ConsumableSharedFlow<Boolean>(canReplay = true)
    private var _onDevicesListed = ConsumableSharedFlow<List<DeviceItem>>(canReplay = true)

    private var session: BroadcastSession? = null
    private var cameraDevice: Device? = null
    private var microphoneDevice: Device.Descriptor? = null
    private var cameraOffDevice: SurfaceSource? = null
    private var screenDevices = mutableListOf<Device>()

    private var broadcastListener = object: BroadcastSession.Listener() {
        override fun onStateChanged(state: BroadcastSession.State) {
            CLog.d("Broadcast state changed: $state")
            when (state) {
                BroadcastSession.State.INVALID,
                BroadcastSession.State.DISCONNECTED,
                BroadcastSession.State.ERROR -> {
                    currentState = state
                    _onBroadcastState.emitNew(currentState)
                    resetTimer()
                }
                BroadcastSession.State.CONNECTING -> {
                    currentState = state
                    _onBroadcastState.emitNew(currentState)
                }
                BroadcastSession.State.CONNECTED -> {
                    currentState = state
                    _onBroadcastState.emitNew(currentState)
                    timerRunnable.run()
                }
            }
        }

        override fun onDeviceRemoved(descriptor: Device.Descriptor) {
            super.onDeviceRemoved(descriptor)
            if (descriptor.deviceId == microphoneDevice?.deviceId && descriptor.isExternal() && descriptor.isValid) {
                CLog.d("Microphone removed: ${descriptor.deviceId}, ${descriptor.position}")
                microphoneDevice = null
                session?.detachDevice(descriptor)
            }
            if (descriptor.deviceId == cameraDevice?.descriptor?.deviceId && descriptor.isExternal() && descriptor.isValid) {
                CLog.d("Camera removed: ${descriptor.deviceId}, ${descriptor.position}")
                cameraDevice = null
                session?.detachDevice(descriptor)
            }
        }

        override fun onDeviceAdded(descriptor: Device.Descriptor) {
            super.onDeviceAdded(descriptor)
            if (descriptor.isExternal() && descriptor.type == Device.Descriptor.DeviceType.MICROPHONE) {
                CLog.d("Microphone added: ${descriptor.deviceId}, ${descriptor.position}, ${descriptor.type}")
                microphoneDevice = descriptor
            }
        }

        override fun onError(error: BroadcastException) {
            CLog.d("Broadcast error: $error")
            if (error.error == ErrorType.ERROR_DEVICE_DISCONNECTED && error.source == microphoneDevice?.urn) {
                microphoneDevice?.let {
                    try {
                        session?.exchangeDevices(it, it) { microphone ->
                            CLog.d("Device with id ${microphoneDevice?.deviceId} reattached")
                            microphoneDevice = microphone.descriptor
                        }
                    } catch (e: BroadcastException) {
                        CLog.e(e)
                        _onError.tryEmit(ErrorType.ERROR_DEVICE_DISCONNECTED)
                    }
                }
            } else if (error.error == ErrorType.ERROR_DEVICE_DISCONNECTED && microphoneDevice == null) {
                _onError.tryEmit(ErrorType.ERROR_DEVICE_DISCONNECTED)
            } else if (error.isFatal) {
                error.printStackTrace()
                _onError.tryEmit(error.error)
            }
        }
    }

    var isVideoMuted = false
        private set
    var isAudioMuted = false
        private set
    val isStreamOnline get() = currentState == BroadcastSession.State.CONNECTED

    val onError = _onError.asSharedFlow()
    val onBroadcastState = _onBroadcastState.asSharedFlow()
    val onPreviewUpdated = _onPreviewUpdated.asSharedFlow()
    val onAudioMuted = _onAudioMuted.asSharedFlow()
    val onVideoMuted = _onVideoMuted.asSharedFlow()
    val onStreamDataChanged = _onStreamDataChanged.asSharedFlow()

    fun createSession(context:Context, orientation:Int) {
        startBytes = (TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()).toFloat()
        currentConfiguration = createConfiguration(orientation)
        CLog.d("Creating session with configuration: ${currentConfiguration.asString()}")
        session = BroadcastSession(context, broadcastListener, currentConfiguration, null)
        attachInitialDevices(context)
        CLog.d("Session created")
    }
    lateinit var currentConfiguration: BroadcastConfiguration
        private set
    private fun createConfiguration(orientation:Int) : BroadcastConfiguration {
        return BroadcastConfiguration().apply {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                video.size = BroadcastConfiguration.Vec2(720f, 1280f)
            } else {
                video.size = BroadcastConfiguration.Vec2(1280f, 720f)
            }
            video.initialBitrate = 1500000
            video.maxBitrate = 1500000
            video.minBitrate = 1500000
            video.size = BroadcastConfiguration.Vec2(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT)
            video.targetFramerate = 30
            video.isUseAutoBitrate = true
            audio.channels = 1
            mixer.slots = arrayOf(BroadcastConfiguration.Mixer.Slot.with { slot ->
                slot.name = SLOT_DEFAULT
                slot.aspect = BroadcastConfiguration.AspectMode.FILL
                slot
            })
        }
    }

    fun resetSession() {
        var releasingSession = false
        session?.run {
            releasingSession = true
            CLog.d("Releasing session")
            cameraDevice?.run { detachDevice(this) }
            microphoneDevice?.run { detachDevice(this) }
            cameraOffDevice?.run { detachDevice(this) }
            screenDevices.forEach { device ->
                detachDevice(device)
            }
            stopSystemCapture()
            stop()
            release()
        }
        _onBroadcastState.tryEmit(BroadcastSession.State.DISCONNECTED)
        _onPreviewUpdated.tryEmit(null)
        cameraDevice = null
        microphoneDevice = null
        cameraOffDevice = null
        session = null
        screenDevices.clear()
        if (releasingSession) {
            CLog.d("Session released")
        }
    }
    val ingest_endpoint = "rtmps://bbe4f98c2140.global-contribute.live-video.net"
    val stream_key = "sk_ap-northeast-2_uBLqdlROPzjA_jKMvCxW0nhp3GF5TWTyv6Hm0aQv9x1"
    fun startStream() {
        CLog.d("Starting stream: $ingest_endpoint, $stream_key")
        session?.start(ingest_endpoint, stream_key)
    }

    private fun setCameraBack(camera: Device) {
        CLog.e("KDS3393_TEST_LIVE setCameraBack camera[${camera.descriptor.deviceId},${camera.descriptor.friendlyName}]")
        isBackCamera = camera.descriptor.position != Device.Descriptor.Position.FRONT
        cameraDevice = camera
        SharePref.put(SHARE_IVS_IS_BACK_CAMERA,isBackCamera)
        SharePref.put(SHARE_IVS_DEFAULT_CAMERA,camera.descriptor.deviceId)
    }

    fun flipCameraDirection(context:Context) {
        val newDirection = if (isBackCamera) Device.Descriptor.Position.FRONT else Device.Descriptor.Position.BACK
        val newCamera = context.getCamera(newDirection)
        val canFlip = !isVideoMuted && cameraDevice?.descriptor?.isValid == true
        CLog.d("Switching camera direction: $canFlip")
        if (!canFlip) return
        CLog.d("Switching camera direction from: $cameraDirection to: $newDirection")
        if (cameraDevice != null && newCamera != null) {
            _onPreviewUpdated.tryEmit(null)
            session?.exchangeDevices(cameraDevice!!, newCamera) { device ->
                CLog.d("Cameras exchanged from: ${cameraDevice?.descriptor?.friendlyName} to: ${device.descriptor.friendlyName}")
                setCameraBack(device)
                displayCameraOutput()
            }
        }
    }

    fun toggleAudio() {
        CLog.d("Toggling audio state")
        isAudioMuted = !isAudioMuted
        if (isAudioMuted) {
            microphoneDevice?.let { device ->
                session?.detachDevice(device)
            }
        } else {
            microphoneDevice?.let { microphone ->
                attachDevice(microphone) { device ->
                    microphoneDevice = device.descriptor
                }
            }
        }
        _onAudioMuted.tryEmit(isAudioMuted)
    }

    fun toggleVideo() {
        isVideoMuted = !isVideoMuted
        drawCameraOff(isVideoMuted)
        _onVideoMuted.tryEmit(isVideoMuted)
        CLog.d("Toggled video state: $isVideoMuted")
    }

    fun reloadDevices() {
        CLog.d("Reloading devices")
        session?.listAttachedDevices()?.forEach { device ->
            CLog.d("Detaching device: ${device.descriptor.deviceId}, ${device.descriptor.friendlyName}, ${device.descriptor.type}")
            session?.detachDevice(device)
        }
        session?.awaitDeviceChanges {
            CLog.d("Devices detached")
            cameraDevice?.descriptor?.let { camera ->
                attachDevice(camera) { device ->
                    setCameraBack(device)
                    displayCameraOutput()
                    CLog.d("Camera re-attached")
                }
            }
            microphoneDevice?.let { microphone ->
                attachDevice(microphone) { device ->
                    microphoneDevice = device.descriptor
                    CLog.d("Microphone re-attached")
                }
            }
            session?.listAttachedDevices()?.forEach {
                CLog.d("Attached device: ${it.descriptor.deviceId}, ${it.descriptor.friendlyName}, ${it.descriptor.type}")
            }
        }
    }

    fun displayCameraOutput() {
        (cameraDevice as? ImageDevice)?.getPreviewView(BroadcastConfiguration.AspectMode.FILL)?.run {
            launchMain {
                CLog.d("Camera output ready")
                layoutParams = ViewGroup.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                _onPreviewUpdated.tryEmit(this@run)
            }
        }
    }

    private fun attachInitialDevices(context:Context) {
        CLog.d("Attaching devices")
        var cameraFound = false
        var microphoneFound = false
        val availableCameras = mutableListOf<Device.Descriptor>()
        cameraOffDevice = session?.createImageInputSource()
        BroadcastSession.listAvailableDevices(context).forEach { descriptor ->
            if (descriptor.type == Device.Descriptor.DeviceType.CAMERA) {
                val isAcceptableDevice = SharePref[SHARE_IVS_DEFAULT_CAMERA,"0"] == descriptor.deviceId
                availableCameras.add(descriptor)
                if (!cameraFound && isAcceptableDevice) {
                    cameraFound = true
                    CLog.e("Attaching camera: ${descriptor.friendlyName}")
                    attachDevice(descriptor) { device ->
                        setCameraBack(device)
                        displayCameraOutput()
                    }
                }
            }
            if (descriptor.type == Device.Descriptor.DeviceType.MICROPHONE && !microphoneFound) {
                microphoneFound = true
                CLog.e("Attaching mic: ${descriptor.friendlyName}")
                attachDevice(descriptor) { device ->
                    microphoneDevice = device.descriptor
                }
            }
        }
        _onDevicesListed.tryEmit(availableCameras.map { DeviceItem(it.type.name, it.deviceId, it.position.name) })
        CLog.e("Initial devices attached: ${availableCameras.map { it.friendlyName }}")
    }

    private fun attachDevice(descriptor: Device.Descriptor, onAttached: (device: Device) -> Unit) {
        session?.attachDevice(descriptor) { device: Device ->
            session?.mixer?.bind(device, SLOT_DEFAULT)
            CLog.d("Device attached: ${device.descriptor.friendlyName}")
            onAttached(device)
        }
    }

    private fun drawCameraOff(isVideoMuted: Boolean) = launchMain {
        if (isVideoMuted) {
            CLog.d("Binding OFF device")
            session?.mixer?.unbind(cameraDevice)
            session?.mixer?.bind(cameraOffDevice, SLOT_DEFAULT)
        } else {
            CLog.d("Binding Camera device")
            session?.mixer?.unbind(cameraOffDevice)
            session?.mixer?.bind(cameraDevice, SLOT_DEFAULT)
        }
        displayCameraOutput()
    }

    private fun resetTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }
}
