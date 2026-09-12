package com.satya.pinballscorer.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.satya.pinballscorer.game.GameEngine
import com.satya.pinballscorer.game.NetBasket
import com.satya.pinballscorer.game.TABLE_H
import com.satya.pinballscorer.game.TABLE_W
import com.satya.pinballscorer.game.Table
import com.satya.pinballscorer.game.WallKind
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class Star(val x: Float, val y: Float, val radius: Float, val phase: Float, val alpha: Float)

/**
 * Deep-space backdrop behind the cabinet. Drawn in canvas space (not table space) so
 * it always fills the device, including the letterbox bars either side of the table.
 */
fun DrawScope.drawBackdrop(stars: List<Star>, time: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Palette.voidTop, Palette.voidMid, Palette.voidBottom)
        ),
        size = size
    )

    // Two soft nebula clouds give the background depth without any bitmap assets.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Palette.nebulaA.copy(alpha = 0.55f), Color.Transparent),
            center = Offset(size.width * 0.18f, size.height * 0.22f),
            radius = size.width * 0.75f
        ),
        radius = size.width * 0.75f,
        center = Offset(size.width * 0.18f, size.height * 0.22f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Palette.nebulaB.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(size.width * 0.86f, size.height * 0.74f),
            radius = size.width * 0.7f
        ),
        radius = size.width * 0.7f,
        center = Offset(size.width * 0.86f, size.height * 0.74f)
    )

    stars.forEach { s ->
        val twinkle = 0.55f + 0.45f * sin(time * 1.6f + s.phase)
        drawCircle(
            color = Color.White.copy(alpha = s.alpha * twinkle),
            radius = s.radius * size.width / TABLE_W,
            center = Offset(s.x * size.width, s.y * size.height)
        )
    }
}

/**
 * The cabinet the table sits in: an outer bezel with a lit inner edge, which is what
 * makes the playfield read as a recessed physical object rather than a flat drawing.
 */
fun DrawScope.drawCabinet(left: Float, top: Float, w: Float, h: Float, scale: Float) {
    val bezel = 22f * scale
    val tl = Offset(left - bezel / 2f, top - bezel / 2f)
    val sz = Size(w + bezel, h + bezel)

    // Frame body, stroked so it straddles the playfield edge instead of covering it.
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF5A648C), Color(0xFF1B2036), Color(0xFF3C4468)),
            start = tl,
            end = Offset(tl.x + sz.width, tl.y + sz.height)
        ),
        topLeft = tl,
        size = sz,
        style = Stroke(width = bezel)
    )
    // Lit inner lip: bright on the key-light side, shadowed opposite.
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.05f),
                Color.Black.copy(alpha = 0.65f)
            ),
            start = Offset(left, top),
            end = Offset(left + w, top + h)
        ),
        topLeft = Offset(left, top),
        size = Size(w, h),
        style = Stroke(width = bezel * 0.28f)
    )
}

/** Main entry point: draws the entire playfield in table coordinates. */
fun DrawScope.drawTable(engine: GameEngine, time: Float) {
    val t = engine.table
    drawPlayfieldBed(time)
    drawPlayfieldArt(t, engine, time)
    drawNetBaskets(t, time)
    drawDropTargets(t)
    drawSlingshots(t)
    drawReactor(t, engine, time)
    drawBumpers(t, time)
    drawWalls(t)
    drawPlungerLane(engine, t)
    drawFlippers(t)
    drawBallAndTrail(engine)
    drawNetMeshOverlay(t)          // mesh threads in front of the ball as it passes through
    drawPipe(t, engine, time)      // overhead: drawn last so it sits above the bed
    drawParticles(engine)
}

// ---------------------------------------------------------------------------
// Playfield surface
// ---------------------------------------------------------------------------

