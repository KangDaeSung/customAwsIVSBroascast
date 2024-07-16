package com.amazon.ivs.broadcast.ui.fragments.autoconfiguration.configurationsummary

import android.os.Bundle
import android.view.View
import com.amazon.ivs.broadcast.R
import com.amazon.ivs.broadcast.common.openFragment
import com.amazon.ivs.broadcast.common.toFormattedGbPerHour
import com.amazon.ivs.broadcast.common.toFormattedKbps
import com.amazon.ivs.broadcast.common.viewBinding
import com.amazon.ivs.broadcast.databinding.FragmentConfigurationSummaryBinding
import com.amazon.ivs.broadcast.ui.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfigurationSummaryFragment : BaseFragment(R.layout.fragment_configuration_summary) {
    private val binding by viewBinding(FragmentConfigurationSummaryBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.summaryBitrateValue.text = toFormattedKbps(1500000)
        binding.summaryDataUsageValue.text = toFormattedGbPerHour(1500000)
        binding.summaryQualityValue.text = getString(
            R.string.quality_template,
            720,
            30
        )
        binding.continueToApp.setOnClickListener {
            openFragment(R.id.navigation_main)
        }

        binding.rerunConfiguration.setOnClickListener {
            openFragment(R.id.navigation_configuration_setup)
        }
    }
}
