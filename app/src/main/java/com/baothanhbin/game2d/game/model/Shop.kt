package com.baothanhbin.game2d.game.model

/**
 * Shop slot - một ô trong cửa hàng
 */
data class ShopSlot(
    val unit: Unit?,
    val price: Int = 0,
    val isEmpty: Boolean = unit == null
) {
    companion object {
        fun empty() = ShopSlot(null, 0, true)
        
        fun withUnit(unit: Unit) = ShopSlot(
            unit = unit,
            price = unit.tier.cost,
            isEmpty = false
        )
    }
}

/**
 * Shop state - cửa hàng 5 ô
 */
data class Shop(
    val slots: List<ShopSlot> = List(5) { ShopSlot.empty() },
    val rollCount: Int = 0
) {
    
    companion object {
        const val SHOP_SIZE = 5
        
        // Odds table theo level - % cho mỗi tier
        val ODDS_TABLE = mapOf(
            2 to listOf(100, 0, 0, 0, 0),
            3 to listOf(75, 25, 0, 0, 0),
            4 to listOf(55, 30, 15, 0, 0),
            5 to listOf(45, 33, 20, 2, 0),
            6 to listOf(25, 40, 30, 5, 0),
            7 to listOf(19, 30, 35, 15, 1),
            8 to listOf(15, 25, 35, 20, 5),
            9 to listOf(10, 20, 35, 25, 10)
        )
        
        // Cache Tier.values() để tránh gọi lại nhiều lần
        private val tierValues = Tier.values()
    }
    
    /**
     * Có slot trống không
     */
    val hasEmptySlot: Boolean
        get() = slots.any { it.isEmpty }
    
    /**
     * Có thể mua slot này không
     */
    fun canBuy(slotIndex: Int, playerGold: Int): Boolean {
        if (slotIndex !in 0 until SHOP_SIZE) return false
        val slot = slots[slotIndex]
        return !slot.isEmpty && playerGold >= slot.price
    }
    
    /**
     * Mua unit từ slot
     */
    fun buyFromSlot(slotIndex: Int): Pair<Shop, Unit?> {
        if (slotIndex !in 0 until SHOP_SIZE) return Pair(this, null)
        
        val slot = slots[slotIndex]
        if (slot.isEmpty) return Pair(this, null)
        
        val newSlots = slots.toMutableList().apply {
            set(slotIndex, ShopSlot.empty())
        }
        
        return Pair(
            copy(slots = newSlots),
            slot.unit
        )
    }
    
    /**
     * Roll shop mới theo odds
     */
    fun reroll(playerLevel: Int): Shop {
        val odds = ODDS_TABLE[playerLevel] ?: ODDS_TABLE[2]!!
        val newSlots = List(SHOP_SIZE) { generateSlot(odds) }
        
        return copy(
            slots = newSlots,
            rollCount = rollCount + 1
        )
    }
    
    /**
     * Tạo slot mới theo odds
     */
    private fun generateSlot(odds: List<Int>): ShopSlot {
        val random = (1..100).random()
        var cumulative = 0
        
        for (i in odds.indices) {
            cumulative += odds[i]
            if (random <= cumulative) {
                val tier = tierValues[i]
                val unit = Unit.createRandom(tier)
                return ShopSlot.withUnit(unit)
            }
        }
        
        // Fallback to T1
        return ShopSlot.withUnit(Unit.createRandom(Tier.T1))
    }
    
    /**
     * Tạo shop ban đầu
     */
    fun initialRoll(playerLevel: Int): Shop {
        return copy(rollCount = 0).reroll(playerLevel)
    }
}
