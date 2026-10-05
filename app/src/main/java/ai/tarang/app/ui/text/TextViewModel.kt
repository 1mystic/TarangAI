package ai.tarang.app.ui.text

import ai.tarang.app.AppContainer
import ai.tarang.app.core.Language
import ai.tarang.app.core.Mode
import ai.tarang.app.engine.EngineUsed
import ai.tarang.app.engine.SpeechEvent
import ai.tarang.app.engine.TranslationResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TextState(
    val input: String = "",
    val output: String = "",
    val engine: EngineUsed = EngineUsed.NONE,
    val translating: Boolean = false,
    val error: String? = null,
    val needsPack: Language? = null,
    val downloading: Boolean = false,
    val listening: Boolean = false,
    val level: Float = 0f,
    val favoriteId: Long? = null,
)

class TextViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(TextState())
    val state: StateFlow<TextState> = _state.asStateFlow()
    private var translateJob: Job? = null
    private var historyJob: Job? = null

    init {
        viewModelScope.launch {
            c.settings.state.map { it.myLanguage to it.partnerLanguage }.distinctUntilChanged().drop(1).collect { schedule(0) }
        }
        viewModelScope.launch {
            c.translator.installed.drop(1).collect { if (_state.value.needsPack != null) schedule(0) }
        }
    }

    fun setInput(text: String) {
        _state.update { it.copy(input = text, favoriteId = null) }
        schedule()
    }

    fun clear() {
        translateJob?.cancel()
        _state.update { TextState() }
    }

    /** Swap languages and carry the translation over as the new input, like a two-way dictionary. */
    fun swap() {
        val out = _state.value.output
        if (out.isNotBlank()) _state.update { it.copy(input = out, output = "", favoriteId = null) }
        c.settings.swapLanguages()
    }

    private fun schedule(delayMs: Long? = null) {
        translateJob?.cancel()
        historyJob?.cancel()
        val s = c.settings.value
        val wait = delayMs ?: if (c.translator.isOfflineReady(s.myLanguage, s.partnerLanguage)) 280L else 800L
        translateJob = viewModelScope.launch {
            delay(wait)
            translateNow()
        }
    }

    private suspend fun translateNow() {
        val input = _state.value.input
        val from = c.settings.value.myLanguage
        val to = c.settings.value.partnerLanguage
        if (input.isBlank()) {
            _state.update { it.copy(output = "", error = null, needsPack = null, translating = false, engine = EngineUsed.NONE) }
            return
        }
        _state.update { it.copy(translating = true) }
        when (val r = c.translator.translate(input, from, to)) {
            is TranslationResult.Success -> {
                _state.update { it.copy(output = r.text, engine = r.engine, translating = false, error = null, needsPack = null) }
                // Save to history once the user has stopped typing for a moment.
                historyJob = viewModelScope.launch {
                    delay(2500)
                    c.history.add(input, r.text, from, to, Mode.TEXT)
                }
            }
            is TranslationResult.Failure ->
                _state.update { it.copy(translating = false, error = r.message, needsPack = r.needsPack) }
        }
    }

    fun downloadPack() {
        val lang = _state.value.needsPack ?: return
        _state.update { it.copy(downloading = true) }
        c.scope.launch {
            val err = c.translator.download(lang)
            _state.update { it.copy(downloading = false, error = err ?: it.error) }
            if (err == null) schedule(0)
        }
    }

    fun speak() {
        val s = _state.value
        if (s.output.isBlank()) return
        val to = c.settings.value.partnerLanguage
        if (c.speaker.speak(s.output, to, c.settings.value.speechRate, "text-output") == null) {
            _state.update { it.copy(error = "No ${to.englishName} voice installed. Add it in Settings → Speech", needsPack = null) }
        }
    }

    fun speakInput() {
        val s = _state.value
        if (s.input.isBlank()) return
        c.speaker.speak(s.input, c.settings.value.myLanguage, c.settings.value.speechRate, "text-input")
    }

    fun toggleFavorite() {
        val s = _state.value
        if (s.output.isBlank()) return
        val existing = s.favoriteId
        if (existing != null) {
            c.history.toggleFavorite(existing)
            _state.update { it.copy(favoriteId = null) }
        } else {
            historyJob?.cancel()
            val id = c.history.add(s.input, s.output, c.settings.value.myLanguage, c.settings.value.partnerLanguage, Mode.TEXT, favorite = true)
            _state.update { it.copy(favoriteId = id) }
        }
    }

    fun toggleDictation() {
        if (_state.value.listening) {
            c.speech.stop()
            return
        }
        c.speaker.stop()
        val base = _state.value.input.trimEnd()
        val prefix = if (base.isEmpty()) "" else "$base "
        _state.update { it.copy(listening = true, error = null) }
        c.speech.start(c.settings.value.myLanguage, c.settings.value.preferOfflineSpeech) { e ->
            when (e) {
                SpeechEvent.Ready -> Unit
                is SpeechEvent.Level -> _state.update { it.copy(level = e.level) }
                is SpeechEvent.Partial -> _state.update { it.copy(input = prefix + e.text) }
                is SpeechEvent.Final -> {
                    _state.update { it.copy(input = prefix + e.text, listening = false, level = 0f) }
                    schedule(0)
                }
                is SpeechEvent.Error -> _state.update { it.copy(listening = false, level = 0f, error = if (e.quiet) null else e.message) }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null, needsPack = null) }

    fun stopAudio() {
        c.speech.cancel()
        _state.update { it.copy(listening = false, level = 0f) }
    }
}
