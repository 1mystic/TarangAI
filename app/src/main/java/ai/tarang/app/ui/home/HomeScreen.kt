package ai.tarang.app.ui.home

import ai.tarang.app.ui.components.SavedLanguagePair
import ai.tarang.app.ui.components.SectionLabel
import ai.tarang.app.ui.components.TarangWave
import ai.tarang.app.ui.components.Wordmark
import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import ai.tarang.app.ui.history.HistoryCard
import ai.tarang.app.ui.theme.Brand
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class HomeDestination { Converse, Live, Text, Scan, Phrases, History, Settings, Packs }

@Composable
fun HomeScreen(onNavigate: (HomeDestination) -> Unit) {
    val c = rememberContainer()
    val settings by c.settings.state.collectAsStateLifecycle()
    val installed by c.translator.installed.collectAsStateLifecycle()
    val history by c.history.entries.collectAsStateLifecycle()
    val offlineReady = settings.myLanguage in installed && settings.partnerLanguage in installed

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Wordmark(Modifier.weight(1f))
            IconButton(onClick = { onNavigate(HomeDestination.History) }) { Icon(Icons.Rounded.History, "History") }
            IconButton(onClick = { onNavigate(HomeDestination.Settings) }) { Icon(Icons.Rounded.Settings, "Settings") }
        }

        // Hero — the primary action
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Brand.heroGradient),
        ) {
            TarangWave(
                Modifier.fillMaxWidth().height(120.dp).align(Alignment.BottomCenter).padding(bottom = 8.dp),
                level = 0.35f,
                colors = listOf(Color.White.copy(alpha = 0.35f), Brand.Saffron.copy(alpha = 0.55f), Color.White.copy(alpha = 0.2f)),
                strokeWidth = 2.dp,
                periodMillis = 4200,
            )
            Column(Modifier.padding(22.dp)) {
                Text("Talk to anyone,\nin their language.", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(
                    "Speak naturally. Tarang listens, translates and speaks back — across 8 Indian languages and English.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = { onNavigate(HomeDestination.Converse) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand.IndigoDeep),
                    modifier = Modifier.padding(top = 18.dp, bottom = 36.dp),
                ) {
                    Icon(Icons.Rounded.RecordVoiceOver, null, Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Start conversation")
                }
            }
        }

        SavedLanguagePair(myCaption = "I speak", partnerCaption = "Talking with")

        Surface(
            onClick = { onNavigate(HomeDestination.Packs) },
            shape = RoundedCornerShape(16.dp),
            color = if (offlineReady) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (offlineReady) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload, null,
                    tint = if (offlineReady) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    if (offlineReady) "Works offline for ${settings.myLanguage.englishName} ↔ ${settings.partnerLanguage.englishName}"
                    else "Get offline packs for instant, private translation",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (offlineReady) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }

        SectionLabel("Modes", Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard("Live captions", "Real-time subtitles of speech around you", Icons.Rounded.ClosedCaption, Brand.Violet, Modifier.weight(1f)) { onNavigate(HomeDestination.Live) }
            ModeCard("Type", "Type, paste or dictate any text", Icons.Rounded.Keyboard, Brand.Indigo, Modifier.weight(1f)) { onNavigate(HomeDestination.Text) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard("Scan", "Read signs, menus and notices", Icons.Rounded.DocumentScanner, Brand.Teal, Modifier.weight(1f)) { onNavigate(HomeDestination.Scan) }
            ModeCard("Phrasebook", "Ready-made travel essentials", Icons.AutoMirrored.Rounded.MenuBook, Brand.Saffron, Modifier.weight(1f)) { onNavigate(HomeDestination.Phrases) }
        }

        if (history.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Recent", Modifier.weight(1f))
                TextButton(onClick = { onNavigate(HomeDestination.History) }) { Text("See all") }
            }
            history.take(3).forEach { e ->
                HistoryCard(
                    e,
                    onSpeak = { c.speaker.speak(e.translated, e.to, settings.speechRate) },
                    onFavorite = { c.history.toggleFavorite(e.id) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ModeCard(title: String, subtitle: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent) }
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2, maxLines = 2)
        }
    }
}
