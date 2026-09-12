package com.satya.pinballscorer.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Table entities
// ---------------------------------------------------------------------------

class PopBumper(
    val center: Offset,
    val radius: Float,
    val scoreValue: Int
) {
    var flash = 0f       // 0..1, drives the lit ring + cap lift
    var cooldown = 0f
}

class Slingshot(
    val a: Offset,       // kicker face start
    val b: Offset,       // kicker face end
    val back: Offset,    // third triangle vertex (closes the shape)
    val scoreValue: Int
) {
    var flash = 0f
    var cooldown = 0f

    /** Outward normal of the kicker face, pointing into the playfield. */
    val normal: Offset = run {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val n = Offset(-dy, dx).normalizedOrZero()
        val midX = (a.x + b.x) * 0.5f
        val midY = (a.y + b.y) * 0.5f
        // Flip so the normal points away from the closing vertex.
        if (n.x * (back.x - midX) + n.y * (back.y - midY) > 0f) Offset(-n.x, -n.y) else n
    }

    val face = Wall(a, b, restitution = 0.30f, friction = 0.05f, thickness = 6f)
    val walls = listOf(
        face,
        Wall(a, back, restitution = 0.30f, thickness = 6f),
        Wall(b, back, restitution = 0.30f, thickness = 6f)
    )
}

class DropTarget(
    val center: Offset,
    val halfWidth: Float,
    val halfHeight: Float,
    val scoreValue: Int
) {
    var isDown = false
    var flash = 0f
    /** 0 = fully standing, 1 = fully dropped. Animated so the target visibly sinks. */
    var dropAnim = 0f
}

/**
 * A net pocket. The mesh is a soft, pass-through sensor rather than a wall: the ball
 * flies straight through it (scoring a bonus, boosted further on a direct flipper hit)
 * and keeps travelling into the playfield behind it, so it never gets stuck bouncing
 * around inside.
 */
class NetBasket(
    val id: Int,
    val mouth: Offset,
    val openDeg: Float,
    val depth: Float,
    val halfWidth: Float,
    val isLeft: Boolean
) {
    val openDir: Offset = Offset(
        cos(Math.toRadians(openDeg.toDouble())).toFloat(),
        sin(Math.toRadians(openDeg.toDouble())).toFloat()
    )

    /** Centre of the pocket interior; crossing near here pays the pass-through bonus. */
    val pocketCenter = Offset(
        mouth.x - openDir.x * depth * 0.55f,
        mouth.y - openDir.y * depth * 0.55f
    )

    /** Sensor just outside the mouth that pays the "passed the net" bonus. */
    val laneCenter = Offset(
        mouth.x + openDir.x * 46f,
        mouth.y + openDir.y * 46f
    )

    // Pocket corners, in draw order around the quad. Shared with the renderer so the
    // mesh always lines up exactly with the scoring sensor.
    private val perp = Offset(-openDir.y, openDir.x)
    private val backCenter = Offset(mouth.x - openDir.x * depth, mouth.y - openDir.y * depth)
    val backLeft = Offset(backCenter.x + perp.x * halfWidth, backCenter.y + perp.y * halfWidth)
    val backRight = Offset(backCenter.x - perp.x * halfWidth, backCenter.y - perp.y * halfWidth)
    val mouthLeft = Offset(mouth.x + perp.x * halfWidth, mouth.y + perp.y * halfWidth)
    val mouthRight = Offset(mouth.x - perp.x * halfWidth, mouth.y - perp.y * halfWidth)

    var netSag = 0f          // 0..1, brief mesh bulge as the ball punches through
    var laneCooldown = 0f
    var passCooldown = 0f
    var flash = 0f
}

/**
 * The overhead tube. The ball is taken off the playfield, carried along a fixed
 * spline and spat back out at the far end — the classic Nokia pinball habit-trail.
 */
