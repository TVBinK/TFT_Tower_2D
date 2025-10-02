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
    val sprite: EnemySprite = EnemySprite.BASIC_1,
    // Trạng thái đóng băng và làm chậm
    val isFrozen: Boolean = false,
    val freezeEndTime: Long = 0L,
    val isSlowed: Boolean = false,
    val slowEndTime: Long = 0L,
    val slowMultiplier: Float = 1f
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
        // Giới hạn sát thương mỗi hit để không chết ngay lập tức
        val maxDamagePerHit = (maxHp * 0.4f).coerceAtLeast(1f)
        val appliedDamage = kotlin.math.min(damage, maxDamagePerHit)
        val newHp = (currentHp - appliedDamage).coerceAtLeast(0f)
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
            wave: Int,
            difficulty: Difficulty
        ): Enemy {
            val baseHp = 5f + wave * 1f  // Giảm HP từ 10+2*wave xuống 5+1*wave
            val baseSpeed = 50f + wave * 1f
            val baseReward = 1 + wave / 5
            
            return Enemy(
                x = x,
                y = y,
                maxHp = baseHp * difficulty.hpMultiplier,
                speed = baseSpeed * difficulty.speedMultiplier,
                reward = (baseReward * difficulty.rewardMultiplier).toInt()
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
    ROBE("Robe", 0xFF00BCD4.toInt())
}

/**
 * Sprite lựa chọn cho enemy khi hiển thị
 */
enum class EnemySprite {
    BASIC_1,
    BASIC_2,
}
