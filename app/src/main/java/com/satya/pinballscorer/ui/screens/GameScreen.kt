package com.satya.pinballscorer.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.satya.pinballscorer.game.GameEngine
import com.satya.pinballscorer.game.Haptic
import com.satya.pinballscorer.game.TABLE_H
import com.satya.pinballscorer.game.TABLE_W
import com.satya.pinballscorer.ui.components.RetroButton
import com.satya.pinballscorer.ui.game.Palette
import com.satya.pinballscorer.ui.game.Star
import com.satya.pinballscorer.ui.game.drawBackdrop
import com.satya.pinballscorer.ui.game.drawCabinet
import com.satya.pinballscorer.ui.game.drawGlassOverlay
import com.satya.pinballscorer.ui.game.drawTable
import com.satya.pinballscorer.util.formatDuration
import com.satya.pinballscorer.viewmodels.HighScoresViewModel
import kotlin.math.min
import kotlin.random.Random

@Composable
fun GameScreen(
    level: String,
    onGameOver: () -> Unit,
    highScoresViewModel: HighScoresViewModel = viewModel()
) {
    val engine = remember { GameEngine() }
    val view = LocalView.current
    val textMeasurer = rememberTextMeasurer()
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    var time by remember { mutableFloatStateOf(0f) }

    val stars = remember {
        val rnd = Random(20260912)
        List(140) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                radius = 0.8f + rnd.nextFloat() * 2.4f,
                phase = rnd.nextFloat() * 6.28f,
                alpha = 0.25f + rnd.nextFloat() * 0.65f
            )
        }
    }

    LaunchedEffect(level) { engine.start(level) }

    // Game loop.
    LaunchedEffect(engine.isPlaying) {
        lastFrameTime = 0L
        while (engine.isPlaying) {
            withFrameNanos { frameTime ->
                if (lastFrameTime != 0L) {
                    val dt = (frameTime - lastFrameTime) / 1_000_000_000f
                    time += dt
                    engine.update(dt)
                }
                lastFrameTime = frameTime
            }
        }
    }

    // Persist the score once, when the game actually ends.
    LaunchedEffect(engine.isGameOver) {
        if (engine.isGameOver && engine.score > 0) {
            highScoresViewModel.saveScore(engine.score, time.toInt())
        }
    }

    LaunchedEffect(engine.hapticTick) {
        if (engine.hapticTick == 0) return@LaunchedEffect
        view.performHapticFeedback(
            when (engine.hapticLevel) {
                Haptic.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
                Haptic.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
                Haptic.HEAVY -> HapticFeedbackConstants.LONG_PRESS
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Palette.voidTop)) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { handleTouch(engine) }
        ) {
            // Reading `frame` here is what makes the draw layer invalidate every
            // simulation tick without recomposing the surrounding UI.
            if (engine.frame < 0) return@Canvas

            drawBackdrop(stars, time)

            val bottomMargin = 26.dp.toPx()
            val scale = min(size.width / TABLE_W, (size.height - bottomMargin) / TABLE_H)
            val tableW = TABLE_W * scale
            val tableH = TABLE_H * scale
            val originX = (size.width - tableW) / 2f
            val originY = size.height - bottomMargin - tableH

            withTransform({
                translate(originX, originY)
                clipRect(0f, 0f, tableW, tableH)
                scale(scale, scale, pivot = Offset.Zero)
                translate(engine.shakeOffset.x, engine.shakeOffset.y)
            }) {
                drawTable(engine, time)
            }

            drawCabinet(originX, originY, tableW, tableH, scale)

            // Floating scores live in canvas space so the text stays crisp.
            engine.floatingScores.forEach { fs ->
                val a = fs.life.coerceIn(0f, 1f)
                val p = Offset(originX + fs.position.x * scale, originY + fs.position.y * scale)
                val layout = textMeasurer.measure(
                    text = fs.text,
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = fs.color.copy(alpha = a)
                    )
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f - (1f - a) * 26f)
                )
            }

            drawGlassOverlay(engine.flashIntensity)
        }

        TopHud(engine)

        engine.activeMessage?.let { msg ->
            Text(
                text = msg,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 150.dp)
                    .fillMaxWidth()
            )
        }

        if (engine.isLaunching && engine.isPlaying) {
            Text(
                text = "PULL THE PLUNGER DOWN AND RELEASE",
                color = Palette.neonLime,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
            )
        }

        if (engine.isGameOver) {
            GameOverOverlay(score = engine.score, timeSeconds = time.toInt(), onExit = onGameOver)
        }
    }
}

