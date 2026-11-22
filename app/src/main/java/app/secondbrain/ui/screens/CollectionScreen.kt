package app.secondbrain.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.secondbrain.ui.AppUiState
import app.secondbrain.ui.CollectionViewModel
import app.secondbrain.ui.components.EmptyState
import app.secondbrain.ui.components.ItemList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val labeler = rememberLabeler(state)
    val categoryId = viewModel.categoryId
    val name = if (categoryId == null) "Inbox" else state.categoryName(categoryId) ?: ""
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                if (categoryId != null) {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "Category options") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; renaming = true })
                        DropdownMenuItem(text = { Text("Delete category") }, onClick = { menuOpen = false; confirmDelete = true })
                    }
                }
            },
            windowInsets = WindowInsets(0.dp),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        val list = items
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(
                if (categoryId == null) "Inbox is clear" else "Nothing in $name yet",
                if (categoryId == null) "Anything the app can't place confidently will wait here for you." else "New items land here when they match, or move one in from its detail page.",
            )
            else -> ItemList(list, labelFor = labeler, onOpen = onOpen)
        }
    }

    if (renaming && categoryId != null) {
        NameDialog("Rename category", "Rename", name, onDismiss = { renaming = false }) {
            onRename(categoryId, it); renaming = false
        }
    }
    if (confirmDelete && categoryId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “$name”?") },
            text = { Text("Items stay saved. They'll be sorted again, and anything unclear goes to the Inbox.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(categoryId); onBack() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
