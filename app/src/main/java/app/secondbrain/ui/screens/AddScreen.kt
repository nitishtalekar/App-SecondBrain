package app.secondbrain.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondbrain.ingest.Draft
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.Text as TextTools
import app.secondbrain.tagging.tagLabel
import app.secondbrain.ui.AddUiState
import app.secondbrain.ui.AddViewModel
import app.secondbrain.ui.components.AddTagDialog
import app.secondbrain.ui.components.SectionLabel
import app.secondbrain.ui.components.icon
import app.secondbrain.ui.theme.Spacing
import coil.compose.AsyncImage
import java.io.File

/** The whole tab is for putting things in: write or attach, check the tags, confirm. */
@Composable
fun AddScreen(viewModel: AddViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = Spacing.contentMaxWidth).fillMaxSize()) {
            if (state.reviewing) Review(state, viewModel) else Composer(state, viewModel)
        }
    }
}

@Composable
private fun Composer(state: AddUiState, viewModel: AddViewModel) {
    val focusManager = LocalFocusManager.current
    var cameraTarget by rememberSaveable { mutableStateOf<String?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.attach(uri)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val target = cameraTarget
        if (ok && target != null) viewModel.attach(Uri.parse(target))
    }
    BackHandler(enabled = state.analysing) { viewModel.cancelAnalysis() }

    // Centred while it fits; scrolls when the keyboard or a large font leaves too little room.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter, vertical = Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = spacedBy(Spacing.m),
        ) {
            Text(
                "What's on your mind?",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = Spacing.s).semantics { heading() },
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    TextField(
                        value = state.text,
                        onValueChange = viewModel::setText,
                        enabled = !state.analysing,
                        placeholder = {
                            Text(
                                "Write a thought, add a link, or upload a photo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = MaterialTheme.typography.bodyLarge.fontWeight,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = MaterialTheme.typography.bodyLarge.fontWeight),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = borderlessFieldColors(),
                        // Longer text scrolls inside the box.
                        modifier = Modifier.fillMaxWidth().heightIn(min = 152.dp, max = 240.dp),
                    )
                    if (state.importing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(Spacing.m))
                    state.imagePath?.let { path ->
                        Box(Modifier.padding(Spacing.m)) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = "Attached photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(112.dp).clip(MaterialTheme.shapes.medium),
                            )
                            IconButton(
                                onClick = viewModel::removeImage,
                                enabled = !state.analysing,
                                modifier = Modifier.align(Alignment.TopEnd),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                ),
                            ) { Icon(Icons.Rounded.Close, contentDescription = "Remove photo", Modifier.size(20.dp)) }
                        }
                    }
                    if (state.text.isNotEmpty() || state.imagePath != null) {
                        TextButton(
                            onClick = viewModel::discard,
                            enabled = !state.analysing,
                            modifier = Modifier.align(Alignment.End).padding(horizontal = Spacing.s),
                        ) { Text("Clear") }
                    }
                }
            }
            Row(horizontalArrangement = spacedBy(Spacing.s)) {
                AttachOption(Icons.Rounded.PhotoLibrary, "Upload photo", enabled = !state.analysing, Modifier.weight(1f)) {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                AttachOption(Icons.Rounded.PhotoCamera, "Take photo", enabled = !state.analysing, Modifier.weight(1f)) {
                    val (_, uri) = viewModel.newCaptureTarget()
                    cameraTarget = uri.toString()
                    takePhoto.launch(uri)
                }
            }
            Button(
                onClick = { focusManager.clearFocus(); viewModel.analyse() },
                enabled = state.canAnalyse,
                // Lights up in the accent once there is something to add.
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.analysing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Reading…", Modifier.padding(start = Spacing.s))
                } else {
                    Text("Add to brain")
                }
            }
        }
    }
}

@Composable
private fun AttachOption(icon: ImageVector, label: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled, shape = MaterialTheme.shapes.medium, modifier = modifier.heightIn(min = 52.dp)) {
        Icon(icon, contentDescription = null, Modifier.size(20.dp))
        Text(label, Modifier.padding(start = Spacing.s))
    }
}

