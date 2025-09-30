package com.baothanhbin.game2d.game.model

/**
 * 5 hệ tướng theo phong cách TFT
 */
enum class HeroType(val displayName: String, val color: Int) {
    KIM("Kim", 0xFFFFD700.toInt()),      // Vàng
    MOC("Mộc", 0xFF228B22.toInt()),      // Xanh lá
    THUY("Thủy", 0xFF0000FF.toInt()),    // Xanh dương
    HOA("Hỏa", 0xFFFF0000.toInt()),      // Đỏ
    THO("Thổ", 0xFF8B4513.toInt())       // Nâu đất
}
