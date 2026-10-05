package ai.tarang.app.ui.live

import ai.tarang.app.AppContainer
import ai.tarang.app.core.EngineMode
import ai.tarang.app.core.Mode
import ai.tarang.app.engine.SpeechEvent
import ai.tarang.app.engine.TranslationResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class Caption(val id: Long, val original: String, val translated: String? = null, val error: String? = null)

data class LiveState(
    val active: Boolean = false,
    val partial: String = "",
    val partialTranslation: String = "",
    val level: Float = 0f,
    val captions: List<Caption> = emptyList(),
    val readAloud: Boolean = false,
    val message: String? = null,
)

/**
 * Continuous "subtitles" mode: keeps listening to the other person's language and
 * shows a rolling translation, restarting the recognizer after every phrase.
 */
class LiveViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(LiveState())
    val state: StateFlow<LiveState> = _state.asStateFlow()
    private var partialJob: Job? = null
    private var restartJob: Job? = null

    private val from get() = c.settings.value.partnerLanguage
    private val to get() = c.settings.value.myLanguage

    fun toggle() = if (_state.value.active) stop() else start()

    fun start() {
        c.speaker.stop()
        _state.update { it.copy(active = true, message = null) }
        listen()
    }

    fun stop() {
        restartJob?.cancel()
        partialJob?.cancel()
        c.speech.cancel()
        c.speaker.stop()
        _state.update { it.copy(active = false, partial = "", partialTranslation = "", level = 0f) }
    }

    fun setReadAloud(on: Boolean) = _state.update { it.copy(readAloud = on) }
    fun clear() = _state.update { it.copy(captions = emptyList()) }
    fun dismissMessage() = _state.update { it.copy(message = null) }

    private fun listen() {
        if (!_state.value.active) return
        c.speech.start(from, c.settings.value.preferOfflineSpeech, longForm = true) { event ->
            when (event) {
                SpeechEvent.Ready -> Unit
                is SpeechEvent.Level -> _state.update { it.copy(level = event.level) }
                is SpeechEvent.Partial -> {
                    _state.update { it.copy(partial = event.text) }
                    translatePartial(event.text)
                }
                is SpeechEvent.Final -> commit(event.text)
                is SpeechEvent.Error ->
                    if (event.quiet) restart()
                    else {
                        stop()
                        _state.update { it.copy(message = event.message) }
                    }
            }
        }
    }

    private fun restart(delayMs: Long = 200) {
        restartJob?.cancel()
        restartJob = viewModelScope.launch {
            delay(delayMs)
            if (_state.value.active) listen()
        }
    }

    /** Live preview translation while the person is still talking (on-device only, it's instant and free). */
    private fun translatePartial(text: String) {
        val (f, t) = from to to
        if (c.settings.value.engine == EngineMode.ONLINE || !c.translator.isOfflineReady(f, t)) return
        partialJob?.cancel()
        partialJob = viewModelScope.launch {
            delay(220)
            val r = c.translator.translate(text, f, t)
            if (r is TranslationResult.Success) _state.update { it.copy(partialTranslation = r.text) }
        }
    }

    private fun commit(text: String) {
        partialJob?.cancel()
        val (f, t) = from to to
        val id = System.nanoTime()
        _state.update {
            it.copy(partial = "", partialTranslation = "", level = 0f, captions = (it.captions + Caption(id, text)).takeLast(MAX_CAPTIONS))
        }
        val readAloud = _state.value.readAloud
        if (!readAloud) restart(50)
        viewModelScope.launch {
            val r = c.translator.translate(text, f, t)
            _state.update { s ->
                s.copy(captions = s.captions.map {
                    if (it.id != id) it
                    else when (r) {
                        is TranslationResult.Success -> it.copy(translated = r.text)
                        is TranslationResult.Failure -> it.copy(error = r.message)
                    }
                })
            }
            if (r is TranslationResult.Success) {
                c.history.add(text, r.text, f, t, Mode.LIVE)
                if (readAloud && _state.value.active) {
                    // Pause the mic while speaking so Tarang doesn't transcribe itself.
                    withTimeoutOrNull(30_000) { c.speaker.speakAndWait(r.text, t, c.settings.value.speechRate) }
                }
            }
            if (readAloud) restart(150)
        }
    }

    override fun onCleared() = stop()

    private companion object { const val MAX_CAPTIONS = 100 }
}
