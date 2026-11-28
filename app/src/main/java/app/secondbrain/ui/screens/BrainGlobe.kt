package app.secondbrain.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.secondbrain.brain.BrainMap
import app.secondbrain.brain.Projected
import app.secondbrain.brain.Sphere
import app.secondbrain.brain.Vec3
import app.secondbrain.data.Entry
import app.secondbrain.tagging.tagLabel
import app.secondbrain.ui.theme.LocalClusterColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch

private const val MIN_ZOOM = 0.7f
private const val MAX_ZOOM = 2.2f
private const val FOCUS_ZOOM = 1.12f
private const val MAX_PITCH = 1.35f
private const val IDLE_SPIN = 0.11f
private const val MAX_DOTS = 36

/** How the globe is turned. Read only while drawing, so spinning never recomposes the screen. */
@Stable
class GlobeState(yaw: Float = 0.35f, pitch: Float = 0.22f, zoom: Float = 1f) {
    var yaw by mutableFloatStateOf(yaw)
    var pitch by mutableFloatStateOf(pitch)
    var zoom by mutableFloatStateOf(zoom)
    internal var dragging by mutableStateOf(false)
    internal var gliding = false
    internal var spinYaw = 0f
    internal var spinPitch = 0f
    /** Bumped on every touch so a running glide to a cluster gives way to the finger. */
    internal var touches by mutableIntStateOf(0)

    companion object {
        val Saver: Saver<GlobeState, Any> = listSaver(
            save = { listOf(it.yaw, it.pitch, it.zoom) },
            restore = { GlobeState(it[0], it[1], it[2]) },
        )
    }
}

@Composable
fun rememberGlobeState(): GlobeState = rememberSaveable(saver = GlobeState.Saver) { GlobeState() }

private class Hit(val position: Offset, val radius: Float, val cluster: Int, val entryId: Long?, val depth: Float)

/** Meridians and parallels, computed once. */
private val gridLines: List<List<Vec3>> = buildList {
    fun point(latitude: Double, longitude: Double) = Vec3(
        (cos(latitude) * sin(longitude)).toFloat(),
        sin(latitude).toFloat(),
        (cos(latitude) * cos(longitude)).toFloat(),
    )
    for (m in 0 until 12) {
        val longitude = m * PI / 6
        add(List(37) { i -> point(-PI / 2 + i * PI / 36, longitude) })
    }
    for (p in -2..2) {
        val latitude = p * PI / 6
        add(List(73) { i -> point(latitude, i * PI / 36) })
    }
}

/**
 * The brain as a globe: each cluster is a lit body on a wire sphere, its items orbit it, and arcs
 * join clusters that share tags. Drag to turn, pinch to zoom, tap to travel to a cluster.
 */
