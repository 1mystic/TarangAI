package ai.tarang.app.ui.settings

import ai.tarang.app.BuildConfig
import ai.tarang.app.core.EngineMode
import ai.tarang.app.core.ThemeMode
import ai.tarang.app.ui.components.SavedLanguagePair
import ai.tarang.app.ui.components.SectionLabel
import ai.tarang.app.ui.components.TarangLogo
import ai.tarang.app.ui.components.TarangTopBar
import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenPacks: () -> Unit) {
    val c = rememberContainer()
    val context = LocalContext.current
    val s by c.settings.state.collectAsStateLifecycle()
    val installed by c.translator.installed.collectAsStateLifecycle()

    fun open(action: String) {
        try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TarangTopBar("Settings", onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel("Languages", Modifier.padding(top = 8.dp))
            SavedLanguagePair(myCaption = "I speak", partnerCaption = "Talking with")

            SectionLabel("Translation", Modifier.padding(top = 12.dp))
            Card {
                Text("Engine", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    EngineMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = s.engine == mode,
                            onClick = { c.settings.update { it.copy(engine = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(i, EngineMode.entries.size),
                        ) { Text(mode.label) }
                    }
                }
                Text(s.engine.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            NavRow("Offline language packs", "${installed.size - 1} of 8 Indian languages installed", onOpenPacks)
            ToggleRow("Download packs on Wi-Fi only", "Packs are ~30 MB each", s.wifiOnlyDownloads) { v -> c.settings.update { it.copy(wifiOnlyDownloads = v) } }

            SectionLabel("Speech", Modifier.padding(top = 12.dp))
            ToggleRow("Speak translations automatically", "In conversation mode", s.autoSpeak) { v -> c.settings.update { it.copy(autoSpeak = v) } }
            ToggleRow("Prefer offline speech recognition", "Uses on-device speech packs when installed", s.preferOfflineSpeech) { v -> c.settings.update { it.copy(preferOfflineSpeech = v) } }
            Card {
                Text("Speaking speed · ${"%.1f".format(s.speechRate)}×", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = s.speechRate,
                    onValueChange = { v -> c.settings.update { it.copy(speechRate = (v * 10).toInt() / 10f) } },
                    valueRange = 0.5f..1.6f,
                    steps = 10,
                )
            }
            NavRow("Voices & offline speech", "Install Indian-language voices in your text-to-speech engine") {
                open("com.android.settings.TTS_SETTINGS")
            }
            NavRow("Voice typing languages", "Download offline speech recognition languages") {
                open(Settings.ACTION_VOICE_INPUT_SETTINGS)
            }

            SectionLabel("Appearance", Modifier.padding(top = 12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = s.theme == mode,
                        onClick = { c.settings.update { it.copy(theme = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                    ) { Text(mode.label) }
                }
            }

            SectionLabel("About", Modifier.padding(top = 12.dp))
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TarangLogo(44.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Tarang ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                        Text("Every voice, one wave.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    "Privacy: on-device translation, text reading and (when available) speech recognition never leave your phone. " +
                        "The optional online engine sends only the text being translated to MyMemory. No accounts, no ads, no tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    "Powered by Google ML Kit, Android speech services and MyMemory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
