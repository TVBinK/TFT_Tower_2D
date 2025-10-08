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
    val roundPhase: RoundPhase = RoundPhase.PREP,
    val isGameRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val gameMode: GameMode = GameMode.CAMPAIGN,
    val defeatedBosses: Set<EnemyType> = emptySet(), // Track defeated bosses
    val lastFrameTimeMs: Long = 0L,
    val lastSpawnTimeMs: Long = 0L,
    val dayNumber: Int = 1,
    val enemiesSpawned: Int = 0, // Số quái đã spawn trong ngày này
    val enemiesKilled: Int = 0,  // Số quái đã bị tiêu diệt trong ngày này
    val totalEnemiesPerDay: Int = 5, // Tổng số quái mỗi ngày
    // Tích lũy HP để hồi máu cho hệ Mộc trong combat
    val mocRegenAccumMs: Float = 0f,
    // Cảnh báo boss - lưu day đã cảnh báo để không lặp lại
    val lastBossWarningDay: Int = 0,
    // Sound event tracking
    val pendingSoundEvents: List<SoundEvent> = emptyList()
) {
    
    companion object {
        const val SCREEN_WIDTH = 1080f
        const val SCREEN_HEIGHT = 1920f
        // Ngưỡng chạm đáy (đầu BottomPanel) trong toạ độ game-space
        // Có thể tinh chỉnh để khớp viền nâu của BottomPanel
        const val DEATH_LINE_OFFSET = 150f
        
        /**
         * Tính số lượng quái mỗi ngày (5 + ngày - 1)
         * Ngày 1: 5 quái, Ngày 2: 6 quái, Ngày 3: 7 quái...
         */
        fun calculateEnemiesPerDay(day: Int): Int {
            return 5 + day - 1
        }

        /**
         * Tạo GameState mẫu dùng cho Preview UI
         */
        fun sample(): GameState {
            val base = GameState()
            // Roll shop ban đầu để có dữ liệu hiển thị
            val rolledShop = base.shop.initialRoll(base.player.level)
            return base.copy(shop = rolledShop)
        }
    }
    
    /**
     * Có đang trong pha prep không
     */
    val isInPrep: Boolean
        get() = roundPhase == RoundPhase.PREP
    
    /**
     * Số enemy còn sống
     */
    val aliveEnemyCount: Int
        get() = enemies.count { it.isAlive }
    
    /**
     * Số enemy còn lại cần spawn trong vòng này
     */
    val remainingEnemiesToSpawn: Int
        get() = totalEnemiesPerDay - enemiesSpawned
    
    /**
     * Ngày đã hoàn thành chưa (đã diệt hết quái)
     */
    val isDayComplete: Boolean
        get() = enemiesSpawned >= totalEnemiesPerDay && aliveEnemyCount == 0
    
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
     * Kiểm tra đã thắng chưa (đánh bại 3 boss trong Campaign)
     */
    val shouldWinGame: Boolean
        get() = gameMode == GameMode.CAMPAIGN && 
                defeatedBosses.containsAll(setOf(EnemyType.BOSS1, EnemyType.BOSS2, EnemyType.BOSS3))
    
    /**
     * Bắt đầu vòng chiến đấu (từ prep sang combat)
     */
    fun startCombat(): GameState {
        val now = System.currentTimeMillis()
        val drainedPlayer = player
        return copy(
            roundPhase = RoundPhase.COMBAT,
            player = drainedPlayer,
            enemiesSpawned = 0,
            enemiesKilled = 0,
            lastSpawnTimeMs = now // Khởi tạo thời gian spawn để bắt đầu spawn ngay
        )
    }
    
    /**
     * Hoàn thành ngày (từ combat về prep)
     */
    fun completeDay(): GameState {
        val newDay = player.day + 1
        println("DAY DEBUG: Completing day ${player.day} -> $newDay")
        
        val updatedPlayer = player.copy(
            gold = player.gold + player.totalIncome,
            freeRerollsRemaining = 1, // Reset free reroll
            day = newDay // Tăng day của player
        )
        
        return copy(
            roundPhase = RoundPhase.PREP,
            player = updatedPlayer,
            dayNumber = dayNumber + 1,
            enemies = emptyList(), // Clear remaining enemies
            bullets = emptyList(),  // Clear bullets
            effects = emptyList(),  // Clear effects
            enemiesSpawned = 0,
            enemiesKilled = 0,
            totalEnemiesPerDay = calculateEnemiesPerDay(newDay) // Cập nhật số lượng quái theo ngày mới
        )
    }
    
    /**
     * Spawn enemy mới
     */
    fun spawnEnemy(enemy: Enemy): GameState {
        val newState = copy(
            enemies = enemies + enemy,
            enemiesSpawned = enemiesSpawned + 1
        )
        
        return newState
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
     * Bắt đầu game
     */
    fun startGame(): GameState {
        val now = System.currentTimeMillis()
        val initialShop = shop.initialRoll(player.level)
        
        return copy(
            isGameRunning = true,
            isPaused = false,
            isGameOver = false,
            shop = initialShop,
            roundPhase = RoundPhase.PREP,
            lastFrameTimeMs = now,
            lastSpawnTimeMs = now, // Khởi tạo spawn time để combat phase hoạt động ngay
            enemiesSpawned = 0,
            enemiesKilled = 0,
            totalEnemiesPerDay = calculateEnemiesPerDay(player.day) // Khởi tạo số lượng quái cho ngày đầu
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

    fun winGame(): GameState {
        return copy(
            isGameRunning = false,
            isGameOver = false,
            isVictory = true,
            isPaused = false
        )
    }
    
    /**
     * Thêm boss vào danh sách đã đánh bại
     */
    fun addDefeatedBoss(bossType: EnemyType): GameState {
        return copy(defeatedBosses = defeatedBosses + bossType)
    }
    
    /**
     * Add sound event to be played
     */
    fun addSoundEvent(event: SoundEvent): GameState {
        return copy(pendingSoundEvents = pendingSoundEvents + event)
    }
    
    /**
     * Mark boss warning for a specific day and enqueue sound event
     */
    fun addBossWarningForDay(day: Int): GameState {
        if (lastBossWarningDay == day) return this
        return copy(
            lastBossWarningDay = day,
            pendingSoundEvents = pendingSoundEvents + SoundEvent.BEFORE_BOSS
        )
    }

    /**
     * Clear processed sound events
     */
    fun clearSoundEvents(): GameState {
        return copy(pendingSoundEvents = emptyList())
    }
}

/**
 * Chế độ chơi
 */
enum class GameMode {
    SURVIVAL,   // Vô hạn ngày, boss xuất hiện mỗi 5 ngày (5,10,15,20, ...)
    CAMPAIGN    // Kết thúc khi hoàn thành 15 ngày
}

/**
 * Sound events to be played
 */
enum class SoundEvent {
    ICE_SKILL,
    WATER_SKILL,
    FIRE_SKILL,
    BEFORE_BOSS
}
