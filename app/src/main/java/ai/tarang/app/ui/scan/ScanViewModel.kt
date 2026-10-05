package ai.tarang.app.ui.scan

import ai.tarang.app.AppContainer
import ai.tarang.app.core.Language
import ai.tarang.app.core.Mode
import ai.tarang.app.engine.TranslationResult
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScanState(
    val source: Language,
    val target: Language,
    val image: Bitmap? = null,
    val recognized: String = "",
    val output: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

/** Photo → on-device OCR → translation. Great for signboards, menus and notices. */
class ScanViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<ScanState> = _state.asStateFlow()
    private var job: Job? = null

    private fun initialState(): ScanState {
        val s = c.settings.value
        val source = listOf(s.partnerLanguage, s.myLanguage).firstOrNull { it.scannable } ?: Language.HINDI
        val target = if (s.myLanguage != source) s.myLanguage else s.partnerLanguage.takeIf { it != source } ?: Language.ENGLISH
        return ScanState(source = source, target = if (target == source) Language.HINDI else target)
    }

    fun setSource(l: Language) {
        _state.update { it.copy(source = l) }
        retranslate()
    }

    fun setTarget(l: Language) {
        _state.update { it.copy(target = l) }
        retranslate()
    }

    fun swap() {
        val s = _state.value
        if (!s.target.scannable) return
        _state.update { it.copy(source = s.target, target = s.source) }
    }

    fun onImage(context: Context, uri: Uri) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, recognized = "", output = "") }
            val bmp = withContext(Dispatchers.IO) { runCatching { decode(context, uri) }.getOrNull() }
            _state.update { it.copy(image = bmp) }
            val text = runCatching { c.scanner.read(context, uri) }.getOrElse {
                _state.update { s -> s.copy(busy = false, error = "Couldn't read text: ${it.message ?: "OCR unavailable"}") }
                return@launch
            }
            if (text.isBlank()) {
                _state.update { it.copy(busy = false, error = "No text found. Try a closer, sharper photo.") }
                return@launch
            }
            _state.update { it.copy(recognized = text) }
            translate()
        }
    }

    fun editRecognized(text: String) {
        _state.update { it.copy(recognized = text) }
    }

    fun retranslate() {
        if (_state.value.recognized.isBlank()) return
        job?.cancel()
        job = viewModelScope.launch { translate() }
    }

    private suspend fun translate() {
        val s = _state.value
        _state.update { it.copy(busy = true, error = null) }
        when (val r = c.translator.translate(s.recognized, s.source, s.target)) {
            is TranslationResult.Success -> {
                _state.update { it.copy(output = r.text, busy = false) }
                c.history.add(s.recognized, r.text, s.source, s.target, Mode.SCAN)
            }
            is TranslationResult.Failure -> _state.update { it.copy(busy = false, error = r.message) }
        }
    }

    fun speak() {
        val s = _state.value
        if (c.speaker.speak(s.output, s.target, c.settings.value.speechRate, "scan-output") == null) {
            _state.update { it.copy(error = "No ${s.target.englishName} voice installed") }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun decode(context: Context, uri: Uri, maxDim: Int = 1600): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val largest = maxOf(info.size.width, info.size.height)
                if (largest > maxDim) {
                    val scale = maxDim.toFloat() / largest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim) sample *= 2
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}
