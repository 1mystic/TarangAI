<p align="center">
  <img src="docs/brand/logo.svg" width="96" alt="Tarang logo">
</p>
<h1 align="center">Tarang · तरंग</h1>
<p align="center"><b>Every voice, one wave.</b><br>
A live voice translator for Android covering India's 8 most-spoken languages and English. It's free, private and works offline.</p>

<p align="center">
  <a href="https://1mystic8u.github.io/TarangAI/"><b>🌐 Website</b></a> ·
  <a href="../../releases"><b>⬇ Download the APK</b></a> ·
  <a href="docs/USER_GUIDE.md">User guide</a> ·
  <a href="docs/BRAND.md">Brand guide</a> ·
  <a href="docs/ARCHITECTURE.md">Architecture</a> ·
  <a href="PRIVACY.md">Privacy</a>
</p>

---

## Languages

| | Language | Native | Speakers (L1, Census 2011) |
|---|---|---|---|
| हि | Hindi | हिन्दी | 528 M |
| বা | Bengali | বাংলা | 97 M |
| म | Marathi | मराठी | 83 M |
| తె | Telugu | తెలుగు | 81 M |
| த | Tamil | தமிழ் | 69 M |
| ગુ | Gujarati | ગુજરાતી | 55 M |
| ا | Urdu | اردو | 51 M |
| ಕ | Kannada | ಕನ್ನಡ | 44 M |
| A | English | English | Bridge language |

Any language can be translated into any other, giving 72 directions.

## Modes

| Mode | What it does |
|---|---|
| 🗣 **Conversation** | Split screen, one half per person. Tap your mic, speak, and Tarang says the translation out loud. **Face-to-face** flips the top half so the other person can read it from across a table. |
| 💬 **Live captions** | Continuous real-time subtitles of the speech around you, such as announcements, lectures or a tour guide. The translation updates while people are still talking. Optional read-aloud and adjustable text size. |
| ⌨️ **Type** | Type, paste or dictate. The translation appears as you type. You can listen, copy, share, star it, or show it **full screen** to someone. |
| 📷 **Scan** | Take or pick a photo of a sign, menu or notice. The text is read on your phone and then translated. Supports Latin and Devanagari text (English, Hindi, Marathi). |
| 📖 **Phrasebook** | 48 everyday phrases for greetings, travel, food, shopping, health and emergencies, in any language pair. Tap to speak, long-press to show full screen. |

You can also select text in any app and choose **"Translate with Tarang"**, or share text into Tarang. History,
favourites and search are included.

## Free, light and offline-first

| Layer | Engine | Cost | Offline |
|---|---|---|---|
| Translation | Google **ML Kit** on-device translation | Free | ✅ after a one-time ~30 MB pack per language |
| Translation fallback | **MyMemory** public API | Free (anonymous, ~5k chars/day) | ❌ |
| Speech → text | Android **SpeechRecognizer** (Google speech services) | Free | ✅ with the device's offline speech packs |
| Text → speech | Android **TextToSpeech** | Free | ✅ |
| Text in photos | ML Kit Text Recognition v2 (Latin + Devanagari) via Play services | Free | ✅ |

There are no API keys, accounts or servers. The app has no networking library and no navigation or DI framework. R8
shrinking and per-ABI APK splits keep the download small.

## Install

1. Open **[Releases](../../releases)** and download **`Tarang-arm64-v8a.apk`**, which suits almost every phone from
   2017 onwards. If you're unsure, use `Tarang-universal.apk`.
2. Open the file and allow "Install unknown apps" for your browser or file manager when Android asks.
3. On first launch, pick your languages and optionally download the offline packs.

Requires Android 8.0 (API 26) or later. For the best speech experience, keep the **Google** app and **Speech
Services by Google** up to date.

## Build from source

```bash
./gradlew assembleRelease          # APKs in app/build/outputs/apk/release/
./gradlew installDebug             # run on a connected device
```

You need JDK 17 and the Android SDK (API 35). Open the folder in Android Studio Ladybug or newer and press Run.

**CI:** every push runs `.github/workflows/android.yml`, which builds release APKs, uploads them as an artifact and
publishes a `preview-<branch>` pre-release. Pushing a `v*` tag publishes a proper release.

**Signing:** release builds use your keystore when `keystore.properties` exists (keys `storeFile`, `storePassword`,
`keyAlias`, `keyPassword`) or when the `TARANG_KEYSTORE_*` environment variables are set. In CI these come from the
secrets `TARANG_KEYSTORE_BASE64`, `TARANG_KEYSTORE_PASSWORD`, `TARANG_KEY_ALIAS` and `TARANG_KEY_PASSWORD`. Without
them the APK is signed with the debug key. That is fine for testing, but set up a real key before you distribute,
because Android only accepts updates signed with the same key.

## Project layout

```
app/src/main/java/ai/tarang/app/
├── core/        Language, settings, history (pure Kotlin + SharedPreferences/JSON)
├── engine/      TranslationEngine, SpeechInput, Speaker, TextScanner, Phrasebook
└── ui/          Compose screens: home, converse, live, text, scan, phrases, history, settings, onboarding
    ├── components/  Brand (logo, wave), mic button, language picker, shared widgets
    └── theme/       Brand colours, typography, shapes
docs/            Brand guide, architecture, user guide, SVG brand assets
```

## Credits

Built on Jetpack Compose, Google ML Kit, Android speech services and the MyMemory translation API. Inspired by
the *Saathi* example in the [Bodhan AI Cookbook](https://github.com/rudra431/Bodhan-AI-Cookbook/tree/main/examples/AndroidSpeak),
re-engineered to run on free, on-device models.

## Website

`site/` is a single-file static landing page in the same brand system (inline SVG, no build step). The **Website**
workflow deploys it to GitHub Pages from the default branch. If the first deploy reports that Pages is disabled,
enable it once under **Settings → Pages → Source: GitHub Actions** and re-run the workflow. The download buttons link
to `/releases/latest/download/Tarang-<abi>.apk`, which every default-branch build keeps up to date.
