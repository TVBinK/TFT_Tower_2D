package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*
import com.baothanhbin.game2d.game.datastore.GameDataStore
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
    private val economySystem = EconomySystem()
    private val mergeSystem = MergeSystem()
    private val combatSystem = CombatSystem()
    private val spawnSystem = SpawnSystem()
    private val effectSystem = EffectSystem()

    /**
     * Khởi tạo game (không còn chế độ khó)
     */
    fun initGame(mode: GameMode = GameMode.CAMPAIGN) {
        val newState = GameState(gameMode = mode).startGame()
        _gameState.value = newState
    }
    
    
    /**
     * Cập nhật game state
     */
    fun updateGameState(newState: GameState) {
        _gameState.value = newState
    }
    
    /**
     * Xử lý pha prep
     */
    fun handlePrepPhase(state: GameState, deltaTimeMs: Long): GameState {
        // Trong pha prep, cần update cooldowns của units để regen mana
        return combatSystem.updateUnitCooldowns(state, deltaTimeMs)
    }
    
    /**
     * Xử lý pha combat
     */
    fun CombatLoop(state: GameState, deltaTimeMs: Long): GameState {
        var newState = state
        
        // Debug: Log combat phase info

        // Spawn enemies nếu chưa đủ số lượng
        if (newState.remainingEnemiesToSpawn > 0) {
            newState = spawnSystem.spawnEnemies(newState, deltaTimeMs)
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

        // Mộc: hồi HP theo sao nếu có ít nhất một tướng Mộc trên board
        val flowerUnits = newState.player.board.values.filter { it?.type == com.baothanhbin.game2d.game.model.HeroType.FLOWER }
        println("🌸 FLOWER DEBUG: flowerUnits count = ${flowerUnits.size}, current lives = ${newState.player.lives}")
        
        // Debug: Show all units on board
        newState.player.board.values.forEach { unit ->
            if (unit != null) {
                println("🌸 BOARD DEBUG: Unit type = ${unit.type}, star = ${unit.star}")
            }
        }
        
        if (flowerUnits.isNotEmpty()) {
            // Lấy tướng Mộc có sao cao nhất để xác định hiệu ứng
            val highestStarFlower = flowerUnits.maxByOrNull { it?.star ?: Star.ONE }!!
            
            val healPerSecond = when (highestStarFlower.star) {
                Star.ONE -> 2      // ★☆☆ 2 HP mỗi giây (1% của 200 HP/giây)
                Star.TWO -> 4      // ★★☆ 4 HP mỗi giây (2% của 200 HP/giây)
                Star.THREE -> 10   // ★★★ 10 HP mỗi giây (5% của 200 HP/giây)
            }
            
            val healPerMs = healPerSecond / 1000.0f // HP per millisecond
            val healAccumulatorFloat = newState.mocRegenAccumMs + (healPerMs * deltaTimeMs).toFloat()
            
            println("🌸 HEAL DEBUG: healPerSecond = $healPerSecond, healPerMs = $healPerMs, deltaTimeMs = $deltaTimeMs, healAccumulatorFloat = $healAccumulatorFloat, currentAccum = ${newState.mocRegenAccumMs}")
            
            if (healAccumulatorFloat >= 1.0) { // 1 HP worth of healing
                val healAmount = healAccumulatorFloat.toInt()
                val remainingAccumulator = healAccumulatorFloat - healAmount
                
                val healedPlayer = newState.player.copy(lives = (newState.player.lives + healAmount).coerceAtMost(200))
                newState = newState.copy(
                    player = healedPlayer,
                    mocRegenAccumMs = remainingAccumulator
                )
                
                println("🌸 FLOWER HEAL: +${healAmount} HP (${highestStarFlower.star} star, $healPerSecond HP/s)")
            } else {
                newState = newState.copy(mocRegenAccumMs = healAccumulatorFloat)
            }
        } else if (newState.mocRegenAccumMs != 0f) {
            newState = newState.copy(mocRegenAccumMs = 0f)
        }

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
            .copy(gold = currentState.player.gold - 1) // Fixed cost
            .addToBench(unit) ?: return
        
        // Auto-merge after buying
        val mergedPlayer = mergeSystem.tryAutoMerge(newPlayer)

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
        
        // Có thể bán unit trong cả PREP và COMBAT phase
        if (!currentState.canManageUnits()) return
        
        val unit = currentState.player.bench.find { it.id == unitId } ?: return
        
        val newPlayer = currentState.player
            .removeFromBench(unitId)
            .copy(gold = currentState.player.gold + unit.sellPrice)
        
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
        initGame()
    }
    
    /**
     * Bắt đầu vòng chiến đấu
     */
    fun startCombat() {
        val currentState = _gameState.value
        android.util.Log.d("GameEngine", "🎯 START COMBAT: Current phase=${currentState.roundPhase}, isInPrep=${currentState.isInPrep}")
        
        if (currentState.isInPrep) {
            val newState = currentState.startCombat()
            android.util.Log.d("GameEngine", "🎯 COMBAT STARTED: New phase=${newState.roundPhase}, lastSpawnTimeMs=${newState.lastSpawnTimeMs}, remainingEnemies=${newState.remainingEnemiesToSpawn}")
            _gameState.value = newState
        } else {
            android.util.Log.d("GameEngine", "🎯 CANNOT START COMBAT: Not in prep phase")
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
    fun saveGameOverData(state: GameState) {
        gameDataStore?.let { dataStore ->
            coroutineScope?.launch {
                dataStore.saveBestScore(state.player.score, state.player.day)
                dataStore.saveGameStats(
                    day = state.player.day,
                    goldEarned = state.player.gold.toLong(),
                    enemiesKilled = 0L // TODO: track enemies killed
                )
            }
        }
    }
    
    /**
     * Clear processed sound events
     */
    fun clearSoundEvents() {
        val currentState = _gameState.value
        _gameState.value = currentState.clearSoundEvents()
    }
}
