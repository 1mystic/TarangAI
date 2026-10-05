package ai.tarang.app.ui.history

import ai.tarang.app.core.HistoryEntry
import ai.tarang.app.ui.components.TarangTopBar
import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import ai.tarang.app.ui.components.shareText
import ai.tarang.app.ui.theme.Brand
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val c = rememberContainer()
    val context = LocalContext.current
    val entries by c.history.entries.collectAsStateLifecycle()
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    val shown = entries.filter { e ->
        (!favoritesOnly || e.favorite) &&
            (query.isBlank() || e.source.contains(query, true) || e.translated.contains(query, true))
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TarangTopBar("History", onBack) {
            if (entries.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Rounded.DeleteSweep, "Clear history") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search translations") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !favoritesOnly, onClick = { favoritesOnly = false }, label = { Text("All") }, shape = RoundedCornerShape(50))
            FilterChip(
                selected = favoritesOnly, onClick = { favoritesOnly = true }, label = { Text("Favourites") }, shape = RoundedCornerShape(50),
                leadingIcon = { Icon(Icons.Rounded.Star, null, Modifier.size(16.dp), tint = Brand.Saffron) },
            )
        }
        if (shown.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.History, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Text(
                    if (favoritesOnly) "Star a translation to keep it here." else "Your translations will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.id }) { e ->
                    HistoryCard(
                        e,
                        onSpeak = { c.speaker.speak(e.translated, e.to, c.settings.value.speechRate) },
                        onFavorite = { c.history.toggleFavorite(e.id) },
                        onShare = { shareText(context, e.translated) },
                        onDelete = { c.history.delete(e.id) },
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("Favourites are kept.") },
            confirmButton = { TextButton(onClick = { c.history.clear(keepFavorites = true); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun HistoryCard(
    e: HistoryEntry,
    onSpeak: () -> Unit,
    onFavorite: () -> Unit,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 4.dp)) {
            Text(
                "${e.from.nativeName} → ${e.to.nativeName} · ${e.mode.label} · " +
                    DateUtils.getRelativeTimeSpanString(e.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(e.source, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, end = 12.dp), maxLines = 3)
            Text(
                e.translated,
                fontSize = 19.sp,
                lineHeight = 27.sp,
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                modifier = Modifier.padding(top = 2.dp, end = 12.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Speak", tint = MaterialTheme.colorScheme.primary) }
                if (onShare != null) IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (e.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Favourite",
                        tint = if (e.favorite) Brand.Saffron else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
