package com.satya.pinballscorer.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class Haptic { LIGHT, MEDIUM, HEAVY }

class FloatingScore(
    val id: Long,
    val text: String,
    var position: Offset,
    val color: Color,
    var life: Float = 1f
)

class Particle(
    var position: Offset,
    var velocity: Offset,
    val color: Color,
    var life: Float = 1f,
    val decay: Float = 1.8f,
    val size: Float = 7f
)

/**
 * Fixed-timestep pinball simulation.
 *
 * Two rules drive everything here and fix the "ball drifts on its own" behaviour of
 * the old engine:
 *  1. An impulse is only ever applied when the ball is *approaching* a surface
 *     (relative normal velocity < 0). A ball resting on a still flipper therefore
 *     receives nothing and simply sits there.
 *  2. Every scoring feature that is a region rather than a surface has a cooldown,
 *     so standing inside it cannot re-trigger sixty times a second.
 */
class GameEngine {

    val table = Table()

    // ---- Observed by the HUD ----
    var isPlaying by mutableStateOf(false)
    var isGameOver by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var isLaunching by mutableStateOf(true)
    var score by mutableIntStateOf(0)
    var ballsLeft by mutableIntStateOf(3)
    var multiplier by mutableIntStateOf(1)
    var reactorCharge by mutableFloatStateOf(0f)
    var isOverload by mutableStateOf(false)
    var overloadTimer by mutableFloatStateOf(0f)
    var comboCount by mutableIntStateOf(0)
    var activeMessage by mutableStateOf<String?>(null)
    var ballSaveTimer by mutableFloatStateOf(0f)
    var springPull by mutableFloatStateOf(0f)
    var isPullingSpring by mutableStateOf(false)

    /** Bumped once per frame purely so the Canvas invalidates without recomposing. */
    var frame by mutableIntStateOf(0)

    var hapticTick by mutableIntStateOf(0)
        private set
    var hapticLevel: Haptic = Haptic.LIGHT
        private set

    // ---- Ball ----
    var ballPos = table.plungerRestPos
        private set
    var ballVel = Offset.Zero
        private set
    val ballRadius = 22f
    val trail = ArrayDeque<Offset>()

    // ---- Presentation state read by the renderer ----
    val floatingScores = ArrayList<FloatingScore>()
    val particles = ArrayList<Particle>()
    var shake = 0f
        private set
    var shakeOffset = Offset.Zero
        private set
    var flashIntensity = 0f
        private set

    // ---- Pipe / basket state ----
    var ballInPipe = false
        private set
    var pipeDistance = 0f
        private set
    private var pipeSpeed = 0f

    // ---- Internals ----
    private var gravity = 2400f
    private var maxSpeed = 3400f
    private var launchMin = 2600f
    private var launchMax = 3300f
    private var accumulator = 0f
    private var nextScoreId = 0L
    private var messageTimer = 0f
    private var comboTimer = 0f
    private var stuckTimer = 0f
    private var basketsThisBall = mutableSetOf<Int>()
    private var pipeRunsThisBall = 0
    private var reactorHitCooldown = 0f
    private var trailTick = 0
    private var lastHitByFlipper = false

