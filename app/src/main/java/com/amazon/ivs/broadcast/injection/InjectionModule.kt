package com.amazon.ivs.broadcast.injection

import com.amazon.ivs.broadcast.common.broadcast.BroadcastManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InjectionModule {
    @Singleton
    @Provides
    fun provideBroadcastManager() =
        BroadcastManager()
}