@Composable
fun BrainGlobe(
    map: BrainMap,
    selected: String?,
    state: GlobeState,
    onSelect: (String?) -> Unit,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val palette = LocalClusterColors.current
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val hubStyle = MaterialTheme.typography.labelLarge.copy(color = colors.onSurface)
    val itemStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurface)
    val context = LocalContext.current
    val motion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    val hubs = remember(map.clusters.size) { Sphere.distribute(map.clusters.size) }
    val focusIndex = map.clusters.indexOfFirst { it.tag == selected }
    val baseSpread = (1.15f / sqrt(map.clusters.size.coerceAtLeast(1).toFloat())).coerceAtMost(0.42f)
    val arcs = remember(map) { map.links.map { Sphere.arc(hubs[it.a], hubs[it.b], 24, 0.14f) } }
    val restingDots = remember(map) {
        map.clusters.mapIndexed { i, c -> Sphere.satellites(hubs[i], min(c.entries.size, MAX_DOTS), baseSpread) }
    }
    val focus = remember { Animatable(0f) }
    // Remembered so the globe can fade the old cluster back in after it is deselected.
    var lastFocus by remember { mutableIntStateOf(-1) }
    if (focusIndex >= 0) lastFocus = focusIndex
    val assemble = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(Unit) { assemble.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    val currentMap by rememberUpdatedState(map)
    val currentSelected by rememberUpdatedState(selected)
    val currentSelect by rememberUpdatedState(onSelect)
    val currentOpen by rememberUpdatedState(onOpen)
    val hits = remember { ArrayList<Hit>() }

    // Travel to the chosen cluster (or ease back out).
    LaunchedEffect(selected, focusIndex) {
        state.spinYaw = 0f
        state.spinPitch = 0f
        val fromYaw = state.yaw
        val fromPitch = state.pitch
        val fromZoom = state.zoom
        val target = hubs.getOrNull(focusIndex)?.let(Sphere::facing)
        val yawBy = target?.let { Sphere.turn(fromYaw, it.first) } ?: 0f
        val pitchBy = target?.let { it.second.coerceIn(-MAX_PITCH, MAX_PITCH) - fromPitch } ?: 0f
        val toZoom = when {
            target != null -> FOCUS_ZOOM
            focus.value > 0f -> min(fromZoom, 1f)
            else -> fromZoom
        }
        val touchesAtStart = state.touches
        launch {
            if (target != null) focus.snapTo(0f)
            focus.animateTo(if (target != null) 1f else 0f, tween(if (motion) 750 else 0, easing = FastOutSlowInEasing))
        }
        state.gliding = true
        try {
            animate(0f, 1f, animationSpec = tween(if (motion) 750 else 0, easing = FastOutSlowInEasing)) { t, _ ->
                if (state.touches == touchesAtStart) {
                    state.yaw = fromYaw + yawBy * t
                    state.pitch = fromPitch + pitchBy * t
                    state.zoom = fromZoom + (toZoom - fromZoom) * t
                }
            }
        } finally {
            state.gliding = false
        }
    }

    // Momentum after a flick, and a slow idle turn.
    LaunchedEffect(motion, selected) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
            last = now
            if (state.dragging || state.gliding) continue
            if (abs(state.spinYaw) > 0.03f || abs(state.spinPitch) > 0.03f) {
                state.yaw += state.spinYaw * dt
                state.pitch = (state.pitch + state.spinPitch * dt).coerceIn(-MAX_PITCH, MAX_PITCH)
                val decay = exp(-2.8f * dt)
                state.spinYaw *= decay
                state.spinPitch *= decay
            } else if (selected == null && motion) {
                state.yaw += IDLE_SPIN * dt
            }
        }
    }

    val description = if (map.clusters.isEmpty()) "An empty globe"
    else "Globe of ${map.clusters.size} clusters. The list of clusters below does the same job."

    Canvas(
        modifier
            .clipToBounds()
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    state.dragging = true
                    state.touches++
                    state.spinYaw = 0f
                    state.spinPitch = 0f
                    var travelled = Offset.Zero
                    var moved = false
                    var pinched = false
                    var cancelled = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.isConsumed }) {
                                cancelled = true
                                break
                            }
                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            travelled += pan
                            if (event.changes.count { it.pressed } > 1) pinched = true
                            if (!moved && (travelled.getDistance() > viewConfiguration.touchSlop || pinched)) moved = true
                            if (moved) {
                                val radius = min(size.width, size.height) * 0.4f * state.zoom
                                state.yaw += pan.x / radius
                                state.pitch = (state.pitch + pan.y / radius).coerceIn(-MAX_PITCH, MAX_PITCH)
                                state.zoom = (state.zoom * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                            event.changes.firstOrNull { it.id == down.id }?.let { tracker.addPosition(it.uptimeMillis, it.position) }
                            if (event.changes.none { it.pressed }) break
                        }
                    } finally {
                        state.dragging = false
                    }
                    if (cancelled) return@awaitEachGesture
                    if (moved) {
                        if (!pinched) {
                            val velocity = tracker.calculateVelocity()
                            val radius = min(size.width, size.height) * 0.4f * state.zoom
                            state.spinYaw = (velocity.x / radius).coerceIn(-7f, 7f)
                            state.spinPitch = (velocity.y / radius).coerceIn(-7f, 7f)
                        }
                        return@awaitEachGesture
                    }
                    // A tap: the nearest thing under the finger, front-most first.
                    val hit = hits.filter { (it.position - down.position).getDistance() <= it.radius }
                        .minByOrNull { (it.position - down.position).getDistance() - it.depth * 12.dp.toPx() }
                    val clusters = currentMap.clusters
                    val tag = hit?.let { clusters.getOrNull(it.cluster)?.tag }
                    when {
                        hit == null -> currentSelect(null)
                        hit.entryId != null && tag == currentSelected -> currentOpen(hit.entryId)
                        else -> currentSelect(tag)
                    }
                }
            },
    ) {
        hits.clear()
        val yaw = state.yaw
        val pitch = state.pitch
        val radius = min(size.width, size.height) * 0.4f * state.zoom
        val middle = center
        val f = focus.value
        val grown = 0.25f + 0.75f * assemble.value
        fun place(p: Projected) = Offset(middle.x + p.x * radius, middle.y + p.y * radius)

        drawCircle(colors.surface, radius * 1.046f, middle)
        drawBody(middle, radius * 1.046f, colors.primary)
        drawGrid(yaw, pitch, middle, radius, colors.primary)
        if (map.clusters.isEmpty()) return@Canvas

        val turned = hubs.map { Sphere.rotate(it, yaw, pitch) }

        // Arcs between clusters that share tags.
        map.links.forEachIndexed { linkIndex, link ->
            val touchesFocus = link.a == lastFocus || link.b == lastFocus
            val emphasis = if (touchesFocus) 1f + f else 1f - 0.75f * f
            val points = arcs[linkIndex].map { Sphere.rotate(it * grown, yaw, pitch) }
            val from = palette[map.clusters[link.a].color % palette.size]
            val to = palette[map.clusters[link.b].color % palette.size]
            for (i in 0 until points.lastIndex) {
                val depth = (points[i].z + points[i + 1].z) / 2f
                val alpha = (0.06f + 0.34f * ((depth + 1f) / 2f)) * (0.45f + 0.55f * link.strength) * emphasis
                drawLine(
                    lerp(from, to, i / points.lastIndex.toFloat()).copy(alpha = alpha.coerceIn(0f, 0.85f)),
                    place(Sphere.project(points[i])),
                    place(Sphere.project(points[i + 1])),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Butt,
                )
            }
        }

        val itemLabels = ArrayList<Triple<Float, Offset, String>>()
        val hubLabels = ArrayList<LabelAt>()

        // Far things first, so near ones overlap them.
        turned.indices.sortedBy { turned[it].z }.forEach { index ->
            val cluster = map.clusters[index]
            val color = palette[cluster.color % palette.size]
            val focused = index == lastFocus
            val dim = if (focused) 1f else 1f - 0.72f * f
            val hubPoint = Sphere.project(turned[index] * grown)
            val hubAt = place(hubPoint)
            val hubAlpha = depthAlpha(turned[index].z) * dim
            val hubRadius = (6f + 3f * sqrt(cluster.entries.size.toFloat())).coerceAtMost(20f).dp.toPx() *
                hubPoint.scale * 0.82f * sqrt(state.zoom)

            val shown = cluster.entries.take(MAX_DOTS)
            val dots = if (focused && f > 0f) Sphere.satellites(hubs[index], shown.size, baseSpread * (1f + 0.9f * f))
            else restingDots[index]
            val drawn = dots.mapIndexed { i, dot -> shown[i] to Sphere.rotate(dot * grown, yaw, pitch) }
                .sortedBy { it.second.z }

            fun drawDot(entry: Entry, at: Vec3) {
                val p = Sphere.project(at)
                val offset = place(p)
                val alpha = depthAlpha(at.z) * dim
                val dotRadius = (if (focused) 3f + 3f * f else 3f).dp.toPx() * p.scale
                drawLine(color.copy(alpha = alpha * if (focused) 0.2f + 0.25f * f else 0.18f), hubAt, offset, strokeWidth = 1.dp.toPx())
                if (entry.done) {
                    drawCircle(color.copy(alpha = alpha), dotRadius, offset, style = Stroke(1.2.dp.toPx()))
                } else {
                    drawCircle(color.copy(alpha = alpha), dotRadius, offset)
                    if (entry.openAction != null) {
                        drawCircle(colors.tertiary.copy(alpha = alpha), dotRadius + 3.dp.toPx(), offset, style = Stroke(1.4.dp.toPx()))
                    }
                }
                if (at.z > 0f) {
                    if (focused && f > 0.5f) {
                        hits += Hit(offset, 22.dp.toPx(), index, entry.id, at.z)
                        itemLabels += Triple(at.z, offset + Offset(0f, dotRadius + 3.dp.toPx()), entry.title)
                    } else {
                        hits += Hit(offset, 14.dp.toPx(), index, null, at.z - 0.5f)
                    }
                }
            }

            // Dots behind their cluster, the cluster itself, then dots in front.
            drawn.filter { it.second.z <= turned[index].z }.forEach { drawDot(it.first, it.second) }
            drawOrb(hubAt, hubRadius, color, colors.surface, hubAlpha)
            drawn.filter { it.second.z > turned[index].z }.forEach { drawDot(it.first, it.second) }

            if (turned[index].z > -0.05f) {
                hits += Hit(hubAt, maxOf(hubRadius + 10.dp.toPx(), 26.dp.toPx()), index, null, turned[index].z)
                hubLabels += LabelAt(
                    priority = (if (focused && f > 0f) 10f else 0f) + turned[index].z,
                    top = hubAt + Offset(0f, hubRadius + 6.dp.toPx()),
                    text = tagLabel(cluster.tag),
                    alpha = (0.25f + 0.75f * turned[index].z.coerceIn(0f, 1f)) * dim * assemble.value,
                )
            }
        }

        val occupied = ArrayList<Rect>()
        hubLabels.sortedByDescending { it.priority }.forEach { label ->
            drawLabel(measurer, label.text, hubStyle, label.top, label.alpha, colors.surface, occupied)
        }
        if (f > 0.5f) {
            itemLabels.sortedByDescending { it.first }.take(8).forEach { (depth, top, title) ->
                val short = if (title.length > 22) title.take(21).trimEnd() + "…" else title
                drawLabel(measurer, short, itemStyle, top, (f - 0.5f) * 2f * depth.coerceIn(0f, 1f), colors.surface, occupied)
            }
        }
    }
}

