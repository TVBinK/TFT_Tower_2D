package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*
import kotlin.random.Random

/**
 * Hệ thống spawn enemies
 */
class SpawnSystem {
    
    companion object {
        private const val BASE_SPAWN_DELAY_MS = 1200L // 1.2 giây để tránh spawn liên tiếp
        private const val MIN_SPAWN_DELAY_MS = 800L   // Tối thiểu 0.8 giây
        private const val WAVE_SPAWN_DELAY_REDUCTION = 100L // Giảm 100ms mỗi wave
    }
    
    /**
     * Spawn enemies trong combat phase
     * Cập nhật: Spawn random trên toàn bộ màn hình
     */
    fun spawnEnemies(state: GameState, deltaTimeMs: Long): GameState {
        println("SPAWN DEBUG: spawnEnemies called - day: ${state.player.day}, phase: ${state.roundPhase}, remaining: ${state.remainingEnemiesToSpawn}")
        
        val currentTime = System.currentTimeMillis()
        
        if (state.roundPhase != RoundPhase.COMBAT) {
            println("SPAWN DEBUG: Not in combat phase")
            return state
        }
        // Nếu là boss day và đã spawn boss, không spawn thêm gì
        if (shouldSpawnBoss(state.gameMode, state.player.day)) {
            val hasBossInDay = state.enemies.any { it.isBoss && it.isAlive }
            if (hasBossInDay) {
                println("SPAWN DEBUG: Boss day - boss already spawned, no more spawning")
                return state.copy(
                    enemiesSpawned = state.totalEnemiesPerDay, // Đánh dấu đã spawn đủ
                    lastSpawnTimeMs = currentTime
                )
            }
        }
        
        if (state.remainingEnemiesToSpawn <= 0) {
            println("SPAWN DEBUG: No more enemies to spawn (remaining: ${state.remainingEnemiesToSpawn})")
            return state
        }
        val timeSinceLastSpawn = currentTime - state.lastSpawnTimeMs
        
        val spawnDelay = calculateSpawnDelay(state.player.day)
        
        println("SPAWN DEBUG: timeSinceLastSpawn=$timeSinceLastSpawn, spawnDelay=$spawnDelay")
        
        if (timeSinceLastSpawn >= spawnDelay) {
            // Debug: Log current day
            println("SPAWN DEBUG: Current day = ${state.player.day}, shouldSpawnBoss = ${shouldSpawnBoss(state.gameMode, state.player.day)}")
            
            // Spawn boss nếu ở vòng 5, 10, 15, nếu không spawn enemy thường
            if (shouldSpawnBoss(state.gameMode, state.player.day)) {
                // Phát cảnh báo boss một tick trước khi spawn nếu chưa cảnh báo
                if (state.lastBossWarningDay != state.player.day) {
                    return state.addBossWarningForDay(state.player.day).copy(
                        lastSpawnTimeMs = currentTime // Delay việc spawn boss đến tick sau
                    )
                }
                println("SPAWN DEBUG: 🐉 CREATING BOSS! Day: ${state.player.day}")
                val boss = createBossEnemy(state.player.day, state.player)
                // Boss thay thế HOÀN TOÀN tất cả enemies trong ngày - chỉ spawn 1 boss duy nhất
                return state.copy(
                    enemies = state.enemies + boss,
                    enemiesSpawned = state.totalEnemiesPerDay, // Đánh dấu đã spawn đủ
                    lastSpawnTimeMs = currentTime
                )
            } else {
                println("SPAWN DEBUG: ✅ CREATING NEW ENEMY! Day: ${state.player.day}, Remaining to spawn: ${state.remainingEnemiesToSpawn - 1}")
                val newEnemy = createEnemy(state.gameMode, state.player.day, state.player, state.enemies)
                return state.spawnEnemy(newEnemy).copy(
                    lastSpawnTimeMs = currentTime
                )
            }
        } else {
            println("SPAWN DEBUG: ⏳ Waiting for spawn delay... (${spawnDelay - timeSinceLastSpawn}ms remaining)")
        }
        
        return state
    }
    
    /**
     * Tính toán spawn delay theo day
     */
    private fun calculateSpawnDelay(day: Int): Long {
        val baseDelay = BASE_SPAWN_DELAY_MS - (day * WAVE_SPAWN_DELAY_REDUCTION)
        
        return baseDelay.coerceAtLeast(MIN_SPAWN_DELAY_MS)
    }
    
