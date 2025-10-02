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
            price = 1, // Fixed price for all units
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
        
        // Cache HeroType.values() để tránh gọi lại nhiều lần
        private val heroTypes = HeroType.values()
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
     * Roll shop mới với random hero types
     */
    fun reroll(playerLevel: Int): Shop {
        val newSlots = List(SHOP_SIZE) { generateSlot() }
        
        return copy(
            slots = newSlots,
            rollCount = rollCount + 1
        )
    }
    
    /**
     * Tạo slot mới với random hero type
     */
    private fun generateSlot(): ShopSlot {
        val randomType = heroTypes.random()
        val unit = Unit.create(randomType)
        return ShopSlot.withUnit(unit)
    }
    
    /**
     * Tạo shop ban đầu
     */
    fun initialRoll(playerLevel: Int): Shop {
        return copy(rollCount = 0).reroll(playerLevel)
    }
}
