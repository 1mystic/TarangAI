package ai.tarang.app.ui.components

import ai.tarang.app.ui.theme.Brand
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/**
 * The Tarang mark: two voices (white and saffron) braided into a single wave.
 * Geometry matches res/drawable/ic_launcher_foreground.xml (108-unit grid).
 */
@Composable
fun TarangLogo(size: Dp, modifier: Modifier = Modifier, withBackground: Boolean = true) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 108f
        if (withBackground) {
            drawRoundRect(brush = Brand.heroGradient, cornerRadius = CornerRadius(this.size.minDimension * 0.3f))
            drawBraid(s, offset = 0f, white = Color.White)
        } else {
            // Without the tile, zoom the braid to fill the box.
            drawBraid(this.size.minDimension / 56f, offset = -26f, white = Brand.Indigo)
        }
    }
}

private fun DrawScope.drawBraid(scale: Float, offset: Float, white: Color) {
    fun p(x: Float) = (x + offset) * scale
    val stroke = Stroke(width = 6f * scale, cap = StrokeCap.Round)
    val saffron = Path().apply {
        moveTo(p(30f), p(54f))
        cubicTo(p(35f), p(70f), p(41f), p(70f), p(46f), p(54f))
        cubicTo(p(51f), p(38f), p(57f), p(38f), p(62f), p(54f))
        cubicTo(p(67f), p(70f), p(73f), p(70f), p(78f), p(54f))
    }
    val main = Path().apply {
        moveTo(p(30f), p(54f))
        cubicTo(p(35f), p(38f), p(41f), p(38f), p(46f), p(54f))
        cubicTo(p(51f), p(70f), p(57f), p(70f), p(62f), p(54f))
        cubicTo(p(67f), p(38f), p(73f), p(38f), p(78f), p(54f))
    }
    drawPath(saffron, Brand.Saffron, style = stroke)
    drawPath(main, white, style = stroke)
}

@Composable
fun Wordmark(modifier: Modifier = Modifier, logoSize: Dp = 34.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TarangLogo(logoSize)
        Text(
            "Tarang",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * Animated multi-line sine wave, the brand's signature motif.
 * [level] (0..1) drives the amplitude, e.g. from microphone loudness.
 */
@Composable
fun TarangWave(
    modifier: Modifier = Modifier,
    level: Float = 0.5f,
    colors: List<Color> = listOf(Color.White, Brand.Saffron, Color.White.copy(alpha = 0.5f)),
    strokeWidth: Dp = 3.dp,
    lines: Int = 3,
    periodMillis: Int = 2600,
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
        label = "phase",
    )
    val amp by animateFloatAsState(level.coerceIn(0f, 1f), label = "amp")
    Canvas(modifier) {
        val mid = size.height / 2f
        val steps = 72
        repeat(lines) { i ->
            val k = 1f - i * 0.22f
            val a = size.height * 0.42f * (0.12f + 0.88f * amp) * k
            val cycles = 1.4f + i * 0.35f
            val path = Path()
            for (s in 0..steps) {
                val t = s / steps.toFloat()
                val envelope = sin(PI * t).toFloat()
                val y = mid + a * envelope * sin(2 * PI * cycles * t + phase * (1f + i * 0.35f) + i).toFloat()
                val x = size.width * t
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path,
                colors[i % colors.size],
                alpha = 1f - i * 0.22f,
                style = Stroke(width = strokeWidth.toPx() * k, cap = StrokeCap.Round),
            )
        }
    }
}
