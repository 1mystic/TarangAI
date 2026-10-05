package ai.tarang.app.ui

import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import ai.tarang.app.ui.converse.ConverseScreen
import ai.tarang.app.ui.history.HistoryScreen
import ai.tarang.app.ui.home.HomeDestination
import ai.tarang.app.ui.home.HomeScreen
import ai.tarang.app.ui.live.LiveScreen
import ai.tarang.app.ui.onboarding.OnboardingScreen
import ai.tarang.app.ui.phrases.PhrasesScreen
import ai.tarang.app.ui.scan.ScanScreen
import ai.tarang.app.ui.settings.PacksScreen
import ai.tarang.app.ui.settings.SettingsScreen
import ai.tarang.app.ui.text.TextScreen
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private const val HOME = "Home"

/**
 * A tiny back-stack navigator. Seven screens don't need a navigation library;
 * this keeps the APK lean and the flow obvious.
 */
@Composable
fun AppRoot(incomingText: String?, onIncomingConsumed: () -> Unit) {
    val c = rememberContainer()
    val settings by c.settings.state.collectAsStateLifecycle()
    var stack by rememberSaveable { mutableStateOf(listOf(HOME)) }
    var forward by rememberSaveable { mutableStateOf(true) }

    if (!settings.onboarded) {
        OnboardingScreen(onDone = { stack = listOf(HOME) })
        return
    }

    fun push(dest: HomeDestination) {
        forward = true
        stack = stack + dest.name
    }
    fun pop() {
        forward = false
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    LaunchedEffect(incomingText) {
        if (incomingText != null && stack.last() != HomeDestination.Text.name) push(HomeDestination.Text)
    }
    BackHandler(enabled = stack.size > 1) { pop() }

    AnimatedContent(
        targetState = stack.last(),
        transitionSpec = {
            if (forward) (slideInHorizontally { it / 5 } + fadeIn()) togetherWith fadeOut()
            else fadeIn() togetherWith (slideOutHorizontally { it / 5 } + fadeOut())
        },
        label = "nav",
    ) { route ->
        when (route) {
            HomeDestination.Converse.name -> ConverseScreen(::pop)
            HomeDestination.Live.name -> LiveScreen(::pop)
            HomeDestination.Text.name -> TextScreen(::pop, incomingText, onIncomingConsumed)
            HomeDestination.Scan.name -> ScanScreen(::pop)
            HomeDestination.Phrases.name -> PhrasesScreen(::pop)
            HomeDestination.History.name -> HistoryScreen(::pop)
            HomeDestination.Settings.name -> SettingsScreen(::pop) { push(HomeDestination.Packs) }
            HomeDestination.Packs.name -> PacksScreen(::pop)
            else -> HomeScreen(::push)
        }
    }
}
