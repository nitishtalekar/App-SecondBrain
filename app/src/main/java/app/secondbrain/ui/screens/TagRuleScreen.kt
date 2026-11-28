package app.secondbrain.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondbrain.tagging.RuleList
import app.secondbrain.tagging.cleanEntries
import app.secondbrain.tagging.entries
import app.secondbrain.tagging.tagLabel
import app.secondbrain.ui.TagRuleUiState
import app.secondbrain.ui.TagRuleViewModel
import app.secondbrain.ui.components.SectionLabel
import app.secondbrain.ui.theme.Spacing

private class ListCopy(val heading: String, val add: String, val dialog: String, val example: String)

private fun RuleList.copyText() = when (this) {
    RuleList.WORDS -> ListCopy("Any one of these words", "Add word", "Add words", "night train, sleeper")
    RuleList.HINTS -> ListCopy("Or two of these", "Add word", "Add words", "station, ticket")
    RuleList.DOMAINS -> ListCopy("Or a link from", "Add site", "Add sites", "seat61.com")
    RuleList.LABELS -> ListCopy("Or a photo containing", "Add object", "Add objects", "Train, Vehicle")
    RuleList.IMPLIES -> ListCopy("Also add these tags", "Add tag", "Add tags", "travel")
}

/** One tag: what earns it, a place to try that out, and the way to switch off, reset or delete it. */
@Composable
fun TagRuleScreen(onBack: () -> Unit, onApply: () -> Unit, viewModel: TagRuleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) { if (state.closed) onBack() }
    val draft = state.draft ?: return

    var adding by rememberSaveable { mutableStateOf<RuleList?>(null) }
    var more by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val leave = { if (state.changed) confirmLeave = true else onBack() }
    BackHandler(enabled = state.changed) { confirmLeave = true }

    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = Spacing.contentMaxWidth).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = leave) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                Text(
                    if (state.isNew) "New tag" else tagLabel(draft.tag),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextButton(onClick = viewModel::save, enabled = state.canSave) { Text("Save") }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter)
                    .padding(bottom = Spacing.xl),
                verticalArrangement = spacedBy(Spacing.l),
            ) {
                if (state.isNew) {
                    TextField(
                        value = state.nameInput,
                        onValueChange = viewModel::setName,
                        label = { Text("Name") },
                        singleLine = true,
                        isError = state.nameTaken,
                        supportingText = if (state.nameTaken) {
                            { Text("A tag with this name already exists. Open it from the list to change it.") }
                        } else null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = filledColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (!state.own) {
                    OnSwitch(on = !draft.off, onChange = viewModel::setOn)
                }

                val section: @Composable (RuleList) -> Unit = { list ->
                    EntryList(list, draft.entries(list), onAdd = { adding = list }, onRemove = { viewModel.remove(list, it) })
                }
                if (!state.isNew) Text("Add this tag when something has", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                section(RuleList.WORDS)
                section(RuleList.HINTS)
                section(RuleList.DOMAINS)

                // Open from the start when there is something in it, so nothing is hidden by surprise.
                val hasMore = draft.labels.isNotEmpty() || draft.implies.isNotEmpty()
                TextButton(onClick = { more = !more }, enabled = !hasMore) {
                    Icon(if (more || hasMore) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    Text("More", Modifier.padding(start = Spacing.xs))
                }
                if (more || hasMore) {
                    section(RuleList.LABELS)
                    section(RuleList.IMPLIES)
                }
                if (draft.rules.isNotEmpty()) {
                    Text(
                        "This tag also matches how something is written. Those patterns can be changed in the exported rules file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TryIt(state, viewModel::setSample)

                if (!state.isNew) {
                    if (state.own) {
                        OutlinedButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Text("Delete tag", Modifier.padding(start = Spacing.s), color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { confirmReset = true },
                            enabled = draft != state.shipped,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Reset to default") }
                    }
                }
            }
        }
    }

    adding?.let { list ->
        AddEntryDialog(list, onDismiss = { adding = null }) { viewModel.add(list, it); adding = null }
    }
    if (state.saved) {
        val close = { onBack() }
        AlertDialog(
            onDismissRequest = close,
            title = { Text("Saved") },
            text = { Text("New items use these rules. Apply them to what you've already saved? This only adds tags.") },
            confirmButton = { TextButton(onClick = { onApply(); close() }) { Text("Apply") } },
            dismissButton = { TextButton(onClick = close) { Text("Not now") } },
        )
    }
    if (confirmLeave) {
        Confirm("Discard changes?", "Your changes to this tag haven't been saved.", "Discard", "Keep editing", { confirmLeave = false }) { onBack() }
    }
    if (confirmReset) {
        Confirm("Reset to default?", "Your changes to this tag are replaced by the original rules.", "Reset", "Cancel", { confirmReset = false }) {
            viewModel.reset()
        }
    }
    if (confirmDelete) {
        Confirm("Delete this tag?", "It won't be added automatically any more. Items that already have it keep it.", "Delete", "Cancel", { confirmDelete = false }) {
            viewModel.delete()
        }
    }
}

@Composable
private fun OnSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(on, role = Role.Switch, onValueChange = onChange).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(Spacing.m),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Add automatically", style = MaterialTheme.typography.titleMedium)
            Text(
                if (on) "On" else "Off. Items that already have it keep it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = on, onCheckedChange = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryList(list: RuleList, values: List<String>, onAdd: () -> Unit, onRemove: (String) -> Unit) {
    val text = list.copyText()
    Column {
        SectionLabel(text.heading)
        FlowRow(horizontalArrangement = spacedBy(Spacing.s)) {
            values.forEach { value ->
                val shown = if (list == RuleList.IMPLIES) tagLabel(value) else value
                InputChip(
                    selected = true,
                    onClick = { onRemove(value) },
                    label = { Text(shown) },
                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Remove", Modifier.size(18.dp)) },
                )
            }
            AssistChip(
                onClick = onAdd,
                label = { Text(text.add) },
                leadingIcon = { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)) },
            )
        }
    }
}

@Composable
private fun TryIt(state: TagRuleUiState, onSample: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = spacedBy(Spacing.s)) {
        SectionLabel("Try it")
        TextField(
            value = state.sample,
            onValueChange = onSample,
            placeholder = { Text("Type some text or a link") },
            minLines = 2,
            maxLines = 6,
            colors = filledColors(),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.sample.isNotBlank()) {
            val match = state.match
            val name = state.draft?.tag?.takeIf { it.isNotEmpty() }?.let(::tagLabel) ?: "this tag"
            Surface(
                color = if (match != null) colors.secondaryContainer else colors.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (match != null) Icons.Rounded.Check else Icons.Rounded.Close, contentDescription = null, Modifier.size(20.dp))
                    Text(
                        when {
                            match != null -> "Adds $name: ${match.reasons.joinToString(", ")}"
                            state.draft?.off == true -> "Doesn't add $name because it's switched off"
                            else -> "Doesn't add $name"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEntryDialog(list: RuleList, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val copy = list.copyText()
    val usable = cleanEntries(list, text).isNotEmpty()
    fun submit() {
        if (usable) onAdd(text)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy.dialog) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(copy.example) },
                supportingText = { Text("Separate several with commas.") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (list == RuleList.DOMAINS) KeyboardType.Uri else KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        },
        confirmButton = { TextButton(onClick = ::submit, enabled = usable) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Confirm(title: String, body: String, yes: String, no: String, onDismiss: () -> Unit, onYes: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = { onDismiss(); onYes() }) { Text(yes) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(no) } },
    )
}

@Composable
private fun filledColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)
