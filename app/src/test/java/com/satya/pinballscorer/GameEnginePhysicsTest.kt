package com.satya.pinballscorer

import androidx.compose.ui.geometry.Offset
import com.satya.pinballscorer.game.GameEngine
import com.satya.pinballscorer.game.TABLE_H
import com.satya.pinballscorer.game.TABLE_W
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.hypot

/**
 * Regression tests for the two behaviours the rewrite had to fix — a ball that moved
 * and scored without being hit, and flippers that did not actually transfer energy.
 */
class GameEnginePhysicsTest {

    private lateinit var engine: GameEngine
    private val dt = 1f / 60f

    @Before
    fun setUp() {
        engine = GameEngine()
        engine.start("medium")
    }

    private fun step(seconds: Float, onStep: (() -> Unit)? = null) {
        val frames = (seconds / dt).toInt()
        repeat(frames) {
            engine.update(dt)
            onStep?.invoke()
        }
    }

    private fun speed() = hypot(engine.ballVel.x, engine.ballVel.y)

    /** Mechanical energy per unit mass, with the table floor as the datum. */
    private fun energy(): Float {
        val v = speed()
        return 0.5f * v * v + 2400f * (TABLE_H - engine.ballPos.y)
    }

    /** A point on the left flipper's upper face, exactly touching it. */
    private fun restOnLeftFlipper(): Offset {
        val f = engine.table.flipperLeft
        val mid = Offset((f.pivot.x + f.tip.x) / 2f, (f.pivot.y + f.tip.y) / 2f)
        val d = Offset(f.tip.x - f.pivot.x, f.tip.y - f.pivot.y)
        val len = hypot(d.x, d.y)
        val n = Offset(d.y / len, -d.x / len)          // points up off the face
        val gap = engine.ballRadius + f.radius
        return Offset(mid.x + n.x * gap, mid.y + n.y * gap)
    }

    // -----------------------------------------------------------------------

    @Test
    fun `ball resting on an idle flipper never gains energy`() {
        engine.placeBall(restOnLeftFlipper(), Offset.Zero)
        val startEnergy = energy()
        var maxEnergy = startEnergy

        step(1.2f) {
            if (!engine.isLaunching) maxEnergy = maxOf(maxEnergy, energy())
        }

        assertTrue(
            "Passive contact added energy: $startEnergy -> $maxEnergy",
            maxEnergy <= startEnergy * 1.02f + 1f
        )
    }

    @Test
    fun `ball touching a wall at rest scores nothing`() {
        // Against the left outer wall, which the old engine paid points for touching.
        engine.placeBall(Offset(40f + engine.ballRadius + 7f, 900f), Offset.Zero)
        step(1.5f)
        assertEquals("Touching a passive wall must not score", 0, engine.score)
    }

    @Test
    fun `swinging the flipper launches the ball upward`() {
        val f = engine.table.flipperLeft
        engine.placeBall(Offset(f.pivot.x + 40f, f.pivot.y - 30f), Offset.Zero)
        engine.setFlipper(left = true, pressed = true)
        step(0.25f)

        assertTrue("Flipper should drive the ball upward, vy=${engine.ballVel.y}", engine.ballVel.y < -400f)
        assertTrue("Flipper shot too weak: ${speed()}", speed() > 1000f)
    }

    @Test
    fun `flipper press is honoured on the very next step`() {
        engine.setFlipper(left = true, pressed = true)
        val before = engine.table.flipperLeft.angle
        engine.update(dt)
        assertTrue(
            "Flipper did not begin moving on the first frame",
            engine.table.flipperLeft.angle < before - 5f
        )
    }

    @Test
    fun `a fast ball at full speed never escapes the table`() {
        engine.placeBall(Offset(450f, 900f), Offset(3400f, 0f))
        step(3f) {
            val p = engine.ballPos
            assertTrue("Ball tunnelled out at $p", p.x > -5f && p.x < TABLE_W + 5f)
            assertTrue("Ball tunnelled out at $p", p.y > -5f)
        }
    }

