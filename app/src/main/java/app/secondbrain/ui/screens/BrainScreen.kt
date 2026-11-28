package app.secondbrain.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondbrain.brain.Cluster
import app.secondbrain.brain.Clusterer
import app.secondbrain.search.SearchEngine
import app.secondbrain.tagging.tagLabel
import app.secondbrain.ui.BrainViewModel
import app.secondbrain.ui.components.EntryRow
import app.secondbrain.ui.components.RowDivider
import app.secondbrain.ui.components.ScreenHeader
import app.secondbrain.ui.components.SettingsButton
import app.secondbrain.ui.theme.Spacing
import app.secondbrain.ui.theme.clusterColor

@Composable
fun BrainScreen(
    viewModel: BrainViewModel,
    onOpen: (Long) -> Unit,
    onComplete: (Long, Boolean) -> Unit,
    onSearchTag: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val loaded by viewModel.map.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val globe = rememberGlobeState()
    val map = loaded ?: return
    val cluster = map.clusters.firstOrNull { it.tag == selected }

    // A cluster can vanish when its last item is deleted or retagged.
    LaunchedEffect(map, selected) { if (selected != null && cluster == null) viewModel.select(null) }
    BackHandler(enabled = cluster != null) { viewModel.select(null) }

    val things = map.clusters.sumOf { it.entries.size }
    val subtitle = when {
        things == 0 -> null
        else -> "${if (things == 1) "1 item" else "$things items"} in " +
            if (map.clusters.size == 1) "1 cluster" else "${map.clusters.size} clusters"
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sideBySide = maxWidth > maxHeight
        val sphere: @Composable (Modifier) -> Unit = { modifier ->
            BrainGlobe(map, cluster?.tag, globe, viewModel::select, onOpen, modifier)
        }
        val panel: @Composable (Cluster, Modifier) -> Unit = { shown, modifier ->
            ClusterPanel(
                cluster = shown,
                onClose = { viewModel.select(null) },
                onOpen = onOpen,
                onComplete = onComplete,
                onSearchTag = onSearchTag,
                sideBySide = sideBySide,
                modifier = modifier,
            )
        }

        val header: @Composable () -> Unit = {
            ScreenHeader("Brain", subtitle = subtitle) { SettingsButton(onOpenSettings) }
        }

        if (sideBySide) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    header()
                    sphere(Modifier.weight(1f).fillMaxWidth())
                    if (cluster == null) ClusterRail(map.clusters, viewModel::select)
                }
                if (cluster != null) panel(cluster, Modifier.width(360.dp).fillMaxHeight())
            }
        } else {
            val panelHeight = maxHeight * 0.52f
            Column(Modifier.fillMaxSize()) {
                header()
                sphere(Modifier.weight(1f).fillMaxWidth())
                if (cluster == null) ClusterRail(map.clusters, viewModel::select)
                else panel(cluster, Modifier.fillMaxWidth().height(panelHeight))
            }
        }

        if (things == 0) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.align(Alignment.Center).padding(Spacing.xl),
            ) {
                Column(
                    Modifier.padding(Spacing.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = spacedBy(Spacing.s),
                ) {
                    Text("Your brain is empty", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Save a few items and they gather into clusters here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = onAdd) { Text("Add to brain") }
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Int) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(clusterColor(color)))
}

/** Every cluster by name: the quick way around, and the accessible one. */
@Composable
private fun ClusterRail(clusters: List<Cluster>, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.gutter, vertical = Spacing.s),
        horizontalArrangement = spacedBy(Spacing.s),
    ) {
        items(clusters, key = { it.tag }) { cluster ->
            AssistChip(
                onClick = { onSelect(cluster.tag) },
                label = { Text("${tagLabel(cluster.tag)}  ${cluster.entries.size}") },
                leadingIcon = { Dot(cluster.color) },
            )
        }
    }
}

@Composable
private fun ClusterPanel(
    cluster: Cluster,
    onClose: () -> Unit,
    onOpen: (Long) -> Unit,
    onComplete: (Long, Boolean) -> Unit,
    onSearchTag: (String) -> Unit,
    sideBySide: Boolean,
    modifier: Modifier = Modifier,
) {
    var within by remember(cluster.tag) { mutableStateOf<String?>(null) }
    val subTags = remember(cluster) {
        SearchEngine.tagCounts(cluster.entries, exclude = setOf(cluster.tag))
            .filter { it.count in 2 until cluster.entries.size }
            .take(12)
    }
    val filter = within?.takeIf { name -> subTags.any { it.name == name } }
    val shown = if (filter == null) cluster.entries else cluster.entries.filter { e -> e.tags.any { it.name == filter } }
    val loose = cluster.tag == Clusterer.LOOSE

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = if (sideBySide) RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
        else RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier,
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = Spacing.gutter, end = Spacing.s, top = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = spacedBy(12.dp),
            ) {
                Dot(cluster.color)
                Text(
                    tagLabel(cluster.tag),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (!loose) {
                    IconButton(onClick = { onSearchTag(filter ?: cluster.tag) }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Show in Library")
                    }
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close cluster") }
            }
            if (subTags.isNotEmpty()) {
                LazyRow(contentPadding = PaddingValues(horizontal = Spacing.gutter), horizontalArrangement = spacedBy(Spacing.s)) {
                    items(subTags, key = { it.name }) { tag ->
                        FilterChip(
                            selected = filter == tag.name,
                            onClick = { within = tag.name.takeIf { filter != it } },
                            label = { Text("${tagLabel(tag.name)}  ${tag.count}") },
                        )
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.m)) {
                itemsIndexed(shown, key = { _, entry -> entry.id }) { index, entry ->
                    Column(Modifier.animateItem()) {
                        EntryRow(entry, onClick = { onOpen(entry.id) }, onComplete = { onComplete(entry.id, it) })
                        if (index < shown.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
