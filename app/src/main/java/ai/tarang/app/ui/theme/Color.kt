package ai.tarang.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Tarang brand palette — documented in docs/BRAND.md
object Brand {
    val Indigo = Color(0xFF4F46E5)
    val IndigoDeep = Color(0xFF2B2580)
    val Violet = Color(0xFF7C3AED)
    val Saffron = Color(0xFFFF7A1A)
    val Teal = Color(0xFF0EA5A4)
    val Ink = Color(0xFF0B0D1A)
    val Mist = Color(0xFFF7F7FC)

    /** Signature gradient: deep indigo → indigo → violet. */
    val heroGradient = Brush.linearGradient(listOf(IndigoDeep, Indigo, Violet))
    val sunriseGradient = Brush.linearGradient(listOf(Violet, Saffron))
}
