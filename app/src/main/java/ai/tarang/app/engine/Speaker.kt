package ai.tarang.app.engine

import ai.tarang.app.core.Language
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Text-to-speech via the device's free TTS engine (Google Speech Services ships Indic voices). */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: (() -> Unit)? = null
    private val waiters = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    private val _speaking = MutableStateFlow<String?>(null)
    /** Utterance id currently being spoken, or null. */
    val speaking: StateFlow<String?> = _speaking.asStateFlow()

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speaking.value = utteranceId }
            override fun onDone(utteranceId: String?) = finish(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
        })
        pending?.invoke()
        pending = null
    }

    private fun finish(id: String?) {
        if (_speaking.value == id) _speaking.value = null
        id?.let { waiters.remove(it)?.complete(Unit) }
    }

    fun isSupported(language: Language): Boolean {
        if (!ready) return true
        return tts.isLanguageAvailable(language.locale) >= TextToSpeech.LANG_AVAILABLE ||
            tts.isLanguageAvailable(Locale(language.code)) >= TextToSpeech.LANG_AVAILABLE
    }

    /** Speaks [text]; returns the utterance id, or null if the language has no voice. */
    fun speak(text: String, language: Language, rate: Float = 1f, id: String = UUID.randomUUID().toString()): String? {
        if (text.isBlank()) return null
        if (!ready) {
            pending = { speak(text, language, rate, id) }
            return id
        }
        var result = tts.setLanguage(language.locale)
        if (result < TextToSpeech.LANG_AVAILABLE) result = tts.setLanguage(Locale(language.code))
        if (result < TextToSpeech.LANG_AVAILABLE) return null
        tts.setSpeechRate(rate)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        return id
    }

    /** Speaks and suspends until playback finishes. Returns false if the language has no voice. */
    suspend fun speakAndWait(text: String, language: Language, rate: Float = 1f): Boolean {
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>()
        waiters[id] = done
        if (speak(text, language, rate, id) == null) {
            waiters.remove(id)
            return false
        }
        done.await()
        return true
    }

    fun stop() {
        if (ready) tts.stop()
        _speaking.value = null
        waiters.values.forEach { it.complete(Unit) }
        waiters.clear()
    }
}
