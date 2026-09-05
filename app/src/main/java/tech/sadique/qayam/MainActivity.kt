package tech.sadique.qayam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.sadique.qayam.ui.screens.MainPrayerScreen
import tech.sadique.qayam.ui.screens.SettingsScreen
import tech.sadique.qayam.ui.theme.SalahTheme
import tech.sadique.qayam.ui.viewmodel.PrayerViewModel

enum class AppScreen {
    MAIN,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: PrayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var currentScreenId by rememberSaveable { mutableStateOf(AppScreen.MAIN.name) }
            val currentScreen = AppScreen.valueOf(currentScreenId)

            // Handle back button on Settings screen
            BackHandler(enabled = currentScreen == AppScreen.SETTINGS) {
                currentScreenId = AppScreen.MAIN.name
            }

            SalahTheme(themeMode = uiState.settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == AppScreen.SETTINGS) {
                                (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                        (slideOutHorizontally { width -> -width } + fadeOut())
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                        (slideOutHorizontally { width -> width } + fadeOut())
                            }
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.MAIN -> {
                                MainPrayerScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { currentScreenId = AppScreen.SETTINGS.name }
                                )
                            }
                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreenId = AppScreen.MAIN.name }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
