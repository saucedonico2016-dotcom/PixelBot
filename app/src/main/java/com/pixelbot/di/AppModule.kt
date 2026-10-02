package com.pixelbot.di

import android.content.Context
import com.pixelbot.service.PixelForegroundService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): Context = context
}