class Pipe(
    val entrance: Offset,
    val entranceRadius: Float,
    p0: Offset, p1: Offset, p2: Offset, p3: Offset
) {
    val path: List<Offset> = (0..SAMPLES).map { cubicBezier(p0, p1, p2, p3, it.toFloat() / SAMPLES) }

    /** Cumulative arc length at each sample, so travel speed is constant. */
    val cumulative: FloatArray = FloatArray(path.size).also { arr ->
        for (i in 1 until path.size) {
            arr[i] = arr[i - 1] + (path[i] - path[i - 1]).length()
        }
    }

    val totalLength: Float get() = cumulative.last()

    var entranceGlow = 0f
    var lastTraversal = 0f   // fades the tube's lit state after a run

    /** Position and unit tangent at [dist] along the tube. */
    fun sampleAt(dist: Float): Pair<Offset, Offset> {
        val d = dist.coerceIn(0f, totalLength)
        var lo = 0
        var hi = path.size - 2
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (cumulative[mid] <= d) lo = mid else hi = mid - 1
        }
        val i = lo.coerceIn(0, path.size - 2)
        val segLen = (cumulative[i + 1] - cumulative[i]).coerceAtLeast(1e-4f)
        val t = ((d - cumulative[i]) / segLen).coerceIn(0f, 1f)
        val pos = Offset(
            path[i].x + (path[i + 1].x - path[i].x) * t,
            path[i].y + (path[i + 1].y - path[i].y) * t
        )
        val tangent = (path[i + 1] - path[i]).normalizedOrZero()
        return pos to tangent
    }

    companion object {
        const val SAMPLES = 72
        const val ENTRY_MIN_SPEED = 620f
        const val ENTRY_MIN_UPWARD = 220f
    }
}

/**
 * A rotating flipper modelled as a swinging capsule. Collisions use the real surface
 * velocity at the contact point, so a fast swing transfers energy and a resting
 * flipper transfers none.
 */
class Flipper(
    val pivot: Offset,
    val length: Float,
    val restAngle: Float,
    val pressAngle: Float,
    val radius: Float = 17f,
    val isLeft: Boolean
) {
    var angle = restAngle
    var angVelDeg = 0f
    var pressed = false

    val tip: Offset
        get() {
            val r = Math.toRadians(angle.toDouble())
            return Offset(pivot.x + length * cos(r).toFloat(), pivot.y + length * sin(r).toFloat())
        }

    fun step(dt: Float) {
        val target = if (pressed) pressAngle else restAngle
        val sweepSpeed = if (pressed) UP_SPEED else DOWN_SPEED
        val diff = target - angle
        val maxStep = sweepSpeed * dt
        val next = if (abs(diff) <= maxStep) target else angle + sign(diff) * maxStep
        angVelDeg = (next - angle) / dt
        angle = next
    }

    /** Linear velocity of the flipper surface at world point [p]. */
    fun surfaceVelocity(p: Offset): Offset {
        val w = Math.toRadians(angVelDeg.toDouble()).toFloat()
        val rx = p.x - pivot.x
        val ry = p.y - pivot.y
        return Offset(-w * ry, w * rx)
    }

    companion object {
        const val UP_SPEED = 1500f    // deg/s
        const val DOWN_SPEED = 900f   // deg/s
    }
}

// ---------------------------------------------------------------------------
// Table layout
// ---------------------------------------------------------------------------

/** Everything about the physical table, built once and shared by sim and renderer. */
class Table {
    val archCenter = Offset(500f, 500f)
    val archRadius = 460f

    val plungerLaneX = 858f          // inner wall of the plunger lane
    val plungerRestPos = Offset(909f, 1600f)

    val flipperLeft = Flipper(
        pivot = Offset(267f, 1500f), length = 170f,
        restAngle = 30f, pressAngle = -28f, isLeft = true
    )
    val flipperRight = Flipper(
        pivot = Offset(631f, 1500f), length = 170f,
        restAngle = 150f, pressAngle = 208f, isLeft = false
    )