private fun DrawScope.drawPlayfieldBed(time: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Palette.bedTop, Palette.bedMid, Palette.bedBottom),
            startY = 0f,
            endY = TABLE_H
        ),
        topLeft = Offset.Zero,
        size = Size(TABLE_W, TABLE_H)
    )

    // Pool of light from the upper-left key light.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x33FFFFFF), Color(0x11FFFFFF), Color.Transparent),
            center = Offset(TABLE_W * 0.3f, TABLE_H * 0.18f),
            radius = TABLE_W * 1.15f
        ),
        radius = TABLE_W * 1.15f,
        center = Offset(TABLE_W * 0.3f, TABLE_H * 0.18f)
    )

    // Fine grid, faded toward the bottom, reads as a printed surface texture.
    val gridColor = Color(0x14A7C8FF)
    var x = 0f
    while (x <= TABLE_W) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, TABLE_H), strokeWidth = 1.4f)
        x += 62.5f
    }
    var y = 0f
    while (y <= TABLE_H) {
        drawLine(gridColor, Offset(0f, y), Offset(TABLE_W, y), strokeWidth = 1.4f)
        y += 62.5f
    }

    // Ambient occlusion into the corners so the bed feels sunken.
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.7f)),
            center = Offset(TABLE_W * 0.5f, TABLE_H * 0.45f),
            radius = TABLE_H * 0.72f
        ),
        topLeft = Offset.Zero,
        size = Size(TABLE_W, TABLE_H)
    )
}

/** Screen-printed neon artwork sitting under the (virtual) clear coat. */
private fun DrawScope.drawPlayfieldArt(t: Table, engine: GameEngine, time: Float) {
    val pulse = 0.5f + 0.5f * sin(time * 2f)

    // Concentric orbit rings around the reactor.
    for (i in 1..3) {
        val r = t.reactorRadius + 60f * i
        drawCircle(
            color = Palette.neonCyan.copy(alpha = 0.10f - i * 0.02f),
            radius = r,
            center = t.reactorCenter,
            style = Stroke(width = 3f)
        )
    }

    // Energy conduits fanning out from the reactor toward the flippers.
    val conduit = Palette.neonViolet.copy(alpha = 0.18f + 0.10f * pulse)
    listOf(-1f, 1f).forEach { s ->
        val p = Path().apply {
            moveTo(t.reactorCenter.x + s * 70f, t.reactorCenter.y + 70f)
            cubicTo(
                t.reactorCenter.x + s * 240f, t.reactorCenter.y + 220f,
                t.reactorCenter.x + s * 190f, t.reactorCenter.y + 450f,
                t.reactorCenter.x + s * 178f, 1480f
            )
        }
        drawPath(p, conduit, style = Stroke(width = 10f, cap = StrokeCap.Round))
    }

    // Lane guide arrows in the upper bowl.
    listOf(360f, 480f, 600f).forEachIndexed { i, x ->
        val lit = 0.25f + 0.35f * (0.5f + 0.5f * sin(time * 3f + i))
        drawInsertGlow(Offset(x, 180f), 13f, Palette.neonAmber, lit)
    }

    // "NET" callout inserts beside each basket.
    t.baskets.forEach { b ->
        drawInsertGlow(b.laneCenter, 15f, Palette.neonLime, 0.25f + b.flash * 0.75f)
    }

    // Pipe entrance arrow insert.
    drawInsertGlow(
        Offset(t.pipe.entrance.x + 74f, t.pipe.entrance.y),
        16f,
        Palette.neonCyan,
        0.22f + t.pipe.entranceGlow * 0.78f
    )

    // Apron branding band at the bottom.
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF0A0E20), Color(0xFF05070F)),
            startY = 1690f, endY = TABLE_H
        ),
        topLeft = Offset(0f, 1690f),
        size = Size(TABLE_W, TABLE_H - 1690f)
    )
    drawLine(
        Palette.neonCyan.copy(alpha = 0.45f),
        Offset(0f, 1690f), Offset(TABLE_W, 1690f),
        strokeWidth = 4f
    )
}

// ---------------------------------------------------------------------------
// Features
// ---------------------------------------------------------------------------

private fun DrawScope.drawBumpers(t: Table, time: Float) {
    t.bumpers.forEach { b ->
        // Skirt: the wide ring that sits flush with the playfield.
        drawSoftShadow(b.center, b.radius * 1.25f, 16f, 0.6f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF3A4370), Color(0xFF171C33)),
                center = b.center,
                radius = b.radius * 1.28f
            ),
            radius = b.radius * 1.28f,
            center = b.center
        )
        drawCircle(
            color = Palette.neonMagenta.copy(alpha = 0.35f + b.flash * 0.65f),
            radius = b.radius * 1.28f,
            center = b.center,
            style = Stroke(width = 5f)
        )
        drawGlow(b.center, b.radius * 2.4f, Palette.neonMagenta, 0.30f * b.flash)

        // The cap lifts slightly when struck.
        val lift = 26f + b.flash * 12f
        val flashMix = b.flash
        drawRaisedDisc(
            center = b.center,
            radius = b.radius,
            height = lift,
            capLight = lerpColor(Color(0xFFFF7BA8), Color.White, flashMix),
            capDark = lerpColor(Color(0xFF8E1A46), Color(0xFFFF5C8F), flashMix),
            rimColor = Color.White.copy(alpha = 0.8f)
        )

        // Centre pin.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White, Palette.neonCyan, Color(0xFF0B2A5C)),
                center = Offset(b.center.x - b.radius * 0.15f, b.center.y - b.radius * 0.15f),
                radius = b.radius * 0.34f
            ),
            radius = b.radius * 0.30f,
            center = b.center
        )
    }
}

