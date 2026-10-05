package ai.tarang.app.core

import java.util.Locale

/**
 * The languages Tarang supports: the eight most widely spoken Indian languages
 * (by number of native speakers) plus English as the bridge language.
 * All of them are supported by ML Kit on-device translation, Android speech
 * recognition and Google text-to-speech.
 */
enum class Language(
    val code: String,
    val englishName: String,
    val nativeName: String,
    /** A single representative glyph, used as the language avatar. */
    val glyph: String,
    /** BCP-47 tag used for speech recognition and text-to-speech. */
    val tag: String,
    /** "Tap to speak" written in the language itself, for the person you're talking to. */
    val speakPrompt: String,
    val rtl: Boolean = false,
) {
    ENGLISH("en", "English", "English", "A", "en-IN", "Tap to speak"),
    HINDI("hi", "Hindi", "हिन्दी", "हि", "hi-IN", "बोलिए"),
    BENGALI("bn", "Bengali", "বাংলা", "বা", "bn-IN", "বলুন"),
    MARATHI("mr", "Marathi", "मराठी", "म", "mr-IN", "बोला"),
    TELUGU("te", "Telugu", "తెలుగు", "తె", "te-IN", "మాట్లాడండి"),
    TAMIL("ta", "Tamil", "தமிழ்", "த", "ta-IN", "பேசுங்கள்"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી", "ગુ", "gu-IN", "બોલો"),
    URDU("ur", "Urdu", "اردو", "ا", "ur-IN", "بولیے", rtl = true),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ", "ಕ", "kn-IN", "ಮಾತನಾಡಿ");

    val locale: Locale get() = Locale.forLanguageTag(tag)

    /** ML Kit's text recognizer can read Latin and Devanagari scripts. */
    val scannable: Boolean get() = this == ENGLISH || this == HINDI || this == MARATHI

    /** English ships inside ML Kit; every other language needs a ~30 MB pack for offline use. */
    val builtIn: Boolean get() = this == ENGLISH

    companion object {
        fun fromCode(code: String?): Language? = entries.firstOrNull { it.code == code }
    }
}
