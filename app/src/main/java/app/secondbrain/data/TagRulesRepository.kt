package app.secondbrain.data

import android.content.Context
import app.secondbrain.tagging.TagDef
import app.secondbrain.tagging.TagEngine
import app.secondbrain.tagging.TagRules
import app.secondbrain.tagging.withSaved
import app.secondbrain.tagging.without
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Built-in tag rules from assets, overlaid with whatever the user has imported. */
@Singleton
class TagRulesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val custom get() = File(context.filesDir, CUSTOM_FILE)
    private val mutex = Mutex()
    private var cached: TagEngine? = null

    private val _revision = MutableStateFlow(0)

    /** Goes up whenever the rules change, so screens showing them can reload. */
    val revision: StateFlow<Int> = _revision.asStateFlow()

    /** The rules that shipped with the app, without the user's changes. */
    suspend fun builtIn(): TagRules = withContext(Dispatchers.IO) {
        TagRules.parse(context.assets.open(DEFAULTS_ASSET).bufferedReader().use { it.readText() })
    }

    suspend fun rules(): TagRules {
        val builtIn = builtIn()
        val own = withContext(Dispatchers.IO) { own() }
        return if (own != null) builtIn.merge(own) else builtIn
    }

    suspend fun engine(): TagEngine = mutex.withLock { cached ?: TagEngine(rules()).also { cached = it } }

    /** Upserts by tag name. Throws if [text] isn't a rules file. */
    suspend fun import(text: String) {
        val incoming = TagRules.parse(text)
        require(incoming.tags.isNotEmpty()) { "No tags in this file" }
        change { existing -> existing.merge(incoming) }
    }

    /** Adds or replaces one tag. Saving a built-in tag unchanged forgets the user's version. */
    suspend fun save(def: TagDef) {
        val builtIn = builtIn()
        change { it.withSaved(def, builtIn) }
    }

    /** Drops the user's version of a tag: built-in ones return to how they shipped, others go. */
    suspend fun remove(name: String) = change { it.without(name) }

    private fun own(): TagRules? = custom.takeIf { it.exists() }?.let { runCatching { TagRules.parse(it.readText()) }.getOrNull() }

    private suspend fun change(transform: (TagRules) -> TagRules) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val next = transform(own() ?: TagRules())
                // Written beside the file and moved over it, so a crash can't leave half a file.
                val draft = File(custom.path + ".tmp")
                draft.writeText(next.encode())
                check(draft.renameTo(custom)) { "Couldn't save the tag rules" }
            }
            cached = null
        }
        _revision.update { it + 1 }
    }

    suspend fun export(): String = rules().encode()

    private companion object {
        const val DEFAULTS_ASSET = "tags.json"
        const val CUSTOM_FILE = "tags-custom.json"
    }
}
