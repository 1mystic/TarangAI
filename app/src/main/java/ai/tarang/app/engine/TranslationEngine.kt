package ai.tarang.app.engine

import ai.tarang.app.core.EngineMode
import ai.tarang.app.core.Language
import ai.tarang.app.core.SettingsStore
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class EngineUsed(val label: String) { ON_DEVICE("On-device"), ONLINE("Online"), NONE("") }

sealed interface TranslationResult {
    data class Success(val text: String, val engine: EngineUsed) : TranslationResult
    data class Failure(val message: String, val needsPack: Language? = null) : TranslationResult
}

/**
 * Translation with two free engines:
 *  1. Google ML Kit on-device translation (offline, private, ~30 MB per language).
 *  2. MyMemory's free public API as a no-download online fallback.
 */
class TranslationEngine(
    private val context: Context,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val modelManager = RemoteModelManager.getInstance()
    private val translators = LinkedHashMap<String, Translator>()

    private val _installed = MutableStateFlow(setOf(Language.ENGLISH))
    /** Languages whose on-device pack is installed. */
    val installed: StateFlow<Set<Language>> = _installed.asStateFlow()

    private val _downloading = MutableStateFlow<Set<Language>>(emptySet())
    val downloading: StateFlow<Set<Language>> = _downloading.asStateFlow()

    init {
        scope.launch { refreshInstalled() }
    }

    suspend fun refreshInstalled() {
        runCatching {
            val models = modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
            val codes = models.map { it.language }.toSet()
            _installed.value = Language.entries.filter { it.builtIn || it.code in codes }.toSet()
        }
    }

    fun isOfflineReady(from: Language, to: Language): Boolean {
        val set = _installed.value
        return from in set && to in set
    }

    suspend fun translate(text: String, from: Language, to: Language): TranslationResult {
        val input = text.trim()
        if (input.isEmpty()) return TranslationResult.Success("", EngineUsed.NONE)
        if (from == to) return TranslationResult.Success(input, EngineUsed.NONE)
        val missing = listOf(from, to).firstOrNull { it !in _installed.value }

        return when (settings.value.engine) {
            EngineMode.OFFLINE ->
                if (missing != null) TranslationResult.Failure("Download the ${missing.englishName} pack to translate offline", missing)
                else onDevice(input, from, to)

            EngineMode.ONLINE ->
                online(input, from, to)
                    ?: if (missing == null) onDevice(input, from, to)
                    else TranslationResult.Failure("Couldn't reach the online engine. Check your connection or install offline packs.", missing)

            EngineMode.AUTO -> when {
                missing == null -> onDevice(input, from, to)
                else -> online(input, from, to)
                    ?: TranslationResult.Failure("You're offline. Download the ${missing.englishName} pack once to translate anywhere.", missing)
            }
        }
    }

    private suspend fun onDevice(text: String, from: Language, to: Language): TranslationResult = runCatching {
        val translator = translatorFor(from, to)
        // Translate line by line so long passages keep their structure.
        val out = text.lines().map { line -> if (line.isBlank()) line else translator.translate(line).await() }
        TranslationResult.Success(out.joinToString("\n"), EngineUsed.ON_DEVICE)
    }.getOrElse { TranslationResult.Failure("On-device translation failed: ${it.message ?: "unknown error"}") }

    @Synchronized
    private fun translatorFor(from: Language, to: Language): Translator {
        val key = "${from.code}>${to.code}"
        translators[key]?.let { return it }
        val t = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(from.code).setTargetLanguage(to.code).build()
        )
        translators[key] = t
        // Keep a handful of warm translators; close the oldest to free native memory.
        if (translators.size > 4) {
            val eldest = translators.keys.first()
            translators.remove(eldest)?.close()
        }
        return t
    }

    /** MyMemory free API: anonymous use, ~5,000 characters/day, 500 bytes per request. */
    private suspend fun online(text: String, from: Language, to: Language): TranslationResult? = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext null
        runCatching {
            val out = chunk(text).joinToString(" ") { piece -> myMemory(piece, from, to) }
            TranslationResult.Success(out, EngineUsed.ONLINE)
        }.getOrNull()
    }

    private fun myMemory(q: String, from: Language, to: Language): String {
        val url = URL(
            "https://api.mymemory.translated.net/get?q=" + URLEncoder.encode(q, "UTF-8") +
                "&langpair=" + from.code + "%7C" + to.code
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 8000
            setRequestProperty("User-Agent", "Tarang-Android")
        }
        try {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val status = json.optInt("responseStatus", 200)
            val translated = json.getJSONObject("responseData").getString("translatedText")
            check(status == 200 && !translated.contains("MYMEMORY WARNING", ignoreCase = true)) { translated }
            return translated
        } finally {
            conn.disconnect()
        }
    }

    private fun chunk(text: String, max: Int = 450): List<String> {
        if (text.toByteArray().size <= max) return listOf(text)
        val sentences = text.split(Regex("(?<=[.!?।॥\\n])\\s*")).filter { it.isNotBlank() }
        val chunks = mutableListOf<String>()
        val sb = StringBuilder()
        for (s in sentences) {
            if (sb.isNotEmpty() && (sb.toString() + " " + s).toByteArray().size > max) {
                chunks += sb.toString(); sb.clear()
            }
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(s.take(max / 3))
        }
        if (sb.isNotEmpty()) chunks += sb.toString()
        return chunks
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isOnWifi(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /** Downloads the language pack. Returns null on success or an error message. */
    suspend fun download(language: Language): String? {
        if (language.builtIn) return null
        _downloading.update { it + language }
        return try {
            val conditions = DownloadConditions.Builder().apply {
                if (settings.value.wifiOnlyDownloads) requireWifi()
            }.build()
            if (settings.value.wifiOnlyDownloads && !isOnWifi()) {
                "Connect to Wi-Fi, or allow mobile data downloads in Settings"
            } else {
                modelManager.download(TranslateRemoteModel.Builder(language.code).build(), conditions).await()
                refreshInstalled()
                null
            }
        } catch (e: Exception) {
            e.message ?: "Download failed"
        } finally {
            _downloading.update { it - language }
        }
    }

    suspend fun delete(language: Language) {
        if (language.builtIn) return
        synchronized(this) {
            translators.keys.filter { key -> key.split(">").contains(language.code) }.forEach { translators.remove(it)?.close() }
        }
        runCatching { modelManager.deleteDownloadedModel(TranslateRemoteModel.Builder(language.code).build()).await() }
        refreshInstalled()
    }
}
