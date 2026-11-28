package app.secondbrain.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.secondbrain.data.Accent
import app.secondbrain.data.ThemeMode
import app.secondbrain.ui.AppUiState
import app.secondbrain.ui.AppViewModel
import app.secondbrain.ui.components.SectionLabel
import app.secondbrain.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: AppUiState, viewModel: AppViewModel, onBack: () -> Unit, onOpenTagRules: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = Spacing.contentMaxWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter)
                    .padding(bottom = Spacing.xl),
                verticalArrangement = spacedBy(Spacing.s),
            ) {
                Section("Appearance", "Theme and accent color") {
                    SectionLabel("Theme")
                    val modes = ThemeMode.entries
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = state.themeMode == mode,
                                onClick = { viewModel.setTheme(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                            ) { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        }
                    }
                    SectionLabel("Accent color", Modifier.padding(top = Spacing.s))
                    AccentGrid(selected = state.accent, onSelect = viewModel::setAccent)
                    Hint("${state.accent.label}. Marks things still waiting on you. Everything else stays black and white.")
                }
                Section("Tags", "Tag rules and tagging again") {
                    Hint("Rules decide which tags are added automatically.")
                    OutlinedButton(onClick = onOpenTagRules, Modifier.fillMaxWidth()) { Text("Tag rules") }
                    OutlinedButton(onClick = viewModel::retagAll, Modifier.fillMaxWidth()) { Text("Tag everything again") }
                    Hint("Tagging again only adds tags. Tags you removed stay removed.")
                }
                Section("Privacy", "What stays on this device") {
                    Hint("Your items are saved on this device, and photos are read by on-device models for text and objects. If Android backup is on, a copy of the app's data may go to your backup account. The app itself only goes online to fetch the details and preview images of links you add.")
                }
            }
        }
    }
}

/** Every accent as a swatch, in the shade it takes in the current theme. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentGrid(selected: Accent, onSelect: (Accent) -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = spacedBy(Spacing.s),
        maxItemsInEachRow = 6,
    ) {
        Accent.entries.forEach { accent ->
            val chosen = accent == selected
            val ring = if (chosen) MaterialTheme.colorScheme.onSurface else Color.Transparent
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(accent) })
                    .semantics { contentDescription = accent.label }
                    .border(2.dp, ring, CircleShape)
                    .padding(5.dp)
                    .background(Color(if (dark) accent.dark.color else accent.light.color), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (chosen) Icon(Icons.Rounded.Check, contentDescription = null, tint = if (dark) Color.Black else Color.White)
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A card that opens to show what is inside. Stays open while you visit a screen beyond it. */
@Composable
private fun Section(title: String, summary: String, content: @Composable () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerLow, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.animateContentSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = if (open) "Close" else "Open") { open = !open }
                    .semantics { stateDescription = if (open) "Open" else "Closed" }
                    .heightIn(min = 72.dp)
                    .padding(horizontal = Spacing.m, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = spacedBy(Spacing.s),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    Text(summary, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = colors.onSurfaceVariant)
            }
            if (open) {
                Column(
                    Modifier.padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.m),
                    verticalArrangement = spacedBy(Spacing.s),
                ) { content() }
            }
        }
    }
}
