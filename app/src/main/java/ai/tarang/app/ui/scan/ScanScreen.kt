package ai.tarang.app.ui.scan

import ai.tarang.app.core.Language
import ai.tarang.app.ui.components.LanguagePairBar
import ai.tarang.app.ui.components.LanguagePickerSheet
import ai.tarang.app.ui.components.NoticeBanner
import ai.tarang.app.ui.components.ResultActions
import ai.tarang.app.ui.components.ShowcaseDialog
import ai.tarang.app.ui.components.TarangTopBar
import ai.tarang.app.ui.components.collectAsStateLifecycle
import ai.tarang.app.ui.components.rememberContainer
import ai.tarang.app.ui.theme.TranslationTextStyle
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@Composable
fun ScanScreen(onBack: () -> Unit) {
    val c = rememberContainer()
    val context = LocalContext.current
    val vm: ScanViewModel = viewModel { ScanViewModel(c) }
    val state by vm.state.collectAsStateLifecycle()
    val installed by c.translator.installed.collectAsStateLifecycle()
    val speaking by c.speaker.speaking.collectAsStateLifecycle()
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf<Boolean?>(null) } // true = source
    var showcase by remember { mutableStateOf(false) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingPhoto?.let(Uri::parse)
        if (ok && uri != null) vm.onImage(context, uri)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onImage(context, uri)
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "scans").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // keep only the latest photo
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        pendingPhoto = uri.toString()
        runCatching { takePicture.launch(uri) }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TarangTopBar("Scan & translate", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LanguagePairBar(
                from = state.source,
                to = state.target,
                onFromClick = { picking = true },
                onToClick = { picking = false },
                onSwap = vm::swap,
                fromCaption = "Text in",
                toCaption = "Translate to",
            )

            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val img = state.image
                    if (img != null) {
                        Image(
                            img.asImageBitmap(), "Scanned photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(20.dp)),
                        )
                    } else {
                        Icon(Icons.Rounded.DocumentScanner, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Photograph a sign, menu or notice. Text is read on your phone — nothing is uploaded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                        Button(onClick = ::launchCamera) {
                            Icon(Icons.Rounded.CameraAlt, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Camera")
                        }
                        FilledTonalButton(onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Gallery")
                        }
                    }
                }
            }

            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            AnimatedVisibility(state.error != null) {
                NoticeBanner(state.error.orEmpty(), onDismiss = vm::dismissError)
            }

            if (state.recognized.isNotEmpty()) {
                OutlinedTextField(
                    value = state.recognized,
                    onValueChange = vm::editRecognized,
                    label = { Text("Detected text · edit if needed") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    trailingIcon = { androidx.compose.material3.TextButton(onClick = vm::retranslate) { Text("Translate") } },
                )
            }
            if (state.output.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(state.target.nativeName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(state.output, style = TranslationTextStyle.copy(textDirection = TextDirection.Content), modifier = Modifier.padding(top = 6.dp))
                        ResultActions(state.output, speaking == "scan-output", vm::speak, onFullscreen = { showcase = true })
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding().size(16.dp))
        }
    }

    picking?.let { source ->
        LanguagePickerSheet(
            title = if (source) "Text in the photo" else "Translate to",
            selected = if (source) state.source else state.target,
            installed = installed,
            options = if (source) Language.entries.filter { it.scannable } else Language.entries,
            footnote = if (source) "On-device text reading supports Latin and Devanagari scripts today." else null,
            onDismiss = { picking = null },
            onPick = {
                if (source) vm.setSource(it) else vm.setTarget(it)
                picking = null
            },
        )
    }
    if (showcase) ShowcaseDialog(state.output, state.target.nativeName) { showcase = false }
}
