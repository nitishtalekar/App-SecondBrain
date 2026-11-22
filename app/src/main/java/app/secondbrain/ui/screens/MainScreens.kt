package app.secondbrain.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.ui.AppUiState
import app.secondbrain.ui.SearchViewModel
import app.secondbrain.ui.components.CategoryDot
import app.secondbrain.ui.components.CategoryLabel
import app.secondbrain.ui.components.EmptyState
import app.secondbrain.ui.components.ItemList
import app.secondbrain.ui.components.RowDivider
import app.secondbrain.ui.components.ScreenTitle
import app.secondbrain.ui.theme.Spacing
import app.secondbrain.ui.theme.parseHexColor
import androidx.hilt.navigation.compose.hiltViewModel

/** Maps an item to the label shown beside it. Inbox (and pending) items map to null. */
@Composable
fun rememberLabeler(state: AppUiState): (ItemEntity) -> CategoryLabel? {
    val byId = remember(state.categories) {
        state.categories.associate { it.id to CategoryLabel(it.name, parseHexColor(it.color)) }
    }
    return remember(byId) { { item -> item.categoryId?.let(byId::get) } }
}

@Composable
fun RecentScreen(
    state: AppUiState,
    onOpen: (Long) -> Unit,
    onOpenInbox: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val labeler = rememberLabeler(state)
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Recent") {
            IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
        }
        when {
            !state.loaded -> Unit
            state.items.isEmpty() -> EmptyState(
                "Nothing here yet",
                "Tap + to save a thought, a link or a photo. It sorts itself.",
            )
            else -> ItemList(
                items = state.items,
                labelFor = labeler,
                onOpen = onOpen,
                header = if (state.inboxCount > 0) {
                    { item(key = "inbox-banner") { InboxBanner(state.inboxCount, onOpenInbox) } }
                } else null,
            )
        }
    }
}

@Composable
private fun InboxBanner(count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter, vertical = Spacing.xs)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(Spacing.s),
    ) {
        CategoryDot(null)
        Text(
            if (count == 1) "1 item in Inbox needs a look" else "$count items in Inbox need a look",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun LibraryScreen(
    state: AppUiState,
    onOpenCollection: (String?) -> Unit,
    onAddCategory: (String) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Library")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = Spacing.contentMaxWidth).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                item(key = "inbox") {
                    CollectionRow("Inbox", null, state.inboxCount, onClick = { onOpenCollection(null) })
                    RowDivider()
                }
                items(state.categories.size, key = { state.categories[it].id }) { index ->
                    val category = state.categories[index]
                    CollectionRow(category.name, parseHexColor(category.color), state.counts[category.id] ?: 0) {
                        onOpenCollection(category.id)
                    }
                    RowDivider()
                }
                item(key = "add") {
                    TextButton(
                        onClick = { adding = true },
                        modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.s),
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Text("New category", Modifier.padding(start = Spacing.s))
                    }
                }
            }
        }
    }
    if (adding) {
        NameDialog(
            title = "New category",
            confirm = "Create",
            initial = "",
            onDismiss = { adding = false },
            onConfirm = { onAddCategory(it); adding = false },
        )
    }
}

@Composable
private fun CollectionRow(name: String, color: androidx.compose.ui.graphics.Color?, count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = Spacing.gutter, vertical = Spacing.s)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(14.dp),
    ) {
        CategoryDot(color, size = 10)
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(count.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SearchScreen(state: AppUiState, onOpen: (Long) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val labeler = rememberLabeler(state)
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Search")
        TextField(
            value = query,
            onValueChange = { viewModel.query.value = it },
            placeholder = { Text("Notes, links, text in photos") },
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
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .focusRequester(focus),
        )
        Box(Modifier.weight(1f)) {
            when {
                query.isBlank() -> EmptyState("Find anything", "Searches titles, notes, links, and the text read from your photos.")
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
