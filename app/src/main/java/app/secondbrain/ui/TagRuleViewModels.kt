package app.secondbrain.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.secondbrain.data.TagRulesRepository
import app.secondbrain.tagging.RuleList
import app.secondbrain.tagging.RuleMatch
import app.secondbrain.tagging.TagDef
import app.secondbrain.tagging.TagRules
import app.secondbrain.tagging.canMatch
import app.secondbrain.tagging.cleanEntries
import app.secondbrain.tagging.entries
import app.secondbrain.tagging.find
import app.secondbrain.tagging.normalizeTag
import app.secondbrain.tagging.tryRule
import app.secondbrain.tagging.withEntries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagRow(
    val name: String,
    val def: TagDef,
    /** Made by the user rather than shipped with the app. */
    val own: Boolean,
    val edited: Boolean,
)

data class TagRulesUiState(val query: String = "", val rows: List<TagRow> = emptyList(), val loaded: Boolean = false)

@HiltViewModel
class TagRulesViewModel @Inject constructor(private val repository: TagRulesRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    private val all = repository.revision.map {
        val builtIn = repository.builtIn()
        repository.rules().tags.map { def ->
            val name = normalizeTag(def.tag)
            val shipped = builtIn.find(name)
            TagRow(name, def, own = shipped == null, edited = shipped != null && shipped != def)
        }.sortedBy { it.name }
    }

    val uiState: StateFlow<TagRulesUiState> = combine(all, query) { rows, typed ->
        val needle = typed.trim().lowercase()
        TagRulesUiState(typed, rows.filter { needle in it.name }, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), TagRulesUiState())

    fun setQuery(value: String) {
        query.value = value
    }
}

data class TagRuleUiState(
    val isNew: Boolean = true,
    /** Null until loaded. */
    val draft: TagDef? = null,
    /** What is stored now; null for a tag not yet saved. */
    val stored: TagDef? = null,
    /** How the app shipped it; null for the user's own tags. */
    val shipped: TagDef? = null,
    val nameInput: String = "",
    val nameTaken: Boolean = false,
    val sample: String = "",
    val match: RuleMatch? = null,
    val saved: Boolean = false,
    val closed: Boolean = false,
) {
    val own: Boolean get() = shipped == null
    val changed: Boolean get() = if (isNew) nameInput.isNotBlank() || draft?.canMatch == true else draft != stored
    val canSave: Boolean
        get() {
            val def = draft ?: return false
            return if (isNew) normalizeTag(nameInput).isNotEmpty() && !nameTaken && def.canMatch else def != stored
        }
}

/** One tag being edited. Nothing is written until [save]. */
@HiltViewModel
class TagRuleViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val repository: TagRulesRepository,
) : ViewModel() {

    private val name = handle.toRoute<TagRuleRoute>().name
    private var rules = TagRules()

    private val _state = MutableStateFlow(TagRuleUiState(isNew = name.isEmpty()))
    val state: StateFlow<TagRuleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val builtIn = repository.builtIn()
            rules = repository.rules()
            val stored = rules.find(name).takeIf { name.isNotEmpty() }
            // An unsaved edit survives the app being closed behind the user's back.
            val kept = handle.get<String>(DRAFT)?.let { runCatching { TagRules.parse(it).tags.single() }.getOrNull() }
            val typed = handle.get<String>(NAME).orEmpty()
            val draft = kept ?: stored ?: TagDef(name)
            _state.update {
                it.copy(draft = draft, stored = stored, shipped = builtIn.find(name), nameInput = typed, nameTaken = taken(typed))
            }
            if (name.isNotEmpty() && stored == null) _state.update { it.copy(closed = true) }
        }
    }

    private fun taken(typed: String) = name.isEmpty() && rules.find(normalizeTag(typed)) != null

    fun setName(value: String) {
        handle[NAME] = value
        _state.update { it.copy(nameInput = value, nameTaken = taken(value)) }
        edit { it.copy(tag = normalizeTag(value)) }
    }

    fun add(list: RuleList, raw: String) = edit { def ->
        val added = cleanEntries(list, raw).filter { list != RuleList.IMPLIES || it != normalizeTag(def.tag) }
        def.withEntries(list, (def.entries(list) + added).distinct())
    }

    fun remove(list: RuleList, value: String) = edit { it.withEntries(list, it.entries(list) - value) }

    fun setOn(on: Boolean) = edit { it.copy(off = !on) }

    fun setSample(value: String) {
        _state.update { it.copy(sample = value) }
        tryOut()
    }

    private fun edit(transform: (TagDef) -> TagDef) {
        val next = transform(_state.value.draft ?: return)
        handle[DRAFT] = TagRules(tags = listOf(next)).encode()
        _state.update { it.copy(draft = next) }
        tryOut()
    }

    private fun tryOut() = _state.update { state ->
        val def = state.draft ?: return@update state
        // A new tag has no name yet; any placeholder lets its rules be tried.
        val named = if (def.tag.isEmpty()) def.copy(tag = UNNAMED) else def
        state.copy(match = rules.tryRule(named, state.sample))
    }

    fun save() {
        val state = _state.value
        val def = state.draft ?: return
        if (!state.canSave) return
        viewModelScope.launch {
            repository.save(def)
            _state.update { it.copy(stored = def, saved = true) }
        }
    }

    /** Back to how it shipped. */
    fun reset() {
        val shipped = _state.value.shipped ?: return
        viewModelScope.launch {
            repository.remove(name)
            handle.remove<String>(DRAFT)
            _state.update { it.copy(draft = shipped, stored = shipped, saved = true) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.remove(name)
            _state.update { it.copy(closed = true) }
        }
    }

    private companion object {
        const val DRAFT = "draft_rule"
        const val NAME = "draft_name"
        const val UNNAMED = "new tag"
    }
}
