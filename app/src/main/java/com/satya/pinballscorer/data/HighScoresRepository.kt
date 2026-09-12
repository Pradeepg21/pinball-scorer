package com.satya.pinballscorer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "high_scores_prefs")

/** One completed game: the final score and how long the ball(s) stayed in play. */
data class HighScoreEntry(val score: Int, val timeSeconds: Int)

class HighScoresRepository(private val context: Context) {
    private val SCORES_KEY = stringPreferencesKey("top_high_scores")

    val highScoresFlow: Flow<List<HighScoreEntry>> = context.dataStore.data
        .map { preferences -> parse(preferences[SCORES_KEY] ?: "") }

    suspend fun saveScore(newScore: Int, timeSeconds: Int) {
        if (newScore <= 0) return
        context.dataStore.edit { preferences ->
            val current = parse(preferences[SCORES_KEY] ?: "").toMutableList()
            current.add(HighScoreEntry(newScore, timeSeconds))
            val top = current.sortedByDescending { it.score }.take(MAX_ENTRIES)
            preferences[SCORES_KEY] = top.joinToString(",") { "${it.score}:${it.timeSeconds}" }
        }
    }

    /** Tolerates the older "score,score,..." format with no time component. */
    private fun parse(raw: String): List<HighScoreEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
            .mapNotNull { entry ->
                if (entry.isBlank()) return@mapNotNull null
                val parts = entry.split(":")
                val score = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val time = parts.getOrNull(1)?.toIntOrNull() ?: 0
                HighScoreEntry(score, time)
            }
            .sortedByDescending { it.score }
            .take(MAX_ENTRIES)
    }

    companion object {
        private const val MAX_ENTRIES = 10
    }
}
