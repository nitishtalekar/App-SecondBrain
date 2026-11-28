package app.secondbrain.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondbrain.search.SearchParams
import app.secondbrain.search.StatusFilter
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.tagLabel
import app.secondbrain.ui.SearchViewModel
import app.secondbrain.ui.components.EmptyState
import app.secondbrain.ui.components.EntryRow
import app.secondbrain.ui.components.RowDivider
import app.secondbrain.ui.components.ScreenHeader
import app.secondbrain.ui.components.SectionLabel
import app.secondbrain.ui.components.icon
import app.secondbrain.ui.theme.Spacing

private fun ItemType.plural() = when (this) {
    ItemType.TEXT -> "Notes"
    ItemType.LINK -> "Links"
    ItemType.IMAGE -> "Photos"
}

/** Words, tags, state and type, all at once. Results appear once there is something to look for. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpen: (Long) -> Unit,
    onComplete: (Long, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val params = state.params
    val list = rememberLazyListState()
    // A changed search starts from the top.
    var shownFor by rememberSaveable { mutableStateOf(params.toString()) }
    LaunchedEffect(params) {
        if (shownFor != params.toString()) {
            shownFor = params.toString()
            list.scrollToItem(0)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = Spacing.contentMaxWidth).fillMaxSize()) {
            ScreenHeader("Library") {
                IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
            }
            TextField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search words, tags, sites, dates") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear text")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter),
            )
            when {
                !state.loaded -> Unit
                state.total == 0 -> EmptyState("Your library is empty", "Everything you save to your brain appears here.")
                else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = PaddingValues(bottom = Spacing.xl)) {
                    if (params.tags.isNotEmpty()) item(key = "selected") {
                        FlowRow(Modifier.padding(horizontal = Spacing.gutter).padding(top = Spacing.s), horizontalArrangement = spacedBy(Spacing.s)) {
                            params.tags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.toggleTag(tag) },
                                    label = { Text(tagLabel(tag)) },
                                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Remove filter", Modifier.size(18.dp)) },
                                )
                            }
                        }
                    }
                    if (state.suggestions.isNotEmpty()) item(key = "suggestions") {
                        Column(Modifier.padding(horizontal = Spacing.gutter).padding(top = 12.dp)) {
                            SectionLabel("Suggested tags")
                            FlowRow(horizontalArrangement = spacedBy(Spacing.s)) {
                                state.suggestions.forEach { tag ->
                                    SuggestionChip(
                                        onClick = { viewModel.pickSuggestion(tag.name) },
                                        label = { Text(tagLabel(tag.name)) },
                                        icon = { Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(18.dp)) },
                                    )
                                }
                            }
                        }
                    }
                    item(key = "filters") {
                        FilterGrid(params, viewModel::toggleStatus, viewModel::toggleType)
                    }
                    if (!params.isEmpty) item(key = "count") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = Spacing.gutter, end = Spacing.s, top = Spacing.m),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val count = state.results.size
                            SectionLabel(
                                if (count == 1) "1 match" else "$count matches",
                                Modifier.weight(1f).padding(vertical = 12.dp),
                            )
                            TextButton(onClick = viewModel::clear) { Text("Clear all") }
                        }
                    }
                    if (!params.isEmpty && state.results.isEmpty()) item(key = "none") {
                        Text(
                            "No matches. Try fewer words or clear a filter.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.l),
                        )
                    }
                    itemsIndexed(state.results, key = { _, hit -> hit.entry.id }) { index, hit ->
                        Column(Modifier.animateItem()) {
                            EntryRow(
                                hit.entry,
                                onClick = { focusManager.clearFocus(); onOpen(hit.entry.id) },
                                onComplete = { onComplete(hit.entry.id, it) },
                                snippet = hit.snippet,
                            )
                            if (index < state.results.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}

private class Filter(val label: String, val icon: ImageVector, val selected: Boolean, val waiting: Boolean, val toggle: () -> Unit)

private fun StatusFilter.icon(): ImageVector = when (this) {
    StatusFilter.DO -> Icons.Rounded.Checklist
    StatusFilter.READ -> Icons.AutoMirrored.Rounded.MenuBook
    StatusFilter.BUY -> Icons.Rounded.ShoppingBag
    StatusFilter.WATCH -> Icons.Rounded.PlayCircle
    StatusFilter.DONE -> Icons.Rounded.CheckCircle
}

/** State and type filters as tiles, four to a row. Waiting states light up in the accent. */
@Composable
private fun FilterGrid(params: SearchParams, onStatus: (StatusFilter) -> Unit, onType: (ItemType) -> Unit) {
    val filters = StatusFilter.entries.map { status ->
        Filter(status.label, status.icon(), params.status == status, status != StatusFilter.DONE) { onStatus(status) }
    } + ItemType.entries.map { type ->
        Filter(type.plural(), type.icon(), params.type == type, false) { onType(type) }
    }
    Column(
        Modifier.padding(horizontal = Spacing.gutter).padding(top = 12.dp),
        verticalArrangement = spacedBy(Spacing.s),
    ) {
        filters.chunked(4).forEach { row ->
            Row(horizontalArrangement = spacedBy(Spacing.s)) {
                row.forEach { FilterTile(it, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FilterTile(filter: Filter, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val container = when {
        !filter.selected -> colors.surfaceContainerLow
        filter.waiting -> colors.tertiaryContainer
        else -> colors.primary
    }
    val content = when {
        !filter.selected -> colors.onSurface
        filter.waiting -> colors.onTertiaryContainer
        else -> colors.onPrimary
    }
    Surface(
        selected = filter.selected,
        onClick = filter.toggle,
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (filter.selected) container else colors.outlineVariant),
        modifier = modifier,
    ) {
        Column(
            Modifier.heightIn(min = 72.dp).padding(horizontal = Spacing.xs, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Icon(filter.icon, contentDescription = null)
            Text(filter.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}
