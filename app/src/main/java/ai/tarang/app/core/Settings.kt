package ai.tarang.app.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EngineMode(val label: String, val description: String) {
    AUTO("Smart", "On-device when the language pack is installed, free online engine otherwise"),
    OFFLINE("Offline", "Always on-device. Private, works without internet"),
    ONLINE("Online", "Free online engine (MyMemory). No downloads needed"),
}

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class AppSettings(
    val myLanguage: Language = Language.ENGLISH,
    val partnerLanguage: Language = Language.HINDI,
    val engine: EngineMode = EngineMode.AUTO,
    val autoSpeak: Boolean = true,
    val speechRate: Float = 1.0f,
    val preferOfflineSpeech: Boolean = false,
    val wifiOnlyDownloads: Boolean = true,
    val faceToFace: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val captionScale: Float = 1.0f,
    val onboarded: Boolean = false,
)

/** Tiny SharedPreferences-backed settings store exposed as a [StateFlow]. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("tarang_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()
    val value: AppSettings get() = _state.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
        save(next)
    }

    fun swapLanguages() = update { it.copy(myLanguage = it.partnerLanguage, partnerLanguage = it.myLanguage) }

    private fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            myLanguage = Language.fromCode(prefs.getString("my", null)) ?: d.myLanguage,
            partnerLanguage = Language.fromCode(prefs.getString("partner", null)) ?: d.partnerLanguage,
            engine = enumOr(prefs.getString("engine", null), d.engine),
            autoSpeak = prefs.getBoolean("autoSpeak", d.autoSpeak),
            speechRate = prefs.getFloat("speechRate", d.speechRate),
            preferOfflineSpeech = prefs.getBoolean("offlineSpeech", d.preferOfflineSpeech),
            wifiOnlyDownloads = prefs.getBoolean("wifiOnly", d.wifiOnlyDownloads),
            faceToFace = prefs.getBoolean("faceToFace", d.faceToFace),
            theme = enumOr(prefs.getString("theme", null), d.theme),
            captionScale = prefs.getFloat("captionScale", d.captionScale),
            onboarded = prefs.getBoolean("onboarded", d.onboarded),
        )
    }

    private fun save(s: AppSettings) {
        prefs.edit()
            .putString("my", s.myLanguage.code)
            .putString("partner", s.partnerLanguage.code)
            .putString("engine", s.engine.name)
            .putBoolean("autoSpeak", s.autoSpeak)
            .putFloat("speechRate", s.speechRate)
            .putBoolean("offlineSpeech", s.preferOfflineSpeech)
            .putBoolean("wifiOnly", s.wifiOnlyDownloads)
            .putBoolean("faceToFace", s.faceToFace)
            .putString("theme", s.theme.name)
            .putFloat("captionScale", s.captionScale)
            .putBoolean("onboarded", s.onboarded)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
