package ai.tarang.app.ui.text

import ai.tarang.app.engine.EngineUsed
import ai.tarang.app.ui.components.NoticeBanner
import ai.tarang.app.ui.components.Pill
import ai.tarang.app.ui.components.ResultActions
import ai.tarang.app.ui.components.SavedLanguagePair
import ai.tarang.app.ui.components.ShowcaseDialog
import ai.tarang.app.ui.components.TarangTopBar
import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import ai.tarang.app.ui.components.rememberMicGate
import ai.tarang.app.ui.theme.Brand
import ai.tarang.app.ui.theme.TranslationTextStyle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TextScreen(onBack: () -> Unit, initialText: String?, onInitialConsumed: () -> Unit) {
    val c = rememberContainer()
    val vm: TextViewModel = viewModel { TextViewModel(c) }
    val state by vm.state.collectAsStateLifecycle()
    val settings by c.settings.state.collectAsStateLifecycle()
    val speaking by c.speaker.speaking.collectAsStateLifecycle()
    val clipboard = LocalClipboardManager.current
    val micGate = rememberMicGate()
    val focus = remember { FocusRequester() }
    var showcase by remember { mutableStateOf(false) }

    LaunchedEffect(initialText) {
        if (initialText != null) {
            vm.setInput(initialText)
            onInitialConsumed()
        } else if (state.input.isEmpty()) {
            runCatching { focus.requestFocus() }
        }
    }
    DisposableEffect(Unit) { onDispose { vm.stopAudio() } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        TarangTopBar("Type & translate", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SavedLanguagePair()

            // Input card
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Box {
                        if (state.input.isEmpty()) {
                            Text("Type or speak in ${settings.myLanguage.nativeName}…", style = TextStyle(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BasicTextField(
                            value = state.input,
                            onValueChange = vm::setInput,
                            textStyle = TextStyle(fontSize = 20.sp, lineHeight = 30.sp, color = MaterialTheme.colorScheme.onSurface, textDirection = TextDirection.Content),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).focusRequester(focus),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${state.input.length}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        if (state.input.isNotEmpty()) {
                            IconButton(onClick = vm::speakInput) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Listen", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(onClick = vm::clear) { Icon(Icons.Rounded.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            IconButton(onClick = { clipboard.getText()?.text?.let(vm::setInput) }) {
                                Icon(Icons.Rounded.ContentPaste, "Paste", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Surface(
                            onClick = { micGate { vm.toggleDictation() } },
                            shape = RoundedCornerShape(50),
                            color = if (state.listening) Brand.Saffron else MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Icon(if (state.listening) Icons.Rounded.Stop else Icons.Rounded.Mic, "Dictate", Modifier.padding(10.dp).size(22.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(state.error != null) {
                NoticeBanner(
                    state.error.orEmpty(),
                    actionLabel = if (state.needsPack != null) (if (state.downloading) "Downloading…" else "Download") else null,
                    onAction = if (state.needsPack != null && !state.downloading) vm::downloadPack else null,
                    onDismiss = vm::dismissError,
                )
            }

            // Output card
            if (state.output.isNotEmpty() || state.translating) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(settings.partnerLanguage.nativeName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            if (state.translating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else if (state.engine != EngineUsed.NONE) Pill(
                                state.engine.label,
                                if (state.engine == EngineUsed.ON_DEVICE) Icons.Rounded.OfflineBolt else Icons.Rounded.Cloud,
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                        if (state.output.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(
                            state.output,
                            style = TranslationTextStyle.copy(textDirection = TextDirection.Content),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        ResultActions(
                            text = state.output,
                            speaking = speaking == "text-output",
                            onSpeak = vm::speak,
                            favorite = state.favoriteId != null,
                            onFavorite = vm::toggleFavorite,
                            onFullscreen = { showcase = true },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding().size(16.dp))
        }
    }

    if (showcase) ShowcaseDialog(state.output, settings.partnerLanguage.nativeName) { showcase = false }
}
