package app.secondbrain

import app.secondbrain.data.Accent
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class AccentTest {

    private fun luminance(argb: Long): Double {
        val (r, g, b) = listOf(16, 8, 0).map { shift ->
            val c = ((argb shr shift) and 0xFF) / 255.0
            if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrast(a: Long, b: Long): Double {
        val (low, high) = listOf(luminance(a), luminance(b)).sorted()
        return (high + 0.05) / (low + 0.05)
    }

    @Test
    fun everyAccentIsReadableInBothThemes() {
        // The darkest light surface and the lightest dark surface that accent text sits on.
        val lightSurface = 0xFFF2F2F2
        val darkSurface = 0xFF1C1C1C
        Accent.entries.forEach { accent ->
            assertTrue("${accent.label} on light", contrast(accent.light.color, lightSurface) >= 4.5)
            assertTrue("${accent.label} on dark", contrast(accent.dark.color, darkSurface) >= 4.5)
            listOf(accent.light, accent.dark).forEach {
                assertTrue("${accent.label} container", contrast(it.onContainer, it.container) >= 4.5)
            }
        }
    }
}