/**
 * Raw pointer handling. Gesture detectors add slop and press-timeout latency and only
 * track a single pointer, which is what made the old flippers feel laggy and made it
 * impossible to hold both at once. Reading the pointer event directly removes both
 * problems: a flipper fires on the very first ACTION_DOWN frame.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.handleTouch(engine: GameEngine) {
    awaitPointerEventScope {
        var plungerId: PointerId? = null
        var plungerStartY = 0f

        while (true) {
            val event = awaitPointerEvent()
            var leftHeld = false
            var rightHeld = false

            event.changes.forEach { change ->
                val inPlungerZone = engine.isLaunching &&
                    change.position.x > size.width * 0.60f &&
                    change.position.y > size.height * 0.62f

                if (!change.pressed) {
                    if (change.id == plungerId) {
                        plungerId = null
                        engine.isPullingSpring = false
                        engine.releaseSpring()
                    }
                    return@forEach
                }

                when {
                    change.id == plungerId -> {
                        engine.isPullingSpring = true
                        engine.springPull =
                            ((change.position.y - plungerStartY) / (size.height * 0.16f))
                                .coerceIn(0f, 1f)
                    }
                    plungerId == null && inPlungerZone -> {
                        plungerId = change.id
                        plungerStartY = change.position.y
                        engine.isPullingSpring = true
                    }
                    else -> {
                        if (change.position.x < size.width / 2f) leftHeld = true else rightHeld = true
                    }
                }
            }

            engine.setFlipper(left = true, pressed = leftHeld)
            engine.setFlipper(left = false, pressed = rightHeld)
        }
    }
}

@Composable
private fun TopHud(engine: GameEngine) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A2244), Color(0xFF0A0E1F))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("SCORE", color = Color(0xFF7D8AAE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "%,d".format(engine.score),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (engine.multiplier > 1) {
                    HudChip("${engine.multiplier}X", Palette.neonMagenta)
                    Spacer(Modifier.width(6.dp))
                }
                if (engine.comboCount > 1) {
                    HudChip("COMBO ${engine.comboCount}", Palette.neonAmber)
                    Spacer(Modifier.width(6.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("BALLS", color = Color(0xFF7D8AAE), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Row {
                        repeat(3) { i ->
                            Box(
                                modifier = Modifier
                                    .padding(start = 3.dp)
                                    .size(11.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (i < engine.ballsLeft)
                                            Brush.linearGradient(listOf(Color.White, Color(0xFF6B7C9B)))
                                        else
                                            Brush.linearGradient(listOf(Color(0xFF262E48), Color(0xFF161B2C)))
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        // Reactor charge meter.
        Canvas(modifier = Modifier.fillMaxWidth().height(9.dp)) {
            val r = size.height / 2f
            drawRoundRect(
                color = Color(0xFF10162C),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
            )
            val w = size.width * (engine.reactorCharge / 100f)
            if (w > 1f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        if (engine.isOverload)
                            listOf(Palette.neonMagenta, Color.White, Palette.neonMagenta)
                        else
                            listOf(Color(0xFF1E6BA8), Palette.neonCyan)
                    ),
                    size = Size(w, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
                )
            }
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                style = Stroke(width = 1.5f)
            )
        }

        if (engine.ballSaveTimer > 0f) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "BALL SAVE  ${"%.0f".format(engine.ballSaveTimer)}s",
                color = Palette.neonLime,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HudChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.22f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GameOverOverlay(score: Int, timeSeconds: Int, onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", color = Palette.neonMagenta, fontSize = 40.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("FINAL SCORE", color = Color(0xFF8794B5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "%,d".format(score),
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "TIME PLAYED  ${formatDuration(timeSeconds)}",
                color = Color(0xFF8794B5),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(32.dp))
            RetroButton(text = "MAIN MENU", onClick = onExit)
        }
    }
}
