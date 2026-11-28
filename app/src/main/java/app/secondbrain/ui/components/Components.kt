package app.secondbrain.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.secondbrain.data.Entry
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.TagKind
import app.secondbrain.tagging.parseTags
import app.secondbrain.tagging.tagLabel
import app.secondbrain.ui.theme.Spacing
import coil.compose.AsyncImage
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = Spacing.gutter, end = Spacing.s, top = Spacing.s, bottom = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun EmptyState(title: String, hint: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Box(modifier.fillMaxSize().padding(horizontal = Spacing.xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = spacedBy(Spacing.s)) {
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp),
                textAlign = TextAlign.Center,
            )
            action()
        }
    }
}

fun ItemType.icon(): ImageVector = when (this) {
    ItemType.TEXT -> Icons.AutoMirrored.Rounded.Notes
    ItemType.LINK -> Icons.Rounded.Link
    ItemType.IMAGE -> Icons.Rounded.Image
}

@Composable
private fun Thumbnail(entry: Entry) {
    val item = entry.item
    val model: Any? = when {
        item.type == ItemType.IMAGE && item.imagePath != null -> File(item.imagePath)
        item.type == ItemType.LINK -> item.linkImageUrl
        else -> null
    }
    Box(
        Modifier.size(52.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(item.type.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (model != null) {
            AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * One saved thing: what it is, its leading tags, and (if it is waiting on the user) a one-tap circle
 * to complete it.
 */
@Composable
fun EntryRow(
    entry: Entry,
    onClick: () -> Unit,
    onComplete: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    snippet: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val tagLine = entry.tags
        .filter { it.kind != TagKind.TIME && it.kind != TagKind.FORMAT }
        .take(3).joinToString(" · ") { tagLabel(it.name) }
    val status = entry.item.action?.let { if (entry.done) it.completed else it.pending }

    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // The completion button brings its own padding.
            .padding(
                start = Spacing.gutter,
                end = if (entry.item.action != null) Spacing.s else Spacing.gutter,
                top = 12.dp,
                bottom = 12.dp,
            ),
        horizontalArrangement = spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(entry)
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = spacedBy(2.dp)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.done) colors.onSurfaceVariant else colors.onSurface,
                textDecoration = if (entry.done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (snippet != null) {
                Text(snippet, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = spacedBy(Spacing.s)) {
                if (status != null) {
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (entry.done) colors.onSurfaceVariant else colors.tertiary,
                        maxLines = 1,
                    )
                }
                Text(
                    tagLine.ifEmpty { relativeTime(entry.item.createdAt) },
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val action = entry.item.action
        if (action != null) {
            IconButton(onClick = { onComplete(!entry.done) }) {
                Icon(
                    if (entry.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (entry.done) "${action.completed}. Tap to undo" else action.verb,
                    tint = if (entry.done) colors.primary else colors.tertiary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.gutter),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** Type one tag, or several separated by spaces or commas. */
@Composable
fun AddTagDialog(onDismiss: () -> Unit, onAdd: (List<String>) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val tags = parseTags(text)
    fun submit() {
        if (tags.isNotEmpty()) onAdd(tags)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add tags") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Travel, Weekend") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        },
        confirmButton = { TextButton(onClick = ::submit, enabled = tags.isNotEmpty()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - timestamp
    val minute = 60_000L
    return when {
        diff < minute -> "Just now"
        diff < 60 * minute -> "${diff / minute} min ago"
        diff < 24 * 60 * minute -> {
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val then = Calendar.getInstance().apply { timeInMillis = timestamp }
            if (cal.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) "${diff / (60 * minute)} h ago" else "Yesterday"
        }
        diff < 48 * 60 * minute -> "Yesterday"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
    }
}
