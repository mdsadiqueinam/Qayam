package tech.sadique.qayam.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.sadique.qayam.data.calculator.PrayerTimeCalculator
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerSchedule
import java.util.Calendar
import javax.inject.Inject

class TickerManager @Inject constructor() {

    private val _tickerState = MutableStateFlow(PrayerTickerState())
    val tickerState: StateFlow<PrayerTickerState> = _tickerState.asStateFlow()

    private var tickerJob: Job? = null

    fun start(scope: CoroutineScope, stateProvider: () -> Pair<PrayerSchedule?, LocationInfo>) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val (schedule, loc) = stateProvider()
                refreshTicker(schedule, loc, Calendar.getInstance())
                val now = System.currentTimeMillis()
                delay(1000 - (now % 1000))
            }
        }
    }

    fun refreshTicker(schedule: PrayerSchedule?, loc: LocationInfo, now: Calendar = Calendar.getInstance()) {
        val currentState = if (schedule != null) {
            PrayerTimeCalculator.calculateCurrentState(
                currentTime = now,
                schedule = schedule,
                latitude = loc.latitude,
                longitude = loc.longitude
            )
        } else null
        _tickerState.value = PrayerTickerState(
            currentTimeMillis = now.timeInMillis,
            currentState = currentState
        )
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
    }
}
