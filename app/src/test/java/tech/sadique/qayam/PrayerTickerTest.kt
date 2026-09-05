package tech.sadique.qayam

import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import tech.sadique.qayam.ui.viewmodel.PrayerViewModel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerTickerTest {

    @Test
    fun `ticker emits clock state while uiState stays clock-free`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = ViewModelStore()
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val vm = ViewModelProvider(store, factory)[PrayerViewModel::class.java]
        try {
            // Run the viewModelScope ticker posted on the main looper.
            shadowOf(Looper.getMainLooper()).idle()

            val ticker = vm.tickerState.value
            assertTrue(
                "ticker clock not recent: ${ticker.currentTimeMillis}",
                kotlin.math.abs(System.currentTimeMillis() - ticker.currentTimeMillis) < 60_000
            )
            assertNotNull("ticker produced no prayer state", ticker.currentState)
            // Static state resolves (settings default + computed schedule) without clock fields.
            assertNotNull(vm.uiState.value.schedule)
            assertNotNull(vm.uiState.value.settings)
        } finally {
            store.clear()
        }
    }
}
