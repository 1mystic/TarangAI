package ai.tarang.app.engine

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * On-device OCR with ML Kit (models delivered by Google Play services, so the APK stays small).
 * The Devanagari recognizer reads both Devanagari (Hindi, Marathi) and Latin (English) text.
 */
class TextScanner {
    suspend fun read(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        return try {
            val result = recognizer.process(image).await()
            result.textBlocks.joinToString("\n") { block -> block.lines.joinToString(" ") { it.text } }
        } finally {
            recognizer.close()
        }
    }
}
