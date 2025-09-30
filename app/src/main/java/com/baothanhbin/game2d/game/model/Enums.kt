package com.baothanhbin.game2d.game.model

/**
 * Số sao của tướng
 */
enum class Star(val value: Int, val symbol: String) {
    ONE(1, "★"),
    TWO(2, "★★"),
    THREE(3, "★★★")
}

/**
 * Tier của tướng (giá mua và độ hiếm)
 */
enum class Tier(val cost: Int, val displayName: String) {
    T1(1, "Tier 1"),
    T2(2, "Tier 2"),
    T3(3, "Tier 3"),
    T4(4, "Tier 4"),
    T5(5, "Tier 5")
}

/**
 * Độ khó game
 */
enum class Difficulty(
    val displayName: String,
    val hpMultiplier: Float,
    val speedMultiplier: Float,
    val spawnDelayMultiplier: Float,
    val rewardMultiplier: Float
) {
    EASY("Dễ", 0.8f, 0.9f, 1.2f, 1.0f),
    NORMAL("Thường", 1.0f, 1.0f, 1.0f, 1.0f),
    HARD("Khó", 1.3f, 1.1f, 0.8f, 1.2f)
}

/**
 * Pha game (Chuẩn bị hoặc Chiến đấu)
 */
enum class RoundPhase {
    PREP,    // Pha mua sắm/chuẩn bị
    COMBAT   // Pha chiến đấu
}

/**
 * Vị trí slot trên board (5 slot có thể xếp tướng mọi hệ)
 */
enum class BoardSlot(val position: Int) {
    SLOT_1(0),
    SLOT_2(1),
    SLOT_3(2),
    SLOT_4(3),
    SLOT_5(4);
    
    companion object {
        fun fromPosition(position: Int): BoardSlot? {
            return values().find { it.position == position }
        }
    }
}