private fun DrawScope.drawReactor(t: Table, engine: GameEngine, time: Float) {
    val c = t.reactorCenter
    val r = t.reactorRadius
    val chargeRatio = engine.reactorCharge / 100f
    val hot = if (engine.isOverload) Palette.neonMagenta else Palette.neonCyan
    val pulse = 0.5f + 0.5f * sin(time * (if (engine.isOverload) 9f else 3.2f))

    drawGlow(c, r * 3.2f, hot, (0.14f + chargeRatio * 0.22f) * (0.7f + 0.3f * pulse))
    drawSoftShadow(c, r, 30f, 0.65f)

    // Housing.
    drawRaisedDisc(
        center = c, radius = r, height = 30f,
        capLight = Color(0xFF2E3B6B), capDark = Color(0xFF0C1128),
        rimColor = hot.copy(alpha = 0.9f), specular = 0.35f
    )

    // Charge ring: an arc that fills as the reactor powers up.
    val ringR = r * 0.82f
    drawArc(
        color = Color.Black.copy(alpha = 0.55f),
        startAngle = -90f, sweepAngle = 360f, useCenter = false,
        topLeft = Offset(c.x - ringR, c.y - ringR),
        size = Size(ringR * 2, ringR * 2),
        style = Stroke(width = 13f, cap = StrokeCap.Round)
    )
    drawArc(
        brush = Brush.sweepGradient(listOf(hot, Color.White, hot), center = c),
        startAngle = -90f, sweepAngle = 360f * chargeRatio, useCenter = false,
        topLeft = Offset(c.x - ringR, c.y - ringR),
        size = Size(ringR * 2, ringR * 2),
        style = Stroke(width = 11f, cap = StrokeCap.Round)
    )

    // Rotating core vanes.
    val spin = time * (if (engine.isOverload) 260f else 70f)
    for (i in 0 until 6) {
        val a = Math.toRadians((spin + i * 60f).toDouble())
        val inner = r * 0.22f
        val outer = r * 0.60f
        drawLine(
            color = hot.copy(alpha = 0.55f),
            start = Offset(c.x + cos(a).toFloat() * inner, c.y + sin(a).toFloat() * inner),
            end = Offset(c.x + cos(a).toFloat() * outer, c.y + sin(a).toFloat() * outer),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )
    }

    // Glass dome over the core.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, hot, Color(0xFF06122E)),
            center = Offset(c.x - r * 0.18f, c.y - r * 0.2f),
            radius = r * 0.55f
        ),
        radius = r * 0.40f,
        center = c
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.5f),
        radius = r * 0.13f,
        center = Offset(c.x - r * 0.14f, c.y - r * 0.16f)
    )
}

