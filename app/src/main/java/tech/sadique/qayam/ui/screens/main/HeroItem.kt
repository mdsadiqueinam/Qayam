package tech.sadique.qayam.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import tech.sadique.qayam.ui.components.MasjidHorizonCanvas
import tech.sadique.qayam.ui.theme.DarkPrimary
import tech.sadique.qayam.ui.theme.GoldLight
import tech.sadique.qayam.ui.viewmodel.PrayerTickerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HeroItem(
    tickerState: PrayerTickerState,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val state = tickerState.currentState
    val timeFormatter = remember(is24Hour) {
        if (is24Hour) SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        else SimpleDateFormat("h:mm:ss a", Locale.getDefault())
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_horizon_card"),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        // Fixed height: Box(fillMaxSize) below must resolve against a bounded
        // height, otherwise (e.g. inside a LazyColumn item) it expands to the
        // whole viewport and the hero swallows the screen.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            MasjidHorizonCanvas(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription =
                            "Animated sky for ${state?.currentPrayer?.displayName ?: "loading"} prayer"
                    }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Current Active Prayer Pill (placeholder until the first tick resolves)
                val currentPrayer = state?.currentPrayer
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.testTag("current_prayer_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DarkPrimary)
                        )
                        Text(
                            text = currentPrayer?.let { "${it.displayName.uppercase()} TIME" }
                                ?: "LOADING PRAYER TIMES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        )
                        currentPrayer?.let {
                            Text(
                                text = it.arabicName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = timeFormatter.format(Date(tickerState.currentTimeMillis)),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.testTag("live_clock_text")
                )

                val alt = state?.sunAltitudeDegrees ?: 0.0
                val sunStatus = remember(alt) {
                    if (alt > 0) String.format(Locale.US, "Sun Altitude: +%.1f° (Day)", alt)
                    else String.format(Locale.US, "Sun Altitude: %.1f° (Night)", alt)
                }
                Text(
                    text = sunStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun HeroItem(
    tickerFlow: StateFlow<PrayerTickerState>,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val ticker by tickerFlow.collectAsStateWithLifecycle()
    HeroItem(tickerState = ticker, is24Hour = is24Hour, modifier = modifier)
}
