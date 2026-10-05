package ai.tarang.app.ui.converse

import ai.tarang.app.AppContainer
import ai.tarang.app.core.Language
import ai.tarang.app.core.Mode
import ai.tarang.app.engine.SpeechEvent
import ai.tarang.app.engine.TranslationResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Side { ME, PARTNER;
    val other: Side get() = if (this == ME) PARTNER else ME
}

data class Bubble(
    val id: Long,
    val side: Side,
    val original: String,
    val originalLang: Language,
    val translated: String? = null,
    val translatedLang: Language,
    val error: String? = null,
)

data class ConverseState(
    val listening: Side? = null,
    val partial: String = "",
    val level: Float = 0f,
    val bubbles: List<Bubble> = emptyList(),
    val message: String? = null,
)

class ConverseViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ConverseState())
    val state: StateFlow<ConverseState> = _state.asStateFlow()

    fun languageOf(side: Side): Language =
        if (side == Side.ME) c.settings.value.myLanguage else c.settings.value.partnerLanguage

    fun toggle(side: Side) {
        if (_state.value.listening == side) {
            c.speech.stop()
            return
        }
        c.speaker.stop()
        val lang = languageOf(side)
        _state.update { it.copy(listening = side, partial = "", level = 0f, message = null) }
        c.speech.start(lang, c.settings.value.preferOfflineSpeech) { event ->
            when (event) {
                SpeechEvent.Ready -> Unit
                is SpeechEvent.Level -> _state.update { it.copy(level = event.level) }
                is SpeechEvent.Partial -> _state.update { it.copy(partial = event.text) }
                is SpeechEvent.Final -> {
                    _state.update { it.copy(listening = null, partial = "", level = 0f) }
                    submit(side, event.text)
                }
                is SpeechEvent.Error -> _state.update {
                    it.copy(listening = null, partial = "", level = 0f, message = event.message)
                }
            }
        }
    }

    fun submit(side: Side, text: String) {
        val from = languageOf(side)
        val to = languageOf(side.other)
        val id = System.nanoTime()
        _state.update { it.copy(bubbles = it.bubbles + Bubble(id, side, text.trim(), from, null, to)) }
        viewModelScope.launch {
            when (val r = c.translator.translate(text, from, to)) {
                is TranslationResult.Success -> {
                    updateBubble(id) { it.copy(translated = r.text) }
                    c.history.add(text, r.text, from, to, Mode.CONVERSE)
                    if (c.settings.value.autoSpeak) speak(id)
                }
                is TranslationResult.Failure -> updateBubble(id) { it.copy(error = r.message) }
            }
        }
    }

    fun speak(id: Long) {
        val b = _state.value.bubbles.firstOrNull { it.id == id } ?: return
        val text = b.translated ?: return
        if (c.speaker.speak(text, b.translatedLang, c.settings.value.speechRate, id.toString()) == null) {
            _state.update { it.copy(message = "No ${b.translatedLang.englishName} voice installed. Add it in Settings → Speech") }
        }
    }

    fun clear() {
        stopAll()
        _state.update { ConverseState() }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun stopAll() {
        c.speech.cancel()
        c.speaker.stop()
        _state.update { it.copy(listening = null, partial = "", level = 0f) }
    }

    private fun updateBubble(id: Long, transform: (Bubble) -> Bubble) =
        _state.update { s -> s.copy(bubbles = s.bubbles.map { if (it.id == id) transform(it) else it }) }

    override fun onCleared() {
        c.speech.cancel()
    }
}