private fun DrawScope.drawSlingshots(t: Table) {
    t.slingshots.forEach { s ->
        val body = trianglePath(s.a, s.b, s.back)
        drawExtrudedPath(
            path = body,
            height = 20f,
            faceBrush = Brush.linearGradient(
                listOf(Color(0xFF3D4878), Color(0xFF131931)),
                start = s.a, end = s.back
            ),
            sideColor = Color(0xFF0A0E1E)
        )
        // Rubber kicker band across the live face.
        val glow = 0.35f + s.flash * 0.65f
        drawLine(
            brush = Brush.linearGradient(
                listOf(Palette.neonAmber.copy(alpha = glow), Color.White.copy(alpha = glow))
            ),
            start = s.a, end = s.b,
            strokeWidth = 15f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.35f + s.flash * 0.6f),
            start = Offset(s.a.x + Light.dir.x * 4f, s.a.y + Light.dir.y * 4f),
            end = Offset(s.b.x + Light.dir.x * 4f, s.b.y + Light.dir.y * 4f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        if (s.flash > 0.05f) {
            val mid = Offset((s.a.x + s.b.x) / 2f, (s.a.y + s.b.y) / 2f)
            drawGlow(mid, 150f, Palette.neonAmber, 0.45f * s.flash)
        }
    }
}

private fun DrawScope.drawDropTargets(t: Table) {
    t.dropTargets.forEach { d ->
        val sink = d.dropAnim
        if (sink > 0.97f) {
            // Fully dropped: just the slot in the playfield.
            drawRect(
                color = Color.Black.copy(alpha = 0.75f),
                topLeft = Offset(d.center.x - d.halfWidth, d.center.y - 4f),
                size = Size(d.halfWidth * 2, 8f)
            )
            return@forEach
        }
        val h = d.halfHeight * (1f - sink)
        val top = d.center.y - h
        val face = Rect(d.center.x - d.halfWidth, top, d.center.x + d.halfWidth, d.center.y + d.halfHeight)

        // Slot shadow.
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(face.left + 5f, d.center.y + d.halfHeight - 2f),
            size = Size(face.width, 10f)
        )
        val lit = d.flash
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    lerpColor(Color(0xFF6BFF8F), Color.White, lit),
                    lerpColor(Color(0xFF13612F), Color(0xFF7CFF6B), lit)
                ),
                startY = face.top, endY = face.bottom
            ),
            topLeft = Offset(face.left, face.top),
            size = Size(face.width, face.height)
        )
        // Top bevel catching the light.
        drawRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(face.left, face.top),
            size = Size(face.width, 5f)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(face.left, face.bottom - 4f),
            size = Size(face.width, 4f)
        )
        if (lit > 0.05f) drawGlow(d.center, 90f, Palette.neonLime, 0.5f * lit)
    }
}

/**
 * See-through fish-net pockets. Unlike a solid wall, the net never stops the ball — it
 * is only a scoring sensor — so the pocket is drawn as a genuinely transparent hollow
 * (you can see the playfield glow through the backing) with a diamond-mesh lattice
 * that briefly bulges when the ball punches through it.
 */
private fun DrawScope.drawNetBaskets(t: Table, time: Float) {
    t.baskets.forEach { b ->
        // Barely-there backing: just enough tint to read as a hollow, not a solid hole.
        val pocket = quadPath(b.mouthLeft, b.backLeft, b.backRight, b.mouthRight)
        drawPath(
            pocket,
            brush = Brush.linearGradient(
                colors = listOf(Color(0x330A2015), Color(0x1A03060A)),
                start = b.mouth, end = b.pocketCenter
            )
        )

        drawNetMesh(b)

        // Chromed rim around the mouth and side lips.
        drawMetalRail(b.mouthLeft, b.backLeft, 13f)
        drawMetalRail(b.mouthRight, b.backRight, 13f)
        drawMetalRail(b.backLeft, b.backRight, 11f)

        // Mouth hoop, lit when the net is live.
        val hoopAlpha = 0.55f + b.flash * 0.45f
        drawLine(
            brush = Brush.linearGradient(
                listOf(
                    Palette.neonLime.copy(alpha = hoopAlpha),
                    Color.White.copy(alpha = hoopAlpha)
                ),
                start = b.mouthLeft, end = b.mouthRight
            ),
            start = b.mouthLeft, end = b.mouthRight,
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )

        if (b.flash > 0.05f) drawGlow(b.pocketCenter, 220f, Palette.neonLime, 0.45f * b.flash)

        // Approach lane markers: dashed chevrons pointing into the mouth.
        val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
        drawLine(
            color = Palette.neonLime.copy(alpha = 0.30f),
            start = b.laneCenter,
            end = Offset(b.laneCenter.x + b.openDir.x * 110f, b.laneCenter.y + b.openDir.y * 110f),
            strokeWidth = 5f,
            pathEffect = dash
        )
    }
}

/**
 * Redraws just the mesh lattice on top of the ball. Real net mesh sits proud of what's
 * behind it, so once the ball is inside the pocket the threads should cross in front of
 * it rather than being hidden underneath — this second pass is what sells "passing
 * through a net" instead of "vanishing into a hole".
 */
private fun DrawScope.drawNetMeshOverlay(t: Table) {
    t.baskets.forEach { drawNetMesh(it, boosted = true) }
}