    /**
     * Tạo enemy mới
     * Cập nhật: Spawn random trên toàn bộ màn hình với tránh đè lên nhau
     */
    private fun createEnemy(mode: GameMode, day: Int, player: Player, existingEnemies: List<Enemy> = emptyList()): Enemy {
        // Tìm vị trí spawn không bị đè lên enemies hiện có
        var spawnX: Float
        var spawnY: Float
        var attempts = 0
        val maxAttempts = 10
        
        do {
            val columnIndex = Random.nextInt(0, 5)
            spawnX = getColumnCenterX(columnIndex)
            spawnY = -50f - Random.nextInt(0, 50) // Spawn ở trên màn hình với random offset lớn hơn
            
            // Kiểm tra collision với enemies hiện có
            val hasCollision = existingEnemies.any { enemy ->
                val distance = kotlin.math.sqrt(
                    (enemy.x - spawnX) * (enemy.x - spawnX) + 
                    (enemy.y - spawnY) * (enemy.y - spawnY)
                )
                distance < 100f // Khoảng cách tối thiểu 100 pixels
            }
            
            attempts++
            if (!hasCollision || attempts >= maxAttempts) break
            
        } while (attempts < maxAttempts)
        
        println("SPAWN DEBUG: Spawning enemy at position ($spawnX, $spawnY) after $attempts attempts")
        
        // Chọn archetype theo tỉ lệ
        val archetypeWeights = listOf(
            EnemyArchetype.BASIC to 0.6f,
            EnemyArchetype.TANK to 0.2f,
            EnemyArchetype.FAST to 0.2f
        )
        val pick = Random.nextFloat()
        val archetype = when {
            pick < archetypeWeights[0].second -> archetypeWeights[0].first
            pick < archetypeWeights[0].second + archetypeWeights[1].second -> archetypeWeights[1].first
            else -> archetypeWeights[2].first
        }
        // Chọn sprite theo archetype để phân biệt rõ trong PlayArea
        val effectiveDay = if (mode == GameMode.SURVIVAL) (day * 2) else day
        val baseEnemy = Enemy.create(spawnX, spawnY, effectiveDay, archetype = archetype)
        val type = when (archetype) {
            EnemyArchetype.BASIC -> EnemyType.BASIC
            EnemyArchetype.TANK -> EnemyType.TANK
            EnemyArchetype.FAST -> EnemyType.FAST
        }
        return baseEnemy.copy(enemyType = type)
    }
    
    /**
     * Tạo boss enemy
     * Cập nhật: Boss spawn random trên toàn bộ màn hình
     */
    private fun createBossEnemy(day: Int, player: Player, mode: GameMode = GameMode.CAMPAIGN): Enemy {
        // Boss luôn spawn ở ô chính giữa (column 2)
        val columnIndex = 2
        
        val spawnX = getColumnCenterX(columnIndex)
        val spawnY = -100f - Random.nextInt(0, 20) // Boss spawn cao hơn với random offset
        
        val scalingMultiplier = if (mode == GameMode.SURVIVAL) 1.5f else 1.0f
        val baseHp = (200f + day * 50f) * scalingMultiplier  // Boss trâu hơn theo chế độ
        val baseSpeed = 20f + day * 0.3f
        val baseReward = 10 + day * 2
        
        val bossType = getBossTypeForDay(day)
        val ability = when (bossType) {
            EnemyType.BOSS1 -> listOf(BossAbility.SHOOT_BULLETS, BossAbility.FREEZE_RANDOM_UNIT).random()
            EnemyType.BOSS2 -> BossAbility.SUMMON_MURID
            EnemyType.BOSS3 -> BossAbility.FREEZE_HEROES
            else -> BossAbility.SHOOT_BULLETS
        }
        val boss = Enemy(
            x = spawnX,
            y = spawnY,
            maxHp = baseHp,
            speed = baseSpeed,
            size = 180f, // Boss siêu to
            reward = baseReward,
            enemyType = bossType,
            archetype = EnemyArchetype.BASIC,
            isBoss = true,
            bossAbility = ability,
            abilityCooldownMs = when (bossType) {
                EnemyType.BOSS3 -> 8000L // Boss3 có cooldown dài hơn (8 giây)
                else -> 5000L // Boss khác vẫn 5 giây
            }
        )
        
        return boss
    }

    /**
     * Tính toạ độ X giữa cột theo bề rộng màn hình (5 cột)
     */
    private fun getColumnCenterX(columnIndex: Int): Float {
        val slotWidth = GameState.SCREEN_WIDTH / 5f
        return columnIndex * slotWidth + slotWidth / 2f
    }
    
    /**
     * Kiểm tra có nên spawn boss không (chỉ ở vòng 5, 10, 15)
     */
    fun shouldSpawnBoss(mode: GameMode, day: Int): Boolean {
        return when (mode) {
            GameMode.SURVIVAL -> day % 5 == 0 // Boss mỗi 5 ngày: 5,10,15,20,...
           /* GameMode.CAMPAIGN -> (day == 5 || day == 10 || day == 15)*/
            GameMode.CAMPAIGN -> (day == 2 || day == 3 || day == 4)
        }
    }
    
    /**
     * Lấy loại boss theo ngày
     */
    private fun getBossTypeForDay(day: Int): EnemyType {
        return when (day) {
            2 -> EnemyType.BOSS1
            3 -> EnemyType.BOSS2
            4 -> EnemyType.BOSS3
            else -> EnemyType.BOSS1 // Default (không nên xảy ra với logic mới)
        }
    }

}
