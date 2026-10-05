# Architecture

Tarang is a single-module, single-activity Jetpack Compose app. It is built to be small, fast and readable.

```
┌─────────────────────────── UI (Compose) ───────────────────────────┐
│ Home · Converse · Live · Text · Scan · Phrases · History · Settings│
│      each screen ↔ its ViewModel (StateFlow<UiState>)              │
└───────────────┬────────────────────────────────────────────────────┘
                │ AppContainer (manual DI, created in TarangApp)
┌───────────────┴──────────────── engine ────────────────────────────┐
│ SpeechInput  ── android.speech.SpeechRecognizer  (mic → text)      │
│ TranslationEngine ─┬─ ML Kit on-device Translator (offline)        │
│                    └─ MyMemory HTTPS API (free online fallback)    │
│ Speaker      ── android.speech.tts.TextToSpeech  (text → voice)    │
│ TextScanner  ── ML Kit Text Recognition v2, Devanagari+Latin (OCR) │
└───────────────┬────────────────────────────────────────────────────┘
┌───────────────┴──────────────── core ──────────────────────────────┐
│ Language (enum) · SettingsStore (SharedPreferences → StateFlow)    │
│ HistoryStore (JSON file, atomic writes, favourites never trimmed)  │
└────────────────────────────────────────────────────────────────────┘
```

## Key decisions

| Decision | Why |
|---|---|
| **ML Kit on-device translation** | Free and private. It supports exactly the 8 most-spoken Indian languages (Malayalam, Punjabi and Odia are not available yet). Packs are about 30 MB and downloaded on demand, so they don't inflate the APK. |
| **Smart engine mode** | Uses on-device translation when both packs are installed (instant and private), otherwise the free MyMemory API, so translation works on first launch without a download. Users can force *Offline* or *Online*. |
| **Platform speech services** | `SpeechRecognizer` and `TextToSpeech` are free, high quality for Indic languages and can run offline. Bundling Whisper or Vosk models would add 40–500 MB. |
| **Unbundled OCR** | `play-services-mlkit-text-recognition*` downloads its models through Play services, which saves about 20 MB of APK. |
| **No nav, DI or network libraries** | A 9-route string back-stack, one `AppContainer` and `HttpURLConnection` cover our needs and keep the dependency tree and APK small. |
| **R8 + resource shrinking + ABI splits** | The release APKs ship only the code and native libraries each phone needs. |

## Flows

**Conversation:** mic tap → `SpeechInput.start(lang)` streams `Partial` events (shown live) → `Final(text)` →
`TranslationEngine.translate` → bubble updated in both panels → `Speaker.speak` (if auto-speak is on) → saved to
history.

**Live captions:** a loop. Listen (long-form silence settings) → commit the final phrase → restart the recognizer
immediately → translate in the background. Partial text is also translated on-device with a 220 ms debounce, so
subtitles move while the person is still speaking. With *read aloud* on, the mic pauses until TTS finishes, so Tarang
doesn't transcribe itself.

**Type:** debounced as-you-type translation (280 ms on-device, 800 ms online). A result is saved to history after
2.5 s of no typing.

## Threading

All engine calls run on the main thread except ML Kit tasks (background, awaited with
`kotlinx-coroutines-play-services`) and HTTP/file I/O (`Dispatchers.IO`). `SpeechRecognizer` must be created and used
on the main thread, and ViewModels call it from `viewModelScope` (Main).

## Extending

* **Add a language:** add an entry to `core/Language.kt`. It must be supported by ML Kit (`TranslateLanguage`) and
  have a BCP-47 tag that Android speech supports.
* **Add a translation engine:** add another branch in `TranslationEngine.translate` and return
  `TranslationResult.Success(text, EngineUsed.X)`.
* **Add a phrase category:** edit `engine/Phrasebook.kt`. Phrases are written in English and translated at runtime.
