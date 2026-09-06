package tech.sadique.qayam.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import tech.sadique.qayam.ui.screens.MainPrayerScreen
import tech.sadique.qayam.ui.screens.SettingsScreen
import tech.sadique.qayam.ui.viewmodel.PrayerViewModel

enum class AppScreen {
    MAIN,
    SETTINGS
}

@Composable
fun AppNavHost(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreenId by rememberSaveable { mutableStateOf(AppScreen.MAIN.name) }
    val currentScreen = AppScreen.valueOf(currentScreenId)

    BackHandler(enabled = currentScreen == AppScreen.SETTINGS) {
        currentScreenId = AppScreen.MAIN.name
    }

    AnimatedContent(
        targetState = currentScreen,
        modifier = modifier.fillMaxSize(),
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
