package ai.tarang.app.ui.live

import ai.tarang.app.ui.components.MicButton
import ai.tarang.app.ui.components.NoticeBanner
import ai.tarang.app.ui.components.SavedLanguagePair
import ai.tarang.app.ui.components.TarangTopBar
import ai.tarang.app.ui.components.TarangWave
import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import ai.tarang.app.ui.components.rememberMicGate
import ai.tarang.app.ui.theme.Brand
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LiveScreen(onBack: () -> Unit) {
    val c = rememberContainer()
    val vm: LiveViewModel = viewModel { LiveViewModel(c) }
    val state by vm.state.collectAsStateLifecycle()
    val settings by c.settings.state.collectAsStateLifecycle()
    val micGate = rememberMicGate()
    val listState = rememberLazyListState()
    val scale = settings.captionScale

    DisposableEffect(Unit) { onDispose { vm.stop() } }
    LaunchedEffect(state.captions.size, state.partial.length / 20, state.captions.lastOrNull()?.translated) {
        listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TarangTopBar("Live captions", onBack) {
            IconButton(onClick = { c.settings.update { it.copy(captionScale = (it.captionScale - 0.15f).coerceAtLeast(0.7f)) } }) {
                Icon(Icons.Rounded.TextDecrease, "Smaller text")
            }
            IconButton(onClick = { c.settings.update { it.copy(captionScale = (it.captionScale + 0.15f).coerceAtMost(1.9f)) } }) {
                Icon(Icons.Rounded.TextIncrease, "Larger text")
            }
            IconButton(onClick = vm::clear) { Icon(Icons.Rounded.DeleteSweep, "Clear captions") }
        }
        SavedLanguagePair(myFirst = false, modifier = Modifier.padding(horizontal = 16.dp), myCaption = "Show in", partnerCaption = "Listen to")

        AnimatedVisibility(state.message != null) {
            NoticeBanner(state.message.orEmpty(), Modifier.padding(16.dp, 8.dp), onDismiss = vm::dismissMessage)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            if (state.captions.isEmpty() && !state.active) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.ClosedCaption, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Real-time subtitles for announcements, lectures, guides and conversations around you.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp),
                        )
                    }
                }
            }
            items(state.captions, key = { it.id }) { cap ->
                Column {
                    Text(cap.original, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp * scale)
                    Text(
                        cap.translated ?: cap.error?.let { "⚠ $it" } ?: "…",
                        fontSize = 24.sp * scale,
                        lineHeight = 34.sp * scale,
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            item {
                if (state.active) {
                    Column {
                        Text(
                            state.partial.ifEmpty { "Listening…" },
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp * scale,
                        )
                        if (state.partialTranslation.isNotEmpty()) {
                            Text(
                                state.partialTranslation,
                                fontSize = 24.sp * scale,
                                lineHeight = 34.sp * scale,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.navigationBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.active) {
                    TarangWave(
                        Modifier.fillMaxWidth().height(36.dp),
                        level = 0.25f + state.level * 0.75f,
                        colors = listOf(MaterialTheme.colorScheme.primary, Brand.Saffron, MaterialTheme.colorScheme.secondary),
                        strokeWidth = 2.5.dp,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Read aloud", style = MaterialTheme.typography.titleSmall)
                        Text("Pauses listening while speaking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MicButton(
                        listening = state.active,
                        level = state.level,
                        onClick = { if (state.active) vm.stop() else micGate { vm.start() } },
                        size = 72.dp,
                        contentDescription = "Start live captions",
                    )
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                        Switch(checked = state.readAloud, onCheckedChange = vm::setReadAloud)
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}
