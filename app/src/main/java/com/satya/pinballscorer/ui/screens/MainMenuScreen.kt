package com.satya.pinballscorer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.satya.pinballscorer.BuildConfig
import com.satya.pinballscorer.ui.components.RetroButton
import com.satya.pinballscorer.ui.game.Palette
import com.satya.pinballscorer.viewmodels.HighScoresViewModel

@Composable
fun MainMenuScreen(
    onStartGameClicked: () -> Unit,
    onHighScoresClicked: () -> Unit,
    highScoresViewModel: HighScoresViewModel = viewModel()
) {
    val highScores by highScoresViewModel.highScores.collectAsState()
    val personalBest = highScores.firstOrNull()?.score

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Palette.voidMid, Palette.voidTop, Palette.voidBottom))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoMark()

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "RETRO POCKET",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                text = "PINBALL",
                color = Palette.neonCyan,
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            PersonalBestBadge(personalBest)

            Spacer(modifier = Modifier.height(44.dp))

            RetroButton(text = "START GAME", onClick = onStartGameClicked)
            Spacer(modifier = Modifier.height(14.dp))
            RetroButton(text = "HIGH SCORES", onClick = onHighScoresClicked)
        }

        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            color = Color(0xFF3E4870),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        )
    }
}

/** A small chrome-ball-and-flipper mark drawn with the same lighting model as the table. */
@Composable
private fun LogoMark() {
    Canvas(modifier = Modifier.size(84.dp)) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Palette.neonCyan.copy(alpha = 0.35f), Color.Transparent),
                center = center,
                radius = r * 1.7f
            ),
            radius = r * 1.7f,
            center = center
        )
        drawArc(
            color = Palette.neonCyan.copy(alpha = 0.55f),
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(center.x - r * 0.95f, center.y - r * 0.95f),
            size = androidx.compose.ui.geometry.Size(r * 1.9f, r * 1.9f),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFD3DEEC), Color(0xFF7E8CA6), Color(0xFF2A3346)),
                center = Offset(center.x - r * 0.22f, center.y - r * 0.22f),
                radius = r * 0.95f
            ),
            radius = r * 0.5f,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = r * 0.11f,
            center = Offset(center.x - r * 0.18f, center.y - r * 0.2f)
        )
    }
}

@Composable
private fun PersonalBestBadge(personalBest: Int?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (personalBest != null) Palette.neonLime else Color(0xFF3E4870))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (personalBest != null) {
                "PERSONAL BEST  %,d".format(personalBest)
            } else {
                "NO RUNS YET — SET YOUR FIRST SCORE"
            },
            color = Color(0xFFB9C2DE),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
