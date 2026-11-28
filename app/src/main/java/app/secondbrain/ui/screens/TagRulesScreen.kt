package app.secondbrain.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondbrain.tagging.TagDef
import app.secondbrain.tagging.tagLabel
import app.secondbrain.ui.TagRow
import app.secondbrain.ui.TagRulesViewModel
import app.secondbrain.ui.components.RowDivider
import app.secondbrain.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Every tag that can be added automatically. Open one to change what earns it. */
@Composable
fun TagRulesScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onExport: suspend () -> String,
    onImport: (String) -> Unit,
    viewModel: TagRulesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = onExport()
            withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) } }
            }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            }
            onImport(text.orEmpty())
        }
    }

    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = Spacing.contentMaxWidth).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                Text("Tag rules", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).semantics { heading() })
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "Import or export") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Export rules") },
                            onClick = { menu = false; export.launch("secondbrain-tags.json") },
                        )
                        DropdownMenuItem(
                            text = { Text("Import rules") },
                            onClick = { menu = false; import.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        )
                    }
                }
            }
            TextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Find a tag") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear text")
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter),
            )
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.s)) {
                itemsIndexed(state.rows, key = { _, row -> row.name }) { index, row ->
                    TagRuleRow(row) { onOpen(row.name) }
                    if (index < state.rows.lastIndex) RowDivider()
                }
                if (state.loaded && state.rows.isEmpty()) item(key = "none") {
                    Text(
                        "No tag with that name.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.l),
                    )
                }
            }
            Button(
                onClick = { onOpen("") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter, vertical = 12.dp).height(52.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("New tag", Modifier.padding(start = Spacing.s))
            }
        }
    }
}

@Composable
private fun TagRuleRow(row: TagRow, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val status = listOfNotNull(
        "Off".takeIf { row.def.off },
        "Yours".takeIf { row.own },
        "Edited".takeIf { row.edited },
    ).joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.gutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(Spacing.s),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = spacedBy(2.dp)) {
            Text(
                tagLabel(row.name),
                style = MaterialTheme.typography.titleMedium,
                color = if (row.def.off) colors.onSurfaceVariant else colors.onSurface,
            )
            Text(row.def.summary(), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
        if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

private fun TagDef.summary(): String {
    fun count(number: Int, noun: String) = when (number) {
        0 -> null
        1 -> "1 $noun"
        else -> "$number ${noun}s"
    }
    return listOfNotNull(
        count(words.size + hints.size, "word"),
        count(domains.size, "site"),
        count(labels.size, "photo label"),
        count(rules.size, "pattern"),
    ).joinToString(" · ").ifEmpty { "Added alongside other tags" }
}
