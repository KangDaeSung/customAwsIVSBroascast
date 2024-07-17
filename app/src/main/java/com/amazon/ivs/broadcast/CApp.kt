package com.amazon.ivs.broadcast

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CApp: Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CApp

        fun get(): CApp {
            return instance
        }
    }
}
