package app.secondbrain

import app.secondbrain.brain.Clusterer
import app.secondbrain.brain.Sphere
import app.secondbrain.brain.Vec3
import app.secondbrain.tagging.TagKind
import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrainTest {

    @Test fun `every item lands in exactly one cluster`() {
        val entries = listOf(
            entry(1, tags = listOf("food", "recipes")),
            entry(2, tags = listOf("food")),
            entry(3, tags = listOf("travel")),
            entry(4, tags = listOf("travel", "food")),
            entry(5),
        )
        val map = Clusterer.build(entries)
        val placed = map.clusters.flatMap { c -> c.entries.map { it.id } }
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), placed.sorted())
        assertEquals("food", map.clusters.first().tag)
        assertEquals(Clusterer.LOOSE, map.clusters.last().tag)
        assertEquals(listOf(5L), map.clusters.last().entries.map { it.id })
    }

    @Test fun `clusters sharing tags are linked`() {
        val map = Clusterer.build(listOf(entry(1, tags = listOf("food", "weekend")), entry(2, tags = listOf("food")), entry(3, tags = listOf("travel", "weekend"))))
        val tags = map.links.map { setOf(map.clusters[it.a].tag, map.clusters[it.b].tag) }
        assertTrue(tags.toString(), setOf("food", "travel") in tags)
        assertTrue(map.links.all { it.strength > 0f && it.strength <= 1f })
    }

    @Test fun `a word used once does not make a cluster`() {
        val map = Clusterer.build(listOf(entry(1, tags = listOf("zebra"), kind = TagKind.KEYWORD)))
        assertEquals(listOf(Clusterer.LOOSE), map.clusters.map { it.tag })
    }

    @Test fun `format and time tags never make clusters`() {
        val map = Clusterer.build(listOf(entry(1, tags = listOf("note"), kind = TagKind.FORMAT), entry(2, tags = listOf("note"), kind = TagKind.FORMAT)))
        assertEquals(listOf(Clusterer.LOOSE), map.clusters.map { it.tag })
    }

    @Test fun `clusters get different colours while there are enough`() {
        val map = Clusterer.build((1L..Clusterer.COLORS.toLong()).map { entry(it, tags = listOf("t$it")) })
        assertEquals(Clusterer.COLORS, map.clusters.map { it.color }.distinct().size)
    }

    @Test fun `cluster count is capped and nothing is lost`() {
        val map = Clusterer.build((1L..60L).map { entry(it, tags = listOf("t$it")) }, maxClusters = 10)
        assertEquals(11, map.clusters.size)
        assertEquals(60, map.clusters.sumOf { it.entries.size })
    }

    @Test fun `points are spread on the unit sphere`() {
        for (n in listOf(1, 2, 7, 15)) {
            val points = Sphere.distribute(n)
            assertEquals(n, points.size)
            assertTrue(points.all { kotlin.math.abs(it.length() - 1f) < 1e-4f })
        }
        assertTrue(Sphere.satellites(Vec3(0f, 1f, 0f), 12, 0.4f).all { it.dot(Vec3(0f, 1f, 0f)) >= kotlin.math.cos(0.4f) - 1e-4f })
    }

    @Test fun `facing turns any point towards the viewer`() {
        (Sphere.distribute(15) + Vec3(0f, 0f, -1f)).forEach { point ->
            val (yaw, pitch) = Sphere.facing(point)
            val turned = Sphere.rotate(point, yaw, pitch)
            assertTrue("$point -> $turned", turned.z > 0.999f)
        }
    }

    @Test fun `dragging right and down moves the front the same way`() {
        val right = Sphere.project(Sphere.rotate(Vec3(0f, 0f, 1f), 0.3f, 0f))
        val down = Sphere.project(Sphere.rotate(Vec3(0f, 0f, 1f), 0f, 0.3f))
        assertTrue(right.x > 0f)
        assertTrue(down.y > 0f)
    }

    @Test fun `turn takes the short way round`() {
        assertEquals(0.2f, Sphere.turn((2 * PI).toFloat() - 0.1f, 0.1f), 1e-4f)
        assertEquals(-0.2f, Sphere.turn(0.1f, -0.1f), 1e-4f)
    }

    @Test fun `arcs start and end on the surface and bow outwards`() {
        val arc = Sphere.arc(Vec3(1f, 0f, 0f), Vec3(0f, 1f, 0f), 10, 0.2f)
        assertEquals(1f, arc.first().length(), 1e-4f)
        assertEquals(1f, arc.last().length(), 1e-4f)
        assertEquals(1.2f, arc[5].length(), 1e-4f)
    }
}
