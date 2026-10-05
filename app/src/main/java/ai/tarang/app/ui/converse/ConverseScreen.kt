package ai.tarang.app.ui.converse

import ai.tarang.app.core.Language
import ai.tarang.app.ui.components.LanguageChip
import ai.tarang.app.ui.components.LanguagePickerSheet
import ai.tarang.app.ui.components.MicButton
import ai.tarang.app.ui.components.NoticeBanner
import ai.tarang.app.ui.components.TarangTopBar
import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import ai.tarang.app.ui.components.rememberMicGate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ConverseScreen(onBack: () -> Unit) {
    val c = rememberContainer()
    val vm: ConverseViewModel = viewModel { ConverseViewModel(c) }
    val state by vm.state.collectAsStateLifecycle()
    val settings by c.settings.state.collectAsStateLifecycle()
    val installed by c.translator.installed.collectAsStateLifecycle()
    val speaking by c.speaker.speaking.collectAsStateLifecycle()
    val micGate = rememberMicGate()
    var picking by remember { mutableStateOf<Side?>(null) }

    DisposableEffect(Unit) { onDispose { vm.stopAll() } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TarangTopBar("Conversation", onBack) {
            IconButton(onClick = { c.settings.update { it.copy(faceToFace = !it.faceToFace) } }) {
                Icon(
                    Icons.Rounded.ScreenRotation,
                    "Face-to-face mode",
                    tint = if (settings.faceToFace) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = vm::clear) { Icon(Icons.Rounded.DeleteSweep, "Clear conversation") }
        }

        AnimatedVisibility(state.message != null) {
            NoticeBanner(state.message.orEmpty(), Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onDismiss = vm::dismissMessage)
        }

        Panel(
            side = Side.PARTNER,
            language = settings.partnerLanguage,
            state = state,
            speakingId = speaking,
            accent = MaterialTheme.colorScheme.secondary,
            onMic = { micGate { vm.toggle(Side.PARTNER) } },
            onSpeak = vm::speak,
            onPickLanguage = { picking = Side.PARTNER },
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { rotationZ = if (settings.faceToFace) 180f else 0f },
        )

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FilledTonalIconButton(onClick = c.settings::swapLanguages) { Icon(Icons.Rounded.SwapVert, "Swap languages") }
        }

        Panel(
            side = Side.ME,
            language = settings.myLanguage,
            state = state,
            speakingId = speaking,
            accent = MaterialTheme.colorScheme.primary,
            onMic = { micGate { vm.toggle(Side.ME) } },
            onSpeak = vm::speak,
            onPickLanguage = { picking = Side.ME },
            modifier = Modifier.weight(1f).navigationBarsPadding(),
        )
    }

    picking?.let { side ->
        LanguagePickerSheet(
            title = if (side == Side.ME) "Your language" else "Their language",
            selected = if (side == Side.ME) settings.myLanguage else settings.partnerLanguage,
            installed = installed,
            onDismiss = { picking = null },
            onPick = { lang ->
                c.settings.update {
                    if (side == Side.ME) {
                        if (lang == it.partnerLanguage) it.copy(myLanguage = lang, partnerLanguage = it.myLanguage) else it.copy(myLanguage = lang)
                    } else {
                        if (lang == it.myLanguage) it.copy(partnerLanguage = lang, myLanguage = it.partnerLanguage) else it.copy(partnerLanguage = lang)
                    }
                }
                picking = null
            },
        )
    }
}

/** One person's half of the screen. Everything in it is shown in that person's language. */
@Composable
private fun Panel(
    side: Side,
    language: Language,
    state: ConverseState,
    speakingId: String?,
    accent: Color,
    onMic: () -> Unit,
    onSpeak: (Long) -> Unit,
    onPickLanguage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val count = state.bubbles.size + if (state.listening == side) 1 else 0
    LaunchedEffect(count, state.bubbles.lastOrNull()?.translated) {
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            LanguageChip(language, onPickLanguage)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.bubbles.isEmpty() && state.listening != side) {
                item {
                    Text(
                        if (side == Side.ME) "Tap the mic and speak ${language.englishName}. Tarang will say it out loud in the other language."
                        else language.speakPrompt + " · " + language.nativeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            items(state.bubbles, key = { it.id }) { b ->
                val mine = b.side == side
                val text = if (mine) b.original else (b.translated ?: if (b.error != null) "⚠ ${b.error}" else "…")
                val sub = if (mine) null else b.original
                BubbleView(
                    text = text,
                    sub = sub,
                    mine = mine,
                    accent = accent,
                    speaking = speakingId == b.id.toString(),
                    onClick = if (!mine && b.translated != null) ({ onSpeak(b.id) }) else null,
                )
            }
            if (state.listening == side) {
                item {
                    BubbleView(
                        text = state.partial.ifEmpty { "Listening…" },
                        sub = null,
                        mine = true,
                        accent = accent,
                        speaking = false,
                        live = true,
                        onClick = null,
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            MicButton(
                listening = state.listening == side,
                level = state.level,
                onClick = onMic,
                color = accent,
                size = 68.dp,
                contentDescription = language.speakPrompt,
            )
        }
        Text(
            language.speakPrompt,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun BubbleView(
    text: String,
    sub: String?,
    mine: Boolean,
    accent: Color,
    speaking: Boolean,
    onClick: (() -> Unit)?,
    live: Boolean = false,
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (mine) 20.dp else 6.dp, bottomEnd = if (mine) 6.dp else 20.dp,
            ),
            color = when {
                mine -> accent.copy(alpha = if (live) 0.10f else 0.16f)
                speaking -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            modifier = Modifier
                .widthIn(max = 320.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text,
                    fontSize = if (mine) 17.sp else 21.sp,
                    lineHeight = if (mine) 24.sp else 30.sp,
                    fontStyle = if (live) FontStyle.Italic else FontStyle.Normal,
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                )
                if (sub != null) {
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
