package com.amazon.ivs.broadcast.ui.fragments.settings.networkpropertiesfragment

import android.os.Bundle
import android.view.View
import com.amazon.ivs.broadcast.R
import com.amazon.ivs.broadcast.common.*
import com.amazon.ivs.broadcast.databinding.FragmentNetworkPropertiesBinding
import com.amazon.ivs.broadcast.ui.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NetworkPropertiesFragment : BaseFragment(R.layout.fragment_network_properties) {
    private val binding by viewBinding(FragmentNetworkPropertiesBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.manualBitrateSwitch.isChecked = configurationViewModel.useCustomBitrateLimits
        binding.manualBitrateInputContainer.setVisible(configurationViewModel.useCustomBitrateLimits)

        binding.estimatedDataUseValue.text = toFormattedGbPerHour(30)
        binding.targetBitrateValue.text = toFormattedKbps(30)

        binding.backButton.setOnClickListener {
            openFragment(R.id.navigation_settings)
        }

        binding.manualBitrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.manualBitrateInputContainer.setVisible(isChecked)
            configurationViewModel.useCustomBitrateLimits = isChecked
        }

        binding.minimumBitrate.setOnClickListener {
            binding.root.showInputDialog(
                getString(R.string.minimum_bitrate),
                getString(
                    R.string.minimum_bitrate_description_template,
                    30.toKbps().toString()
                ),
                getString(R.string.bitrate),
                30.toKbps().toString()
            ) { kbps ->
            }
        }

        binding.targetBitrate.setOnClickListener {
            binding.root.showInputDialog(
                getString(R.string.target_bitrate),
                getString(R.string.target_bitrate_description),
                getString(R.string.bitrate),
                30.toKbps().toString()
            ) { kbps ->
            }
        }
    }
}
