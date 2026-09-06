package tech.sadique.qayam.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.sadique.qayam.data.location.AndroidGeocoderServiceImpl
import tech.sadique.qayam.data.location.FusedLocationProviderImpl
import tech.sadique.qayam.data.location.GeocoderService
import tech.sadique.qayam.data.location.LocationProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: FusedLocationProviderImpl): LocationProvider

    @Binds
    @Singleton
    abstract fun bindGeocoderService(impl: AndroidGeocoderServiceImpl): GeocoderService
}
