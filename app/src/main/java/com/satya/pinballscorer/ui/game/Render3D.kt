package com.satya.pinballscorer.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.hypot

/**
 * Shared lighting model for the whole table.
 *
 * Everything is lit by a single key light in the upper-left. Objects get three
 * passes — a contact shadow offset down-right, a body gradient running light-to-dark
 * along the light axis, and a small specular highlight — which is what sells the
 * raised, physical look on a flat canvas.
 */
object Light {
    /** Unit vector from surface toward the light. */
    val dir = Offset(-0.55f, -0.83f)

    /** Offset of a cast shadow for an object standing [height] units above the bed. */
    fun shadowOffset(height: Float) = Offset(-dir.x * height, -dir.y * height)
}

object Palette {
    val voidTop = Color(0xFF05060F)
    val voidMid = Color(0xFF0B1030)
    val voidBottom = Color(0xFF03040A)

    val nebulaA = Color(0xFF2A1B5E)
    val nebulaB = Color(0xFF0E3B5C)

    val bedTop = Color(0xFF1B2450)
    val bedMid = Color(0xFF121A3A)
    val bedBottom = Color(0xFF080C1E)

    val chromeLight = Color(0xFFE8F1FF)
    val chromeMid = Color(0xFF8BA0C4)
    val chromeDark = Color(0xFF2C3550)

    val neonCyan = Color(0xFF4DE1FF)
    val neonMagenta = Color(0xFFFF4D9D)
    val neonLime = Color(0xFF9BFFB0)
    val neonAmber = Color(0xFFFFC23D)
    val neonViolet = Color(0xFFB388FF)
}

// ---------------------------------------------------------------------------
// Primitive 3D helpers
// ---------------------------------------------------------------------------

/** Soft contact shadow: concentric translucent rings, cheap and blur-free. */
fun DrawScope.drawSoftShadow(center: Offset, radius: Float, height: Float, strength: Float = 0.55f) {
    val o = Light.shadowOffset(height)
    val c = Offset(center.x + o.x, center.y + o.y)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = strength),
                Color.Black.copy(alpha = strength * 0.45f),
                Color.Transparent
            ),
            center = c,
            radius = radius * 1.45f
        ),
        radius = radius * 1.45f,
        center = c
    )
}

/**
 * A circular object standing proud of the playfield: shadow, side wall (the visible
 * "thickness"), lit cap and specular dot.
 */
fun DrawScope.drawRaisedDisc(
    center: Offset,
    radius: Float,
    height: Float,
    capLight: Color,
    capDark: Color,
    rimColor: Color,
    specular: Float = 0.9f
) {
    drawSoftShadow(center, radius, height)

    // Side wall, pushed away from the light so you read the object's height.
    val wallOffset = Light.shadowOffset(height * 0.55f)
    drawCircle(
        color = capDark.copy(alpha = 0.95f),
        radius = radius,
        center = Offset(center.x + wallOffset.x, center.y + wallOffset.y)
    )

    // Lit cap.
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(capLight, capDark),
            start = Offset(center.x + Light.dir.x * radius, center.y + Light.dir.y * radius),
            end = Offset(center.x - Light.dir.x * radius, center.y - Light.dir.y * radius)
        ),
        radius = radius,
        center = center
    )

    // Bevel rim: bright on the lit side, dark opposite.
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(rimColor, rimColor.copy(alpha = 0.12f)),
            start = Offset(center.x + Light.dir.x * radius, center.y + Light.dir.y * radius),
            end = Offset(center.x - Light.dir.x * radius, center.y - Light.dir.y * radius)
        ),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.10f)
    )

    if (specular > 0f) {
        val sp = Offset(center.x + Light.dir.x * radius * 0.45f, center.y + Light.dir.y * radius * 0.45f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.85f * specular), Color.Transparent),
                center = sp,
                radius = radius * 0.42f
            ),
            radius = radius * 0.42f,
            center = sp
        )
    }
}

