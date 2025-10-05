package com.baothanhbin.game2d.game.model

import java.util.UUID

/**
 * Enemy - kẻ địch rơi xuống từ trên
 */
data class Enemy(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val maxHp: Float,
    val currentHp: Float = maxHp,
    val speed: Float,
    val size: Float = 80f,
    val reward: Int = 1,
    val enemyType: EnemyType = EnemyType.BASIC,
    val archetype: EnemyArchetype = EnemyArchetype.BASIC,
    // Trạng thái đóng băng và làm chậm
    val isFrozen: Boolean = false,
    val freezeEndTime: Long = 0L,
    val isSlowed: Boolean = false,
    val slowEndTime: Long = 0L,
    val slowMultiplier: Float = 1f,
    // Boss fields
    val isBoss: Boolean = false,
    val bossAbility: BossAbility? = null,
    val abilityCooldownMs: Long = 4000L,
    val lastAbilityAtMs: Long = 0L
) {
    
    /**
     * Có còn sống không
     */
    val isAlive: Boolean
        get() = currentHp > 0
    
    /**
     * Đã chết chưa
     */
    val isDead: Boolean
        get() = !isAlive
    
    /**
     * HP percentage
     */
    val hpPercentage: Float
        get() = if (maxHp > 0) currentHp / maxHp else 0f
    
    /**
     * Nhận damage
     */
    fun takeDamage(damage: Float): Enemy {
        // Không giới hạn damage - heroes bắn full damage
        val newHp = (currentHp - damage).coerceAtLeast(0f)
        return copy(currentHp = newHp)
    }
    
    /**
     * Áp dụng đóng băng
     */
    fun freeze(durationMs: Long): Enemy {
        val freezeEndTime = System.currentTimeMillis() + durationMs
        return copy(
            isFrozen = true,
            freezeEndTime = freezeEndTime
        )
    }
    
    /**
     * Áp dụng làm chậm
     */
    fun slow(durationMs: Long, multiplier: Float = 0.5f): Enemy {
        val slowEndTime = System.currentTimeMillis() + durationMs
        return copy(
            isSlowed = true,
            slowEndTime = slowEndTime,
            slowMultiplier = multiplier
        )
    }
    
    /**
     * Cập nhật trạng thái đóng băng và làm chậm
     */
    fun updateStatus(): Enemy {
        val currentTime = System.currentTimeMillis()
        var updated = this
        
        // Kiểm tra đóng băng
        if (updated.isFrozen && currentTime >= updated.freezeEndTime) {
            updated = updated.copy(isFrozen = false, freezeEndTime = 0L)
        }
        
        // Kiểm tra làm chậm
        if (updated.isSlowed && currentTime >= updated.slowEndTime) {
            updated = updated.copy(isSlowed = false, slowEndTime = 0L, slowMultiplier = 1f)
        }
        
        return updated
    }
    
    /**
     * Tốc độ thực tế sau khi áp dụng làm chậm
     */
    val actualSpeed: Float
        get() = if (isFrozen) 0f else if (isSlowed) speed * slowMultiplier else speed
    
    /**
     * Di chuyển xuống
     */
    fun moveDown(deltaTime: Float): Enemy {
        return copy(y = y + actualSpeed * deltaTime)
    }
    
    /**
     * Kiểm tra va chạm với bullet
     */
    fun collidesWith(bullet: Bullet): Boolean {
        val dx = x - bullet.x
        val dy = y - bullet.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        return distance <= (size + bullet.size) / 2
    }
    
    companion object {
        /**
         * Tạo enemy mới theo difficulty và wave
         */
        fun create(
            x: Float,
            y: Float,
            day: Int,
            archetype: EnemyArchetype = EnemyArchetype.BASIC,
            isBoss: Boolean = false,
            bossAbility: BossAbility? = null
        ): Enemy {
            val baseHp = 30f + day * 8f  // Base HP tăng dần theo ngày
            val baseSpeed = 50f + day * 1f // Base speed tăng nhẹ theo ngày
            val baseReward = 1 + day / 5 // Base reward tăng theo ngày
            val hpMul: Float
            val speedMul: Float
            val sizeMul: Float
            val rewardMul: Float
            when (archetype) {
                EnemyArchetype.BASIC -> {
                    hpMul = 1.0f; speedMul = 1.0f; sizeMul = 1.0f; rewardMul = 1.0f
                }
                EnemyArchetype.TANK -> {
                    hpMul = 3.0f; speedMul = 0.6f; sizeMul = 1.2f; rewardMul = 2.0f
                }
                EnemyArchetype.FAST -> {
                    hpMul = 0.6f; speedMul = 1.8f; sizeMul = 0.9f; rewardMul = 1.0f
                }
            }
            
            val rewardFloat = baseReward.toFloat() * rewardMul
            return Enemy(
                x = x,
                y = y,
                maxHp = baseHp * hpMul,
                speed = baseSpeed * speedMul,
                size = 80f * sizeMul,
                reward = rewardFloat.toInt(),
                archetype = archetype,
                isBoss = isBoss,
                bossAbility = bossAbility,
                lastAbilityAtMs = if (isBoss) System.currentTimeMillis() else 0L
            )
        }
    }
}

/**
 * Loại enemy
 */
enum class EnemyType(val displayName: String, val color: Int) {
    BASIC("Cơ bản", 0xFFFF0000.toInt()),
    FAST("Nhanh", 0xFFFFFF00.toInt()),
    TANK("Bọc thép", 0xFF800080.toInt()),
    MURID("Murid", 0xFF00FFFF.toInt()),
    BOSS1("Boss 1", 0xFFFF6B35.toInt()),
    BOSS2("Boss 2", 0xFF00FF00.toInt()),
    BOSS3("Boss 3", 0xFF0000FF.toInt())
}

/**
 * Phân loại gameplay của quái (để map theo yêu cầu: basic/tank/fast)
 */
enum class EnemyArchetype {
    BASIC,        // monter_basic
    TANK,         // monter_big_hp_slow_walk
    FAST          // monter_paper_hp_fast_walk
}

// Đã loại bỏ EnemySprite; hiển thị dựa trên archetype

/**
 * Loại kỹ năng của Boss
 */
enum class BossAbility {
    SUMMON_MINIONS,   // Gọi thêm đệ tử
    SUMMON_MURID,     // Boss2: Gọi murid với tốc độ nhanh
    SHOOT_BULLETS,    // Bắn đạn (tạm thời chưa gây sát thương lên tướng)
    FREEZE_RANDOM_UNIT, // Đóng băng 1 tướng bất kỳ bên mình
    FREEZE_HEROES     // Boss3: Đóng băng tướng để không cho tung skill
}
