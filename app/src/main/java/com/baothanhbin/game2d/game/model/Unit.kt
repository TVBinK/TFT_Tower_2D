package com.baothanhbin.game2d.game.model

import java.util.UUID

/**
 * Unit (Hero) trong game - đại diện cho một tướng
 */
data class Unit(
    val id: String = generateId(),
    val type: HeroType,
    val star: Star = Star.ONE,
    val baseDamage: Float,
    val baseFireRateMs: Long,
    val bulletSpeed: Float = 300f,
    val range: Float = 2000f,  // Tăng tầm bắn để bắn sớm hơn
    val cooldownRemainingMs: Long = 0L,
    val lastShotAtMs: Long = 0L,
    val lastWaveAtMs: Long = 0L, // Thời gian tạo wave cuối cùng
    val lastFireSkillAtMs: Long = 0L, // Thời gian sử dụng Fire skill cuối cùng
    val fireEffectEndAtMs: Long = 0L, // Thời điểm hàng lửa kết thúc lần gần nhất
    
    // Mana system cho skills
    val currentMana: Float = 0f,
    val maxMana: Float = 100f,
    val manaRegenPerSecond: Float = 10f,
    
    // Vị trí nếu đang trên board
    val boardPosition: BoardSlot? = null,
    val isOnBoard: Boolean = false,
    val isFrozen: Boolean = false,
    val freezeEndTime: Long = 0L
) {
    
    /**
     * Damage thực tế sau khi áp dụng sao
     */
    val actualDamage: Float
        get() = when (star) {
            Star.ONE -> baseDamage
            Star.TWO -> baseDamage * 1.75f
            Star.THREE -> baseDamage * 2.8f
        }
    
    /**
     * Fire rate thực tế sau khi áp dụng sao (ms)
     */
    val actualFireRateMs: Long
        get() = when (star) {
            Star.ONE -> (baseFireRateMs).coerceAtLeast(MIN_FIRE_RATE_MS)
            Star.TWO -> (baseFireRateMs * 0.9f).toLong().coerceAtLeast(MIN_FIRE_RATE_MS)
            Star.THREE -> (baseFireRateMs * 0.8f).toLong().coerceAtLeast(MIN_FIRE_RATE_MS)
        }

    /**
     * Có thể bắn?
     */
    val canAct: Boolean
        get() = !isFrozen
    
    /**
     * Có thể sử dụng Fire skill? (cooldown 5 giây)
     */
    val canUseFireSkill: Boolean
        get() = !isFrozen && System.currentTimeMillis() >= (fireEffectEndAtMs + FIRE_SKILL_COOLDOWN_MS)
    
    /**
     * Giá bán lại (cố định cho tất cả hero)
     */
    val sellPrice: Int
        get() = 1
    
    companion object {
        private const val MIN_FIRE_RATE_MS: Long = 700L
        private const val FIRE_SKILL_COOLDOWN_MS: Long = 5000L // 5 giây cooldown
        private var idCounter = 0L
        
        // Cache HeroType.values() để tránh gọi lại nhiều lần
        private val heroTypes = HeroType.values()
        
        /**
         * Tạo ID nhanh hơn UUID
         */
        private fun generateId(): String {
            return "unit_${++idCounter}_${System.currentTimeMillis()}"
        }
        /**
         * Tạo Unit mới theo type
         */
        fun create(type: HeroType): Unit {
            val (damage, fireRate) = getBaseStatsForType(type)
            val currentTime = System.currentTimeMillis()
            
            // Khởi tạo mana cho Fire hero
            val initialMana = when (type) {
                HeroType.FIRE -> 0f // Fire hero bắt đầu với 0 mana
                else -> 0f // Các hero khác cũng bắt đầu với 0 mana
            }
            
            return Unit(
                type = type,
                baseDamage = damage,
                baseFireRateMs = fireRate,
                // Khởi tạo cooldown ngẫu nhiên để tránh tất cả bắn cùng lúc khi vào game
                cooldownRemainingMs = (Math.random() * fireRate).toLong().coerceAtLeast(0L),
                lastShotAtMs = currentTime,
                lastWaveAtMs = currentTime, // Khởi tạo lastWaveAtMs để tránh wave ngay lập tức
                currentMana = initialMana
            )
        }
        
        /**
         * Dame cơ bản
         */
        private fun getBaseStatsForType(type: HeroType): Pair<Float, Long> {
            return when (type) {
                HeroType.METAL -> Pair(15f, 2000L)  // Damage, FireRate
                HeroType.FLOWER -> Pair(0f, 2200L)  // Healer, chậm hơn
                HeroType.WATER -> Pair(0f, 1800L)   // Strong, nhanh hơn
                HeroType.FIRE -> Pair(0f, 1600L)    // Very strong
                HeroType.ICE -> Pair(10f, 2400L)     // Slower, freeze effect
            }
        }
    }
    
    /**
     * Tạo bản copy với sao mới (cho merge)
     */
    fun withStar(newStar: Star): Unit {
        return copy(star = newStar)
    }
    
    /**
     * Tạo bản copy với vị trí board mới
     */
    fun withBoardPosition(slot: BoardSlot?): Unit {
        return copy(
            boardPosition = slot,
            isOnBoard = slot != null
        )
    }

    /**
     * Đóng băng unit trong durationMs
     */
    fun freeze(durationMs: Long): Unit {
        val end = System.currentTimeMillis() + durationMs
        return copy(isFrozen = true, freezeEndTime = end)
    }

    /**
     * Cập nhật trạng thái (hết đóng băng?)
     * Note: Mana regen được xử lý trong CombatSystem.updateUnitCooldown()
     */
    fun updateStatus(): Unit {
        // Cập nhật freeze status
        if (isFrozen && System.currentTimeMillis() >= freezeEndTime) {
            return copy(isFrozen = false, freezeEndTime = 0L)
        }
        
        return this
    }
    

}
