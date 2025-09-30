package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*
import kotlin.random.Random

/**
 * Hệ thống spawn enemies
 */
class SpawnSystem {
    
    companion object {
        private const val BASE_SPAWN_DELAY_MS = 4000L // 4 giây (giảm từ 2 giây)
        private const val MIN_SPAWN_DELAY_MS = 2000L   // Tối thiểu 2 giây (tăng từ 0.5 giây)
        private const val WAVE_SPAWN_DELAY_REDUCTION = 100L // Giảm 100ms mỗi wave (tăng từ 50ms)
    }
    
    /**
     * Spawn enemies trong combat phase
     * Cập nhật: Chỉ spawn enemies ở những cột có tướng
     */
    fun spawnEnemies(state: GameState, deltaTimeMs: Long): GameState {
        if (state.roundPhase != RoundPhase.COMBAT) return state
        if (state.remainingEnemiesToSpawn <= 0) return state
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSpawn = currentTime - state.lastSpawnTimeMs
        
        val spawnDelay = calculateSpawnDelay(state.player.wave, state.difficulty)
        
        if (timeSinceLastSpawn >= spawnDelay) {
            val newEnemy = createEnemy(state.player.wave, state.difficulty, state.player)
            
            return state.spawnEnemy(newEnemy).copy(
                lastSpawnTimeMs = currentTime
            )
        }
        
        return state
    }
    
    /**
     * Tính toán spawn delay theo wave và difficulty
     */
    private fun calculateSpawnDelay(wave: Int, difficulty: Difficulty): Long {
        val baseDelay = BASE_SPAWN_DELAY_MS - (wave * WAVE_SPAWN_DELAY_REDUCTION)
        val adjustedDelay = (baseDelay * difficulty.spawnDelayMultiplier).toLong()
        
        return adjustedDelay.coerceAtLeast(MIN_SPAWN_DELAY_MS)
    }
    
    /**
     * Tạo enemy mới
     * Cập nhật: Chỉ spawn ở những cột có tướng
     */
    private fun createEnemy(wave: Int, difficulty: Difficulty, player: Player): Enemy {
        // Lấy danh sách các cột có tướng
        val occupiedColumns = player.board
            .filter { (_, unit) -> unit != null }
            .map { (slot, _) -> slot.position }
            .toList()
        
        // Debug: Log thông tin spawn
        println("SPAWN DEBUG: Occupied columns: $occupiedColumns")
        
        // Nếu không có tướng nào, spawn ở cột 0 (fallback)
        val columnIndex = if (occupiedColumns.isNotEmpty()) {
            occupiedColumns.random()
        } else {
            0
        }
        
        val spawnX = getColumnCenterX(columnIndex)
        val spawnY = -50f // Spawn ở trên màn hình
        
        println("SPAWN DEBUG: Spawning enemy at column $columnIndex, position ($spawnX, $spawnY)")
        
        // Random sprite giữa BASIC_1 và BASIC_2 (GIF), vẫn dùng enemyType ROBE để được render GIF
        val baseEnemy = Enemy.create(spawnX, spawnY, wave, difficulty)
        val sprite = if (Random.nextBoolean()) EnemySprite.BASIC_1 else EnemySprite.BASIC_2
        return baseEnemy.copy(enemyType = EnemyType.ROBE, sprite = sprite)
    }
    
