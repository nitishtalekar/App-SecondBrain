package app.secondbrain.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondbrain.data.Entry
import app.secondbrain.data.labelList
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.TagKind
import app.secondbrain.tagging.Text as TextTools
import app.secondbrain.tagging.tagLabel
import app.secondbrain.ui.DetailViewModel
import app.secondbrain.ui.components.AddTagDialog
import app.secondbrain.ui.components.SectionLabel
import app.secondbrain.ui.components.icon
import app.secondbrain.ui.theme.Spacing
import coil.compose.AsyncImage
import java.io.File
import java.text.DateFormat
import java.util.Date

/**
 * Just the thing itself. If it is waiting on the user there is one button to complete it; tags,
 * facts, editing and deletion live under Advanced.
 */
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenItem: (Long) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(ui.gone) { if (ui.gone) onBack() }
    val entry = ui.entry ?: return

    var advanced by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }

    if (editing) {
        Editor(
            entry,
            onClose = { editing = false },
            onSave = { title, body, url -> viewModel.save(title, body, url); editing = false },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.s)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Box(Modifier.weight(1f))
            IconButton(onClick = { advanced = true }) { Icon(Icons.Rounded.Tune, contentDescription = "Advanced") }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = Spacing.contentMaxWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter)
                    .padding(bottom = Spacing.xl),
                verticalArrangement = spacedBy(Spacing.m),
            ) { Content(entry) }
        }
        entry.item.action?.let { action ->
            val button = Modifier
                .widthIn(max = Spacing.contentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter, vertical = 12.dp)
                .height(52.dp)
                .align(Alignment.CenterHorizontally)
            if (entry.done) {
                FilledTonalButton(onClick = { viewModel.setCompleted(false) }, modifier = button) {
                    Text("${action.completed}. Undo")
                }
            } else {
                Button(onClick = { viewModel.setCompleted(true) }, modifier = button) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Text(action.verb, Modifier.padding(start = Spacing.s))
                }
            }
        }
    }

    if (advanced) {
        AdvancedSheet(
            entry = entry,
            related = ui.related,
            viewModel = viewModel,
            onDismiss = { advanced = false },
            onEdit = { advanced = false; editing = true },
            onOpenTag = { advanced = false; onOpenTag(it) },
            onOpenItem = { advanced = false; onOpenItem(it) },
        )
    }
}

