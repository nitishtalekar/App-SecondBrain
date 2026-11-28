package app.secondbrain.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.secondbrain.brain.BrainMap
import app.secondbrain.brain.Clusterer
import app.secondbrain.data.Entry
import app.secondbrain.data.ItemRepository
import app.secondbrain.data.LibraryLayout
import app.secondbrain.data.SettingsRepository
import app.secondbrain.data.TagRulesRepository
import app.secondbrain.data.Accent
import app.secondbrain.data.ThemeMode
import app.secondbrain.search.FeedOrder
import app.secondbrain.search.SearchDoc
import app.secondbrain.search.SearchEngine
import app.secondbrain.search.SearchHit
import app.secondbrain.search.SearchParams
import app.secondbrain.search.StatusFilter
import app.secondbrain.search.TagCount
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

private const val KEEP_ALIVE = 5_000L
private const val FEED_SEED = "feed_seed"

data class AppUiState(
    val loaded: Boolean = false,
    val entries: List<Entry> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.Default,
)

/** Activity-scoped: what several screens share, plus settings. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val items: ItemRepository,
    private val rules: TagRulesRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = combine(items.entries, settings.themeMode, settings.accent) { entries, theme, accent ->
        AppUiState(loaded = true, entries = entries, themeMode = theme, accent = accent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_ALIVE), AppUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun setCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch { items.setCompleted(id, completed) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setAccent(accent: Accent) {
        viewModelScope.launch { settings.setAccent(accent) }
    }

    fun retagAll() {
        viewModelScope.launch {
            val changed = items.retagAll()
            _messages.send(
                when (changed) {
                    0 -> "Tags are already up to date"
                    1 -> "Added tags to 1 item"
                    else -> "Added tags to $changed items"
                },
            )
        }
    }

    suspend fun exportRules(): String = rules.export()

    fun importRules(text: String) {
        viewModelScope.launch {
            if (runCatching { rules.import(text) }.isSuccess) {
                items.retagAll()
                _messages.send("Tag rules imported")
            } else {
                _messages.send("That file isn't a valid tag rules file")
            }
        }
    }
}

data class SearchUiState(
    val loaded: Boolean = false,
    val params: SearchParams = SearchParams(),
    val results: List<SearchHit> = emptyList(),
    /** Tags that complete the word being typed. */
    val suggestions: List<TagCount> = emptyList(),
    val total: Int = 0,
    val layout: LibraryLayout = LibraryLayout.GRID,
)

/** Activity-scoped, so a search survives switching tabs. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    items: ItemRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    /** Picked once per launch, so the feed is mixed differently each time but holds still while in use. */
    private val seed: Long = savedStateHandle[FEED_SEED] ?: Random.nextLong().also { savedStateHandle[FEED_SEED] = it }

    private val params = MutableStateFlow(SearchParams())

    /** The field's text, kept apart from the results so typing never waits on a search. */
    val query: StateFlow<String> = params.map { it.query }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val docs = items.entries.map { entries -> entries.map(::SearchDoc) }

    private val found = combine(docs, params) { all, current ->
        val hits = SearchEngine.search(all, current)
        // Typed words rank by how well they match; otherwise the feed stays mixed, filtered or not.
        val ordered = if (current.query.isBlank()) FeedOrder.shuffle(hits, seed) { it.entry.id } else hits
        SearchUiState(true, current, ordered, SearchEngine.suggestTags(all, current), all.size)
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<SearchUiState> = combine(found, settings.libraryLayout) { state, layout ->
        state.copy(layout = layout)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_ALIVE), SearchUiState())

    fun setLayout(layout: LibraryLayout) {
        viewModelScope.launch { settings.setLibraryLayout(layout) }
    }

    fun setQuery(query: String) = params.update { it.copy(query = query) }
    fun toggleStatus(status: StatusFilter) = params.update { it.copy(status = status.takeIf { s -> s != it.status }) }
    fun toggleType(type: ItemType) = params.update { it.copy(type = type.takeIf { t -> t != it.type }) }
    fun toggleTag(tag: String) = params.update { it.copy(tags = if (tag in it.tags) it.tags - tag else it.tags + tag) }
    /** Swap the word being typed for the suggested [tag]. */
    fun pickSuggestion(tag: String) = params.update {
        it.copy(query = it.query.dropLast(SearchEngine.typedWord(it.query).length), tags = it.tags + tag)
    }

    fun clear() { params.value = SearchParams() }

    /** Start over, showing everything with [tag]. */
    fun showTag(tag: String) { params.value = SearchParams(tags = setOf(tag)) }
}

@HiltViewModel
class BrainViewModel @Inject constructor(items: ItemRepository) : ViewModel() {

    /** Null until the first load, so an empty brain isn't flashed while reading the database. */
    val map: StateFlow<BrainMap?> = items.entries.map { Clusterer.build(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_ALIVE), null)

    private val _selected = MutableStateFlow<String?>(null)
    val selected = _selected.asStateFlow()

    fun select(tag: String?) { _selected.value = tag }
}

data class DetailUiState(
    val entry: Entry? = null,
    /** Other posts from the same capture. */
    val related: List<Entry> = emptyList(),
    val gone: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val items: ItemRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<DetailRoute>().id

    val uiState: StateFlow<DetailUiState> = items.entries.map { all ->
        val entry = all.firstOrNull { it.id == id }
        val group = entry?.item?.groupId
        DetailUiState(
            entry = entry,
            related = if (group == null) emptyList() else all.filter { it.item.groupId == group && it.id != id },
            gone = entry == null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_ALIVE), DetailUiState())

    fun setCompleted(completed: Boolean) = viewModelScope.launch { items.setCompleted(id, completed) }
    fun setAction(action: ItemAction?) = viewModelScope.launch { items.setAction(id, action) }
    fun addTags(names: List<String>) = viewModelScope.launch { items.addTags(id, names) }
    fun removeTag(name: String) = viewModelScope.launch { items.removeTag(id, name) }
    fun retry() = viewModelScope.launch { items.analyse(id) }
    fun save(title: String?, body: String, url: String?) = viewModelScope.launch { items.edit(id, title, body, url) }
    fun delete() = viewModelScope.launch { items.delete(id) }
}
