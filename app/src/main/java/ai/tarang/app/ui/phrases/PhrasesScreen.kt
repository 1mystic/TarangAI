package ai.tarang.app.ui.phrases

import ai.tarang.app.engine.Phrasebook
import ai.tarang.app.ui.components.NoticeBanner
import ai.tarang.app.ui.components.SavedLanguagePair
import ai.tarang.app.ui.components.ShowcaseDialog
import ai.tarang.app.ui.components.TarangTopBar
import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import ai.tarang.app.ui.theme.Brand
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhrasesScreen(onBack: () -> Unit) {
    val c = rememberContainer()
    val vm: PhrasesViewModel = viewModel { PhrasesViewModel(c) }
    val state by vm.state.collectAsStateLifecycle()
    val settings by c.settings.state.collectAsStateLifecycle()
    val speaking by c.speaker.speaking.collectAsStateLifecycle()
    var showcase by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TarangTopBar("Phrasebook", onBack)
        SavedLanguagePair(modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(Phrasebook.categories) { i, cat ->
                FilterChip(
                    selected = state.category == i,
                    onClick = { vm.select(i) },
                    label = { Text("${cat.emoji}  ${cat.title}") },
                    shape = RoundedCornerShape(50),
                )
            }
        }
        state.error?.let { NoticeBanner(it, Modifier.padding(horizontal = 16.dp)) }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(state.items, key = { _, it -> it.english }) { _, item ->
                val isSpeaking = speaking == "phrase:${item.english}"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSpeaking) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .combinedClickable(onClick = { vm.speak(item) }, onLongClick = { item.theirs?.let { showcase = it } }),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.theirs ?: "…",
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                            )
                            Text(
                                item.mine ?: item.english,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        IconButton(onClick = { vm.speak(item) }, enabled = item.theirs != null) {
                            Icon(
                                if (isSpeaking) Icons.Rounded.GraphicEq else Icons.AutoMirrored.Rounded.VolumeUp,
                                "Speak",
                                tint = if (isSpeaking) Brand.Saffron else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Tap to speak · long-press to show full screen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                )
            }
        }
    }
    showcase?.let { ShowcaseDialog(it, settings.partnerLanguage.nativeName) { showcase = null } }
}