@Composable
private fun Review(state: AddUiState, viewModel: AddViewModel) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    BackHandler { viewModel.backToCompose() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = Spacing.xs, end = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::backToCompose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to writing")
            }
            Text("Review", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { confirmDiscard = true }) { Text("Discard") }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Spacing.gutter, vertical = Spacing.s),
            verticalArrangement = spacedBy(12.dp),
        ) {
            items(state.drafts, key = { it.key }) { draft -> DraftCard(draft, viewModel, Modifier.animateItem()) }
        }
        val count = state.savable.size
        Button(
            onClick = viewModel::confirm,
            enabled = count > 0 && !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter, vertical = 12.dp).height(52.dp),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null)
            Text(if (count > 1) "Save $count items" else "Save", Modifier.padding(start = Spacing.s))
        }
    }

    if (confirmDiscard) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this?") },
            text = { Text("Nothing has been saved yet. The text and photo will be cleared.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; viewModel.discard() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

private fun Draft.kindLabel(): String = when {
    type == ItemType.IMAGE -> "Photo"
    type == ItemType.LINK -> "Link"
    scanned -> "Text from the photo"
    else -> "Note"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DraftCard(draft: Draft, viewModel: AddViewModel, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var addingTag by rememberSaveable { mutableStateOf(false) }

    Surface(color = colors.surfaceContainerLow, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.m), verticalArrangement = spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = spacedBy(Spacing.s)) {
                Icon(draft.type.icon(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text(draft.kindLabel(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.removeDraft(draft.key) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove this item")
                }
            }
            if (draft.type == ItemType.IMAGE && draft.imagePath != null) {
                AsyncImage(
                    model = File(draft.imagePath),
                    contentDescription = "Photo to be saved",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium),
                )
            }
            if (draft.type == ItemType.LINK) {
                Column(verticalArrangement = spacedBy(2.dp)) {
                    Text(
                        draft.meta?.title ?: draft.url.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(TextTools.hostOf(draft.url) ?: draft.url.orEmpty(), style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    if (draft.meta == null) {
                        Text(
                            "Couldn't load the page, so tags come from the address. It's tried again later.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
            TextField(
                value = draft.body,
                onValueChange = { viewModel.setBody(draft.key, it) },
                placeholder = { Text(if (draft.type == ItemType.TEXT) "Note" else "Add a note (optional)") },
                maxLines = 10,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainerHigh,
                    unfocusedContainerColor = colors.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Is it waiting on you?")
            ActionChips(draft.action) { viewModel.chooseAction(draft.key, it) }
            draft.action?.let { action ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(draft.done, role = Role.Checkbox) { viewModel.setDone(draft.key, it) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = spacedBy(12.dp),
                ) {
                    Checkbox(checked = draft.done, onCheckedChange = null)
                    Text("Already ${action.completed.lowercase()}", style = MaterialTheme.typography.bodyLarge)
                }
            }

            val kept = draft.keptTags.size
            SectionLabel(if (kept == 1) "1 tag" else "$kept tags")
            FlowRow(horizontalArrangement = spacedBy(Spacing.s)) {
                draft.tags.forEach { tag ->
                    val on = tag.name !in draft.removed
                    FilterChip(
                        selected = on,
                        onClick = { viewModel.toggleTag(draft.key, tag.name) },
                        label = { Text(tagLabel(tag.name), textDecoration = if (on) null else TextDecoration.LineThrough) },
                        trailingIcon = {
                            Icon(
                                if (on) Icons.Rounded.Close else Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.semantics { stateDescription = if (on) "Will be saved" else "Removed" },
                    )
                }
                AssistChip(
                    onClick = { addingTag = true },
                    label = { Text("Add tag") },
                    leadingIcon = { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)) },
                )
            }
        }
    }
    if (addingTag) {
        AddTagDialog(onDismiss = { addingTag = false }) { viewModel.addTags(draft.key, it); addingTag = false }
    }
}

/** Pick what the item is waiting for; tapping the chosen one clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionChips(selected: ItemAction?, onSelect: (ItemAction?) -> Unit) {
    FlowRow(horizontalArrangement = spacedBy(Spacing.s)) {
        ItemAction.entries.forEach { action ->
            val on = selected == action
            FilterChip(
                selected = on,
                onClick = { onSelect(if (on) null else action) },
                label = { Text(action.pending) },
                leadingIcon = if (on) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun borderlessFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)
