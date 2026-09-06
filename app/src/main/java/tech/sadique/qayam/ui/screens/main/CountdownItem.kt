@file:Suppress("FunctionNaming")

package tech.sadique.qayam.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import tech.sadique.qayam.data.model.CurrentPrayerState
import tech.sadique.qayam.ui.components.CountdownTimerView
import tech.sadique.qayam.ui.viewmodel.PrayerTickerState

@Composable
fun CountdownItem(currentState: CurrentPrayerState?, is24Hour: Boolean, modifier: Modifier = Modifier) {
    CountdownTimerView(
        state = currentState,
        is24Hour = is24Hour,
        modifier = modifier,
    )
}

@Composable
fun CountdownItem(tickerFlow: StateFlow<PrayerTickerState>, is24Hour: Boolean, modifier: Modifier = Modifier) {
    val ticker by tickerFlow.collectAsStateWithLifecycle()
    CountdownItem(
        currentState = ticker.currentState,
        is24Hour = is24Hour,
        modifier = modifier,
    )
}
