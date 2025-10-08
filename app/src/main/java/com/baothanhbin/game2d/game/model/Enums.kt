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
 * Pha game (Chuẩn bị hoặc Chiến đấu)
 */
enum class RoundPhase {
    PREP,    // Pha chuẩn bị
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

}
