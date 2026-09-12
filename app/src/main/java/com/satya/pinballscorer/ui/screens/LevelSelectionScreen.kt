package com.satya.pinballscorer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satya.pinballscorer.ui.game.Palette

private data class Difficulty(
    val id: String,
    val label: String,
    val tagline: String,
    val detail: String,
    val accent: Color
)

private val DIFFICULTIES = listOf(
    Difficulty(
        id = "easy",
        label = "EASY",
        tagline = "Learn the table",
        detail = "Softer gravity, gentler ball speed — best for finding your rhythm.",
        accent = Palette.neonLime
    ),
    Difficulty(
        id = "medium",
        label = "MEDIUM",
        tagline = "The classic run",
        detail = "Balanced gravity and pace — the way the table is meant to play.",
        accent = Palette.neonCyan
    ),
    Difficulty(
        id = "hard",
        label = "HARD",
        tagline = "Full speed",
        detail = "Heavier gravity, faster ball — every shot has to count.",
        accent = Palette.neonMagenta
    )
)

@Composable
fun LevelSelectionScreen(
    onLevelSelected: (String) -> Unit,
    onBackPressed: () -> Unit
) {
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
                .padding(horizontal = 22.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SELECT DIFFICULTY",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "CHOOSE YOUR TABLE SETTINGS",
                color = Color(0xFF7D8AAE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            DIFFICULTIES.forEach { difficulty ->
                DifficultyCard(difficulty = difficulty, onClick = { onLevelSelected(difficulty.id) })
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))

            TextButton(onClick = onBackPressed) {
                Text("BACK", color = Color(0xFF8794B5), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DifficultyCard(difficulty: Difficulty, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1A2244).copy(alpha = 0.9f), Color(0xFF0C1026).copy(alpha = 0.9f))
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(difficulty.accent)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = difficulty.label,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = difficulty.tagline,
                color = difficulty.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = difficulty.detail,
                color = Color(0xFF8794B5),
                fontSize = 11.sp
            )
        }

        Text(
            text = "▶",
            color = difficulty.accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
    }
}
