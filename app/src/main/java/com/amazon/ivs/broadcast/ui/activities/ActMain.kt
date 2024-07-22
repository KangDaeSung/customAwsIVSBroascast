package com.amazon.ivs.broadcast.ui.activities

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import android.view.TextureView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import com.amazon.ivs.broadcast.CLog
import com.amazon.ivs.broadcast.R
import com.amazon.ivs.broadcast.common.broadcast.BroadcastManager
import com.amazon.ivs.broadcast.common.broadcast.DEFAULT_VIDEO_HEIGHT
import com.amazon.ivs.broadcast.common.broadcast.DEFAULT_VIDEO_WIDTH
import com.amazon.ivs.broadcast.common.collectUI
import com.amazon.ivs.broadcast.common.disableAndEnable
import com.amazon.ivs.broadcast.common.isViewLandscape
import com.amazon.ivs.broadcast.common.lazyViewModel
import com.amazon.ivs.broadcast.common.onDrawn
import com.amazon.ivs.broadcast.common.setCollapsed
import com.amazon.ivs.broadcast.common.setVisible
import com.amazon.ivs.broadcast.databinding.FragmentMainBinding
import com.amazon.ivs.broadcast.models.Orientation
import com.amazon.ivs.broadcast.models.ResolutionModel
import com.amazon.ivs.broadcast.models.ui.StreamTopBarModel
import com.amazonaws.ivs.broadcast.BroadcastSession
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates


@AndroidEntryPoint
class ActMain : AppCompatActivity() {
    private val binding by lazy(LazyThreadSafetyMode.NONE) { FragmentMainBinding.inflate(layoutInflater) }
    private val broadcastManager: BroadcastManager by lazyViewModel({ this }, { BroadcastManager() })

    private var orientationId by Delegates.observable(Orientation.PORTRAIT.id) { _, oldValue, newValue ->
    }
    private var isLandscape by Delegates.observable(false) { _, _, newValue ->
        resolution.isLandscape = newValue
    }
    private var resolution by Delegates.observable(ResolutionModel(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT, orientation = Orientation.PORTRAIT.id)) { _, oldValue, newValue ->
        orientationId = when {
            newValue.initialWidth > newValue.initialHeight -> Orientation.LANDSCAPE.id
            newValue.initialWidth < newValue.initialHeight -> Orientation.PORTRAIT.id
            else -> Orientation.SQUARE.id
        }
        newValue.orientation = orientationId
    }

    private val bottomSheet: BottomSheetBehavior<View> by lazy {
        BottomSheetBehavior.from(binding.broadcastBottomSheet.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CLog.d("onViewCreated")
        setContentView(binding.root)
        resolution.orientation = orientationId
        updateControlPanelVisibility(this.isViewLandscape(), true)
        if (!broadcastManager.isStreamOnline) {
            isLandscape = this.isViewLandscape()
            broadcastManager.resetSession()
            broadcastManager.createSession(this, resources.configuration.orientation)
            binding.videoConfiguration = broadcastManager.currentConfiguration.video
            binding.topBarUpdate = StreamTopBarModel(streamStatus = BroadcastSession.State.DISCONNECTED)
        } else {
            binding.videoConfiguration = broadcastManager.currentConfiguration.video
            broadcastManager.reloadDevices()
        }

        binding.isStreamMuted = broadcastManager.isAudioMuted
        binding.isCameraOff = broadcastManager.isVideoMuted
        binding.isScreenCaptureOn = false
        binding.broadcastBottomSheet.showDebugInfo.setVisible(true)

        bottomSheet.peekHeight = resources.getDimension(R.dimen.bottom_sheet_developer_peek_height).toInt()

        binding.streamFramerate.text = getString(R.string.fps_template, 30)
        binding.streamQuality.text = getString(
            R.string.resolution_template,
            resolution.width.toInt(),
            resolution.height.toInt(),
        )

        binding.isViewLandscape = this.isViewLandscape()

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
            broadcastManager.toggleAudio()
        }
        binding.broadcastBottomSheet.broadcastCamera.setOnClickListener {
            binding.broadcastBottomSheet.broadcastCamera.disableAndEnable()
            binding.broadcastSideSheet.broadcastCamera.disableAndEnable()
            binding.cameraOffSlotContainer.doOnLayout {
                broadcastManager.toggleVideo()
            }
        }
        binding.broadcastBottomSheet.broadcastFlip.setOnClickListener {
            binding.broadcastBottomSheet.broadcastFlip.disableAndEnable()
            binding.broadcastSideSheet.broadcastFlip.disableAndEnable()
            broadcastManager.flipCameraDirection(this)
        }
        binding.broadcastBottomSheet.broadcastGoLive.setOnClickListener {
            CLog.d("Will start stream: ${!broadcastManager.isStreamOnline}")
            if (broadcastManager.isStreamOnline) {
                broadcastManager.resetSession()
                broadcastManager.createSession(this, resources.configuration.orientation)
            } else {
                broadcastManager.startStream()
            }
        }

        broadcastManager.onError.collectUI(this) { error ->
            CLog.e("KDS3393_TEST_onError collectUI")
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
                BroadcastSession.State.CONNECTING -> {

                }
                BroadcastSession.State.CONNECTED -> {
                    binding.topBarUpdate = StreamTopBarModel(
                        streamStatus = BroadcastSession.State.CONNECTED,
                        pillBackground = R.drawable.bg_connecting_pill
                    )
                }
                BroadcastSession.State.INVALID,
                BroadcastSession.State.DISCONNECTED,
                BroadcastSession.State.ERROR -> {
                    binding.topBarUpdate = StreamTopBarModel(
                        streamStatus = BroadcastSession.State.DISCONNECTED,
                        pillBackground = R.drawable.bg_offline_pill
                    )
                }
                else -> { /* Ignore */ }
            }
        }

