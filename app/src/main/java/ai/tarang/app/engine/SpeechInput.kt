package ai.tarang.app.engine

import ai.tarang.app.core.Language
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

sealed interface SpeechEvent {
    data object Ready : SpeechEvent
    data class Level(val level: Float) : SpeechEvent
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    /** [quiet] errors (no speech heard) are expected and can be retried silently. */
    data class Error(val message: String, val quiet: Boolean) : SpeechEvent
}

/**
 * Thin wrapper around Android's built-in [SpeechRecognizer] — free, supports all
 * Tarang languages, and works offline when the device has the offline speech pack.
 * Must be used from the main thread.
 */
class SpeechInput(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(language: Language, preferOffline: Boolean, longForm: Boolean = false, onEvent: (SpeechEvent) -> Unit) {
        cancel()
        if (!isAvailable) {
            onEvent(SpeechEvent.Error("Speech recognition isn't available. Install or enable the Google app.", quiet = false))
            return
        }
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onEvent(SpeechEvent.Ready)
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) = onEvent(SpeechEvent.Level(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)))
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() = onEvent(SpeechEvent.Level(0f))
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) onEvent(SpeechEvent.Partial(text))
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                release(r)
                if (text.isNullOrBlank()) onEvent(SpeechEvent.Error("Didn't catch that", quiet = true))
                else onEvent(SpeechEvent.Final(text))
            }

            override fun onError(error: Int) {
                release(r)
                onEvent(SpeechEvent.Error(describe(error, language), quiet = error in QUIET_ERRORS))
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.tag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.tag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            if (longForm) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            }
        }
        runCatching { r.startListening(intent) }.onFailure {
            release(r)
            onEvent(SpeechEvent.Error("Couldn't start the microphone", quiet = false))
        }
    }

    /** Stop listening and deliver whatever was heard so far. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** Abort without results. */
    fun cancel() {
        recognizer?.let { release(it) }
    }

    private fun release(r: SpeechRecognizer) {
        runCatching { r.cancel() }
        runCatching { r.destroy() }
        if (recognizer === r) recognizer = null
    }

    private fun describe(error: Int, language: Language): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Try again"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
            "Speech needs internet for ${language.englishName}. Download offline speech in Google settings"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Microphone busy. Try again"
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error"
        ERROR_LANGUAGE_NOT_SUPPORTED, ERROR_LANGUAGE_UNAVAILABLE ->
            "${language.englishName} speech isn't available on this device yet"
        else -> "Speech recognition stopped (code $error)"
    }

    private companion object {
        const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        const val ERROR_LANGUAGE_UNAVAILABLE = 13
        val QUIET_ERRORS = setOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_CLIENT)
    }
}