/** Two families of lines spanning the pocket quad, bulging briefly as the ball passes. */
private fun DrawScope.drawNetMesh(b: NetBasket, boosted: Boolean = false) {
    val sag = b.netSag
    val sagVec = Offset(-b.openDir.x * 20f * sag, -b.openDir.y * 20f * sag)
    val baseAlpha = if (boosted) 0.30f else 0.20f
    val mesh = Palette.neonLime.copy(alpha = baseAlpha + b.flash * 0.55f)
    val rows = 6
    val cols = 7
    for (i in 0..rows) {
        val f = i.toFloat() / rows
        val p1 = lerpOffset(b.mouthLeft, b.backLeft, f)
        val p2 = lerpOffset(b.mouthRight, b.backRight, f)
        val bow = Offset(sagVec.x * f, sagVec.y * f)
        drawLine(
            mesh,
            Offset(p1.x + bow.x, p1.y + bow.y),
            Offset(p2.x + bow.x, p2.y + bow.y),
            strokeWidth = 2.2f
        )
    }
    for (i in 0..cols) {
        val f = i.toFloat() / cols
        val p1 = lerpOffset(b.mouthLeft, b.mouthRight, f)
        val p2 = lerpOffset(b.backLeft, b.backRight, f)
        drawLine(
            mesh,
            p1,
            Offset(p2.x + sagVec.x, p2.y + sagVec.y),
            strokeWidth = 2.2f
        )
    }
}

/**
 * The overhead pipe. Three stacked strokes — dark casing, coloured bore, then an
 * offset highlight — give the tube its round cross-section.
 */
private fun DrawScope.drawPipe(t: Table, engine: GameEngine, time: Float) {
    val pipe = t.pipe
    val path = Path().apply {
        moveTo(pipe.path[0].x, pipe.path[0].y)
        pipe.path.drop(1).forEach { lineTo(it.x, it.y) }
    }
    val active = pipe.lastTraversal

    // Shadow the tube casts on the playfield below.
    translateScope(26f, 34f) {
        drawPath(path, Color.Black.copy(alpha = 0.45f), style = Stroke(width = 62f, cap = StrokeCap.Round))
    }

    // Support posts.
    listOf(0.25f, 0.55f, 0.85f).forEach { f ->
        val p = pipe.sampleAt(pipe.totalLength * f).first
        drawMetalRail(Offset(p.x + 18f, p.y + 26f), Offset(p.x, p.y), 11f)
    }

    // Casing.
    drawPath(path, Color(0xFF10172E), style = Stroke(width = 64f, cap = StrokeCap.Round))
    // Bore.
    drawPath(
        path,
        brush = Brush.linearGradient(
            listOf(
                Palette.neonCyan.copy(alpha = 0.30f + active * 0.45f),
                Color(0xFF0B3C63).copy(alpha = 0.85f)
            ),
            start = Offset(0f, 0f), end = Offset(TABLE_W, TABLE_H)
        ),
        style = Stroke(width = 52f, cap = StrokeCap.Round)
    )
    // Top highlight, offset toward the light, is what makes it read as a cylinder.
    translateScope(Light.dir.x * 13f, Light.dir.y * 13f) {
        drawPath(
            path,
            Color.White.copy(alpha = 0.28f + active * 0.32f),
            style = Stroke(width = 15f, cap = StrokeCap.Round)
        )
    }
    // Rib rings along the tube.
    for (i in 1 until 14) {
        val (p, tan) = pipe.sampleAt(pipe.totalLength * i / 14f)
        val n = Offset(-tan.y, tan.x)
        drawLine(
            Color.Black.copy(alpha = 0.30f),
            Offset(p.x - n.x * 30f, p.y - n.y * 30f),
            Offset(p.x + n.x * 30f, p.y + n.y * 30f),
            strokeWidth = 4f
        )
    }

    // Entrance funnel.
    val e = pipe.entrance
    drawGlow(e, 150f, Palette.neonCyan, 0.25f + pipe.entranceGlow * 0.6f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF061428), Color(0xFF0F2E52)),
            center = e, radius = pipe.entranceRadius
        ),
        radius = pipe.entranceRadius,
        center = e
    )
    drawCircle(
        color = Palette.neonCyan.copy(alpha = 0.7f + pipe.entranceGlow * 0.3f),
        radius = pipe.entranceRadius,
        center = e,
        style = Stroke(width = 9f)
    )

    // Ball travelling inside the tube, drawn slightly larger since it is closer.
    if (engine.ballInPipe) {
        val p = pipe.sampleAt(engine.pipeDistance).first
        drawSoftShadow(Offset(p.x + 26f, p.y + 34f), engine.ballRadius, 0f, 0.4f)
        drawBall(p, engine.ballRadius * 1.12f)
    }
}

