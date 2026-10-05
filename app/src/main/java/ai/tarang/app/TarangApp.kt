package ai.tarang.app

import ai.tarang.app.core.HistoryStore
import ai.tarang.app.core.SettingsStore
import ai.tarang.app.engine.Speaker
import ai.tarang.app.engine.SpeechInput
import ai.tarang.app.engine.TextScanner
import ai.tarang.app.engine.TranslationEngine
import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TarangApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container — no DI framework needed for an app this size. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings = SettingsStore(context)
    val history = HistoryStore(context, scope)
    val translator = TranslationEngine(context, settings, scope)
    val speaker by lazy { Speaker(context) }
    val speech = SpeechInput(context)
    val scanner = TextScanner()
}

val Context.container: AppContainer get() = (applicationContext as TarangApp).container
