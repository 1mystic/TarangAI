package ai.tarang.app.ui.components

import ai.tarang.app.AppContainer
import ai.tarang.app.container
import ai.tarang.app.ui.theme.Brand
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

@Composable
fun rememberContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) { context.container }
}

@Composable
fun <T> StateFlow<T>.collectAsStateLifecycle(): State<T> = collectAsStateWithLifecycle()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarangTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

/**
 * Returns a function that runs an action once microphone permission is granted,
 * requesting it first if necessary.
 */
@Composable
fun rememberMicGate(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    val pending = remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pending.value?.invoke()
        else Toast.makeText(context, "Microphone permission is needed to listen", Toast.LENGTH_LONG).show()
        pending.value = null
    }
    return remember(launcher) {
        { action: () -> Unit ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                action()
            } else {
                pending.value = action
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

/** Big round microphone button that pulses with the speaker's voice. */
@Composable
fun MicButton(
    listening: Boolean,
    level: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector = Icons.Rounded.Mic,
    contentDescription: String = "Speak",
) {
    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse",
    )
    val lvl by animateFloatAsState(if (listening) level else 0f, label = "lvl")
    Box(modifier.size(size * 1.5f), contentAlignment = Alignment.Center) {
        if (listening) {
            Box(Modifier.size(size).scale(1.18f + 0.3f * lvl + 0.08f * pulse).background(Brand.Saffron.copy(alpha = 0.16f), CircleShape))
            Box(Modifier.size(size).scale(1.06f + 0.15f * lvl).background(Brand.Saffron.copy(alpha = 0.24f), CircleShape))
        }
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (listening) Brand.Saffron else color,
            contentColor = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.size(size),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (listening) Icons.Rounded.Stop else icon,
                    contentDescription = if (listening) "Stop" else contentDescription,
                    modifier = Modifier.size(size * 0.42f),
                )
            }
        }
    }
}

/** Row of actions for a translation: speak, copy, share, favourite, show full screen. */
@Composable
fun ResultActions(
    text: String,
    speaking: Boolean,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
    favorite: Boolean? = null,
    onFavorite: (() -> Unit)? = null,
    onFullscreen: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onSpeak) {
            Icon(
                if (speaking) Icons.Rounded.GraphicEq else Icons.AutoMirrored.Rounded.VolumeUp,
                "Speak",
                tint = if (speaking) Brand.Saffron else MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = {
            clipboard.setText(AnnotatedString(text))
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        }) { Icon(Icons.Rounded.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        IconButton(onClick = { shareText(context, text) }) {
            Icon(Icons.Rounded.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (favorite != null && onFavorite != null) {
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    "Favourite",
                    tint = if (favorite) Brand.Saffron else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onFullscreen != null) {
            IconButton(onClick = onFullscreen) {
                Icon(Icons.Rounded.Fullscreen, "Show full screen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share translation"))
}

/** Big, high-contrast text to hold up and show the person you're talking to. */
@Composable
fun ShowcaseDialog(text: String, caption: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brand.heroGradient)
                .safeDrawingPadding()
                .padding(24.dp),
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Rounded.Close, "Close", tint = Color.White)
            }
            Column(
                Modifier.align(Alignment.Center).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(caption, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                Text(
                    text,
                    color = Color.White,
                    fontSize = 40.sp,
                    lineHeight = 54.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/** Inline banner for problems, with an optional action such as "Download pack". */
@Composable
fun NoticeBanner(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null, onDismiss: (() -> Unit)? = null) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 6.dp),
            )
            if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
            if (onDismiss != null) IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, "Dismiss", tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

/** Small pill label, e.g. "On-device" / "Online". */
@Composable
fun Pill(text: String, icon: ImageVector? = null, container: Color = MaterialTheme.colorScheme.secondaryContainer, content: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, null, Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