private fun DrawScope.drawWalls(t: Table) {
    t.walls.forEach { w ->
        when (w.kind) {
            WallKind.APRON -> drawMetalRail(w.a, w.b, 16f, Color(0xFF5A6486))
            WallKind.POST -> drawMetalRail(w.a, w.b, 20f, Palette.chromeMid)
            WallKind.RAIL -> drawMetalRail(w.a, w.b, 15f, Palette.chromeMid)
        }
    }
    // Guide posts with rubber sleeves at the key deflection points.
    listOf(
        Offset(40f, 1180f) to Palette.neonAmber,
        Offset(t.plungerLaneX, 1180f) to Palette.neonAmber,
        Offset(t.plungerLaneX, 470f) to Palette.neonCyan
    ).forEach { (p, c) ->
        drawSoftShadow(p, 20f, 14f, 0.5f)
        drawRaisedDisc(p, 19f, 16f, Color.White, c, Color.White.copy(alpha = 0.8f))
    }
}

private fun DrawScope.drawPlungerLane(engine: GameEngine, t: Table) {
    val laneLeft = t.plungerLaneX + 8f
    val laneRight = TABLE_W - 48f

    // Recessed channel.
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color(0xFF060A16), Color(0xFF16203E), Color(0xFF060A16)),
            startX = laneLeft, endX = laneRight
        ),
        topLeft = Offset(laneLeft, 400f),
        size = Size(laneRight - laneLeft, TABLE_H - 400f)
    )

    if (!engine.isLaunching) return

    // Plunger: shaft, spring coils and knob, compressing as the spring is pulled.
    val pull = engine.springPull
    val cx = (laneLeft + laneRight) * 0.5f
    val restTop = t.plungerRestPos.y + 34f
    val top = restTop + pull * 120f
    val bottom = TABLE_H - 20f

    drawMetalRail(Offset(cx, top), Offset(cx, bottom), 16f)

    val coils = 9
    val span = bottom - top
    for (i in 0 until coils) {
        val y = top + span * i / coils
        val y2 = top + span * (i + 1) / coils
        drawLine(
            brush = Brush.linearGradient(listOf(Palette.chromeLight, Palette.chromeDark)),
            start = Offset(cx - 26f, y),
            end = Offset(cx + 26f, y2),
            strokeWidth = 9f,
            cap = StrokeCap.Round
        )
    }
    drawRaisedDisc(
        center = Offset(cx, bottom),
        radius = 30f, height = 16f,
        capLight = Color(0xFFFF8FA8), capDark = Color(0xFF9B1233),
        rimColor = Color.White.copy(alpha = 0.75f)
    )

    // Power meter beside the lane.
    val meterTop = 520f
    val meterH = 560f
    drawRect(
        color = Color.Black.copy(alpha = 0.55f),
        topLeft = Offset(laneRight + 6f, meterTop),
        size = Size(20f, meterH)
    )
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Palette.neonMagenta, Palette.neonAmber, Palette.neonLime),
            startY = meterTop, endY = meterTop + meterH
        ),
        topLeft = Offset(laneRight + 6f, meterTop + meterH * (1f - pull)),
        size = Size(20f, meterH * pull)
    )
}

