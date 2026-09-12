package com.satya.pinballscorer.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.satya.pinballscorer.data.HighScoreEntry
import com.satya.pinballscorer.ui.game.Palette
import com.satya.pinballscorer.util.formatDuration
import com.satya.pinballscorer.viewmodels.HighScoresViewModel

private val RankGold = Color(0xFFFFD54A)
private val RankSilver = Color(0xFFC7D0E0)
private val RankBronze = Color(0xFFE08B4E)

@Composable
fun HighScoresScreen(
    onBackPressed: () -> Unit,
    viewModel: HighScoresViewModel = viewModel()
) {
    val highScores by viewModel.highScores.collectAsState()

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
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LEADERBOARD",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "TOP 10 RUNS",
                color = Palette.neonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (highScores.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NO RUNS RECORDED YET",
                        color = Color(0xFF8794B5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Play a game to set your first high score.",
                        color = Color(0xFF5C6690),
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(highScores) { index, entry ->
                        HighScoreRow(rank = index + 1, entry = entry)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onBackPressed) {
                Text("BACK TO MENU", color = Color(0xFF8794B5), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HighScoreRow(rank: Int, entry: HighScoreEntry) {
    val accent = when (rank) {
        1 -> RankGold
        2 -> RankSilver
        3 -> RankBronze
        else -> Palette.chromeMid
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1A2244).copy(alpha = 0.85f), Color(0xFF0C1026).copy(alpha = 0.85f))
                )
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (rank <= 3) 0.9f else 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                color = if (rank <= 3) Color(0xFF0C1026) else accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "%,d".format(entry.score),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "TIME PLAYED  ${formatDuration(entry.timeSeconds)}",
                color = Color(0xFF7D8AAE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
