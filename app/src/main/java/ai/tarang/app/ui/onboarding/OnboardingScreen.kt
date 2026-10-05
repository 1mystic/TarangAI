package ai.tarang.app.ui.onboarding

import ai.tarang.app.core.Language
import ai.tarang.app.ui.components.LangAvatar
import ai.tarang.app.ui.components.TarangLogo
import ai.tarang.app.ui.components.TarangWave
import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import ai.tarang.app.ui.theme.Brand
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val c = rememberContainer()
    val s by c.settings.state.collectAsStateLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().background(Brand.heroGradient)) {
        TarangWave(
            Modifier.fillMaxWidth().height(160.dp).align(Alignment.Center),
            level = 0.5f,
            colors = listOf(Color.White.copy(alpha = 0.18f), Brand.Saffron.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f)),
            periodMillis = 5000,
        )
        AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "onboarding") { current ->
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (current) {
                    0 -> Welcome { step = 1 }
                    1 -> PickLanguage(
                        title = "Which language do you speak?",
                        selected = s.myLanguage,
                        onPick = { lang -> c.settings.update { if (lang == it.partnerLanguage) it.copy(myLanguage = lang, partnerLanguage = it.myLanguage) else it.copy(myLanguage = lang) } },
                        onNext = { step = 2 },
                    )
                    2 -> PickLanguage(
                        title = "Who do you want to talk with?",
                        selected = s.partnerLanguage,
                        disabled = s.myLanguage,
                        onPick = { lang -> c.settings.update { it.copy(partnerLanguage = lang) } },
                        onNext = { step = 3 },
                    )
                    else -> OfflineStep(
                        languages = listOf(s.myLanguage, s.partnerLanguage).filterNot { it.builtIn },
                        onDownload = {
                            listOf(s.myLanguage, s.partnerLanguage).filterNot { it.builtIn }.forEach { lang ->
                                c.scope.launch { c.translator.download(lang) }
                            }
                            c.settings.update { it.copy(onboarded = true) }
                            onDone()
                        },
                        onSkip = {
                            c.settings.update { it.copy(onboarded = true) }
                            onDone()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Welcome(onNext: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    TarangLogo(96.dp)
    Text("Tarang", style = MaterialTheme.typography.displaySmall, color = Color.White, modifier = Modifier.padding(top = 20.dp))
    Text("तरंग · Every voice, one wave.", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f))
    Spacer(Modifier.weight(1f))
    Text(
        "Live voice translation for Hindi, Bengali, Marathi, Telugu, Tamil, Gujarati, Urdu, Kannada and English. Free, private and works offline.",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.88f),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onNext,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand.IndigoDeep),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) { Text("Get started") }
}

@Composable
private fun ColumnScope.PickLanguage(title: String, selected: Language, onPick: (Language) -> Unit, onNext: () -> Unit, disabled: Language? = null) {
    Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Language.entries.filter { it != disabled }.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { lang ->
                    val isSel = lang == selected
                    Surface(
                        onClick = { onPick(lang) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSel) Color.White else Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f).border(if (isSel) 0.dp else 1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            LangAvatar(
                                lang, 36.dp,
                                container = if (isSel) Brand.Indigo else Color.White.copy(alpha = 0.18f),
                                content = Color.White,
                            )
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(lang.nativeName, style = MaterialTheme.typography.titleSmall, color = if (isSel) Brand.IndigoDeep else Color.White)
                                Text(lang.englishName, style = MaterialTheme.typography.labelSmall, color = if (isSel) Brand.Indigo else Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    Button(
        onClick = onNext,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand.IndigoDeep),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
    ) { Text("Continue") }
}

@Composable
private fun ColumnScope.OfflineStep(languages: List<Language>, onDownload: () -> Unit, onSkip: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Text("Make it work offline", style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
    Text(
        if (languages.isEmpty()) "English is built in — you're ready to go."
        else "Download ${languages.joinToString(" & ") { it.englishName }} once (~${languages.size * 30} MB) for instant, private translation with no internet. " +
            "Until then Tarang uses a free online engine.",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.88f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 12.dp),
    )
    Spacer(Modifier.weight(1f))
    Text(
        "Tip: for offline speech too, add your languages under Settings → Voice typing languages.",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.size(16.dp))
    Button(
        onClick = onDownload,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand.IndigoDeep),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) { Text(if (languages.isEmpty()) "Start translating" else "Download & start") }
    if (languages.isNotEmpty()) {
        TextButton(onClick = onSkip, modifier = Modifier.padding(top = 4.dp)) { Text("Later", color = Color.White) }
    }
}
