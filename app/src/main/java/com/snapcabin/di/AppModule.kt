package com.snapcabin.di

import android.content.Context
import com.snapcabin.analytics.AnalyticsTracker
import com.snapcabin.analytics.CrashReporter
import com.snapcabin.camera.CameraManager
import com.snapcabin.filter.EventBrandingRenderer
import com.snapcabin.kiosk.KioskManager
import com.snapcabin.settings.SettingsManager
import com.snapcabin.share.EmailSmsSharer
import com.snapcabin.share.LocalPhotoServer
import com.snapcabin.share.PhotoPrinter
import com.snapcabin.share.PhotoSaver
import com.snapcabin.share.QrCodeGenerator
import com.snapcabin.ui.components.SoundManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCameraManager(
        @ApplicationContext context: Context
    ): CameraManager = CameraManager(context)

    @Provides
    @Singleton
    fun provideSettingsManager(
        @ApplicationContext context: Context
    ): SettingsManager = SettingsManager(context)

    @Provides
    @Singleton
    fun providePhotoSaver(): PhotoSaver = PhotoSaver()

    @Provides
    @Singleton
    fun provideQrCodeGenerator(): QrCodeGenerator = QrCodeGenerator()

    @Provides
    @Singleton
    fun provideLocalPhotoServer(): LocalPhotoServer = LocalPhotoServer()

    @Provides
    @Singleton
    fun provideSoundManager(): SoundManager = SoundManager()

    @Provides
    @Singleton
    fun provideKioskManager(): KioskManager = KioskManager()

    @Provides
    @Singleton
    fun providePhotoPrinter(): PhotoPrinter = PhotoPrinter()

    @Provides
    @Singleton
    fun provideEmailSmsSharer(): EmailSmsSharer = EmailSmsSharer()

    @Provides
    @Singleton
    fun provideEventBrandingRenderer(): EventBrandingRenderer = EventBrandingRenderer()

    @Provides
    @Singleton
    fun provideAnalyticsTracker(): AnalyticsTracker = AnalyticsTracker()

    @Provides
    @Singleton
    fun provideCrashReporter(): CrashReporter = CrashReporter()
}
