package com.baothanhbin.game2d.game.model

/**
 * Trạng thái toàn bộ game
 */
data class GameState(
    val player: Player = Player.createWithStartingUnits(),
    val shop: Shop = Shop(),
    val enemies: List<Enemy> = emptyList(),
    val bullets: List<Bullet> = emptyList(),
    val effects: List<Effect> = emptyList(),
    val difficulty: Difficulty = Difficulty.NORMAL,
    val roundPhase: RoundPhase = RoundPhase.PREP,
    val isGameRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val lastFrameTimeMs: Long = 0L,
    val lastSpawnTimeMs: Long = 0L,
    val roundNumber: Int = 1,
    val enemiesSpawned: Int = 0, // Số quái đã spawn trong vòng này
    val enemiesKilled: Int = 0,  // Số quái đã bị tiêu diệt trong vòng này
    val totalEnemiesPerRound: Int = 5 // Tổng số quái mỗi vòng
) {
    
    companion object {
        const val SCREEN_WIDTH = 1080f
        const val SCREEN_HEIGHT = 1920f
    }
    
    /**
     * Có đang trong pha prep không
     */
    val isInPrep: Boolean
        get() = roundPhase == RoundPhase.PREP
    
    /**
     * Có đang trong pha combat không
     */
    val isInCombat: Boolean
        get() = roundPhase == RoundPhase.COMBAT
    
    /**
     * Số enemy còn sống
     */
    val aliveEnemyCount: Int
        get() = enemies.count { it.isAlive }
    
    /**
     * Số enemy còn lại cần spawn trong vòng này
     */
    val remainingEnemiesToSpawn: Int
        get() = totalEnemiesPerRound - enemiesSpawned
    
    /**
     * Vòng đã hoàn thành chưa (đã diệt hết quái)
     */
    val isRoundComplete: Boolean
        get() = enemiesSpawned >= totalEnemiesPerRound && aliveEnemyCount == 0
    
    /**
     * Có thể mua unit không (luôn có thể mua)
     */
    fun canBuyUnit(slotIndex: Int): Boolean {
        return shop.canBuy(slotIndex, player.gold) && player.hasBenchSpace
    }
    
    /**
     * Có thể reroll không (luôn có thể reroll)
     */
    fun canReroll(): Boolean {
        return player.canReroll
    }
    
    /**
     * Có thể mua XP không (luôn có thể mua XP)
     */
    fun canBuyXP(): Boolean {
        return player.canBuyXP
    }
    
    /**
     * Có thể drag/drop unit không (luôn có thể quản lý)
     */
    fun canManageUnits(): Boolean {
        return !isPaused
    }
    
    /**
     * Game đã kết thúc chưa
     */
    val shouldEndGame: Boolean
        get() = player.lives <= 0
    
    /**
     * Bắt đầu vòng chiến đấu (từ prep sang combat)
     */
    fun startCombat(): GameState {
        val now = System.currentTimeMillis()
        val drainedPlayer = player.copy(currentMana = 0f, lastManaUpdateTime = now)
        return copy(
            roundPhase = RoundPhase.COMBAT,
            player = drainedPlayer,
            enemiesSpawned = 0,
            enemiesKilled = 0
        )
    }
    
    /**
     * Hoàn thành vòng (từ combat về prep)
     */
    fun completeRound(): GameState {
        val updatedPlayer = player.copy(
            gold = player.gold + player.totalIncome,
            freeRerollsRemaining = 1 // Reset free reroll
        )
        
        return copy(
            roundPhase = RoundPhase.PREP,
            player = updatedPlayer,
            roundNumber = roundNumber + 1,
            enemies = emptyList(), // Clear remaining enemies
            bullets = emptyList(),  // Clear bullets
            effects = emptyList(),  // Clear effects
            enemiesSpawned = 0,
            enemiesKilled = 0
        )
    }
    
    /**
     * Spawn enemy mới
     */
    fun spawnEnemy(enemy: Enemy): GameState {
        return copy(
            enemies = enemies + enemy,
            enemiesSpawned = enemiesSpawned + 1
        )
    }
    
    /**
     * Enemy bị tiêu diệt
     */
    fun killEnemy(enemyId: String): GameState {
        val updatedEnemies = enemies.map { enemy ->
            if (enemy.id == enemyId) enemy.takeDamage(enemy.currentHp) else enemy
        }
        
        return copy(
            enemies = updatedEnemies,
            enemiesKilled = enemiesKilled + 1
        )
    }
    
    /**
     * Tạo game state mới cho difficulty
     */
    fun withDifficulty(newDifficulty: Difficulty): GameState {
        return copy(
            difficulty = newDifficulty,
            shop = shop.initialRoll(player.level)
        )
    }
    
    /**
     * Bắt đầu game
     */
    fun startGame(): GameState {
        val initialShop = shop.initialRoll(player.level)
        
        return copy(
            isGameRunning = true,
            isPaused = false,
            isGameOver = false,
            shop = initialShop,
            roundPhase = RoundPhase.PREP,
            lastFrameTimeMs = System.currentTimeMillis(),
            enemiesSpawned = 0,
            enemiesKilled = 0
        )
    }
    
    /**
     * Pause/unpause game
     */
    fun togglePause(): GameState {
        return copy(isPaused = !isPaused)
    }
    
    /**
     * Game over
     */
    fun endGame(): GameState {
        return copy(
            isGameRunning = false,
            isGameOver = true,
            isPaused = false
        )
    }
}