    companion object {
        private const val FIXED_DT = 1f / 240f
        private const val MAX_STEPS = 14
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun start(level: String) {
        when (level.lowercase()) {
            "easy" -> { gravity = 1900f; maxSpeed = 3000f }
            "hard" -> { gravity = 3000f; maxSpeed = 3800f }
            else -> { gravity = 2400f; maxSpeed = 3400f }
        }
        // Even the softest plunge must clear the plunger lane, so derive the launch
        // window from gravity rather than hard-coding it per difficulty.
        launchMin = kotlin.math.sqrt(2f * gravity * 1460f) * 1.02f
        launchMax = maxSpeed * 0.98f
        table.reset()
        score = 0
        ballsLeft = 3
        multiplier = 1
        reactorCharge = 0f
        isOverload = false
        overloadTimer = 0f
        comboCount = 0
        comboTimer = 0f
        accumulator = 0f
        floatingScores.clear()
        particles.clear()
        trail.clear()
        isGameOver = false
        isPaused = false
        isPlaying = true
        newBall()
    }

    private fun newBall() {
        ballPos = table.plungerRestPos
        ballVel = Offset.Zero
        isLaunching = true
        springPull = 0f
        isPullingSpring = false
        ballInPipe = false
        stuckTimer = 0f
        basketsThisBall.clear()
        pipeRunsThisBall = 0
        comboCount = 0
        trail.clear()
        table.baskets.forEach { it.netSag = 0f; it.passCooldown = 0f }
    }

    fun releaseSpring() {
        if (!isLaunching) return
        val power = springPull.coerceIn(0f, 1f)
        ballVel = Offset(0f, -(launchMin + power * (launchMax - launchMin)))
        isLaunching = false
        springPull = 0f
        ballSaveTimer = 7f
        message("BALL SAVE ACTIVE")
        haptic(Haptic.MEDIUM)
    }

    /**
     * Places the ball directly on the playfield, skipping the plunger. Used by the
     * physics tests to set up specific shots reproducibly.
     */
    fun placeBall(pos: Offset, vel: Offset) {
        ballPos = pos
        ballVel = vel
        isLaunching = false
        accumulator = 0f
        trail.clear()
    }

    fun setFlipper(left: Boolean, pressed: Boolean) {
        val f = if (left) table.flipperLeft else table.flipperRight
        if (f.pressed != pressed) {
            f.pressed = pressed
            if (pressed) haptic(Haptic.LIGHT)
        }
    }

    // -----------------------------------------------------------------------
    // Frame update
    // -----------------------------------------------------------------------

    fun update(frameDt: Float) {
        if (!isPlaying || isPaused) return
        val dt = frameDt.coerceIn(0f, 0.05f)

        updatePresentation(dt)
        updateModes(dt)

        accumulator += dt
        var steps = 0
        while (accumulator >= FIXED_DT && steps < MAX_STEPS) {
            physicsStep(FIXED_DT)
            accumulator -= FIXED_DT
            steps++
        }
        if (steps == MAX_STEPS) accumulator = 0f   // don't let a stall spiral

        frame++
    }

    private fun updateModes(dt: Float) {
        if (messageTimer > 0f) {
            messageTimer -= dt
            if (messageTimer <= 0f) activeMessage = null
        }
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) comboCount = 0
        }
        if (ballSaveTimer > 0f) ballSaveTimer = max(0f, ballSaveTimer - dt)
        if (isOverload) {
            overloadTimer -= dt
            if (overloadTimer <= 0f) {
                isOverload = false
                multiplier = 1
                reactorCharge = 0f
                message("REACTOR STABILISED")
            }
        }
    }

    private fun updatePresentation(dt: Float) {
        if (shake > 0f) {
            shake = max(0f, shake - dt * 3.2f)
            val s = shake * 22f
            shakeOffset = Offset(
                (Random.nextFloat() - 0.5f) * s,
                (Random.nextFloat() - 0.5f) * s
            )
        } else {
            shakeOffset = Offset.Zero
        }
        if (flashIntensity > 0f) flashIntensity = max(0f, flashIntensity - dt * 2.6f)

        table.bumpers.forEach {
            if (it.flash > 0f) it.flash = max(0f, it.flash - dt * 4f)
            if (it.cooldown > 0f) it.cooldown -= dt
        }
        table.slingshots.forEach {
            if (it.flash > 0f) it.flash = max(0f, it.flash - dt * 5f)
            if (it.cooldown > 0f) it.cooldown -= dt
        }
        table.dropTargets.forEach {
            if (it.flash > 0f) it.flash = max(0f, it.flash - dt * 4f)
            val target = if (it.isDown) 1f else 0f
            it.dropAnim += (target - it.dropAnim) * min(1f, dt * 14f)
        }
        table.baskets.forEach {
            if (it.flash > 0f) it.flash = max(0f, it.flash - dt * 3f)
            if (it.laneCooldown > 0f) it.laneCooldown -= dt
            if (it.passCooldown > 0f) it.passCooldown -= dt
            // netSag is a one-shot bulge fired when the ball punches through the mesh.
            it.netSag = max(0f, it.netSag - dt * 2.4f)
        }
        if (table.pipe.entranceGlow > 0f) {
            table.pipe.entranceGlow = max(0f, table.pipe.entranceGlow - dt * 2f)
        }
        if (table.pipe.lastTraversal > 0f) {
            table.pipe.lastTraversal = max(0f, table.pipe.lastTraversal - dt * 0.7f)
        }

        val fsIt = floatingScores.iterator()
        while (fsIt.hasNext()) {
            val fs = fsIt.next()
            fs.life -= dt * 0.85f
            fs.position = Offset(fs.position.x, fs.position.y - 70f * dt)
            if (fs.life <= 0f) fsIt.remove()
        }

        val pIt = particles.iterator()
        while (pIt.hasNext()) {
            val p = pIt.next()
            p.position = Offset(p.position.x + p.velocity.x * dt, p.position.y + p.velocity.y * dt)
            p.velocity = Offset(p.velocity.x * (1f - dt * 1.6f), p.velocity.y + 900f * dt)
            p.life -= dt * p.decay
            if (p.life <= 0f) pIt.remove()
        }
    }

    // -----------------------------------------------------------------------
    // Physics
    // -----------------------------------------------------------------------

    private fun physicsStep(dt: Float) {
        table.flippers.forEach { it.step(dt) }
        if (reactorHitCooldown > 0f) reactorHitCooldown -= dt

        when {
            ballInPipe -> { stepPipe(dt); return }
            isLaunching -> { ballPos = table.plungerRestPos; ballVel = Offset.Zero; return }
        }

        // Integrate.
        ballVel = Offset(ballVel.x, ballVel.y + gravity * dt)
        ballVel = Offset(ballVel.x * (1f - dt * 0.12f), ballVel.y * (1f - dt * 0.02f))
        clampSpeed()
        ballPos = Offset(ballPos.x + ballVel.x * dt, ballPos.y + ballVel.y * dt)

        table.walls.forEach { collideWall(it) }
        table.bumpers.forEach { collideBumper(it) }
        collideReactor()
        table.slingshots.forEach { collideSlingshot(it) }
        table.dropTargets.forEach { collideDropTarget(it) }
        table.flippers.forEach { collideFlipper(it) }

        checkPipeEntrance()
        table.baskets.forEach { checkBasket(it) }

        recordTrail()
        checkStuck(dt)
        checkPlungerReturn()
        checkDrain()
    }

    /** A weak plunge leaves the ball rolling back down the lane — let it be re-plunged. */
    private fun checkPlungerReturn() {
        if (ballPos.x > table.plungerLaneX + ballRadius &&
            ballPos.y > 1470f &&
            ballVel.length() < 130f
        ) {
            ballPos = table.plungerRestPos
            ballVel = Offset.Zero
            isLaunching = true
            springPull = 0f
        }
    }

    private fun clampSpeed() {
        val s = ballVel.length()
        if (s > maxSpeed) ballVel = Offset(ballVel.x / s * maxSpeed, ballVel.y / s * maxSpeed)
    }

    private fun recordTrail() {
        if (++trailTick % 6 != 0) return
        trail.addLast(ballPos)
        while (trail.size > 10) trail.removeFirst()
    }

    /** Circle-vs-capsule with an approach-only impulse. */
    private fun collideWall(w: Wall): Boolean {
        val c = closestPointOnSegment(ballPos, w.a, w.b)
        var nx = ballPos.x - c.x
        var ny = ballPos.y - c.y
        var dist = hypot(nx, ny)
        val minD = ballRadius + w.thickness
        if (dist >= minD) return false

        if (dist < 1e-4f) {
            val sx = w.b.x - w.a.x
            val sy = w.b.y - w.a.y
            val l = hypot(sx, sy).coerceAtLeast(1e-4f)
            nx = -sy / l
            ny = sx / l
            if (nx * ballVel.x + ny * ballVel.y > 0f) { nx = -nx; ny = -ny }
            dist = 0f
        } else {
            nx /= dist
            ny /= dist
        }

        ballPos = Offset(ballPos.x + nx * (minD - dist), ballPos.y + ny * (minD - dist))
        applySurfaceImpulse(nx, ny, w.restitution, w.friction)
        return true
    }

    private fun applySurfaceImpulse(nx: Float, ny: Float, restitution: Float, friction: Float) {
        val vn = ballVel.x * nx + ballVel.y * ny
        if (vn >= 0f) return                       // moving away: never add energy
        val tx = (ballVel.x - nx * vn) * (1f - friction)
        val ty = (ballVel.y - ny * vn) * (1f - friction)
        val newVn = -vn * restitution
        ballVel = Offset(tx + nx * newVn, ty + ny * newVn)
    }

    private fun collideBumper(b: PopBumper) {
        lastHitByFlipper = false
        val dx = ballPos.x - b.center.x
        val dy = ballPos.y - b.center.y
        val dist = hypot(dx, dy)
        val minD = ballRadius + b.radius
        if (dist >= minD) return

        val nx = if (dist > 1e-4f) dx / dist else 0f
        val ny = if (dist > 1e-4f) dy / dist else -1f
        ballPos = Offset(ballPos.x + nx * (minD - dist), ballPos.y + ny * (minD - dist))
        applySurfaceImpulse(nx, ny, 0.45f, 0.02f)

        if (b.cooldown > 0f) return
        b.cooldown = 0.12f
        b.flash = 1f

        // Pop bumpers are powered: they always throw the ball back out hard.
        val kick = 1250f
        ballVel = Offset(ballVel.x + nx * kick, ballVel.y + ny * kick)
        clampSpeed()

        awardScore(b.scoreValue, b.center, Color(0xFFFF4D6D))
        burst(b.center, Color(0xFFFF4D6D), 14, 520f)
        chargeReactor(5f)
        registerCombo()
        addShake(0.22f)
        haptic(Haptic.LIGHT)
    }

    private fun collideReactor() {
        lastHitByFlipper = false
        val dx = ballPos.x - table.reactorCenter.x
        val dy = ballPos.y - table.reactorCenter.y
        val dist = hypot(dx, dy)
        val minD = ballRadius + table.reactorRadius
        if (dist >= minD) return

        val nx = if (dist > 1e-4f) dx / dist else 0f
        val ny = if (dist > 1e-4f) dy / dist else -1f
        ballPos = Offset(ballPos.x + nx * (minD - dist), ballPos.y + ny * (minD - dist))
        applySurfaceImpulse(nx, ny, 0.62f, 0.02f)

        if (reactorHitCooldown > 0f) return
        reactorHitCooldown = 0.2f
        ballVel = Offset(ballVel.x + nx * 700f, ballVel.y + ny * 700f)
        clampSpeed()
        awardScore(150, table.reactorCenter, Color(0xFF4DE1FF))
        burst(table.reactorCenter, Color(0xFF4DE1FF), 16, 600f)
        chargeReactor(11f)
        registerCombo()
        addShake(0.3f)
        haptic(Haptic.MEDIUM)
    }

    private fun collideSlingshot(s: Slingshot) {
        lastHitByFlipper = false
        var touchedFace = false
        s.walls.forEach { w -> if (collideWall(w) && w === s.face) touchedFace = true }
        if (!touchedFace || s.cooldown > 0f) return

        // Only fire if the ball actually arrived at the face with some pace, so a ball
        // trickling down the slingshot doesn't machine-gun.
        if (ballVel.length() < 90f) return

        s.cooldown = 0.16f
        s.flash = 1f
        val kick = 1150f
        ballVel = Offset(ballVel.x + s.normal.x * kick, ballVel.y + s.normal.y * kick - 220f)
        clampSpeed()

        val mid = Offset((s.a.x + s.b.x) * 0.5f, (s.a.y + s.b.y) * 0.5f)
        awardScore(s.scoreValue, mid, Color(0xFFFFC23D))
        burst(mid, Color(0xFFFFC23D), 9, 420f)
        addShake(0.16f)
        haptic(Haptic.LIGHT)
    }

    private fun collideDropTarget(t: DropTarget) {
        lastHitByFlipper = false
        if (t.isDown) return
        val nearestX = ballPos.x.coerceIn(t.center.x - t.halfWidth, t.center.x + t.halfWidth)
        val nearestY = ballPos.y.coerceIn(t.center.y - t.halfHeight, t.center.y + t.halfHeight)
        val dx = ballPos.x - nearestX
        val dy = ballPos.y - nearestY
        val dist = hypot(dx, dy)
        if (dist >= ballRadius) return

        val nx = if (dist > 1e-4f) dx / dist else 0f
        val ny = if (dist > 1e-4f) dy / dist else -1f
        ballPos = Offset(ballPos.x + nx * (ballRadius - dist), ballPos.y + ny * (ballRadius - dist))
        applySurfaceImpulse(nx, ny, 0.5f, 0.05f)

        t.isDown = true
        t.flash = 1f
        awardScore(t.scoreValue, t.center, Color(0xFF7CFF6B))
        burst(t.center, Color(0xFF7CFF6B), 10, 380f)
        chargeReactor(7f)
        registerCombo()
        haptic(Haptic.LIGHT)

        if (table.dropTargets.all { it.isDown }) {
            awardScore(2500, table.reactorCenter, Color(0xFFFFE45E))
            message("TARGET BANK CLEARED!")
            burst(table.reactorCenter, Color(0xFFFFE45E), 34, 800f)
            addShake(0.6f)
            flashIntensity = 1f
            haptic(Haptic.HEAVY)
            table.dropTargets.forEach { it.isDown = false }
        }
    }

    /**
     * The heart of "reactive" flippers: the impulse is computed against the flipper's
     * real surface velocity, so a fast swing launches the ball and a still flipper
     * just holds it.
     */
    private fun collideFlipper(f: Flipper) {
        val c = closestPointOnSegment(ballPos, f.pivot, f.tip)
        var nx = ballPos.x - c.x
        var ny = ballPos.y - c.y
        var dist = hypot(nx, ny)
        val minD = ballRadius + f.radius
        if (dist >= minD) return

        if (dist < 1e-4f) { nx = 0f; ny = -1f; dist = 0f } else { nx /= dist; ny /= dist }
        ballPos = Offset(ballPos.x + nx * (minD - dist), ballPos.y + ny * (minD - dist))

        val surf = f.surfaceVelocity(c)
        val relX = ballVel.x - surf.x
        val relY = ballVel.y - surf.y
        val vn = relX * nx + relY * ny
        if (vn >= 0f) return

        val swinging = abs(f.angVelDeg) > 120f
        val e = if (swinging) 0.62f else 0.22f
        val j = -(1f + e) * vn
        var vx = ballVel.x + nx * j
        var vy = ballVel.y + ny * j

        // Tangential damping so a cradled ball settles instead of rolling forever.
        val tvx = vx - nx * (vx * nx + vy * ny)
        val tvy = vy - ny * (vx * nx + vy * ny)
        vx -= tvx * 0.10f
        vy -= tvy * 0.10f

        ballVel = Offset(vx, vy)

        if (swinging) {
            lastHitByFlipper = true
            // Guarantee a live shot has real pace regardless of where it was struck.
            val s = ballVel.length()
            if (s < 1250f && s > 1f) {
                ballVel = Offset(ballVel.x / s * 1250f, ballVel.y / s * 1250f)
            }
            if (ballVel.y > -400f) {
                ballVel = Offset(ballVel.x, -1200f)
            }
            burst(c, Color(0xFFB388FF), 6, 300f)
            haptic(Haptic.MEDIUM)
            addShake(0.12f)
        }
        clampSpeed()
    }

    // -----------------------------------------------------------------------
    // Pipe
    // -----------------------------------------------------------------------

    private fun checkPipeEntrance() {
        val p = table.pipe
        val d = (ballPos - p.entrance).length()
        if (d > p.entranceRadius + ballRadius) return

        val speed = ballVel.length()
        if (speed < Pipe.ENTRY_MIN_SPEED || ballVel.y > -Pipe.ENTRY_MIN_UPWARD) {
            p.entranceGlow = 0.6f     // teased but missed
            return
        }

        ballInPipe = true
        pipeDistance = 0f
        pipeSpeed = speed.coerceIn(1000f, 2000f)
        p.entranceGlow = 1f
        p.lastTraversal = 1f
        awardScore(500, p.entrance, Color(0xFF6BE8FF))
        message("PIPE RUN!")
        registerCombo()
        haptic(Haptic.MEDIUM)
    }

    private fun stepPipe(dt: Float) {
        val p = table.pipe
        pipeDistance += pipeSpeed * dt
        pipeSpeed = max(760f, pipeSpeed - 240f * dt)

        if (pipeDistance >= p.totalLength) {
            val (pos, tangent) = p.sampleAt(p.totalLength)
            ballInPipe = false
            ballPos = Offset(pos.x + tangent.x * ballRadius, pos.y + tangent.y * ballRadius)
            ballVel = Offset(tangent.x * pipeSpeed, tangent.y * pipeSpeed)

            pipeRunsThisBall++
            val bonus = 1500 * pipeRunsThisBall
            awardScore(bonus, ballPos, Color(0xFF6BE8FF))
            message(if (pipeRunsThisBall > 1) "PIPE x$pipeRunsThisBall!" else "PIPE BONUS!")
            burst(ballPos, Color(0xFF6BE8FF), 20, 620f)
            chargeReactor(18f)
            addShake(0.35f)
            haptic(Haptic.HEAVY)
        } else {
            ballPos = p.sampleAt(pipeDistance).first
        }
    }

    // -----------------------------------------------------------------------
    // Net baskets
    // -----------------------------------------------------------------------

    /**
     * The net is a soft mesh, not a wall: the ball is never stopped or held here. It
     * simply flies through on its existing trajectory, paying a lane bonus on the way
     * in and a bigger pass-through bonus deep in the pocket — doubled if a flipper just
     * fired it in directly — then carries on into the playfield behind the net.
     */
    private fun checkBasket(b: NetBasket) {
        // "Passed the net" lane bonus, just outside the mouth.
        if (b.laneCooldown <= 0f && (ballPos - b.laneCenter).length() < 52f + ballRadius) {
            b.laneCooldown = 1.2f
            b.flash = 0.7f
            val extraLane = if (lastHitByFlipper) 500 else 0
            val lanePts = 300 + extraLane
            awardScore(lanePts, b.laneCenter, Color(0xFF9BFFB0))
            if (extraLane > 0) {
                message("DIRECT NET LANE! +$lanePts")
            }
            registerCombo()
            haptic(Haptic.LIGHT)
        }

        val inPocket = (ballPos - b.pocketCenter).length() < b.halfWidth * 0.92f
        if (!inPocket || b.passCooldown > 0f) return

        b.passCooldown = 0.9f
        b.flash = 1f
        b.netSag = 1f

        val directBonus = if (lastHitByFlipper) 2000 else 0
        val pts = 2500 + directBonus
        awardScore(pts, b.pocketCenter, Color(0xFF9BFFB0))
        message(
            if (directBonus > 0) {
                if (b.isLeft) "DIRECT LEFT NET! +$pts" else "DIRECT RIGHT NET! +$pts"
            } else {
                if (b.isLeft) "LEFT NET!" else "RIGHT NET!"
            }
        )
        lastHitByFlipper = false
        burst(b.pocketCenter, Color(0xFF9BFFB0), 18, 460f)
        chargeReactor(22f)
        registerCombo()
        addShake(0.3f)
        flashIntensity = 0.5f
        haptic(Haptic.MEDIUM)

        basketsThisBall.add(b.id)
        if (basketsThisBall.size == table.baskets.size) {
            awardScore(10000, table.reactorCenter, Color(0xFFFFE45E))
            message("DOUBLE NET RUN! +10000")
            burst(table.reactorCenter, Color(0xFFFFE45E), 40, 900f)
            flashIntensity = 1f
            addShake(0.8f)
        }
    }

    // -----------------------------------------------------------------------
    // Drain / stuck handling
    // -----------------------------------------------------------------------

    private fun checkStuck(dt: Float) {
        if (ballVel.length() < 45f && ballPos.y < table.drainY - 200f) {
            stuckTimer += dt
            if (stuckTimer > 3.5f) {
                stuckTimer = 0f
                ballVel = Offset((Random.nextFloat() - 0.5f) * 700f, -650f)
                addShake(0.3f)
                message("NUDGE")
            }
        } else {
            stuckTimer = 0f
        }
    }

    private fun checkDrain() {
        if (ballPos.y < TABLE_H + 60f) return

        if (ballSaveTimer > 0f) {
            ballSaveTimer = 0f
            message("BALL SAVED!")
            ballPos = table.plungerRestPos
            ballVel = Offset.Zero
            isLaunching = true
            springPull = 0f
            trail.clear()
            haptic(Haptic.MEDIUM)
            return
        }

        ballsLeft--
        comboCount = 0
        if (isOverload) {
            isOverload = false
            multiplier = 1
            overloadTimer = 0f
        }
        reactorCharge = 0f

        if (ballsLeft <= 0) {
            ballsLeft = 0
            isPlaying = false
            isGameOver = true
        } else {
            newBall()
            message("BALL DRAINED")
            haptic(Haptic.HEAVY)
        }
    }

    // -----------------------------------------------------------------------
    // Scoring helpers
    // -----------------------------------------------------------------------

    private fun awardScore(base: Int, at: Offset, color: Color) {
        val comboBonus = 1f + comboCount * 0.1f
        val pts = (base * multiplier * comboBonus).toInt()
        score += pts
        floatingScores.add(FloatingScore(nextScoreId++, "+$pts", at, color))
        if (floatingScores.size > 24) floatingScores.removeAt(0)
    }

    private fun registerCombo() {
        comboCount++
        comboTimer = 2.4f
        if (comboCount == 5) message("COMBO x5!")
        if (comboCount == 10) {
            message("MEGA COMBO!")
            awardScore(5000, ballPos, Color(0xFFFFE45E))
            flashIntensity = 0.8f
        }
    }

    private fun chargeReactor(amount: Float) {
        if (isOverload) return
        reactorCharge = min(100f, reactorCharge + amount)
        if (reactorCharge >= 100f) {
            isOverload = true
            overloadTimer = 18f
            multiplier = 3
            message("REACTOR OVERLOAD — 3X!")
            burst(table.reactorCenter, Color(0xFFFF4D6D), 44, 950f)
            flashIntensity = 1f
            addShake(0.9f)
            haptic(Haptic.HEAVY)
        }
    }

    private fun message(msg: String) {
        activeMessage = msg
        messageTimer = 2.0f
    }

    private fun addShake(amount: Float) {
        shake = min(1f, shake + amount)
    }

    private fun haptic(level: Haptic) {
        hapticLevel = level
        hapticTick++
    }

    private fun burst(at: Offset, color: Color, count: Int, speed: Float) {
        if (particles.size > 260) return
        repeat(count) {
            val a = Random.nextFloat() * 2f * Math.PI.toFloat()
            val s = speed * (0.35f + Random.nextFloat() * 0.65f)
            particles.add(
                Particle(
                    position = at,
                    velocity = Offset(cos(a) * s, sin(a) * s),
                    color = color,
                    decay = 1.4f + Random.nextFloat(),
                    size = 5f + Random.nextFloat() * 5f
                )
            )
        }
    }
}
