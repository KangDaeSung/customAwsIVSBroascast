package com.amazon.ivs.broadcast.ui.fragments

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.transition.Fade
import com.amazon.ivs.broadcast.common.ANIMATION_DURATION

open class BaseFragment(@LayoutRes layout: Int) : Fragment(layout) {
    override fun getEnterTransition() = Fade(Fade.MODE_IN).apply {
        duration = ANIMATION_DURATION
    }

    override fun getReenterTransition() = Fade(Fade.MODE_IN).apply {
        duration = ANIMATION_DURATION
    }

    override fun getExitTransition() = Fade(Fade.MODE_OUT).apply {
        duration = ANIMATION_DURATION
    }
}