/** Polished metal rail drawn as a capsule with a highlight running along its lit edge. */
fun DrawScope.drawMetalRail(a: Offset, b: Offset, width: Float, tint: Color = Palette.chromeMid) {
    val o = Light.shadowOffset(width * 0.5f)
    drawLine(
        color = Color.Black.copy(alpha = 0.5f),
        start = Offset(a.x + o.x, a.y + o.y),
        end = Offset(b.x + o.x, b.y + o.y),
        strokeWidth = width * 1.15f,
        cap = StrokeCap.Round
    )
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(Palette.chromeLight, tint, Color(0xFF1B2238)),
            start = Offset(a.x + Light.dir.x * width, a.y + Light.dir.y * width),
            end = Offset(a.x - Light.dir.x * width, a.y - Light.dir.y * width)
        ),
        start = a, end = b,
        strokeWidth = width,
        cap = StrokeCap.Round
    )
    // Specular streak along the top edge.
    val hl = Offset(Light.dir.x * width * 0.26f, Light.dir.y * width * 0.26f)
    drawLine(
        color = Color.White.copy(alpha = 0.55f),
        start = Offset(a.x + hl.x, a.y + hl.y),
        end = Offset(b.x + hl.x, b.y + hl.y),
        strokeWidth = width * 0.24f,
        cap = StrokeCap.Round
    )
}

/** A lamp under the playfield surface: diffuse bloom plus a crisp lens. */
fun DrawScope.drawInsertGlow(center: Offset, radius: Float, color: Color, intensity: Float) {
    if (intensity <= 0.01f) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.55f * intensity),
                color.copy(alpha = 0.18f * intensity),
                Color.Transparent
            ),
            center = center,
            radius = radius * 2.6f
        ),
        radius = radius * 2.6f,
        center = center
    )
    drawCircle(color = color.copy(alpha = 0.85f * intensity), radius = radius, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.5f * intensity),
        radius = radius * 0.45f,
        center = Offset(center.x + Light.dir.x * radius * 0.3f, center.y + Light.dir.y * radius * 0.3f)
    )
}

/** Glow halo used behind neon artwork and active features. */
fun DrawScope.drawGlow(center: Offset, radius: Float, color: Color, alpha: Float) {
    if (alpha <= 0.01f) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/**
 * Extrudes [path] toward the viewer by drawing stacked dark copies before the lit
 * face, giving flat shapes a sense of real thickness.
 */
fun DrawScope.drawExtrudedPath(
    path: Path,
    height: Float,
    faceBrush: Brush,
    sideColor: Color,
    layers: Int = 6
) {
    val o = Light.shadowOffset(height)
    // Cast shadow.
    translateScope(o.x * 1.3f, o.y * 1.3f) {
        drawPath(path, Color.Black.copy(alpha = 0.45f))
    }
    for (i in layers downTo 1) {
        val t = i.toFloat() / layers
        translateScope(o.x * t, o.y * t) {
            drawPath(path, sideColor.copy(alpha = 0.95f))
        }
    }
    drawPath(path, faceBrush)
}

/** Small helper so extrusion layers read clearly. */
inline fun DrawScope.translateScope(dx: Float, dy: Float, block: DrawScope.() -> Unit) {
    drawContext.transform.translate(dx, dy)
    block()
    drawContext.transform.translate(-dx, -dy)
}

/** Builds a tapered flipper silhouette from pivot to tip. */
fun flipperPath(pivot: Offset, tip: Offset, baseRadius: Float, tipRadius: Float): Path {
    val d = Offset(tip.x - pivot.x, tip.y - pivot.y)
    val len = hypot(d.x, d.y).coerceAtLeast(1e-3f)
    val ux = d.x / len
    val uy = d.y / len
    val px = -uy
    val py = ux
    return Path().apply {
        moveTo(pivot.x + px * baseRadius, pivot.y + py * baseRadius)
        lineTo(tip.x + px * tipRadius, tip.y + py * tipRadius)
        // Round the tip.
        quadraticBezierTo(
            tip.x + ux * tipRadius * 1.5f, tip.y + uy * tipRadius * 1.5f,
            tip.x - px * tipRadius, tip.y - py * tipRadius
        )
        lineTo(pivot.x - px * baseRadius, pivot.y - py * baseRadius)
        quadraticBezierTo(
            pivot.x - ux * baseRadius * 1.5f, pivot.y - uy * baseRadius * 1.5f,
            pivot.x + px * baseRadius, pivot.y + py * baseRadius
        )
        close()
    }
}

/** Quad path through four corners. */
fun quadPath(a: Offset, b: Offset, c: Offset, d: Offset): Path = Path().apply {
    moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
}

/** Triangle path through three corners. */
fun trianglePath(a: Offset, b: Offset, c: Offset): Path = Path().apply {
    moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close()
}
