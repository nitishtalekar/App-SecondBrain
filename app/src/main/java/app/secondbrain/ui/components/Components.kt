package app.secondbrain.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.db.ItemType
import app.secondbrain.data.db.ProcessingStatus
import app.secondbrain.data.displaySubtitle
import app.secondbrain.data.displayTitle
import app.secondbrain.ui.theme.Newsreader
import app.secondbrain.ui.theme.Spacing
import coil.compose.AsyncImage
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** Dot that identifies a category. Hollow when [color] is null (Inbox). */
@Composable
fun CategoryDot(color: Color?, modifier: Modifier = Modifier, size: Int = 8) {
    val ring = MaterialTheme.colorScheme.outline
    Canvas(modifier.size(size.dp)) {
        if (color != null) drawCircle(color) else drawCircle(ring, style = Stroke(width = 1.5.dp.toPx()), radius = this.size.minDimension / 2 - 1.dp.toPx())
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = Spacing.gutter, end = Spacing.s, top = Spacing.m, bottom = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing()
    }
}

@Composable
fun EmptyState(title: String, hint: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(horizontal = Spacing.xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = spacedBy(Spacing.s)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = Newsreader, fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Display state of an item's category, resolved by the caller from the category table. */
data class CategoryLabel(val name: String, val color: Color?)

@Composable
fun ItemRow(
    item: ItemEntity,
    category: CategoryLabel?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    snippet: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val pending = item.status == ProcessingStatus.PENDING
    val thumb: Any? = when {
        item.type == ItemType.IMAGE && item.imagePath != null -> File(item.imagePath)
        item.type == ItemType.LINK -> item.linkImageUrl
        else -> null
    }
    val subtitle = snippet ?: item.displaySubtitle()

    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.gutter, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = spacedBy(Spacing.m),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = spacedBy(4.dp)) {
            Text(
                item.displayTitle(),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                if (pending) {
                    Text("Sorting…", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                } else {
                    CategoryDot(category?.color)
                    Text(category?.name ?: "Inbox", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
                Text("·", style = MaterialTheme.typography.labelMedium, color = colors.outline)
                Text(relativeTime(item.createdAt), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
        if (thumb != null) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.medium),
            )
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

/** Centres list content to a readable column on tablets and foldables. */
@Composable
fun ItemList(
    items: List<ItemEntity>,
    labelFor: (ItemEntity) -> CategoryLabel?,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(bottom = 96.dp),
    snippetFor: (ItemEntity) -> String? = { null },
    header: (LazyListScope.() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = Spacing.contentMaxWidth).fillMaxSize(), state = state, contentPadding = contentPadding) {
            header?.invoke(this)
            items(items.size, key = { items[it].id }) { index ->
                val item = items[index]
                Column(Modifier.animateItem()) {
                    ItemRow(item, labelFor(item), onClick = { onOpen(item.id) }, snippet = snippetFor(item))
                    if (index < items.lastIndex) RowDivider()
                }
            }
        }
    }
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
