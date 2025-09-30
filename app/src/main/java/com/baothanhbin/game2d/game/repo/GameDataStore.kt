package com.baothanhbin.game2d.game.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.baothanhbin.game2d.game.model.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore để lưu trữ dữ liệu game
 */
class GameDataStore(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_prefs")
        
        // Keys
        private val BEST_SCORE = longPreferencesKey("best_score")
        private val LAST_DIFFICULTY = stringPreferencesKey("last_difficulty")
        private val SOUND_ON = booleanPreferencesKey("sound_on")
        private val VIBRATE_ON = booleanPreferencesKey("vibrate_on")
        private val TOTAL_GAMES_PLAYED = intPreferencesKey("total_games_played")
        private val HIGHEST_WAVE = intPreferencesKey("highest_wave")
        private val TOTAL_GOLD_EARNED = longPreferencesKey("total_gold_earned")
        private val TOTAL_ENEMIES_KILLED = longPreferencesKey("total_enemies_killed")
    }
    
    /**
     * Lưu best score
     */
    suspend fun saveBestScore(score: Long) {
        context.dataStore.edit { preferences ->
            val currentBest = preferences[BEST_SCORE] ?: 0L
            if (score > currentBest) {
                preferences[BEST_SCORE] = score
            }
        }
    }
    
    /**
     * Lấy best score
     */
    val bestScore: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[BEST_SCORE] ?: 0L
    }
    
    /**
     * Lưu difficulty cuối cùng
     */
    suspend fun saveLastDifficulty(difficulty: Difficulty) {
        context.dataStore.edit { preferences ->
            preferences[LAST_DIFFICULTY] = difficulty.name
        }
    }
    
    /**
     * Lấy difficulty cuối cùng
     */
    val lastDifficulty: Flow<Difficulty> = context.dataStore.data.map { preferences ->
        val difficultyName = preferences[LAST_DIFFICULTY] ?: Difficulty.NORMAL.name
        try {
            Difficulty.valueOf(difficultyName)
        } catch (e: Exception) {
            Difficulty.NORMAL
        }
    }
    
    /**
     * Settings âm thanh
     */
    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_ON] = enabled
        }
    }
    
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SOUND_ON] ?: true
    }
    
    /**
     * Settings rung
     */
    suspend fun setVibrateEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATE_ON] = enabled
        }
    }
    
    val vibrateEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIBRATE_ON] ?: true
    }
    
    /**
     * Lưu thống kê game
     */
    suspend fun saveGameStats(
        wave: Int,
        goldEarned: Long,
        enemiesKilled: Long
    ) {
        context.dataStore.edit { preferences ->
            // Tăng số game đã chơi
            val currentGames = preferences[TOTAL_GAMES_PLAYED] ?: 0
            preferences[TOTAL_GAMES_PLAYED] = currentGames + 1
            
            // Cập nhật highest wave
            val currentHighestWave = preferences[HIGHEST_WAVE] ?: 0
            if (wave > currentHighestWave) {
                preferences[HIGHEST_WAVE] = wave
            }
            
            // Cộng dồn thống kê
            val currentGold = preferences[TOTAL_GOLD_EARNED] ?: 0L
            preferences[TOTAL_GOLD_EARNED] = currentGold + goldEarned
            
            val currentEnemies = preferences[TOTAL_ENEMIES_KILLED] ?: 0L
            preferences[TOTAL_ENEMIES_KILLED] = currentEnemies + enemiesKilled
        }
    }
    
    /**
     * Lấy thống kê tổng hợp
     */
    data class GameStats(
        val totalGamesPlayed: Int,
        val bestScore: Long,
        val highestWave: Int,
        val totalGoldEarned: Long,
        val totalEnemiesKilled: Long
    )
    
    val gameStats: Flow<GameStats> = context.dataStore.data.map { preferences ->
        GameStats(
            totalGamesPlayed = preferences[TOTAL_GAMES_PLAYED] ?: 0,
            bestScore = preferences[BEST_SCORE] ?: 0L,
            highestWave = preferences[HIGHEST_WAVE] ?: 0,
            totalGoldEarned = preferences[TOTAL_GOLD_EARNED] ?: 0L,
            totalEnemiesKilled = preferences[TOTAL_ENEMIES_KILLED] ?: 0L
        )
    }
    
    /**
     * Reset tất cả dữ liệu
     */
    suspend fun resetAllData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