@Composable
private fun Content(entry: Entry) {
    val item = entry.item
    val uriHandler = LocalUriHandler.current
    if (item.type == ItemType.IMAGE && item.imagePath != null) {
        AsyncImage(
            model = File(item.imagePath),
            contentDescription = entry.title,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
        )
    }
    val heading = item.title?.takeIf { it.isNotBlank() }
        ?: if (item.type == ItemType.LINK) item.linkTitle?.takeIf { it.isNotBlank() } else null
    if (heading != null) {
        Text(heading, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    }
    if (item.body.isNotBlank()) {
        SelectionContainer {
            // A short single thought gets to be large.
            val brief = item.type == ItemType.TEXT && item.body.length < 120 && '\n' !in item.body
            val style = if (brief) MaterialTheme.typography.headlineSmall
            else MaterialTheme.typography.bodyLarge
            Text(item.body, style = style)
        }
    }
    if (item.type == ItemType.LINK && item.url != null) {
        // A page with no app to open it must not crash the screen.
        OutlinedButton(onClick = { runCatching { uriHandler.openUri(item.url) } }) {
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, Modifier.size(18.dp))
            Text(
                "Open ${TextTools.hostOf(item.url) ?: "link"}",
                Modifier.padding(start = Spacing.s),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AdvancedSheet(
    entry: Entry,
    related: List<Entry>,
    viewModel: DetailViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenItem: (Long) -> Unit,
) {
    val item = entry.item
    val colors = MaterialTheme.colorScheme
    var addingTag by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showWhy by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.l)
                .navigationBarsPadding(),
            verticalArrangement = spacedBy(12.dp),
        ) {
            SectionLabel("Tags")
            FlowRow(horizontalArrangement = spacedBy(Spacing.s)) {
                entry.tags.forEach { tag ->
                    SavedTagChip(
                        name = tagLabel(tag.name),
                        mine = tag.kind == TagKind.MINE,
                        onOpen = { onOpenTag(tag.name) },
                        onRemove = { viewModel.removeTag(tag.name) },
                    )
                }
                AssistChip(
                    onClick = { addingTag = true },
                    label = { Text("Add tag") },
                    leadingIcon = { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)) },
                )
            }
            val reasons = entry.tags.filter { it.why != null }
            if (reasons.isNotEmpty()) {
                TextButton(onClick = { showWhy = !showWhy }) { Text(if (showWhy) "Hide reasons" else "Why these tags?") }
                if (showWhy) reasons.forEach { Fact(tagLabel(it.name), it.why!!) }
            }

            HorizontalDivider(color = colors.outlineVariant)
            SectionLabel("Is it waiting on you?")
            ActionChips(item.action) { viewModel.setAction(it) }

            if (related.isNotEmpty()) {
                HorizontalDivider(color = colors.outlineVariant)
                SectionLabel("Saved together")
                related.forEach { other ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenItem(other.id) }.heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = spacedBy(12.dp),
                    ) {
                        Icon(other.item.type.icon(), contentDescription = null, tint = colors.primary)
                        Text(other.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            HorizontalDivider(color = colors.outlineVariant)
            SectionLabel("Details")
            Fact("Saved", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.createdAt)))
            item.completedAt?.let {
                Fact(item.action?.completed ?: "Done", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)))
            }
            item.url?.let { Fact("Address", it) }
            item.linkSiteName?.takeIf { it.isNotBlank() }?.let { Fact("Site", it) }
            item.linkDescription?.takeIf { it.isNotBlank() }?.let { Fact("About", it) }
            item.ocrText?.takeIf { it.isNotBlank() }?.let { Fact("Text in photo", it) }
            if (item.labelList().isNotEmpty()) Fact("Looks like", item.labelList().joinToString(", "))
            if (item.needsAnalysis) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (item.type == ItemType.LINK) "The page hasn't loaded yet." else "The photo hasn't been fully read.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.retry() }) { Text("Try again") }
                }
            }

            HorizontalDivider(color = colors.outlineVariant)
            Row(horizontalArrangement = spacedBy(Spacing.s)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, Modifier.size(18.dp))
                    Text("Edit", Modifier.padding(start = Spacing.s))
                }
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, Modifier.size(18.dp), tint = colors.error)
                    Text("Delete", Modifier.padding(start = Spacing.s), color = colors.error)
                }
            }
        }
    }
    if (addingTag) {
        AddTagDialog(onDismiss = { addingTag = false }) { viewModel.addTags(it); addingTag = false }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this?") },
            text = {
                Text(
                    if (item.type == ItemType.IMAGE) "The photo is removed from Second Brain. This can't be undone."
                    else "It is removed from this device. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDismiss(); viewModel.delete() }) { Text("Delete", color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/**
 * A tag with two jobs: the name searches for it, the cross removes it. It looks like a 32dp chip,
 * but each job has its own 48dp target so a near miss on the cross doesn't leave the screen.
 */
@Composable
private fun SavedTagChip(name: String, mine: Boolean, onOpen: () -> Unit, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val content = if (mine) colors.onSecondaryContainer else colors.onSurfaceVariant
    val shape = MaterialTheme.shapes.small
    Box {
        Box(
            Modifier
                .matchParentSize()
                .padding(vertical = 8.dp)
                .then(
                    if (mine) Modifier.background(colors.secondaryContainer, shape)
                    else Modifier.border(1.dp, colors.outlineVariant, shape),
                ),
        )
        Row(Modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f, fill = false)
                    .fillMaxHeight()
                    .widthIn(min = 48.dp)
                    .clip(shape)
                    .clickable(onClickLabel = "Show in Library", role = Role.Button, onClick = onOpen)
                    .padding(start = 12.dp, end = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(name, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove tag $name", tint = content, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer { Text(value, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun Editor(entry: Entry, onClose: () -> Unit, onSave: (String?, String, String?) -> Unit) {
    val item = entry.item
    var title by rememberSaveable { mutableStateOf(item.title.orEmpty()) }
    var body by rememberSaveable { mutableStateOf(item.body) }
    var url by rememberSaveable { mutableStateOf(item.url.orEmpty()) }
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Discard changes") }
            Text("Edit", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(
                onClick = { onSave(title, body, url.takeIf { item.type == ItemType.LINK }) },
                enabled = item.type != ItemType.TEXT || body.isNotBlank(),
            ) { Text("Save") }
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.s),
            verticalArrangement = spacedBy(Spacing.m),
        ) {
            TextField(title, { title = it }, label = { Text("Title (optional)") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())
            if (item.type == ItemType.LINK) {
                TextField(url, { url = it }, label = { Text("Link") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())
            }
            TextField(
                body, { body = it },
                label = { Text(if (item.type == ItemType.TEXT) "Note" else "Note (optional)") },
                minLines = 6, colors = colors, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