private class LabelAt(val priority: Float, val top: Offset, val text: String, val alpha: Float)

private fun depthAlpha(z: Float): Float = (0.22f + 0.78f * ((z + 1f) / 2f)).coerceIn(0f, 1f)

private fun DrawScope.drawBody(middle: Offset, radius: Float, tint: Color) {
    drawCircle(
        Brush.radialGradient(
            0f to tint.copy(alpha = 0.16f),
            0.7f to tint.copy(alpha = 0.06f),
            1f to tint.copy(alpha = 0.11f),
            center = middle + Offset(-radius * 0.22f, -radius * 0.28f),
            radius = radius * 1.35f,
        ),
        radius, middle,
    )
    drawCircle(tint.copy(alpha = 0.28f), radius, middle, style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawGrid(yaw: Float, pitch: Float, middle: Offset, radius: Float, tint: Color) {
    // Segments are batched by depth: four paths instead of hundreds of lines.
    val buckets = List(4) { Path() }
    gridLines.forEach { line ->
        var previous = Sphere.rotate(line[0], yaw, pitch)
        var previousAt = Sphere.project(previous)
        for (i in 1 until line.size) {
            val next = Sphere.rotate(line[i], yaw, pitch)
            val nextAt = Sphere.project(next)
            val bucket = (((previous.z + next.z) / 2f + 1f) * 2f).toInt().coerceIn(0, 3)
            buckets[bucket].moveTo(middle.x + previousAt.x * radius, middle.y + previousAt.y * radius)
            buckets[bucket].lineTo(middle.x + nextAt.x * radius, middle.y + nextAt.y * radius)
            previous = next
            previousAt = nextAt
        }
    }
    val alphas = floatArrayOf(0.05f, 0.08f, 0.15f, 0.24f)
    buckets.forEachIndexed { i, path -> drawPath(path, tint.copy(alpha = alphas[i]), style = Stroke(1.dp.toPx())) }
}

/** A cluster: a shaded ball with an orbit ring. */
private fun DrawScope.drawOrb(at: Offset, radius: Float, color: Color, ground: Color, alpha: Float) {
    // Opaque base first so the grid doesn't show through near clusters.
    drawCircle(ground.copy(alpha = alpha), radius, at)
    drawCircle(
        Brush.radialGradient(
            0f to lerp(color, Color.White, 0.55f),
            0.55f to color,
            1f to lerp(color, ground, 0.35f),
            center = at + Offset(-radius * 0.35f, -radius * 0.4f),
            radius = radius * 1.55f,
        ),
        radius, at, alpha = alpha,
    )
    drawCircle(color.copy(alpha = alpha * 0.5f), radius + 4.dp.toPx(), at, style = Stroke(1.dp.toPx()))
}

/** Draws a centred label unless it would collide with one already placed. */
private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    top: Offset,
    alpha: Float,
    ground: Color,
    occupied: MutableList<Rect>,
) {
    if (alpha <= 0.05f) return
    val layout: TextLayoutResult = measurer.measure(text, style, maxLines = 1)
    val padX = 6.dp.toPx()
    val width = layout.size.width.toFloat()
    val height = layout.size.height.toFloat()
    val x = (top.x - width / 2f).coerceIn(padX, maxOf(padX, size.width - width - padX))
    val rect = Rect(x - padX, top.y, x + width + padX, top.y + height)
    if (rect.bottom > size.height || rect.top < 0f || occupied.any { it.overlaps(rect) }) return
    occupied += rect
    drawRoundRect(ground.copy(alpha = 0.78f * alpha), rect.topLeft, rect.size, CornerRadius(height / 2f))
    drawText(layout, topLeft = Offset(x, top.y), alpha = alpha.coerceIn(0f, 1f))
}
