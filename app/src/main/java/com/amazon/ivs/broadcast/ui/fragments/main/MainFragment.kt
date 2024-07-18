package com.amazon.ivs.broadcast.ui.fragments.main

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.core.view.doOnLayout
import com.amazon.ivs.broadcast.CLog
import com.amazon.ivs.broadcast.R
import com.amazon.ivs.broadcast.common.broadcast.BroadcastManager
import com.amazon.ivs.broadcast.common.broadcast.BroadcastState
import com.amazon.ivs.broadcast.common.broadcast.DEFAULT_VIDEO_HEIGHT
import com.amazon.ivs.broadcast.common.broadcast.DEFAULT_VIDEO_WIDTH
import com.amazon.ivs.broadcast.common.collectUI
import com.amazon.ivs.broadcast.common.disableAndEnable
import com.amazon.ivs.broadcast.common.formatTime
import com.amazon.ivs.broadcast.common.formatTopBarNetwork
import com.amazon.ivs.broadcast.common.getCpuTemperature
import com.amazon.ivs.broadcast.common.getUsedMemory
import com.amazon.ivs.broadcast.common.isViewLandscape
import com.amazon.ivs.broadcast.common.lazyFragmentViewModel
import com.amazon.ivs.broadcast.common.onDrawn
import com.amazon.ivs.broadcast.common.setCollapsed
import com.amazon.ivs.broadcast.common.setVisible
import com.amazon.ivs.broadcast.common.viewBinding
import com.amazon.ivs.broadcast.databinding.FragmentMainBinding
import com.amazon.ivs.broadcast.models.Orientation
import com.amazon.ivs.broadcast.models.ResolutionModel
import com.amazon.ivs.broadcast.models.ui.DeviceHealth
import com.amazon.ivs.broadcast.models.ui.StreamTopBarModel
import com.amazon.ivs.broadcast.ui.fragments.BaseFragment
import com.amazonaws.ivs.broadcast.BroadcastSession.State.CONNECTED
import com.amazonaws.ivs.broadcast.BroadcastSession.State.CONNECTING
import com.amazonaws.ivs.broadcast.BroadcastSession.State.DISCONNECTED
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class MainFragment : BaseFragment(R.layout.fragment_main) {
    private val binding by viewBinding(FragmentMainBinding::bind)
    private val broadcastManager: BroadcastManager by lazyFragmentViewModel({ this }, { BroadcastManager() })
    var orientationId by Delegates.observable(Orientation.PORTRAIT.id) { _, oldValue, newValue ->
    }
    var isLandscape by Delegates.observable(false) { _, _, newValue ->
        resolution.isLandscape = newValue
    }
    var resolution by Delegates.observable(ResolutionModel(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT, orientation = Orientation.PORTRAIT.id)) { _, oldValue, newValue ->
        orientationId = when {
            newValue.initialWidth > newValue.initialHeight -> Orientation.LANDSCAPE.id
            newValue.initialWidth < newValue.initialHeight -> Orientation.PORTRAIT.id
            else -> Orientation.SQUARE.id
        }
        newValue.orientation = orientationId
    }

    private var isInPipMode = false

    private val bottomSheet: BottomSheetBehavior<View> by lazy {
        BottomSheetBehavior.from(binding.broadcastBottomSheet.root)
    }

    private var deviceHealth = DeviceHealth()
    private val deviceHealthHandler = Handler(Looper.getMainLooper())
    private var deviceHealthRunnable = object : Runnable {
        override fun run() {
            try {
                if (!isAdded) return
                deviceHealth = DeviceHealth(
                    requireContext().getUsedMemory(),
                    requireContext().getCpuTemperature()
                )
                binding.deviceHealthUpdate = deviceHealth
            } finally {
                deviceHealthHandler.postDelayed(this, 1000)
            }
        }
    }

    fun resetSession() {
        broadcastManager.resetSession()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CLog.d("onViewCreated")
        resolution.orientation = orientationId
        updateControlPanelVisibility(requireContext().isViewLandscape(), true)
        if (!broadcastManager.isStreamOnline) {
            isLandscape = requireContext().isViewLandscape()
            broadcastManager.resetSession()
            broadcastManager.createSession(requireContext(), resources.configuration.orientation)
            binding.videoConfiguration = broadcastManager.currentConfiguration.video
            binding.topBarUpdate = StreamTopBarModel(streamStatus = DISCONNECTED)
        } else {
            binding.videoConfiguration = broadcastManager.currentConfiguration.video
            broadcastManager.reloadDevices()
        }

        binding.isStreamMuted = broadcastManager.isAudioMuted
        binding.isCameraOff = broadcastManager.isVideoMuted
        binding.isScreenCaptureOn = broadcastManager.isScreenShareEnabled
        binding.broadcastBottomSheet.showDebugInfo.setVisible(true)

        bottomSheet.peekHeight = resources.getDimension(R.dimen.bottom_sheet_developer_peek_height).toInt()

        binding.streamFramerate.text = getString(R.string.fps_template, 30)
        binding.streamQuality.text = getString(
            R.string.resolution_template,
            resolution.width.toInt(),
            resolution.height.toInt(),
        )

        binding.isViewLandscape = requireContext().isViewLandscape()

        binding.broadcastBottomSheet.showDebugInfo.setOnClickListener {
            bottomSheet.setCollapsed()
            binding.debugInfo.setVisible()
            binding.broadcastBottomSheet.showDebugInfo.setVisible(false, View.INVISIBLE)
        }

        binding.hideDebugInfo.setOnClickListener {
            binding.debugInfo.setVisible(false)
            binding.broadcastBottomSheet.showDebugInfo.setVisible()
        }

        binding.streamContainer.setOnClickListener {
            bottomSheet.state = when (bottomSheet.state) {
                BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_HIDDEN
                BottomSheetBehavior.STATE_HIDDEN -> BottomSheetBehavior.STATE_COLLAPSED
                else -> BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.broadcastBottomSheet.broadcastMute.setOnClickListener {
            onMuteButtonClick()
        }
        binding.broadcastBottomSheet.broadcastCamera.setOnClickListener {
            onCameraButtonClick()
        }
        binding.broadcastBottomSheet.broadcastFlip.setOnClickListener {
            onFlipCameraButtonClick()
        }
        binding.broadcastBottomSheet.broadcastGoLive.setOnClickListener {
            onGoLiveButtonClick()
        }

        broadcastManager.onError.collectUI(this) { error ->
            showPopup(getString(R.string.error), getString(error.error), "ERROR")
        }

        broadcastManager.onAudioMuted.collectUI(this) { muted ->
            binding.isStreamMuted = muted
        }

        broadcastManager.onVideoMuted.collectUI(this) { muted ->
            CLog.d("Video muted: $muted")
            if (muted) {
                binding.isCameraOff = true
            }
        }

        broadcastManager.onBroadcastState.collectUI(this) { state ->
            when (state) {
                BroadcastState.BROADCAST_STARTED -> {
                    binding.topBarUpdate = StreamTopBarModel(
                        streamStatus = CONNECTING,
                        pillBackground = R.drawable.bg_connecting_pill
                    )
                }
                BroadcastState.BROADCAST_ENDED -> {
                    binding.topBarUpdate = StreamTopBarModel(
                        streamStatus = DISCONNECTED,
                        pillBackground = R.drawable.bg_offline_pill
                    )
                    if (broadcastManager.isScreenShareEnabled) {
                        showOfflineScreenShareAlert()
                    }
                }
                else -> { /* Ignore */ }
            }
        }

        broadcastManager.onStreamDataChanged.collectUI(this) { topBarModel ->
            binding.topBarUpdate = StreamTopBarModel(
                formattedTime = formatTime(topBarModel.seconds),
                formattedNetwork = formatTopBarNetwork(topBarModel.usedMegaBytes),
                streamStatus = CONNECTED,
                pillBackground = R.drawable.bg_online_pill
            )
        }

        broadcastManager.onPreviewUpdated.collectUI(this) { textureView ->
            switchStreamContainer(textureView)
            binding.isCameraOff = broadcastManager.isVideoMuted
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        CLog.e("KDS3393_TEST_updateControlPanelVisibility newConfig[$newConfig]")
        updateControlPanelVisibility(isLandscape)
        binding.isViewLandscape = isLandscape
        binding.constraintLayout.onDrawn {
            this@MainFragment.isLandscape = isLandscape
            broadcastManager.reloadDevices()

            orientationId = 1
        }

        if (isLandscape) {
            binding.debugInfo.setVisible(false)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        isInPipMode = isInPictureInPictureMode
        broadcastManager.reloadDevices()
    }

    override fun onResume() {
        super.onResume()
        deviceHealthHandler.post(deviceHealthRunnable)
        broadcastManager.displayCameraOutput()
    }

    override fun onPause() {
        super.onPause()
        deviceHealthHandler.removeCallbacks(deviceHealthRunnable)
    }

    fun onBackPressed(): Boolean {
        return if (broadcastManager.isStreamOnline) {
            val params = PictureInPictureParams.Builder()
            params.setAspectRatio(
                Rational(
                    resolution.width.toInt(),
                    resolution.height.toInt()
                )
            )
            activity?.enterPictureInPictureMode(params.build())
            false
        } else {
            true
        }
    }

    private fun updateControlPanelVisibility(isLandscape: Boolean, isInOnCreate: Boolean = false) {
        CLog.e("KDS3393_TEST_updateControlPanelVisibility isLandscape[$isLandscape] isInOnCreate[$isInOnCreate]")
        binding.broadcastBottomSheet.root.setVisible(!isLandscape)
        binding.broadcastSideSheet.motionLayout.setVisible(isLandscape)

        bottomSheet.state = when (binding.broadcastSideSheet.motionLayout.currentState) {
            R.id.menu_full_open -> {
                CLog.e("KDS3393_TEST_updateControlPanelVisibility menu_full_open")
                BottomSheetBehavior.STATE_EXPANDED
            }
            R.id.menu_half_open -> {
                CLog.e("KDS3393_TEST_updateControlPanelVisibility menu_half_open")
                BottomSheetBehavior.STATE_COLLAPSED
            }
            else -> {
                if (isInOnCreate) {
                    CLog.e("KDS3393_TEST_updateControlPanelVisibility STATE_COLLAPSED ${binding.broadcastSideSheet.motionLayout.currentState}")
                    BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    CLog.e("KDS3393_TEST_updateControlPanelVisibility STATE_HIDDEN ${binding.broadcastSideSheet.motionLayout.currentState}")
                    BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }
        binding.broadcastSideSheet.motionLayout.setTransition(R.id.transition_half_open_to_close)

        binding.broadcastSideSheet.motionLayout.progress = 1f
        binding.broadcastSideSheet.motionLayout.transitionToEnd()
    }

    private fun onGoLiveButtonClick() {
        CLog.d("Will start stream: ${!broadcastManager.isStreamOnline}")
        if (broadcastManager.isStreamOnline) {
            broadcastManager.resetSession()
            broadcastManager.createSession(requireContext(), resources.configuration.orientation)
        } else {
            broadcastManager.startStream()
        }
    }

    private fun onMuteButtonClick() {
        broadcastManager.toggleAudio()
    }

    private fun onFlipCameraButtonClick() {
        binding.broadcastBottomSheet.broadcastFlip.disableAndEnable()
        binding.broadcastSideSheet.broadcastFlip.disableAndEnable()
        broadcastManager.flipCameraDirection(requireContext())
    }

    private fun onCameraButtonClick() {
        binding.broadcastBottomSheet.broadcastCamera.disableAndEnable()
        binding.broadcastSideSheet.broadcastCamera.disableAndEnable()
        if (broadcastManager.isScreenShareEnabled) {
            binding.miniCameraOffSlotContainer.doOnLayout {
                broadcastManager.toggleVideo()
            }
        } else {
            binding.cameraOffSlotContainer.doOnLayout {
                broadcastManager.toggleVideo()
            }
        }
    }

    private fun switchStreamContainer(textureView: TextureView?) {
        binding.broadcastSideSheet.defaultSlotContainerLandscape.removeAllViews()
        binding.defaultSlotContainer.removeAllViews()
        binding.broadcastSideSheet.miniPreview.removeAllViews()
        binding.miniPreview.removeAllViews()
        binding.pipPreviewContainer.removeAllViews()
        if (textureView == null) return
        CLog.d("Add preview to container")
        when {
            isInPipMode -> binding.pipPreviewContainer.addView(textureView)
            !isInPipMode && broadcastManager.isScreenShareEnabled -> {
                if (requireContext().isViewLandscape()) {
                    binding.broadcastSideSheet.miniPreview.addView(textureView)
                } else {
                    binding.miniPreview.addView(textureView)
                }
            }
            else -> {
                scaleToMatchResolution(textureView)
                if (requireContext().isViewLandscape()) {
                    binding.broadcastSideSheet.defaultSlotContainerLandscape.addView(textureView)
                } else {
                    CLog.d("Adding preview to default slot container")
                    binding.defaultSlotContainer.addView(textureView)
                }
            }
        }
    }

    private fun showOfflineScreenShareAlert() {
        showPopup(getString(R.string.alert), getString(R.string.offline_screen_sharing_alert), "WARNING")
    }

    private fun showPopup(title:String,text:String,type:String) {
        Toast.makeText(requireContext(),"${title}\n${text}\n${type}",Toast.LENGTH_SHORT).show()
    }

    private fun scaleToMatchResolution(view: View) {
        val container = if (requireContext().isViewLandscape()) binding.broadcastSideSheet.streamContainerLandscape else
            binding.streamContainer
        val screenWidth = container.width
        val screenHeight = container.height
        var width = 1 * resolution.widthAgainstHeightRatio
        var height = 1

        while (width < screenWidth && height < screenHeight) {
            width += 1 * resolution.widthAgainstHeightRatio
            height += 1
        }

        view.layoutParams.width = width.toInt()
        view.layoutParams.height = height
        binding.broadcastSideSheet.streamContainerCardview.layoutParams.width = width.toInt()
        binding.broadcastSideSheet.streamContainerCardview.layoutParams.height = height
        CLog.d("Screen size: $screenWidth, $screenHeight, Video size: $width, $height")
    }
}
