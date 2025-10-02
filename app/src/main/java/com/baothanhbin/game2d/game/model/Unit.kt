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
    val range: Float = 1600f,  // Tăng tầm bắn để bắn sớm hơn
    val manaCost: Float = 20f, // Cost mana để bắn
    val cooldownRemainingMs: Long = 0L,
    val lastShotAtMs: Long = 0L,
    val lastWaveAtMs: Long = 0L, // Thời gian tạo wave cuối cùng
    
    // Vị trí nếu đang trên board
    val boardPosition: BoardSlot? = null,
    val isOnBoard: Boolean = false
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
     * Giá bán lại (cố định cho tất cả hero)
     */
    val sellPrice: Int
        get() = 1
    
    companion object {
        private const val MIN_FIRE_RATE_MS: Long = 700L
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
            val (damage, fireRate, manaCost) = getBaseStatsForType(type)
            val currentTime = System.currentTimeMillis()
            
            return Unit(
                type = type,
                baseDamage = damage,
                baseFireRateMs = fireRate,
                manaCost = manaCost,
                // Khởi tạo cooldown ngẫu nhiên để tránh tất cả bắn cùng lúc khi vào game
                cooldownRemainingMs = (Math.random() * fireRate).toLong().coerceAtLeast(0L),
                lastShotAtMs = currentTime,
                lastWaveAtMs = currentTime // Khởi tạo lastWaveAtMs để tránh wave ngay lập tức
            )
        }
        
        /**
         * Lấy stats cơ bản theo type
         */
        private fun getBaseStatsForType(type: HeroType): Triple<Float, Long, Float> {
            return when (type) {
                HeroType.METAL -> Triple(25f, 2000L, 25f)  // Damage, FireRate, ManaCost
                HeroType.FLOWER -> Triple(20f, 2200L, 20f)  // Healer, chậm hơn
                HeroType.WATER -> Triple(30f, 1800L, 30f)   // Strong, nhanh hơn
                HeroType.FIRE -> Triple(35f, 1600L, 35f)    // Very strong
                HeroType.ICE -> Triple(22f, 2400L, 22f)     // Slower, freeze effect
            }
        }
        
        /**
         * Tạo random Unit
         */
        fun createRandom(): Unit {
            val randomType = heroTypes.random()
            return create(randomType)
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

}
