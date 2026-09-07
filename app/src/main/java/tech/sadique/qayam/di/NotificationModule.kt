package tech.sadique.qayam.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.sadique.qayam.notification.AlarmScheduler
import tech.sadique.qayam.notification.ExactAlarmGateway
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: ExactAlarmGateway): AlarmScheduler
}
