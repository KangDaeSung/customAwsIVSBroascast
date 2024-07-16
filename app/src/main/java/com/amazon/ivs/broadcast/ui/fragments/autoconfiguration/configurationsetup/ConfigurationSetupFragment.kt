package com.amazon.ivs.broadcast.ui.fragments.autoconfiguration.configurationsetup

import android.os.Bundle
import android.view.View
import com.amazon.ivs.broadcast.R
import com.amazon.ivs.broadcast.common.openFragment
import com.amazon.ivs.broadcast.common.setVisible
import com.amazon.ivs.broadcast.common.viewBinding
import com.amazon.ivs.broadcast.databinding.FragmentConfigurationSetupBinding
import com.amazon.ivs.broadcast.ui.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfigurationSetupFragment : BaseFragment(R.layout.fragment_configuration_setup) {
    private val binding by viewBinding(FragmentConfigurationSetupBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clearPopUp()

        binding.skipConfiguration.setOnClickListener {
            openFragment(R.id.navigation_main)
        }


        binding.popupContainer.setOnClickListener {
            clearPopUp()
        }

    }

    private fun clearPopUp() {
        binding.popupContainer.setVisible(false)
    }
}
