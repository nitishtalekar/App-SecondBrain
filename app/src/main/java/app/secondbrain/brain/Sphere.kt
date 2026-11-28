package app.secondbrain.brain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** A point in the globe's space: x right, y up, z towards the viewer. */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 = length().let { if (it < 1e-6f) Vec3(0f, 0f, 1f) else times(1f / it) }
}

/** Where a point lands on screen, relative to the globe's centre, in units of its radius. */
data class Projected(val x: Float, val y: Float, val scale: Float, val depth: Float)

object Sphere {
    private const val CAMERA = 3.4f
    private val GOLDEN_ANGLE = PI * (3.0 - sqrt(5.0))

    /** [count] points spread evenly over the unit sphere, avoiding the poles. */
    fun distribute(count: Int): List<Vec3> {
        if (count == 1) return listOf(Vec3(0f, 0f, 1f))
        return List(count) { i ->
            // Squeezed towards the equator so labels near the top and bottom stay readable.
            val y = (1.0 - 2.0 * (i + 0.5) / count) * 0.82
            val ring = sqrt(1.0 - y * y)
            val angle = GOLDEN_ANGLE * i
            Vec3((sin(angle) * ring).toFloat(), y.toFloat(), (cos(angle) * ring).toFloat())
        }
    }

    /** [count] points scattered in a sunflower spiral on the surface around [center], within [spread] radians. */
    fun satellites(center: Vec3, count: Int, spread: Float): List<Vec3> {
        val helper = if (abs(center.y) < 0.9f) Vec3(0f, 1f, 0f) else Vec3(1f, 0f, 0f)
        val east = helper.cross(center).normalized()
        val north = center.cross(east)
        return List(count) { i ->
            // The inner third is left clear for the cluster itself.
            val radius = spread * (0.38f + 0.62f * sqrt((i + 1f) / count))
            val angle = GOLDEN_ANGLE * i
            val offset = east * cos(angle).toFloat() + north * sin(angle).toFloat()
            (center * cos(radius) + offset * sin(radius)).normalized()
        }
    }

    /** Turn about the vertical axis by [yaw], then tip about the horizontal axis by [pitch]. */
    fun rotate(v: Vec3, yaw: Float, pitch: Float): Vec3 {
        val cy = cos(yaw)
        val sy = sin(yaw)
        val x = v.x * cy + v.z * sy
        val z = -v.x * sy + v.z * cy
        val cp = cos(pitch)
        val sp = sin(pitch)
        return Vec3(x, v.y * cp - z * sp, v.y * sp + z * cp)
    }

    /** The (yaw, pitch) that brings [v] to face the viewer. */
    fun facing(v: Vec3): Pair<Float, Float> = atan2(-v.x, v.z) to atan2(v.y, hypot(v.x, v.z))

    fun project(v: Vec3): Projected {
        val scale = CAMERA / (CAMERA - v.z)
        return Projected(v.x * scale, -v.y * scale, scale, v.z)
    }

    /** The shortest signed turn from [from] to [to], in radians. */
    fun turn(from: Float, to: Float): Float {
        val full = (2 * PI).toFloat()
        var delta = (to - from) % full
        if (delta > full / 2) delta -= full
        if (delta < -full / 2) delta += full
        return delta
    }

    /** Points along the great circle from [a] to [b], bowed [lift] above the surface in the middle. */
    fun arc(a: Vec3, b: Vec3, steps: Int, lift: Float): List<Vec3> {
        val angle = acos(a.dot(b).coerceIn(-1f, 1f))
        val s = sin(angle)
        return List(steps + 1) { i ->
            val t = i / steps.toFloat()
            val p = if (s < 1e-4f) a else a * (sin((1 - t) * angle) / s) + b * (sin(t * angle) / s)
            p.normalized() * (1f + lift * sin(PI.toFloat() * t))
        }
    }
}
