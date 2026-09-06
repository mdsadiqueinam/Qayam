

package tech.sadique.qayam.ui.screens.main

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.CurrentPrayerState
import tech.sadique.qayam.data.model.PrayerSchedule
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.UserSettings
import tech.sadique.qayam.ui.components.PrayerCard
import tech.sadique.qayam.ui.viewmodel.PrayerTickerState

fun LazyListScope.prayerScheduleItems(
    schedule: PrayerSchedule,
    tickerState: PrayerTickerState,
    settings: UserSettings,
    isPlayingSound: Boolean,
    playingSoundType: AdhanSoundType?,
    onSoundClick: (PrayerType) -> Unit,
) {
    items(PrayerType.dailyPrayers) { prayer ->
        val isEnabled = settings.prayerAlertEnabled[prayer] ?: prayer.defaultAlertEnabled
        val soundType = settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
        PrayerCardRow(
            prayer = prayer,
            schedule = schedule,
            currentState = tickerState.currentState,
            is24Hour = settings.is24HourFormat,
            soundType = soundType,
            isEnabled = isEnabled,
            isPlayingThisSound = isPlayingSound && playingSoundType == soundType,
            onSoundClick = { onSoundClick(prayer) },
        )
    }
}

@Composable
fun PrayerCardRow(
    prayer: PrayerType,
    schedule: PrayerSchedule,
    currentState: CurrentPrayerState?,
    is24Hour: Boolean,
    soundType: AdhanSoundType,
    isEnabled: Boolean,
    isPlayingThisSound: Boolean,
    onSoundClick: () -> Unit,
) {
    PrayerCard(
        prayer = prayer,
        time = schedule.getTime(prayer),
        isCurrent = currentState?.currentPrayer == prayer,
        isNext = currentState?.nextPrayer == prayer,
        is24Hour = is24Hour,
        soundType = soundType,
        isEnabled = isEnabled,
        isPlayingThisSound = isPlayingThisSound,
        onSoundClick = onSoundClick,
    )
}
