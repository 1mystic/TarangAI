package ai.tarang.app.core

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class Mode(val label: String) { CONVERSE("Conversation"), LIVE("Live"), TEXT("Text"), SCAN("Scan"), PHRASE("Phrase") }

data class HistoryEntry(
    val id: Long,
    val source: String,
    val translated: String,
    val from: Language,
    val to: Language,
    val mode: Mode,
    val time: Long,
    val favorite: Boolean = false,
)

/** Translation history persisted as a small JSON file. Favourites are never trimmed. */
class HistoryStore(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "history.json")
    private val mutex = Mutex()
    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) { _entries.value = read() }
    }

    fun add(source: String, translated: String, from: Language, to: Language, mode: Mode, favorite: Boolean = false): Long {
        val clean = source.trim()
        if (clean.isEmpty() || translated.isBlank()) return -1
        val existing = _entries.value.firstOrNull { it.source == clean && it.from == from && it.to == to }
        if (existing != null) {
            // Bump an identical translation to the top instead of duplicating it.
            mutate { list -> listOf(existing.copy(time = System.currentTimeMillis(), favorite = existing.favorite || favorite)) + list.filterNot { it.id == existing.id } }
            return existing.id
        }
        val entry = HistoryEntry(System.nanoTime(), clean, translated.trim(), from, to, mode, System.currentTimeMillis(), favorite)
        mutate { list ->
            val next = listOf(entry) + list
            val favs = next.filter { it.favorite }
            val rest = next.filterNot { it.favorite }.take(MAX_ENTRIES)
            (favs + rest).sortedByDescending { it.time }
        }
        return entry.id
    }

    fun toggleFavorite(id: Long) = mutate { list -> list.map { if (it.id == id) it.copy(favorite = !it.favorite) else it } }
    fun delete(id: Long) = mutate { list -> list.filterNot { it.id == id } }
    fun clear(keepFavorites: Boolean) = mutate { list -> if (keepFavorites) list.filter { it.favorite } else emptyList() }

    private fun mutate(transform: (List<HistoryEntry>) -> List<HistoryEntry>) {
        _entries.update(transform)
        val snapshot = _entries.value
        scope.launch(Dispatchers.IO) { mutex.withLock { write(snapshot) } }
    }

    private fun read(): List<HistoryEntry> = runCatching {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            HistoryEntry(
                id = o.getLong("id"),
                source = o.getString("s"),
                translated = o.getString("t"),
                from = Language.fromCode(o.getString("f")) ?: return@mapNotNull null,
                to = Language.fromCode(o.getString("to")) ?: return@mapNotNull null,
                mode = Mode.entries.firstOrNull { it.name == o.optString("m") } ?: Mode.TEXT,
                time = o.getLong("time"),
                favorite = o.optBoolean("fav"),
            )
        }
    }.getOrDefault(emptyList())

    private fun write(list: List<HistoryEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id).put("s", it.source).put("t", it.translated)
                    .put("f", it.from.code).put("to", it.to.code).put("m", it.mode.name)
                    .put("time", it.time).put("fav", it.favorite)
            )
        }
        val tmp = File(file.parentFile, "history.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }

    private companion object { const val MAX_ENTRIES = 200 }
}
