package com.baothanhbin.game2d.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.game2d.game.logic.GameEngine
import com.baothanhbin.game2d.game.model.*
import com.baothanhbin.game2d.game.datastore.GameDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * GameViewModel - quản lý state và logic của game
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    private val gameDataStore = GameDataStore(application)
    private val gameEngine = GameEngine(gameDataStore, viewModelScope)
    
    // Game state
    val gameState: StateFlow<GameState> = gameEngine.gameState
    
    // Game loop job
    private var gameLoopJob: Job? = null
    
    companion object {
        private const val TARGET_FPS = 60
        private const val FRAME_TIME_MS = 1000L / TARGET_FPS
    }
    
    /**
     * Bắt đầu game
     */
    fun startGame(mode: GameMode = GameMode.CAMPAIGN) {
        android.util.Log.d("GameViewModel", "🎮 Starting game")
        stopGameLoop()
        gameEngine.initGame(mode) // Khởi tạo game state theo chế độ
        startGameLoop()
    }
    
    /**
     * Bắt đầu game loop
     */
    private fun startGameLoop() {
        android.util.Log.d("GameViewModel", "🔄 Starting game loop")
        gameLoopJob = viewModelScope.launch {
            var lastFrameTime = System.currentTimeMillis()
            var frameCount = 0
            
            while (isActive) {
                try {
                    val currentTime = System.currentTimeMillis()
                    val deltaTime = currentTime - lastFrameTime
                    
                    // Limit delta time to prevent large jumps
                    val clampedDeltaTime = deltaTime.coerceAtMost(100L)
                    
                    // Game tick
                    gameEngine.tick(clampedDeltaTime)
                    
                    // Log every 60 frames (1 second at 60fps)
                    frameCount++
                    if (frameCount % 60 == 0) {
                        android.util.Log.d("GameViewModel", "🔄 Game loop running... frame $frameCount")
                    }
                    
                    lastFrameTime = currentTime
                    
                    // Delay để duy trì target FPS
                    val sleepTime = FRAME_TIME_MS - (System.currentTimeMillis() - currentTime)
                    if (sleepTime > 0) {
                        delay(sleepTime)
                    }
                } catch (e: Exception) {
                    // Log error and continue to prevent crash
                    android.util.Log.e("GameLoop", "Error in game loop: ${e.message}", e)
                    delay(FRAME_TIME_MS) // Continue with normal timing
                }
            }
        }
    }
    
    /**
     * Dừng game loop
     */
    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }
    
    /**
     * Pause/unpause game
     */
    fun togglePause() {
        gameEngine.togglePause()
    }
    
    /**
     * Restart game
     */
    fun restartGame() {
        gameEngine.restartGame()
    }
    
    /**
     * Bắt đầu vòng chiến đấu
     */
    fun startCombat() {
        gameEngine.startCombat()
    }
    
    /**
     * Force chuyển sang combat phase (for testing)
     */
    fun forceCombatPhase() {
        gameEngine.forceCombatPhase()
    }
    
    /**
     * Shop actions
     */
    fun buyUnit(slotIndex: Int) {
        gameEngine.buyUnit(slotIndex)
    }
    
    fun sellUnit(unitId: String) {
        gameEngine.sellUnit(unitId)
    }
    
    fun rerollShop() {
        gameEngine.rerollShop()
    }
    
    fun buyXP() {
        gameEngine.buyXP()
    }
    
    /**
     * Unit management actions
     */
    fun deployUnit(unitId: String, slot: BoardSlot) {
        gameEngine.deployUnit(unitId, slot)
    }
    
    fun recallUnit(slot: BoardSlot) {
        gameEngine.recallUnit(slot)
    }
    
    fun swapUnit(unitId: String, slot: BoardSlot) {
        gameEngine.swapUnit(unitId, slot)
    }
    
    /**
     * Drag & Drop actions
     */
    fun onDragStart(unitId: String) {
        // Placeholder for drag start logic
    }
    
    fun onDragEnd() {
        // Placeholder for drag end logic
    }
    
    fun onDropToBoard(unitId: String, slot: BoardSlot) {
        swapUnit(unitId, slot)
    }
    
    fun onDropToBench(unitId: String) {
        // If unit is on board, recall it
        val currentState = gameState.value
        val unit = currentState.player.board.values.find { it?.id == unitId }
        
        if (unit?.boardPosition != null) {
            recallUnit(unit.boardPosition)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}