        broadcastManager.onStreamDataChanged.collectUI(this) { topBarModel ->
            binding.topBarUpdate = StreamTopBarModel(
                streamStatus = BroadcastSession.State.CONNECTED,
                pillBackground = R.drawable.bg_online_pill
            )
        }

        broadcastManager.onPreviewUpdated.collectUI(this) { textureView ->
            switchStreamContainer(textureView)
            binding.isCameraOff = broadcastManager.isVideoMuted
        }

        addOnPictureInPictureModeChangedListener { info ->
            CLog.e("KDS3393_TEST_info = isPIP[${info.isInPictureInPictureMode}]")
            broadcastManager.reloadDevices()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        CLog.e("KDS3393_TEST_updateControlPanelVisibility newConfig[$newConfig]")
        updateControlPanelVisibility(isLandscape)
        binding.isViewLandscape = isLandscape
        binding.constraintLayout.onDrawn {
            this.isLandscape = isLandscape
            broadcastManager.reloadDevices()
            orientationId = 1
        }

        if (isLandscape) {
            binding.debugInfo.setVisible(false)
        }
    }

    override fun onResume() {
        super.onResume()
        broadcastManager.displayCameraOutput()
    }

    override fun onDestroy() {
        super.onDestroy()
        broadcastManager.resetSession()
    }

    override fun onBackPressed() {
        if (broadcastManager.isStreamOnline) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(resolution.width.toInt(), resolution.height.toInt())).build())
        } else {
            super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (broadcastManager.isStreamOnline) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(resolution.width.toInt(), resolution.height.toInt())).build())
        }
    }
    private fun updateControlPanelVisibility(isLandscape: Boolean, isInOnCreate: Boolean = false) {
        CLog.e("KDS3393_TEST_updateControlPanelVisibility isLandscape[$isLandscape] isInOnCreate[$isInOnCreate]")
        binding.broadcastBottomSheet.root.setVisible(!isLandscape)
        binding.broadcastSideSheet.motionLayout.setVisible(isLandscape)

        bottomSheet.state = when (binding.broadcastSideSheet.motionLayout.currentState) {
            R.id.menu_full_open -> {
                BottomSheetBehavior.STATE_EXPANDED
            }
            R.id.menu_half_open -> {
                BottomSheetBehavior.STATE_COLLAPSED
            }
            else -> {
                if (isInOnCreate) {
                    BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }
        binding.broadcastSideSheet.motionLayout.setTransition(R.id.transition_half_open_to_close)

        binding.broadcastSideSheet.motionLayout.progress = 1f
        binding.broadcastSideSheet.motionLayout.transitionToEnd()
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
            isInPictureInPictureMode -> binding.pipPreviewContainer.addView(textureView)
            else -> {
                scaleToMatchResolution(textureView)
                if (this.isViewLandscape()) {
                    binding.broadcastSideSheet.defaultSlotContainerLandscape.addView(textureView)
                } else {
                    CLog.d("Adding preview to default slot container")
                    binding.defaultSlotContainer.addView(textureView)
                }
            }
        }
    }

    private fun scaleToMatchResolution(view: View) {
        val container = if (this.isViewLandscape()) binding.broadcastSideSheet.streamContainerLandscape else
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
