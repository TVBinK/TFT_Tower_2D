package com.baothanhbin.game2d.game.model

/**
 * 5 hệ tướng theo phong cách TFT
 */
enum class HeroType(val displayName: String, val color: Int) {
    METAL("Metal", 0xFFFFD700.toInt()),    // Gold/Metal
    FLOWER("Flower", 0xFF228B22.toInt()),   // Green/Flower
    WATER("Water", 0xFF0000FF.toInt()),   // Blue/Water
    FIRE("Fire", 0xFFFF0000.toInt()),     // Red/Fire
    ICE("Ice", 0xFF87CEEB.toInt())      // Ice
}
