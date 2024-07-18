package com.amazon.ivs.broadcast.ui.activities

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.amazon.ivs.broadcast.R
import com.amazon.ivs.broadcast.common.getCurrentFragment
import com.amazon.ivs.broadcast.common.openFragment
import com.amazon.ivs.broadcast.databinding.ActivityMainBinding
import com.amazon.ivs.broadcast.ui.fragments.main.MainFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        openFragment(R.id.navigation_main)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                getCurrentFragment()?.let { currentFragment ->
                    when (currentFragment) {
                        is MainFragment -> if (currentFragment.onBackPressed()) finish() else Unit
                        else -> findNavController(R.id.nav_host_fragment).navigateUp()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        getCurrentFragment()?.let { currentFragment ->
            if (currentFragment is MainFragment) {
                currentFragment.resetSession()
            }
        }

    }
}