    @Test
    fun `a slow ball punches through the net and keeps moving`() {
        val b = engine.table.baskets.first()
        val entry = Offset(b.mouth.x + b.openDir.x * 40f, b.mouth.y + b.openDir.y * 40f)
        engine.placeBall(entry, Offset(-b.openDir.x * 380f, -b.openDir.y * 380f))

        step(0.6f)

        assertTrue("A shot through the net should pay out", engine.score >= 2500)
        assertTrue(
            "The ball must not be frozen in the pocket, it should sail through",
            hypot(engine.ballVel.x, engine.ballVel.y) > 1f
        )
    }

    @Test
    fun `a fast ball through the net also scores and is never held`() {
        val b = engine.table.baskets.first()
        val entry = Offset(b.mouth.x + b.openDir.x * 40f, b.mouth.y + b.openDir.y * 40f)
        engine.placeBall(entry, Offset(-b.openDir.x * 2200f, -b.openDir.y * 2200f))

        step(0.3f)

        assertTrue("A fast shot through the net should still score", engine.score >= 2500)
    }

    @Test
    fun `passing through both nets on one ball pays the double-run bonus`() {
        val before = engine.score
        engine.table.baskets.forEach { b ->
            val entry = Offset(b.mouth.x + b.openDir.x * 40f, b.mouth.y + b.openDir.y * 40f)
            engine.placeBall(entry, Offset(-b.openDir.x * 380f, -b.openDir.y * 380f))
            step(0.3f)
        }
        assertTrue(
            "Running both nets on the same ball should pay the big double bonus on top",
            engine.score - before >= 2500 * 2 + 10000
        )
    }

    @Test
    fun `a fast upward shot enters the pipe and pays a bonus`() {
        val p = engine.table.pipe
        engine.placeBall(Offset(p.entrance.x, p.entrance.y + 40f), Offset(-60f, -1500f))

        step(0.15f)
        assertTrue("Shot should have been swallowed by the pipe", engine.ballInPipe)
        assertTrue("Pipe entry should score", engine.score >= 500)

        val entryScore = engine.score
        step(2.5f)
        assertTrue("Pipe should have completed", !engine.ballInPipe)
        assertTrue("Completing the pipe should pay a bonus", engine.score > entryScore + 1000)
    }

    @Test
    fun `a slow ball is not swallowed by the pipe`() {
        val p = engine.table.pipe
        engine.placeBall(Offset(p.entrance.x, p.entrance.y + 40f), Offset(0f, -200f))
        step(0.1f)
        assertTrue("A dribble must not enter the pipe", !engine.ballInPipe)
    }

    @Test
    fun `draining costs a ball and re-racks at the plunger`() {
        engine.placeBall(Offset(450f, TABLE_H - 40f), Offset(0f, 2000f))
        engine.ballSaveTimer = 0f
        val balls = engine.ballsLeft

        step(0.6f)

        assertEquals(balls - 1, engine.ballsLeft)
        assertTrue("A fresh ball should be waiting at the plunger", engine.isLaunching)
    }

    @Test
    fun `the softest plunge still clears the plunger lane`() {
        engine.springPull = 0f
        engine.releaseSpring()
        var highest = TABLE_H
        step(1.2f) { highest = minOf(highest, engine.ballPos.y) }
        assertTrue("Minimum plunge left the ball stuck in the lane (y=$highest)", highest < 440f)
    }

    @Test
    fun `an idle ball in open play does not accumulate score`() {
        engine.placeBall(Offset(450f, 1300f), Offset.Zero)
        step(0.5f)
        val settled = engine.score
        step(1.0f)
        assertTrue(
            "Score crept up without a hit: $settled -> ${engine.score}",
            engine.score - settled < 400
        )
    }
}
