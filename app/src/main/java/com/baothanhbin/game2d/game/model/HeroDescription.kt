package com.baothanhbin.game2d.game.model

/**
 * Enum chứa mô tả abilities cho các hero khác nhau
 */
enum class HeroDescription(
    val heroType: HeroType,
    val displayName: String,
    val star1Description: String,
    val star2Description: String,
    val star3Description: String
) {
    METAL(
        heroType = HeroType.METAL,
        displayName = "Metal",
        star1Description = "★☆☆ Summons 1 blazing fire line, dealing heavy damage.",
        star2Description = "★★☆ Summons 2 blazing fire lines.",
        star3Description = "★★★ Summons 3 blazing fire lines for maximum devastation."
    ),
    
    FLOWER(
        heroType = HeroType.FLOWER,
        displayName = "Flower",
        star1Description = "★☆☆ Summons 1 blooming flower, healing allies.",
        star2Description = "★★☆ Summons 2 blooming flowers.",
        star3Description = "★★★ Summons 3 blooming flowers for maximum healing."
    ),
    
    WATER(
        heroType = HeroType.WATER,
        displayName = "Water",
        star1Description = "★☆☆ Raises 1 towering water column, pushing enemies back.",
        star2Description = "★★☆ Unleashes 2 water columns.",
        star3Description = "★★★ Conjures 3 water columns."
    ),
    
    FIRE(
        heroType = HeroType.FIRE,
        displayName = "Fire",
        star1Description = "★☆☆ Fires arrows with base damage and speed.",
        star2Description = "★★☆ +20 damage and +5% attack speed.",
        star3Description = "★★★ +50 damage and +15% attack speed."
    ),
    
    ICE(
        heroType = HeroType.ICE,
        displayName = "Ice",
        star1Description = "★☆☆ Attacks slow enemies with 5% chance to freeze.",
        star2Description = "★★☆ Freeze chance increases to 10%.",
        star3Description = "★★★ Freeze chance increases to 20%."
    );
    
    /**
     * Tìm description theo HeroType
     */
    companion object {
        fun getByHeroType(heroType: HeroType?): HeroDescription? {
            return values().find { it.heroType == heroType }
        }
        
        /**
         * Lấy description mặc định
         */
        fun getDefault(): HeroDescription = METAL
    }
}