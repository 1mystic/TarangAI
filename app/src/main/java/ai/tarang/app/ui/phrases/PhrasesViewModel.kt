package ai.tarang.app.ui.phrases

import ai.tarang.app.AppContainer
import ai.tarang.app.core.Language
import ai.tarang.app.engine.Phrasebook
import ai.tarang.app.engine.TranslationResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhraseItem(val english: String, val mine: String?, val theirs: String?)

data class PhrasesState(
    val category: Int = 0,
    val items: List<PhraseItem> = emptyList(),
    val error: String? = null,
)

class PhrasesViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(PhrasesState())
    val state: StateFlow<PhrasesState> = _state.asStateFlow()
    private val cache = HashMap<String, String>()
    private var job: Job? = null

    init {
        viewModelScope.launch {
            c.settings.state.map { it.myLanguage to it.partnerLanguage }.distinctUntilChanged().collect { load() }
        }
    }

    fun select(index: Int) {
        _state.update { it.copy(category = index) }
        load()
    }

    private fun load() {
        job?.cancel()
        val phrases = Phrasebook.categories[_state.value.category].phrases
        val my = c.settings.value.myLanguage
        val partner = c.settings.value.partnerLanguage
        _state.update { s ->
            s.copy(error = null, items = phrases.map { PhraseItem(it, cached(it, my), cached(it, partner)) })
        }
        job = viewModelScope.launch {
            phrases.forEachIndexed { i, phrase ->
                val theirs = translate(phrase, partner)
                val mine = translate(phrase, my)
                _state.update { s ->
                    s.copy(items = s.items.toMutableList().also { list ->
                        if (i < list.size) list[i] = PhraseItem(phrase, mine ?: phrase, theirs)
                    })
                }
                if (theirs == null && _state.value.error == null) {
                    _state.update { it.copy(error = "Download the ${partner.englishName} pack or go online to see these phrases.") }
                }
            }
        }
    }

    private fun cached(phrase: String, lang: Language): String? =
        if (lang == Language.ENGLISH) phrase else cache["${lang.code}:$phrase"]

    private suspend fun translate(phrase: String, lang: Language): String? {
        cached(phrase, lang)?.let { return it }
        val r = c.translator.translate(phrase, Language.ENGLISH, lang)
        return (r as? TranslationResult.Success)?.text?.also { cache["${lang.code}:$phrase"] = it }
    }

    fun speak(item: PhraseItem) {
        val text = item.theirs ?: return
        val lang = c.settings.value.partnerLanguage
        c.speaker.speak(text, lang, c.settings.value.speechRate, "phrase:${item.english}")
    }
}