    /**
     * Spawn mini-wave (nhiều enemies cùng lúc)
     * Cập nhật: Chỉ spawn enemies ở những cột có tướng
     */
    fun spawnMiniWave(state: GameState, enemyCount: Int): GameState {
        if (state.roundPhase != RoundPhase.COMBAT) return state
        
        // Lấy danh sách các cột có tướng
        val occupiedColumns = state.player.board
            .filter { (_, unit) -> unit != null }
            .map { (slot, _) -> slot.position }
            .toList()
        
        // Nếu không có tướng nào, spawn ở cột 0 (fallback)
        val availableColumns = if (occupiedColumns.isNotEmpty()) {
            occupiedColumns
        } else {
            listOf(0)
        }
        
        val newEnemies = (0 until enemyCount).map { idx ->
            val columnIndex = availableColumns[idx % availableColumns.size]
            val spawnX = getColumnCenterX(columnIndex)
            val spawnY = -50f - ((idx + 1) * 30f)
            // ROBE + random sprite
            Enemy.create(spawnX, spawnY, state.player.wave, state.difficulty).copy(
                enemyType = EnemyType.ROBE,
                sprite = if (Random.nextBoolean()) EnemySprite.BASIC_1 else EnemySprite.BASIC_2
            )
        }
        
        return state.copy(
            enemies = state.enemies + newEnemies,
            lastSpawnTimeMs = System.currentTimeMillis()
        )
    }
    
    /**
     * Spawn boss enemy
     * Cập nhật: Boss chỉ xuất hiện ở những cột có tướng
     */
    fun spawnBoss(state: GameState): GameState {
        if (state.roundPhase != RoundPhase.COMBAT) return state
        
        val boss = createBossEnemy(state.player.wave, state.difficulty, state.player)
        
        return state.copy(
            enemies = state.enemies + boss,
            lastSpawnTimeMs = System.currentTimeMillis()
        )
    }
    
    /**
     * Tạo boss enemy
     * Cập nhật: Boss chỉ xuất hiện ở những cột có tướng
     */
    private fun createBossEnemy(wave: Int, difficulty: Difficulty, player: Player): Enemy {
        // Lấy danh sách các cột có tướng
        val occupiedColumns = player.board
            .filter { (_, unit) -> unit != null }
            .map { (slot, _) -> slot.position }
            .toList()
        
        // Boss xuất hiện ở cột có tướng, ưu tiên cột giữa nếu có
        val columnIndex = if (occupiedColumns.isNotEmpty()) {
            if (occupiedColumns.contains(2)) 2 else occupiedColumns.random()
        } else {
            2 // Fallback về cột giữa
        }
        
        val spawnX = getColumnCenterX(columnIndex)
        val spawnY = -100f
        
        val baseHp = 50f + wave * 10f
        val baseSpeed = 30f + wave * 0.5f
        val baseReward = 5 + wave / 3
        
        return Enemy(
            x = spawnX,
            y = spawnY,
            maxHp = baseHp * difficulty.hpMultiplier,
            speed = baseSpeed * difficulty.speedMultiplier,
            size = 80f, // Boss lớn hơn
            reward = (baseReward * difficulty.rewardMultiplier).toInt(),
            enemyType = EnemyType.ROBE,
            sprite = if (Random.nextBoolean()) EnemySprite.BASIC_1 else EnemySprite.BASIC_2
        )
    }

    /**
     * Tính toạ độ X giữa cột theo bề rộng màn hình (5 cột)
     */
    private fun getColumnCenterX(columnIndex: Int): Float {
        val slotWidth = GameState.SCREEN_WIDTH / 5f
        return columnIndex * slotWidth + slotWidth / 2f
    }
    
    /**
     * Kiểm tra có nên spawn boss không (mỗi 5 wave)
     */
    fun shouldSpawnBoss(wave: Int): Boolean {
        return wave % 5 == 0
    }
    
    /**
     * Kiểm tra có nên spawn mini-wave không
     */
    fun shouldSpawnMiniWave(wave: Int, enemiesSpawned: Int, totalEnemies: Int): Boolean {
        // Spawn mini-wave khi đã spawn được 1/2 số enemy
        val halfEnemies = totalEnemies / 2
        return enemiesSpawned >= halfEnemies && enemiesSpawned < totalEnemies
    }
    
    /**
     * Tính số enemy trong mini-wave
     */
    fun getMiniWaveSize(wave: Int): Int {
        return 2 + wave / 3
    }
}
