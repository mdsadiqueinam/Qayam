package tech.sadique.qayam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.sadique.qayam.audio.AudioPreviewController
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.domain.RecalculateScheduleUseCase
import tech.sadique.qayam.domain.RefreshLocationUseCase
import tech.sadique.qayam.fakes.FakeAlarmScheduler
import tech.sadique.qayam.fakes.FakeAudioPlayer
import tech.sadique.qayam.fakes.FakeGeocoderService
import tech.sadique.qayam.fakes.FakeLocationProvider
import tech.sadique.qayam.fakes.FakeSettingsRepository
import tech.sadique.qayam.notification.SchedulePrayerAlarmsUseCase
import tech.sadique.qayam.ui.viewmodel.PrayerViewModel
import tech.sadique.qayam.ui.viewmodel.TickerManager

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTickerTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepo: FakeSettingsRepository
    private lateinit var fakeScheduler: FakeAlarmScheduler
    private lateinit var fakeAudio: FakeAudioPlayer
    private lateinit var fakeLocation: FakeLocationProvider
    private lateinit var fakeGeocoder: FakeGeocoderService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSettingsRepository()
        fakeScheduler = FakeAlarmScheduler()
        fakeAudio = FakeAudioPlayer()
        fakeLocation = FakeLocationProvider()
        fakeGeocoder = FakeGeocoderService()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): Pair<PrayerViewModel, ViewModelStore> {
        val scheduleAlarmsUseCase = SchedulePrayerAlarmsUseCase(fakeScheduler)
        val recalculateScheduleUseCase = RecalculateScheduleUseCase()
        val refreshLocationUseCase = RefreshLocationUseCase(fakeLocation, fakeGeocoder, fakeRepo)
        val audioPreviewController = AudioPreviewController(fakeAudio)
        val tickerManager = TickerManager()

        val store = ViewModelStore()
        val vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PrayerViewModel(
                    settingsRepository = fakeRepo,
                    schedulePrayerAlarmsUseCase = scheduleAlarmsUseCase,
                    alarmScheduler = fakeScheduler,
                    recalculateScheduleUseCase = recalculateScheduleUseCase,
                    refreshLocationUseCase = refreshLocationUseCase,
                    audioPreviewController = audioPreviewController,
                    tickerManager = tickerManager
                ) as T
            }
        })[PrayerViewModel::class.java]
        return vm to store
    }

    @Test
    fun `ticker emits clock state while uiState stays clock-free`() = runTest(testDispatcher) {
        val (vm, store) = createViewModel()
        try {
            testDispatcher.scheduler.runCurrent()

            val ticker = vm.tickerState.value
            assertTrue("ticker clock not recent: ${ticker.currentTimeMillis}", ticker.currentTimeMillis > 0)
            assertNotNull(vm.uiState.value)
            assertNotNull(vm.uiState.value.schedule)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `settings updates recalculate schedule and schedule alarms`() = runTest(testDispatcher) {
        val (vm, store) = createViewModel()
        try {
            testDispatcher.scheduler.runCurrent()

            // 6 daily prayers enabled by default should be scheduled
            assertEquals(6, fakeScheduler.scheduledAlarms.size)

            // Disable Fajr alarm
            vm.updatePrayerAlertEnabled(PrayerType.FAJR, false)
            testDispatcher.scheduler.runCurrent()
            testDispatcher.scheduler.runCurrent()

            assertTrue(fakeScheduler.cancelledAlarms.contains(PrayerType.FAJR))
        } finally {
            store.clear()
        }
    }

    @Test
    fun `audio preview controls audio player`() = runTest(testDispatcher) {
        val (vm, store) = createViewModel()
        try {
            testDispatcher.scheduler.runCurrent()

            vm.playPreviewSound(AdhanSoundType.MAKKAH)
            testDispatcher.scheduler.runCurrent()

            assertTrue(vm.uiState.value.isPlayingSound)
            assertEquals(AdhanSoundType.MAKKAH, vm.uiState.value.playingSoundType)

            vm.stopPreviewSound()
            testDispatcher.scheduler.runCurrent()

            assertTrue(!vm.uiState.value.isPlayingSound)
        } finally {
            store.clear()
        }
    }
}
