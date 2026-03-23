package com.photobooth.di

import android.content.Context
import com.photobooth.camera.CameraManager
import com.photobooth.share.LocalPhotoServer
import com.photobooth.share.PhotoSaver
import com.photobooth.share.QrCodeGenerator
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
    fun providePhotoSaver(): PhotoSaver = PhotoSaver()

    @Provides
    @Singleton
    fun provideQrCodeGenerator(): QrCodeGenerator = QrCodeGenerator()

    @Provides
    @Singleton
    fun provideLocalPhotoServer(): LocalPhotoServer = LocalPhotoServer()
}
