package app.secondbrain.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.secondbrain.data.ItemRepository
import app.secondbrain.data.TagRulesRepository
import app.secondbrain.ingest.Draft
import app.secondbrain.ingest.DraftBuilder
import app.secondbrain.ingest.ImageStore
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.TagEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddUiState(
    val text: String = "",
    val imagePath: String? = null,
    val importing: Boolean = false,
    val analysing: Boolean = false,
    /** Non-empty while the user is reviewing. */
    val drafts: List<Draft> = emptyList(),
    val saving: Boolean = false,
) {
    val reviewing: Boolean get() = drafts.isNotEmpty()
    val canAnalyse: Boolean get() = !importing && !analysing && (text.isNotBlank() || imagePath != null)
    val savable: List<Draft> get() = drafts.filter { it.canSave }
}

/**
 * Add is two steps: compose, then confirm the tagged posts. Nothing is saved until confirmed. The
 * typed text and attached photo survive the app being killed in the background.
 */
@HiltViewModel
class AddViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val builder: DraftBuilder,
    private val items: ItemRepository,
    private val images: ImageStore,
    private val rules: TagRulesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddUiState(text = saved[TEXT] ?: "", imagePath = saved[IMAGE]))
    val state = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val _shared = Channel<Unit>(Channel.CONFLATED)
    /** Fires when something arrives from another app, so the Add tab can come forward. */
    val shared = _shared.receiveAsFlow()

    private var analysis: Job? = null
    private val retagging = HashMap<String, Job>()

    fun newCaptureTarget() = images.newCaptureTarget()

    fun setText(text: String) {
        saved[TEXT] = text
        _state.update { it.copy(text = text) }
    }

    fun attach(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(importing = true) }
            val path = importImage(uri)
            if (path != null) setImage(path)
            _state.update { it.copy(importing = false) }
        }
    }

    fun removeImage() = setImage(null)

    fun share(text: String, image: Uri?) {
        if (text.isBlank() && image == null) return
        analysis?.cancel()
        _state.update { it.copy(drafts = emptyList(), analysing = false, importing = image != null) }
        setText(text)
        setImage(null)
        _shared.trySend(Unit)
        viewModelScope.launch {
            if (image != null) {
                setImage(importImage(image))
                _state.update { it.copy(importing = false) }
            }
            analyse()
        }
    }

    /** Reads the capture and shows the posts it would become. */
    fun analyse() {
        val current = _state.value
        if (!current.canAnalyse) return
        analysis = viewModelScope.launch {
            _state.update { it.copy(analysing = true) }
            try {
                val drafts = builder.build(current.text, current.imagePath)
                _state.update { it.copy(drafts = drafts, analysing = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(analysing = false) }
                _messages.send("Couldn't read that. Try again.")
            }
        }
    }

    fun cancelAnalysis() {
        analysis?.cancel()
        _state.update { it.copy(analysing = false) }
    }

    /** Leaves review with the capture still in the composer. */
    fun backToCompose() = _state.update { it.copy(drafts = emptyList()) }

    fun toggleTag(key: String, tag: String) = change(key) { it.toggleTag(tag) }
    fun addTags(key: String, names: List<String>) = change(key) { it.addTags(names) }
    fun chooseAction(key: String, action: ItemAction?) = change(key) { it.choose(action) }
    fun setDone(key: String, done: Boolean) = change(key) { it.copy(done = done) }

    /** Corrected text (usually from a photo) gets fresh tags once typing pauses. */
    fun setBody(key: String, body: String) {
        change(key) { it.copy(body = body) }
        retagging[key]?.cancel()
        retagging[key] = viewModelScope.launch {
            delay(RETAG_DELAY)
            val engine: TagEngine = rules.engine()
            change(key) { it.retagged(engine) }
        }
    }

    fun removeDraft(key: String) = _state.update { it.copy(drafts = it.drafts.filter { d -> d.key != key }) }

    /** Empties the composer and throws away anything not saved. */
    fun discard() {
        analysis?.cancel()
        setText("")
        setImage(null)
        _state.value = AddUiState()
    }

    fun confirm() {
        val current = _state.value
        val posts = current.savable
        if (posts.isEmpty() || current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = runCatching {
                // Text edited a moment ago may not have been re-tagged yet.
                val engine = rules.engine()
                items.add(posts.map { draft -> if (retagging[draft.key]?.isActive == true) draft.retagged(engine) else draft })
            }
            if (result.isFailure) {
                _state.update { it.copy(saving = false) }
                _messages.send("Couldn't save that. Try again.")
                return@launch
            }
            // A photo post that was removed leaves its file behind.
            val kept = posts.mapNotNull { it.imagePath }.toSet()
            current.imagePath?.takeIf { it !in kept }?.let(images::delete)
            saved[TEXT] = ""
            saved[IMAGE] = null
            _state.value = AddUiState()
            _messages.send(if (posts.size == 1) "Saved to your brain" else "Saved ${posts.size} items to your brain")
        }
    }

    private suspend fun importImage(uri: Uri): String? = runCatching { images.import(uri) }
        .onFailure { _messages.send("Couldn't open that photo") }
        .getOrNull()

    /** The composer owns its photo file until the post is saved; replacing it deletes the old one. */
    private fun setImage(path: String?) {
        val old = _state.value.imagePath
        if (old != null && old != path) images.delete(old)
        saved[IMAGE] = path
        _state.update { it.copy(imagePath = path) }
    }

    private fun change(key: String, transform: (Draft) -> Draft) = _state.update { state ->
        state.copy(drafts = state.drafts.map { if (it.key == key) transform(it) else it })
    }

    private companion object {
        const val TEXT = "draft_text"
        const val IMAGE = "draft_image"
        const val RETAG_DELAY = 500L
    }
}
