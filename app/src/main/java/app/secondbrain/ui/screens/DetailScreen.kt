package app.secondbrain.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondbrain.data.db.CategoryEntity
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.db.ItemType
import app.secondbrain.data.db.ProcessingStatus
import app.secondbrain.data.displayTitle
import app.secondbrain.data.flattened
import app.secondbrain.data.pathName
import app.secondbrain.data.tagList
import app.secondbrain.data.normalizeTag
import app.secondbrain.data.labelList
import app.secondbrain.ui.AppUiState
import app.secondbrain.ui.DetailViewModel
import app.secondbrain.ui.components.CategoryDot
import app.secondbrain.ui.theme.Spacing
import app.secondbrain.ui.theme.parseHexColor
import coil.compose.AsyncImage
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onOpenTag: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(ui.deleted) { if (ui.deleted) onBack() }
    val item = ui.item ?: return

    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable(editing) { mutableStateOf(item.title.orEmpty()) }
    var body by rememberSaveable(editing) { mutableStateOf(item.body) }
    var url by rememberSaveable(editing) { mutableStateOf(item.url.orEmpty()) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = { if (editing) editing = false else onBack() }) {
                    if (editing) Icon(Icons.Rounded.Close, contentDescription = "Discard changes")
                    else Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (editing) {
                    IconButton(onClick = { viewModel.save(title, body, url.takeIf { item.type == ItemType.LINK }); editing = false }) {
                        Icon(Icons.Rounded.Check, contentDescription = "Save changes")
                    }
                } else {
                    IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete") }
                }
            },
            windowInsets = WindowInsets(0.dp),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = Spacing.contentMaxWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter)
                    .padding(bottom = Spacing.xl),
                verticalArrangement = spacedBy(Spacing.l),
            ) {
                if (editing) {
                    EditFields(item, title, body, url, { title = it }, { body = it }, { url = it })
                } else {
                    Content(item)
                }
                if (!editing) {
                    ProcessingNote(item, onRetry = viewModel::retry)
                    CategorySection(
                        item = item,
                        categories = state.categories,
                        suggestions = ui.suggestions.map { it.categoryId },
                        onPick = viewModel::setCategory,
                        onAuto = viewModel::autoSort,
                        onMore = { picking = true },
                    )
                    TagSection(item, onChange = viewModel::setTags, onOpenTag = onOpenTag)
                    ExtractedText(item)
                    Text(
                        "Saved " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.createdAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (picking) {
        CategoryPickerSheet(
            categories = state.categories,
            currentId = item.categoryId,
            onPick = { viewModel.setCategory(it); picking = false },
            onDismiss = { picking = false },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this item?") },
            text = { Text("It will be removed from this device, including the original photo if there is one.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Content(item: ItemEntity) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = spacedBy(Spacing.m)) {
        if (item.type == ItemType.IMAGE && item.imagePath != null) {
            AsyncImage(
                model = File(item.imagePath),
                contentDescription = item.displayTitle(),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(MaterialTheme.shapes.large),
            )
        }
        if (item.type == ItemType.LINK) {
            Column(verticalArrangement = spacedBy(Spacing.s)) {
                item.linkSiteName?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    item.linkTitle?.takeIf { it.isNotBlank() } ?: item.url.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                item.linkDescription?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(item.url.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 2)
                OutlinedButton(onClick = { item.url?.let(uriHandler::openUri) }) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null)
                    Text("Open link", Modifier.padding(start = Spacing.s))
                }
            }
        } else if (!item.title.isNullOrBlank()) {
            Text(item.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        }
        if (item.body.isNotBlank()) {
            SelectionContainer {
                Text(item.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun EditFields(
    item: ItemEntity,
    title: String,
    body: String,
    url: String,
    onTitle: (String) -> Unit,
    onBody: (String) -> Unit,
    onUrl: (String) -> Unit,
) {
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    Column(verticalArrangement = spacedBy(Spacing.m)) {
        TextField(title, onTitle, label = { Text("Title (optional)") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())
        if (item.type == ItemType.LINK) {
            TextField(url, onUrl, label = { Text("Link") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())
        }
        TextField(
            body, onBody,
            label = { Text(if (item.type == ItemType.TEXT) "Note" else "Note (optional)") },
            minLines = 4, colors = colors, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProcessingNote(item: ItemEntity, onRetry: () -> Unit) {
    when (item.status) {
        ProcessingStatus.PENDING -> Column(verticalArrangement = spacedBy(Spacing.s)) {
            Text("Reading and sorting…", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        ProcessingStatus.FAILED -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (item.type == ItemType.LINK) "Couldn't load the page. You may be offline."
                else "Couldn't fully read this.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text("Try again") }
        }
        ProcessingStatus.DONE -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    item: ItemEntity,
    categories: List<CategoryEntity>,
    suggestions: List<String>,
    onPick: (String?) -> Unit,
    onAuto: () -> Unit,
    onMore: () -> Unit,
) {
    val current = categories.firstOrNull { it.id == item.categoryId }
    val alternatives = suggestions.filter { it != item.categoryId }.mapNotNull { id -> categories.firstOrNull { it.id == id } }.take(3)
    val pending = item.status == ProcessingStatus.PENDING

    Column(verticalArrangement = spacedBy(Spacing.s)) {
        RowDivider(Modifier)
        Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { heading() })
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onMore).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = spacedBy(10.dp),
        ) {
            CategoryDot(current?.let { parseHexColor(it.color) }, size = 10)
            Text(
                if (pending) "Sorting…" else categories.pathName(current?.id) ?: "Inbox",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onMore) { Text("Change") }
        }
        val caption = when {
            item.categoryLocked -> "You chose this category."
            pending -> null
            current == null -> "Couldn't tell where this belongs."
            item.reasons.isNotBlank() -> "Matched: ${item.reasons}"
            else -> null
        }
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (item.categoryLocked) {
            TextButton(onClick = onAuto) { Text("Sort automatically again") }
        }
        if (alternatives.isNotEmpty()) {
            Text("Could also be", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.s))
            FlowRow(horizontalArrangement = spacedBy(Spacing.s)) {
                alternatives.forEach { category ->
                    AssistChip(
                        onClick = { onPick(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = { CategoryDot(parseHexColor(category.color)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowDivider(modifier: Modifier) {
    androidx.compose.material3.HorizontalDivider(modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtractedText(item: ItemEntity) {
    val labels = item.labelList()
    if (item.ocrText.isNullOrBlank() && labels.isEmpty()) return
    Column(verticalArrangement = spacedBy(Spacing.s)) {
        RowDivider(Modifier)
        if (!item.ocrText.isNullOrBlank()) {
            Text("Text in image", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { heading() })
            SelectionContainer {
                Text(item.ocrText, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (labels.isNotEmpty()) {
            Text("Looks like", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.s))
            Text(labels.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Normal)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<CategoryEntity>,
    currentId: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            "Move to",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.s).semantics { heading() },
        )
        LazyColumn(Modifier.navigationBarsPadding()) {
            item {
                PickerRow("Inbox", null, selected = currentId == null) { onPick(null) }
            }
            items(categories.flattened(), key = { it.category.id }) { node ->
                PickerRow(
                    node.category.name, parseHexColor(node.category.color),
                    selected = node.category.id == currentId, depth = node.depth,
                ) { onPick(node.category.id) }
            }
        }
    }
}

@Composable
private fun PickerRow(name: String, color: Color?, selected: Boolean, depth: Int = 0, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(start = Spacing.gutter + (depth * 20).dp, end = Spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(14.dp),
    ) {
        CategoryDot(color, size = 10)
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Current", tint = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(item: ItemEntity, onChange: (List<String>) -> Unit, onOpenTag: (String) -> Unit) {
    val tags = item.tagList()
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = spacedBy(Spacing.s)) {
        RowDivider(Modifier)
        Text("Tags", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = spacedBy(Spacing.s), verticalArrangement = spacedBy(Spacing.xs)) {
            tags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = { onOpenTag(tag) },
                    label = { Text("#$tag") },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Remove tag $tag",
                            modifier = Modifier.size(18.dp).clickable { onChange(tags - tag) },
                        )
                    },
                )
            }
            AssistChip(
                onClick = { adding = true },
                label = { Text(if (tags.isEmpty()) "Add a tag" else "Add") },
                leadingIcon = { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)) },
            )
        }
    }
    if (adding) {
        NameDialog("Add tag", "Add", "", onDismiss = { adding = false }) { raw ->
            // Commas or spaces separate several tags typed at once.
            val added = raw.split(',', ' ').map(::normalizeTag).filter { it.isNotEmpty() }
            onChange((tags + added).distinct())
            adding = false
        }
    }
}
