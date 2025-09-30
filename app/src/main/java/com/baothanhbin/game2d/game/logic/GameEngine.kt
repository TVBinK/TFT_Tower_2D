package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*
import com.baothanhbin.game2d.game.repo.GameDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Game Engine - quản lý logic core của game
 */
class GameEngine(
    private val gameDataStore: GameDataStore? = null,
    private val coroutineScope: CoroutineScope? = null
) {
    
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private val shopSystem = ShopSystem()
    private val economySystem = EconomySystem()
    private val mergeSystem = MergeSystem()
    private val combatSystem = CombatSystem()
    private val spawnSystem = SpawnSystem()
    private val effectSystem = EffectSystem()
    private val soundSystem = SoundSystem()
    
    /**
     * Khởi tạo game với difficulty
     */
    fun initGame(difficulty: Difficulty) {
        val newState = GameState()
            .withDifficulty(difficulty)
            .startGame()
        
        _gameState.value = newState
    }
    
    /**
     * Game tick - được gọi mỗi frame
     */
    fun tick(deltaTimeMs: Long) {
        val currentState = _gameState.value
        
        if (!currentState.isGameRunning || currentState.isPaused || currentState.isGameOver) {
            return
        }
        
        // Prevent excessive updates by checking if enough time has passed
        val currentTime = System.currentTimeMillis()
        if (currentTime - currentState.lastFrameTimeMs < 16) { // ~60 FPS max
            return
        }
        
        try {
            var newState = currentState.copy(
                lastFrameTimeMs = currentTime
            )
            
            // Phase-specific logic
            newState = when (newState.roundPhase) {
                RoundPhase.PREP -> handlePrepPhase(newState, deltaTimeMs)
                RoundPhase.COMBAT -> handleCombatPhase(newState, deltaTimeMs)
            }
            
            // Check round completion
            if (newState.isRoundComplete) {
                newState = newState.completeRound()
            }
            
            // Luôn update cooldowns của units trên board
            newState = combatSystem.updateUnitCooldowns(newState, deltaTimeMs)
            
            // Check game over
            if (newState.shouldEndGame) {
                saveGameOverData(newState)
                newState = newState.endGame()
            }
            
            _gameState.value = newState
        } catch (e: Exception) {
            // Log error but don't crash the game loop
            android.util.Log.e("GameEngine", "Error in game tick: ${e.message}", e)
        }
    }
    
    
    /**
     * Xử lý pha prep
     */
    private fun handlePrepPhase(state: GameState, deltaTimeMs: Long): GameState {
        // Trong pha prep, không có logic đặc biệt
        return state
    }
    
    /**
     * Xử lý pha combat
     */
    private fun handleCombatPhase(state: GameState, deltaTimeMs: Long): GameState {
        var newState = state
        
        // Debug: Log combat phase info
        android.util.Log.d("GameEngine", "🔥 COMBAT PHASE: enemies=${newState.enemies.size}, bullets=${newState.bullets.size}, unitsOnBoard=${newState.player.board.values.count { it != null }}")
        
        // Spawn enemies nếu chưa đủ số lượng
        if (newState.remainingEnemiesToSpawn > 0) {
            newState = spawnSystem.spawnEnemies(newState, deltaTimeMs)
            android.util.Log.d("GameEngine", "👹 SPAWN CHECK: remainingToSpawn=${newState.remainingEnemiesToSpawn}")
        }
        
        // Update units cooldowns
        newState = combatSystem.updateUnitCooldowns(newState, deltaTimeMs)
        
        // Units shoot at enemies
        newState = combatSystem.unitsShoot(newState)
        
        // Move bullets
        newState = combatSystem.moveBullets(newState, deltaTimeMs)
        
        // Move enemies
        newState = combatSystem.moveEnemies(newState, deltaTimeMs)
        
        // Handle collisions
        newState = combatSystem.handleCollisions(newState)
        
        // Update effects
        newState = effectSystem.updateEffects(newState, deltaTimeMs)
        
        // Clean up dead enemies and off-screen bullets
        newState = combatSystem.cleanup(newState)
        
        // Clean up finished effects
        newState = effectSystem.cleanupEffects(newState)
        
        // Check if enemies reached bottom
        newState = combatSystem.checkEnemiesReachedBottom(newState)
        
        return newState
    }
    
    /**
     * Mua unit từ shop
     */
    fun buyUnit(slotIndex: Int) {
        val currentState = _gameState.value
        
        if (!currentState.canBuyUnit(slotIndex)) return
        
        val (newShop, unit) = currentState.shop.buyFromSlot(slotIndex)
        unit ?: return
        
        val newPlayer = currentState.player
            .copy(gold = currentState.player.gold - unit.tier.cost)
            .addToBench(unit) ?: return
        
        // Auto-merge after buying
        val mergedPlayer = mergeSystem.tryAutoMerge(newPlayer)
        
        // Phát âm thanh mua
        soundSystem.playBuySound(unit)
        
        _gameState.value = currentState.copy(
            player = mergedPlayer,
            shop = newShop
        )
    }
    
    /**
     * Bán unit
     */
    fun sellUnit(unitId: String) {
        val currentState = _gameState.value
        
        if (!currentState.isInPrep) return
        
        val unit = currentState.player.bench.find { it.id == unitId } ?: return
        
        val newPlayer = currentState.player
            .removeFromBench(unitId)
            .copy(gold = currentState.player.gold + unit.sellPrice)
        
        // Phát âm thanh bán
        soundSystem.playSellSound(unit)
        
        _gameState.value = currentState.copy(player = newPlayer)
    }
    
    /**
     * Reroll shop
     */
    fun rerollShop() {
        val currentState = _gameState.value
        
        if (!currentState.canReroll()) return
        
        val player = currentState.player
        val newShop = currentState.shop.reroll(player.level)
        
        val (newGold, newFreeRerolls) = if (player.freeRerollsRemaining > 0) {
            Pair(player.gold, player.freeRerollsRemaining - 1)
        } else {
            Pair(player.gold - Player.REROLL_COST, player.freeRerollsRemaining)
        }
        
        _gameState.value = currentState.copy(
            shop = newShop,
            player = player.copy(
                gold = newGold,
                freeRerollsRemaining = newFreeRerolls
            )
        )
    }
    
    /**
     * Mua XP
     */
    fun buyXP() {
        val currentState = _gameState.value
        
        if (!currentState.canBuyXP()) return
        
        val newPlayer = economySystem.buyXP(currentState.player)
        
        _gameState.value = currentState.copy(player = newPlayer)
    }
    
    /**
     * Deploy unit lên board
     */
    fun deployUnit(unitId: String, slot: BoardSlot) {
        val currentState = _gameState.value
        
        if (!currentState.canManageUnits()) return
        
        val newPlayer = currentState.player.deployToBoard(unitId, slot) ?: return
        val mergedPlayer = mergeSystem.tryAutoMerge(newPlayer)
        
        _gameState.value = currentState.copy(player = mergedPlayer)
    }
    
    /**
     * Recall unit từ board
     */
    fun recallUnit(slot: BoardSlot) {
        val currentState = _gameState.value
        
        if (!currentState.canManageUnits()) return
        
        val newPlayer = currentState.player.recallFromBoard(slot) ?: return
        
        _gameState.value = currentState.copy(player = newPlayer)
    }
    
    /**
     * Swap unit giữa bench và board
     */
    fun swapUnit(unitId: String, slot: BoardSlot) {
        val currentState = _gameState.value
        
        if (!currentState.canManageUnits()) return
        
        val newPlayer = currentState.player.swapBenchBoard(unitId, slot) ?: return
        val mergedPlayer = mergeSystem.tryAutoMerge(newPlayer)
        
        _gameState.value = currentState.copy(player = mergedPlayer)
    }
    
    /**
     * Toggle pause
     */
    fun togglePause() {
        _gameState.value = _gameState.value.togglePause()
    }
    
    /**
     * Restart game
     */
    fun restartGame() {
        val difficulty = _gameState.value.difficulty
        initGame(difficulty)
    }
    
    /**
     * Bắt đầu vòng chiến đấu
     */
    fun startCombat() {
        val currentState = _gameState.value
        if (currentState.isInPrep) {
            _gameState.value = currentState.startCombat()
        }
    }
    
    /**
     * Force chuyển sang combat phase (for testing)
     */
    fun forceCombatPhase() {
        val currentState = _gameState.value
        android.util.Log.d("GameEngine", "🚀 FORCE COMBAT: Current phase=${currentState.roundPhase}")
        
        // Force start combat
        _gameState.value = currentState.startCombat()
        
        // Debug: Log after force combat
        val newState = _gameState.value
        android.util.Log.d("GameEngine", "🚀 FORCE COMBAT DONE: New phase=${newState.roundPhase}, unitsOnBoard=${newState.player.board.values.count { it != null }}")
    }
    
    /**
     * Lưu dữ liệu khi game over
     */
    private fun saveGameOverData(state: GameState) {
        gameDataStore?.let { dataStore ->
            coroutineScope?.launch {
                dataStore.saveBestScore(state.player.score)
                dataStore.saveLastDifficulty(state.difficulty)
                dataStore.saveGameStats(
                    wave = state.player.wave,
                    goldEarned = state.player.gold.toLong(),
                    enemiesKilled = 0L // TODO: track enemies killed
                )
            }
        }
    }
}
