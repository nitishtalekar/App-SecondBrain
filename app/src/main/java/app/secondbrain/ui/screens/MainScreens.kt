package app.secondbrain.ui.screens

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondbrain.data.children
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.pathName
import app.secondbrain.ui.AppUiState
import app.secondbrain.ui.SearchViewModel
import app.secondbrain.ui.components.CategoryLabel
import app.secondbrain.ui.components.EmptyState
import app.secondbrain.ui.components.FolderEntry
import app.secondbrain.ui.components.ItemList
import app.secondbrain.ui.components.NewFolderTile
import app.secondbrain.ui.components.ScreenTitle
import app.secondbrain.ui.components.folderRows
import app.secondbrain.ui.theme.Spacing
import app.secondbrain.ui.theme.parseHexColor

/** Maps an item to the label shown beside it (the full folder path). Inbox and pending items map to null. */
@Composable
fun rememberLabeler(state: AppUiState): (ItemEntity) -> CategoryLabel? {
    val byId = remember(state.categories) {
        state.categories.associate { it.id to CategoryLabel(state.categories.pathName(it.id) ?: it.name, parseHexColor(it.color)) }
    }
    return remember(byId) { { item -> item.categoryId?.let(byId::get) } }
}

/** The landing screen: the brain, sorted into folders. Recent items are one tap away, not in your face. */
@Composable
fun HomeScreen(
    state: AppUiState,
    onOpenFolder: (String?) -> Unit,
    onOpenRecent: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddFolder: (String) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val top = state.categories.children(null)

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = Spacing.gutter, end = Spacing.s, top = Spacing.m, bottom = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Brain",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                if (state.loaded) {
                    val total = state.items.size
                    Text(
                        if (total == 1) "1 thing kept" else "$total things kept",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onOpenRecent) { Icon(Icons.Rounded.History, contentDescription = "Recent") }
            IconButton(onClick = onOpenSearch) { Icon(Icons.Rounded.Search, contentDescription = "Search") }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = Spacing.contentMaxWidth).fillMaxSize(),
                contentPadding = PaddingValues(top = Spacing.s, bottom = 112.dp),
            ) {
                val folders = buildList {
                    if (state.inboxCount > 0) {
                        add(FolderEntry("inbox", "Inbox", null, state.inboxCount, emphasised = true) { onOpenFolder(null) })
                    }
                    top.forEach { c ->
                        add(FolderEntry(c.id, c.name, parseHexColor(c.color), state.totals[c.id] ?: 0) { onOpenFolder(c.id) })
                    }
                }
                folderRows(folders) { m -> NewFolderTile("New folder", { adding = true }, m) }
            }
        }
    }
    if (adding) {
        NameDialog("New folder", "Create", "", onDismiss = { adding = false }) { onAddFolder(it); adding = false }
    }
}

/** Newest first. Deliberately off the front page. */
@Composable
fun RecentScreen(state: AppUiState, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val labeler = rememberLabeler(state)
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Recent", onBack = onBack)
        when {
            !state.loaded -> Unit
            state.items.isEmpty() -> EmptyState("Nothing here yet", "Tap + to save a thought, a link or a photo. It sorts itself.")
            else -> ItemList(items = state.items, labelFor = labeler, onOpen = onOpen)
        }
    }
}

@Composable
fun SearchScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    initialQuery: String? = null,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val labeler = rememberLabeler(state)
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(initialQuery) {
        if (initialQuery != null) viewModel.query.value = initialQuery else focus.requestFocus()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Search", onBack = onBack)
        TextField(
            value = query,
            onValueChange = { viewModel.query.value = it },
            placeholder = { Text("Notes, links, tags, text in photos") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { viewModel.query.value = "" }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = MaterialTheme.shapes.large,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .focusRequester(focus),
        )
        Box(Modifier.weight(1f)) {
            when {
                query.isBlank() -> EmptyState("Find anything", "Searches titles, notes, links, tags, and the text read from your photos.")
                results.isEmpty() -> EmptyState("No matches", "Nothing saved contains “${query.trim()}”.")
                else -> ItemList(
                    items = results,
                    labelFor = labeler,
                    onOpen = onOpen,
                    snippetFor = { snippetFor(it, query) },
                    contentPadding = PaddingValues(top = Spacing.s, bottom = 96.dp),
                )
            }
        }
    }
}

/** Shows where the match was found when it isn't already visible in the title. */
fun snippetFor(item: ItemEntity, query: String): String? {
    val term = query.trim().lowercase().split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotEmpty() } ?: return null
    val sources = listOfNotNull(item.body, item.ocrText, item.linkDescription, item.url)
    for (text in sources) {
        val index = text.lowercase().indexOf(term)
        if (index < 0) continue
        val start = (index - 28).coerceAtLeast(0)
        val end = (index + term.length + 56).coerceAtMost(text.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        return prefix + text.substring(start, end).replace('\n', ' ').trim() + suffix
    }
    return null
}

@Composable
fun NameDialog(title: String, confirm: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Name") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
