package com.baothanhbin.game2d.game.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
 
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * High Score Entry
 */
@Serializable
data class HighScoreEntry(
    val score: Long,
    val day: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * DataStore để lưu trữ dữ liệu game
 */
class GameDataStore(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_prefs")
        
        // Keys
        private val BEST_SCORE = longPreferencesKey("best_score")
        private val LAST_DIFFICULTY = stringPreferencesKey("last_difficulty")
        private val SOUND_ON = booleanPreferencesKey("sound_on")
        private val VIBRATE_ON = booleanPreferencesKey("vibrate_on")
        private val TOTAL_GAMES_PLAYED = intPreferencesKey("total_games_played")
        private val HIGHEST_DAY = intPreferencesKey("highest_day")
        private val TOTAL_GOLD_EARNED = longPreferencesKey("total_gold_earned")
        private val TOTAL_ENEMIES_KILLED = longPreferencesKey("total_enemies_killed")
        
        // Top 6 scores keys
        private val TOP_6_SCORES = stringPreferencesKey("top_6_scores")
        private val TOP_6_DAYS = stringPreferencesKey("top_6_days")
    }
    
    /**
     * Lưu best score và top 6 scores
     */
    suspend fun saveBestScore(score: Long, day: Int) {
        context.dataStore.edit { preferences ->
            val currentBest = preferences[BEST_SCORE] ?: 0L
            if (score > currentBest) {
                preferences[BEST_SCORE] = score
            }
            
            // Cập nhật top 6 scores
            val currentScoresJson = preferences[TOP_6_SCORES] ?: "[]"
            val currentDaysJson = preferences[TOP_6_DAYS] ?: "[]"
            
            try {
                val currentScores = json.decodeFromString<List<Long>>(currentScoresJson)
                val currentDays = json.decodeFromString<List<Int>>(currentDaysJson)
                
                val newEntry = HighScoreEntry(score, day)
                val entries = mutableListOf<HighScoreEntry>()
                
                // Thêm entries hiện tại
                currentScores.forEachIndexed { index, score ->
                    if (index < currentDays.size) {
                        entries.add(HighScoreEntry(score, currentDays[index]))
                    }
                }
                
                // Thêm entry mới
                entries.add(newEntry)
                
                // Sắp xếp theo score giảm dần và lấy top 6
                val top6 = entries.sortedByDescending { it.score }.take(6)
                
                val top6Scores = top6.map { it.score }
                val top6Days = top6.map { it.day }
                
                preferences[TOP_6_SCORES] = json.encodeToString(top6Scores)
                preferences[TOP_6_DAYS] = json.encodeToString(top6Days)
                
            } catch (e: Exception) {
                // Nếu có lỗi parse, tạo mới
                preferences[TOP_6_SCORES] = json.encodeToString(listOf(score))
                preferences[TOP_6_DAYS] = json.encodeToString(listOf(day))
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
     * Lấy top 6 high scores
     */
    val top6Scores: Flow<List<HighScoreEntry>> = context.dataStore.data.map { preferences ->
        val scoresJson = preferences[TOP_6_SCORES] ?: "[]"
        val daysJson = preferences[TOP_6_DAYS] ?: "[]"
        
        try {
            val scores = json.decodeFromString<List<Long>>(scoresJson)
            val days = json.decodeFromString<List<Int>>(daysJson)
            
            val entries = mutableListOf<HighScoreEntry>()
            scores.forEachIndexed { index, score ->
                if (index < days.size) {
                    entries.add(HighScoreEntry(score, days[index]))
                }
            }
            entries.sortedByDescending { it.score }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Không còn lưu difficulty
    
    // Không còn đọc difficulty
    
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
        day: Int,
        goldEarned: Long,
        enemiesKilled: Long
    ) {
        context.dataStore.edit { preferences ->
            // Tăng số game đã chơi
            val currentGames = preferences[TOTAL_GAMES_PLAYED] ?: 0
            preferences[TOTAL_GAMES_PLAYED] = currentGames + 1
            
            // Cập nhật highest day
            val currentHighestDay = preferences[HIGHEST_DAY] ?: 0
            if (day > currentHighestDay) {
                preferences[HIGHEST_DAY] = day
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
        val highestDay: Int,
        val totalGoldEarned: Long,
        val totalEnemiesKilled: Long
    )
    
    val gameStats: Flow<GameStats> = context.dataStore.data.map { preferences ->
        GameStats(
            totalGamesPlayed = preferences[TOTAL_GAMES_PLAYED] ?: 0,
            bestScore = preferences[BEST_SCORE] ?: 0L,
            highestDay = preferences[HIGHEST_DAY] ?: 0,
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
