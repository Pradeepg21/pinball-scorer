package com.satya.pinballscorer.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

/**
 * The table is simulated in a fixed virtual coordinate space and only scaled at draw
 * time. That keeps physics tuning identical on every device instead of drifting with
 * screen density, which is what made the old pixel-space engine feel different on
 * every phone.
 */
const val TABLE_W = 1000f
const val TABLE_H = 1778f

/** A static collision surface: a segment with thickness, so it behaves as a capsule. */
data class Wall(
    val a: Offset,
    val b: Offset,
    val restitution: Float = 0.38f,
    val friction: Float = 0.03f,
    val thickness: Float = 6f,
    val kind: WallKind = WallKind.RAIL
)

enum class WallKind { RAIL, POST, APRON }

fun Offset.length(): Float = hypot(x, y)

fun Offset.normalizedOrZero(): Offset {
    val l = length()
    return if (l < 1e-5f) Offset.Zero else Offset(x / l, y / l)
}

/** Closest point to [p] on segment [a]-[b]. */
fun closestPointOnSegment(p: Offset, a: Offset, b: Offset): Offset {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lenSq = abx * abx + aby * aby
    if (lenSq < 1e-6f) return a
    val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / lenSq).coerceIn(0f, 1f)
    return Offset(a.x + abx * t, a.y + aby * t)
}

fun cubicBezier(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    val a = u * u * u
    val b = 3f * u * u * t
    val c = 3f * u * t * t
    val d = t * t * t
    return Offset(
        a * p0.x + b * p1.x + c * p2.x + d * p3.x,
        a * p0.y + b * p1.y + c * p2.y + d * p3.y
    )
}

/** Samples an arc into line segments, used for the table's top arch. */
fun arcWalls(
    center: Offset,
    radius: Float,
    startDeg: Float,
    endDeg: Float,
    segments: Int,
    restitution: Float = 0.38f
): List<Wall> {
    val walls = ArrayList<Wall>(segments)
    var prev: Offset? = null
    for (i in 0..segments) {
        val deg = startDeg + (endDeg - startDeg) * (i.toFloat() / segments)
        val rad = Math.toRadians(deg.toDouble())
        val p = Offset(
            center.x + radius * kotlin.math.cos(rad).toFloat(),
            center.y + radius * kotlin.math.sin(rad).toFloat()
        )
        prev?.let { walls.add(Wall(it, p, restitution)) }
        prev = p
    }
    return walls
}
