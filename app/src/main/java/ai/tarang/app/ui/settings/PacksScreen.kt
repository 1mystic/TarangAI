package ai.tarang.app.ui.settings

import ai.tarang.app.core.Language
import ai.tarang.app.ui.components.LangAvatar
import ai.tarang.app.ui.components.TarangTopBar
import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Manage the ~30 MB on-device ML Kit translation packs. */
@Composable
fun PacksScreen(onBack: () -> Unit) {
    val c = rememberContainer()
    val context = LocalContext.current
    val installed by c.translator.installed.collectAsStateLifecycle()
    val downloading by c.translator.downloading.collectAsStateLifecycle()
    val settings by c.settings.state.collectAsStateLifecycle()
    LaunchedEffect(Unit) { c.translator.refreshInstalled() }

    fun download(lang: Language) {
        c.scope.launch {
            c.translator.download(lang)?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TarangTopBar("Offline language packs", onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Translate with no internet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            "Each pack is about 30 MB and runs fully on your phone with Google's free ML Kit models — private, fast and free forever. " +
                                if (settings.wifiOnlyDownloads) "Downloads use Wi-Fi only (change in Settings)." else "Downloads may use mobile data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        val missing = Language.entries.filter { it !in installed && it !in downloading }
                        if (missing.isNotEmpty()) {
                            Button(onClick = { missing.forEach(::download) }, modifier = Modifier.padding(top = 12.dp)) {
                                Text("Download all (${missing.size})")
                            }
                        }
                    }
                }
            }
            items(Language.entries) { lang ->
                val isInstalled = lang in installed
                val busy = lang in downloading
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    leadingContent = { LangAvatar(lang, 40.dp) },
                    headlineContent = { Text(lang.nativeName, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = {
                        Text(
                            when {
                                lang.builtIn -> "${lang.englishName} · built in"
                                isInstalled -> "${lang.englishName} · ready offline"
                                busy -> "${lang.englishName} · downloading…"
                                else -> "${lang.englishName} · ~30 MB"
                            },
                        )
                    },
                    trailingContent = {
                        when {
                            lang.builtIn -> Icon(Icons.Rounded.OfflineBolt, "Built in", tint = MaterialTheme.colorScheme.secondary)
                            busy -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            isInstalled -> IconButton(onClick = { c.scope.launch { c.translator.delete(lang) } }) {
                                Icon(Icons.Rounded.Delete, "Remove pack")
                            }
                            else -> FilledTonalButton(onClick = { download(lang) }) {
                                Icon(Icons.Rounded.CloudDownload, null, Modifier.size(18.dp))
                                Text("  Get")
                            }
                        }
                    },
                )
            }
            item { Text("", Modifier.navigationBarsPadding()) }
        }
    }
}
