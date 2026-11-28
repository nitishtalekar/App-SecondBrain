package app.secondbrain.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** A flat, faint grid of dots, like engineering paper. It is centred so both side margins match. */
fun Modifier.dotGrid(ground: Color, dot: Color): Modifier =
    background(ground).drawWithCache {
        val step = 20.dp.toPx()
        // Read here: inside buildList, `size` would mean the list's own.
        val area = size
        val left = (area.width % step) / 2f
        val dots = buildList {
            var y = step / 2f
            while (y < area.height) {
                var x = left
                while (x <= area.width) {
                    add(Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        onDrawBehind {
            drawPoints(dots, PointMode.Points, dot, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
