package ai.tarang.app.ui.components

import ai.tarang.app.core.Language
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Round avatar showing a language's own script. */
@Composable
fun LangAvatar(
    language: Language,
    size: Dp = 36.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            language.glyph,
            color = content,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
            maxLines = 1,
        )
    }
}

@Composable
fun LanguageChip(language: Language, onClick: () -> Unit, modifier: Modifier = Modifier, caption: String? = null) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LangAvatar(language, 34.dp)
        Column(Modifier.weight(1f, fill = false)) {
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                language.nativeName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (caption == null && language != Language.ENGLISH) {
                Text(language.englishName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Two language chips with an animated swap button between them. */
@Composable
fun LanguagePairBar(
    from: Language,
    to: Language,
    onFromClick: () -> Unit,
    onToClick: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
    fromCaption: String? = null,
    toCaption: String? = null,
) {
    var turns by remember { mutableFloatStateOf(0f) }
    val angle by animateFloatAsState(turns, label = "swap")
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            LanguageChip(from, onFromClick, Modifier.weight(1f), fromCaption)
            FilledTonalIconButton(onClick = { turns += 180f; onSwap() }) {
                Icon(Icons.Rounded.SwapHoriz, "Swap languages", Modifier.rotate(angle))
            }
            LanguageChip(to, onToClick, Modifier.weight(1f), toCaption)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
    title: String,
    selected: Language,
    installed: Set<Language>,
    onPick: (Language) -> Unit,
    onDismiss: () -> Unit,
    options: List<Language> = Language.entries,
    footnote: String? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        if (footnote != null) {
            Text(
                footnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        LazyColumn(Modifier.padding(vertical = 8.dp)) {
            items(options) { lang ->
                val isSelected = lang == selected
                ListItem(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPick(lang) },
                    leadingContent = { LangAvatar(lang, 40.dp) },
                    headlineContent = { Text(lang.nativeName, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text(lang.englishName) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (lang in installed) {
                                Icon(Icons.Rounded.OfflineBolt, "Available offline", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            }
                            if (isSelected) Icon(Icons.Rounded.Check, "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent,
                    ),
                )
            }
        }
        Spacer(Modifier.navigationBarsPadding().padding(bottom = 8.dp))
    }
}

/** Language pair bound to the user's saved "my language" / "partner language" settings. */
@Composable
fun SavedLanguagePair(
    myFirst: Boolean = true,
    modifier: Modifier = Modifier,
    myCaption: String? = null,
    partnerCaption: String? = null,
) {
    val c = rememberContainer()
    val s by c.settings.state.collectAsStateLifecycle()
    val installed by c.translator.installed.collectAsStateLifecycle()
    var picking by remember { mutableStateOf<Boolean?>(null) } // true = mine, false = partner
    val pickMine = { picking = true }
    val pickPartner = { picking = false }
    if (myFirst) {
        LanguagePairBar(s.myLanguage, s.partnerLanguage, pickMine, pickPartner, c.settings::swapLanguages, modifier, myCaption, partnerCaption)
    } else {
        LanguagePairBar(s.partnerLanguage, s.myLanguage, pickPartner, pickMine, c.settings::swapLanguages, modifier, partnerCaption, myCaption)
    }
    picking?.let { mine ->
        LanguagePickerSheet(
            title = if (mine) "Your language" else "Their language",
            selected = if (mine) s.myLanguage else s.partnerLanguage,
            installed = installed,
            onDismiss = { picking = null },
            onPick = { lang ->
                c.settings.update {
                    when {
                        mine && lang == it.partnerLanguage -> it.copy(myLanguage = lang, partnerLanguage = it.myLanguage)
                        !mine && lang == it.myLanguage -> it.copy(partnerLanguage = lang, myLanguage = it.partnerLanguage)
                        mine -> it.copy(myLanguage = lang)
                        else -> it.copy(partnerLanguage = lang)
                    }
                }
                picking = null
            },
        )
    }
}
