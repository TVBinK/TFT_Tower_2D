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
    FIRE(
        heroType = HeroType.FIRE,
        displayName = "Fire",
        star1Description = "★☆☆ Creates a fire line dealing high damage for 5s (cooldown 5s).",
        star2Description = "★★☆ Duration increases to 7 seconds.",
        star3Description = "★★★ Duration increases to 9 seconds and deals splash damage."
    ),

    WATER(
        heroType = HeroType.WATER,
        displayName = "Water",
        star1Description = "★☆☆ Summons a wave of water that pushes enemies back in a column.",
        star2Description = "★★☆ Unleashes 2 water waves.",
        star3Description = "★★★ Summons 3 powerful water waves for greater control."
    ),

    FLOWER(
        heroType = HeroType.FLOWER,
        displayName = "Flower",
        star1Description = "★☆☆ Restores +2 HP to the base every 10 seconds.",
        star2Description = "★★☆ Restores +3 HP every 10 seconds.",
        star3Description = "★★★ Restores +5 HP every 8 seconds and reduces skill cooldown."
    ),

    ICE(
        heroType = HeroType.ICE,
        displayName = "Ice",
        star1Description = "★☆☆ Shoots slowing projectiles with a small chance to freeze enemies briefly.",
        star2Description = "★★☆ Increases damage and freeze chance.",
        star3Description = "★★★ Greatly boosts damage and extends freeze duration."
    ),

    METAL(
        heroType = HeroType.METAL,
        displayName = "Metal",
        star1Description = "★☆☆ Fires arrows that deal single-target damage.",
        star2Description = "★★☆ Increases attack damage and shooting speed.",
        star3Description = "★★★ Further boosts both damage and speed, with piercing effect."
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