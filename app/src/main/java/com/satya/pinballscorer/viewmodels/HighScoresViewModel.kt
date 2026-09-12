package com.satya.pinballscorer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.satya.pinballscorer.data.HighScoreEntry
import com.satya.pinballscorer.data.HighScoresRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HighScoresViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HighScoresRepository(application)

    val highScores: StateFlow<List<HighScoreEntry>> = repository.highScoresFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveScore(score: Int, timeSeconds: Int) {
        viewModelScope.launch {
            repository.saveScore(score, timeSeconds)
        }
    }
}
