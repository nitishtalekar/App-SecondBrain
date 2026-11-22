package app.secondbrain.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.secondbrain.classify.CategoryConfig
import app.secondbrain.data.CategoryRepository
import app.secondbrain.data.ItemRepository
import app.secondbrain.data.SettingsRepository
import app.secondbrain.data.ThemeMode
import app.secondbrain.data.Suggestion
import app.secondbrain.data.db.CategoryEntity
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.ingest.ImageStore
import app.secondbrain.ingest.suggestions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUiState(
    val loaded: Boolean = false,
    val items: List<ItemEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val counts: Map<String?, Int> = emptyMap(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val inboxCount: Int get() = counts[null] ?: 0
    fun categoryName(id: String?): String? = categories.firstOrNull { it.id == id }?.name
}

/** Activity-scoped: the data every top-level screen shares, plus capture. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val items: ItemRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val imageStore: ImageStore,
) : ViewModel() {

    fun newCaptureTarget() = imageStore.newCaptureTarget()

    val uiState: StateFlow<AppUiState> = combine(
        items.observeAll(),
        categories.categories,
        items.observeCounts(),
        settings.themeMode,
    ) { all, cats, counts, theme ->
        AppUiState(
            loaded = true,
            items = all,
            categories = cats,
            counts = counts.associate { it.categoryId to it.count },
            themeMode = theme,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun save(text: String, image: Uri?) {
        viewModelScope.launch {
            val id = runCatching { items.saveText(text, image) }
            when {
                id.isFailure -> _messages.send("Couldn't save that. Try again.")
                id.getOrNull() == null -> Unit
                else -> _messages.send("Saved")
            }
        }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { categories.create(name); items.reclassifyAutoSorted() }
    }

    fun renameCategory(id: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { categories.rename(id, name) }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            categories.delete(id)
            items.reclassifyAutoSorted()
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun resort() {
        viewModelScope.launch {
            items.reclassifyAutoSorted()
            _messages.send("Re-sorted")
        }
    }

    suspend fun exportRules(): String = categories.exportConfig().encode()

    fun importRules(text: String) {
        viewModelScope.launch {
            val result = runCatching { categories.applyConfig(CategoryConfig.parse(text)) }
            if (result.isSuccess) {
                items.reclassifyAutoSorted()
                _messages.send("Categories imported")
            } else {
                _messages.send("That file isn't a valid categories file")
            }
        }
    }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(private val items: ItemRepository) : ViewModel() {
    val query = MutableStateFlow("")

    val results: StateFlow<List<ItemEntity>> = query
        .debounce(150)
        .flatMapLatest { if (it.isBlank()) emptyFlow() else items.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: ItemRepository,
) : ViewModel() {
    val categoryId: String? = savedStateHandle.toRoute<CollectionRoute>().categoryId

    val items: StateFlow<List<ItemEntity>?> = (
        if (categoryId == null) repository.observeInbox() else repository.observeCategory(categoryId)
        ).map<List<ItemEntity>, List<ItemEntity>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class DetailUiState(
    val item: ItemEntity? = null,
    val suggestions: List<Suggestion> = emptyList(),
    val deleted: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val items: ItemRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<DetailRoute>().id
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> = combine(items.observe(id), deleted) { item, gone ->
        DetailUiState(item, item?.suggestions().orEmpty(), gone || item == null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun setCategory(categoryId: String?) = viewModelScope.launch { items.setCategory(id, categoryId) }
    fun autoSort() = viewModelScope.launch { items.unlockCategory(id) }
    fun retry() = viewModelScope.launch { items.retry(id) }
    fun save(title: String?, body: String, url: String?) = viewModelScope.launch { items.updateContent(id, title, body, url) }
    fun delete() = viewModelScope.launch { deleted.value = true; items.delete(id) }
}
