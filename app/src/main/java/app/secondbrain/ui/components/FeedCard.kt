package app.secondbrain.ui.components

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.secondbrain.data.Entry
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.Text as TextTools
import app.secondbrain.ui.theme.Spacing
import coil.compose.AsyncImage
import java.io.File

/**
 * One saved thing as a card in the feed. Cards take the height their content needs (a picture keeps
 * its own shape, a note shows more of its words), which is what staggers the grid.
 */
@Composable
fun FeedCard(
    entry: Entry,
    onClick: () -> Unit,
    onComplete: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    snippet: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val item = entry.item
    val picture: Any? = when (item.type) {
        ItemType.IMAGE -> item.imagePath?.let(::File)
        ItemType.LINK -> item.linkImageUrl
        ItemType.TEXT -> null
    }
    // A note without a title of its own is just its words.
    val plainNote = item.type == ItemType.TEXT && item.title.isNullOrBlank() && item.body.isNotBlank()
    // "Photo" under a photo says nothing.
    val barePhoto = item.type == ItemType.IMAGE && item.title.isNullOrBlank() && item.body.isBlank() && item.ocrText.isNullOrBlank()
    val heading = entry.title.takeUnless { plainNote || barePhoto }
    val preview = snippet ?: item.body.trim().takeIf { item.type == ItemType.TEXT && it.isNotEmpty() }
    val textColor = if (entry.done) colors.onSurfaceVariant else colors.onSurface
    val struck = if (entry.done) TextDecoration.LineThrough else null
    val status = entry.statusLabel()
    val host = if (item.type == ItemType.LINK) item.url?.let(TextTools::hostOf) else null
    val foot = host ?: entry.tagLine(2).ifEmpty { relativeTime(item.createdAt) }

    Surface(
        onClick = onClick,
        color = colors.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            if (picture != null) CardPicture(picture, item.type)
            Column(Modifier.padding(12.dp), verticalArrangement = spacedBy(6.dp)) {
                if (heading != null) {
                    Text(
                        heading,
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor,
                        textDecoration = struck,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (preview != null) {
                    Text(
                        preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (plainNote) textColor else colors.onSurfaceVariant,
                        textDecoration = if (plainNote) struck else null,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    foot,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (status != null) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (entry.done) colors.onSurfaceVariant else colors.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    CompletionButton(entry, onComplete)
                }
            }
        }
    }
}

/** The picture in its own shape, within limits so a very tall or wide one doesn't take over. */
@Composable
private fun CardPicture(model: Any, type: ItemType) {
    // Kept across scrolling, so a card comes back at the height it had.
    var ratio by rememberSaveable(model.toString()) { mutableFloatStateOf(4f / 3f) }
    var failed by remember(model) { mutableStateOf(false) }
    if (failed) {
        // A link without its preview is still a good card; a lost photo is marked by an icon.
        if (type == ItemType.IMAGE) {
            Icon(
                type.icon(),
                contentDescription = "Photo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp).size(24.dp),
            )
        }
        return
    }
    AsyncImage(
        model = model,
        contentDescription = if (type == ItemType.IMAGE) "Photo" else null,
        contentScale = ContentScale.Crop,
        onSuccess = { state ->
            val size = state.painter.intrinsicSize
            if (size.width > 0f && size.height > 0f) ratio = (size.width / size.height).coerceIn(0.65f, 1.9f)
        },
        onError = { failed = true },
        modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
    )
}