private fun DrawScope.drawFlippers(t: Table) {
    t.flippers.forEach { f ->
        val path = flipperPath(f.pivot, f.tip, f.radius + 5f, f.radius - 4f)
        val hot = abs(f.angVelDeg) > 120f
        drawExtrudedPath(
            path = path,
            height = 22f,
            faceBrush = Brush.linearGradient(
                colors = if (hot)
                    listOf(Color.White, Palette.neonViolet, Color(0xFF4A1E8C))
                else
                    listOf(Color(0xFFD9C2FF), Color(0xFF7B3FD1), Color(0xFF35156B)),
                start = Offset(f.pivot.x + Light.dir.x * 40f, f.pivot.y + Light.dir.y * 40f),
                end = Offset(f.tip.x - Light.dir.x * 40f, f.tip.y - Light.dir.y * 40f)
            ),
            sideColor = Color(0xFF1B0B38),
            layers = 7
        )

        // Rubber edge along the striking face.
        val d = Offset(f.tip.x - f.pivot.x, f.tip.y - f.pivot.y)
        val len = hypot(d.x, d.y).coerceAtLeast(1f)
        val n = Offset(d.y / len, -d.x / len)
        val inset = f.radius * 0.55f
        drawLine(
            color = Color.White.copy(alpha = if (hot) 0.95f else 0.5f),
            start = Offset(f.pivot.x + n.x * inset, f.pivot.y + n.y * inset),
            end = Offset(f.tip.x + n.x * inset, f.tip.y + n.y * inset),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        // Pivot bearing.
        drawRaisedDisc(
            center = f.pivot, radius = f.radius * 0.72f, height = 12f,
            capLight = Palette.chromeLight, capDark = Color(0xFF3A4468),
            rimColor = Color.White.copy(alpha = 0.7f)
        )

        if (hot) drawGlow(f.tip, 130f, Palette.neonViolet, 0.5f)
    }
}

private fun DrawScope.drawBallAndTrail(engine: GameEngine) {
    if (engine.isLaunching && engine.ballInPipe) return
    if (engine.ballInPipe) return

    // Motion trail.
    engine.trail.forEachIndexed { i, p ->
        val f = (i + 1f) / engine.trail.size
        drawCircle(
            color = Color(0xFF9FD8FF).copy(alpha = 0.20f * f),
            radius = engine.ballRadius * (0.45f + 0.5f * f),
            center = p
        )
    }
    drawBall(engine.ballPos, engine.ballRadius)
}

/** Chromed ball: contact shadow, environment gradient, rim light and specular. */
private fun DrawScope.drawBall(pos: Offset, radius: Float) {
    drawSoftShadow(pos, radius, 20f, 0.6f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFD3DEEC),
                Color(0xFF7E8CA6),
                Color(0xFF2A3346)
            ),
            center = Offset(pos.x + Light.dir.x * radius * 0.45f, pos.y + Light.dir.y * radius * 0.45f),
            radius = radius * 1.45f
        ),
        radius = radius,
        center = pos
    )

    // Rim light bouncing off the playfield on the dark side.
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, Color(0x9985C6FF)),
            start = Offset(pos.x + Light.dir.x * radius, pos.y + Light.dir.y * radius),
            end = Offset(pos.x - Light.dir.x * radius, pos.y - Light.dir.y * radius)
        ),
        radius = radius,
        center = pos,
        style = Stroke(width = radius * 0.16f)
    )

    // Tight specular plus a soft secondary.
    val sp = Offset(pos.x + Light.dir.x * radius * 0.48f, pos.y + Light.dir.y * radius * 0.48f)
    drawCircle(Color.White.copy(alpha = 0.95f), radius * 0.20f, sp)
    drawCircle(
        Color.White.copy(alpha = 0.35f),
        radius * 0.12f,
        Offset(pos.x - Light.dir.x * radius * 0.45f, pos.y - Light.dir.y * radius * 0.55f)
    )
}

private fun DrawScope.drawParticles(engine: GameEngine) {
    engine.particles.forEach { p ->
        val a = p.life.coerceIn(0f, 1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = a), p.color.copy(alpha = a * 0.8f), Color.Transparent),
                center = p.position,
                radius = p.size * 2.2f
            ),
            radius = p.size * 2.2f,
            center = p.position
        )
    }
}

// ---------------------------------------------------------------------------
// Overlays (canvas space)
// ---------------------------------------------------------------------------

/** Cabinet glass: a diagonal sheen plus vignette, sitting over everything. */
fun DrawScope.drawGlassOverlay(flash: Float) {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.075f),
                Color.Transparent,
                Color.Transparent,
                Color.White.copy(alpha = 0.035f)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        ),
        size = size
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
            center = Offset(size.width * 0.5f, size.height * 0.45f),
            radius = size.height * 0.78f
        ),
        size = size
    )
    if (flash > 0.01f) {
        drawRect(color = Color.White.copy(alpha = 0.30f * flash), size = size)
    }
}

// ---------------------------------------------------------------------------
// Small maths helpers
// ---------------------------------------------------------------------------

fun lerpOffset(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

fun lerpColor(a: Color, b: Color, t: Float): Color {
    val u = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * u,
        green = a.green + (b.green - a.green) * u,
        blue = a.blue + (b.blue - a.blue) * u,
        alpha = a.alpha + (b.alpha - a.alpha) * u
    )
}
