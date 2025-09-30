package com.baothanhbin.game2d.game.model

import java.util.UUID

/**
 * Unit (Hero) trong game - đại diện cho một tướng
 */
data class Unit(
    val id: String = generateId(),
    val type: HeroType,
    val tier: Tier,
    val star: Star = Star.ONE,
    val baseDamage: Float,
    val baseFireRateMs: Long,
    val bulletSpeed: Float = 300f,
    val range: Float = 1600f,  // Tăng tầm bắn để bắn sớm hơn
    val manaCost: Float = 20f, // Cost mana để bắn
    val cooldownRemainingMs: Long = 0L,
    val lastShotAtMs: Long = 0L,
    
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
     * Giá bán lại (100% giá gốc)
     */
    val sellPrice: Int
        get() = tier.cost
    
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
         * Tạo Unit mới theo tier và type
         */
        fun create(type: HeroType, tier: Tier): Unit {
            val (damage, fireRate, manaCost) = getBaseStatsForTier(tier)
            
            return Unit(
                type = type,
                tier = tier,
                baseDamage = damage,
                baseFireRateMs = fireRate,
                manaCost = manaCost,
                // Khởi tạo cooldown ngẫu nhiên để tránh tất cả bắn cùng lúc khi vào game
                cooldownRemainingMs = (Math.random() * fireRate).toLong().coerceAtLeast(0L),
                lastShotAtMs = System.currentTimeMillis()
            )
        }
        
        /**
         * Lấy stats cơ bản theo tier
         */
        private fun getBaseStatsForTier(tier: Tier): Triple<Float, Long, Float> {
            return when (tier) {
                // T1 bắn chậm hơn nữa và tốn mana hơn để tránh spam đạn
                Tier.T1 -> Triple(20f, 2600L, 30f)  // Damage, FireRate, ManaCost
                Tier.T2 -> Triple(25f, 1400L, 22f)  // T2 chậm hơn một chút
                Tier.T3 -> Triple(30f, 880L, 25f)   // T3 mạnh hơn nữa
                Tier.T4 -> Triple(40f, 800L, 30f)   // T4 rất mạnh
                Tier.T5 -> Triple(50f, 720L, 35f)   // T5 mạnh nhất
            }
        }
        
        /**
         * Tạo random Unit theo tier
         */
        fun createRandom(tier: Tier): Unit {
            val randomType = heroTypes.random()
            return create(randomType, tier)
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
     * Kiểm tra có thể merge với unit khác không
     */
    fun canMergeWith(other: Unit): Boolean {
        return this.type == other.type && 
               this.tier == other.tier && 
               this.star == other.star &&
               this.star != Star.THREE // Không thể merge 3★
    }
}
