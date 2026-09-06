package tech.sadique.qayam.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.sadique.qayam.audio.AudioPlayer
import tech.sadique.qayam.audio.AudioPlayerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(impl: AudioPlayerImpl): AudioPlayer
}
