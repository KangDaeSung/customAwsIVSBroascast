package com.amazon.ivs.broadcast.ui.fragments.settings.settingsfragment

import android.os.Bundle
import android.view.View
import com.amazon.ivs.broadcast.CLog
import com.amazon.ivs.broadcast.R
import com.amazon.ivs.broadcast.common.openFragment
import com.amazon.ivs.broadcast.common.setVisible
import com.amazon.ivs.broadcast.common.showCameraDialog
import com.amazon.ivs.broadcast.common.viewBinding
import com.amazon.ivs.broadcast.databinding.FragmentSettingsBinding
import com.amazon.ivs.broadcast.ui.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : BaseFragment(R.layout.fragment_settings) {
    private val binding by viewBinding(FragmentSettingsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.developerModeSwitch.isChecked = configurationViewModel.developerMode

        configurationViewModel.resetDefaultCamera()
        configurationViewModel.defaultDeviceItem?.let { camera ->
            binding.defaultCameraValue.text = getString(
                R.string.camera_option_template,
                camera.type,
                camera.deviceId,
                camera.direction
            )
        }

        binding.backButton.setOnClickListener {
            openFragment(R.id.navigation_main)
        }

        binding.popupContainer.setOnClickListener {
            clearPopUp()
        }

        binding.developerModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            configurationViewModel.developerMode = isChecked
        }

        binding.settingsDeveloperModeContainer.setOnClickListener {
            binding.developerModeSwitch.isChecked = !binding.developerModeSwitch.isChecked
        }

        binding.defaultCameraContainer.setOnClickListener {
            binding.root.showCameraDialog(getString(R.string.orientation), configurationViewModel.camerasList) { option ->
                CLog.d("Default camera selected: $option")
                binding.defaultCameraValue.text = getString(
                    R.string.camera_option_template,
                    option.type,
                    option.deviceId,
                    option.direction
                )
                configurationViewModel.defaultCameraId = option.deviceId
                configurationViewModel.defaultCameraPosition = option.direction
            }
        }
    }

    private fun clearPopUp() {
        binding.popupContainer.setVisible(false)
    }
}
