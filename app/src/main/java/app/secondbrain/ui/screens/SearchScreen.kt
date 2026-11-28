package app.secondbrain.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondbrain.data.LibraryLayout
import app.secondbrain.search.SearchParams
import app.secondbrain.search.StatusFilter
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.tagLabel
import app.secondbrain.ui.SearchUiState
import app.secondbrain.ui.SearchViewModel
import app.secondbrain.ui.components.EmptyState
import app.secondbrain.ui.components.EntryRow
import app.secondbrain.ui.components.FeedCard
import app.secondbrain.ui.components.RowDivider
import app.secondbrain.ui.components.ScreenHeader
import app.secondbrain.ui.components.SectionLabel
import app.secondbrain.ui.components.SettingsButton
import app.secondbrain.ui.components.icon
import app.secondbrain.ui.theme.Spacing

private fun ItemType.plural() = when (this) {
    ItemType.TEXT -> "Notes"
    ItemType.LINK -> "Links"
    ItemType.IMAGE -> "Photos"
}

/** Everything saved, as a mixed feed or a list, narrowed by words, tags, state and type. */
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
    val grid = rememberLazyStaggeredGridState()
    // A changed search starts from the top.
    var shownFor by rememberSaveable { mutableStateOf(params.toString()) }
    LaunchedEffect(params) {
        if (shownFor != params.toString()) {
            shownFor = params.toString()
            list.scrollToItem(0)
            grid.scrollToItem(0)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = Spacing.contentMaxWidth).fillMaxSize()) {
            ScreenHeader("Library") {
                val asGrid = state.layout == LibraryLayout.GRID
                IconButton(onClick = { viewModel.setLayout(if (asGrid) LibraryLayout.LIST else LibraryLayout.GRID) }) {
                    Icon(
                        if (asGrid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.Dashboard,
                        contentDescription = if (asGrid) "Show as list" else "Show as grid",
                    )
                }
                SettingsButton(onOpenSettings)
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
            val open: (Long) -> Unit = { id -> focusManager.clearFocus(); onOpen(id) }
            when {
                !state.loaded -> Unit
                state.total == 0 -> EmptyState("Your library is empty", "Everything you save to your brain appears here.")
                state.layout == LibraryLayout.GRID -> LazyVerticalStaggeredGrid(
                    // Two columns on a phone, more as the screen widens.
                    columns = StaggeredGridCells.Adaptive(148.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = grid,
                    contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.xl),
                    horizontalArrangement = spacedBy(Spacing.s),
                    verticalItemSpacing = Spacing.s,
                ) {
                    item(key = "header", span = StaggeredGridItemSpan.FullLine) {
                        // The header brings its own gutters, so let it out of the grid's.
                        SearchHeader(state, viewModel, Modifier.bleed(Spacing.gutter))
                    }
                    items(state.results, key = { it.entry.id }) { hit ->
                        FeedCard(
                            hit.entry,
                            onClick = { open(hit.entry.id) },
                            onComplete = { onComplete(hit.entry.id, it) },
                            modifier = Modifier.animateItem(),
                            snippet = hit.snippet,
                        )
                    }
                }
                else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = PaddingValues(bottom = Spacing.xl)) {
                    item(key = "header") { SearchHeader(state, viewModel) }
                    itemsIndexed(state.results, key = { _, hit -> hit.entry.id }) { index, hit ->
                        Column(Modifier.animateItem()) {
                            EntryRow(
                                hit.entry,
                                onClick = { open(hit.entry.id) },
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

/** Widens a child past its parent's side padding by [by] on each side. */
private fun Modifier.bleed(by: Dp) = layout { measurable, constraints ->
    val extra = by.roundToPx() * 2
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

/** Everything above the items: chosen and suggested tags, the filter tiles and the match count. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHeader(state: SearchUiState, viewModel: SearchViewModel, modifier: Modifier = Modifier) {
    val params = state.params
    Column(modifier.fillMaxWidth()) {
        if (params.tags.isNotEmpty()) {
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
        if (state.suggestions.isNotEmpty()) {
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
        FilterGrid(params, viewModel::toggleStatus, viewModel::toggleType)
        if (params.isEmpty) {
            // Space between the tiles and the first item.
            Spacer(Modifier.height(Spacing.m))
        } else {
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
            if (state.results.isEmpty()) {
                Text(
                    "No matches. Try fewer words or clear a filter.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.l),
                )
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