    val bumpers = listOf(
        PopBumper(Offset(350f, 480f), 54f, 100),
        PopBumper(Offset(610f, 480f), 54f, 100),
        PopBumper(Offset(480f, 320f), 58f, 150)
    )

    /** Central reactor doubles as a high-value circular bumper. */
    val reactorCenter = Offset(480f, 855f)
    val reactorRadius = 92f

    val slingshots = listOf(
        Slingshot(Offset(150f, 1250f), Offset(262f, 1420f), Offset(150f, 1432f), 50),
        Slingshot(Offset(748f, 1250f), Offset(636f, 1420f), Offset(748f, 1432f), 50)
    )

    val dropTargets = listOf(
        DropTarget(Offset(320f, 1012f), 42f, 15f, 250),
        DropTarget(Offset(400f, 1012f), 42f, 15f, 250),
        DropTarget(Offset(480f, 1012f), 42f, 15f, 250),
        DropTarget(Offset(560f, 1012f), 42f, 15f, 250),
        DropTarget(Offset(640f, 1012f), 42f, 15f, 250)
    )

    val baskets = listOf(
        NetBasket(0, mouth = Offset(185f, 1075f), openDeg = -45f, depth = 95f, halfWidth = 52f, isLeft = true),
        NetBasket(1, mouth = Offset(713f, 1075f), openDeg = 225f, depth = 95f, halfWidth = 52f, isLeft = false)
    )

    val pipe = Pipe(
        entrance = Offset(152f, 918f),
        entranceRadius = 46f,
        p0 = Offset(152f, 918f),
        p1 = Offset(30f, 470f),
        p2 = Offset(250f, 70f),
        p3 = Offset(672f, 214f)
    )

    val drainY = 1712f
    val drainLeftX = 215f
    val drainRightX = 683f

    val walls: List<Wall> = buildList {
        // Top arch, drawn from the left edge over the apex to the right edge.
        addAll(arcWalls(archCenter, archRadius, 180f, 360f, 34, restitution = 0.42f))

        // Outer side walls.
        add(Wall(Offset(40f, 500f), Offset(40f, 1180f), 0.42f))
        add(Wall(Offset(960f, 500f), Offset(960f, TABLE_H), 0.42f))

        // Lower funnel toward the drain.
        add(Wall(Offset(40f, 1180f), Offset(95f, 1560f), 0.38f))
        add(Wall(Offset(95f, 1560f), Offset(215f, 1690f), 0.30f, kind = WallKind.APRON))
        add(Wall(Offset(plungerLaneX, 1180f), Offset(803f, 1560f), 0.38f))
        add(Wall(Offset(803f, 1560f), Offset(683f, 1690f), 0.30f, kind = WallKind.APRON))

        // Plunger lane: inner wall runs the full height, then a deflector that turns
        // the launched ball left into the arch and stops it re-entering the lane.
        add(Wall(Offset(plungerLaneX, TABLE_H), Offset(plungerLaneX, 470f), 0.30f))
        add(Wall(Offset(plungerLaneX, 470f), Offset(792f, 384f), 0.45f, kind = WallKind.POST))

        // Slingshot bodies are solid surfaces; the net pockets are not — the ball
        // passes straight through them (see NetBasket / GameEngine.checkBasket).
        slingshots.forEach { addAll(it.walls) }
    }

    val flippers = listOf(flipperLeft, flipperRight)

    fun reset() {
        dropTargets.forEach { it.isDown = false; it.dropAnim = 0f; it.flash = 0f }
        baskets.forEach { it.netSag = 0f; it.flash = 0f; it.laneCooldown = 0f; it.passCooldown = 0f }
        bumpers.forEach { it.flash = 0f; it.cooldown = 0f }
        slingshots.forEach { it.flash = 0f; it.cooldown = 0f }
        flippers.forEach { it.pressed = false; it.angle = it.restAngle; it.angVelDeg = 0f }
    }
}