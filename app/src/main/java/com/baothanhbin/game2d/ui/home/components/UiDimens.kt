package com.baothanhbin.game2d.ui.home.components

import androidx.compose.ui.unit.dp

/**
 * Shared UI dimensions for slots to keep sizes consistent across bench and shop.
 */
object UiDimens {
    // Bench slot size (was 70x90 dp in drag, 64x84 dp in list) → unify
    val BENCH_SLOT_WIDTH = 70.dp
    val BENCH_SLOT_HEIGHT = 90.dp

    // Shop slot size to visually match bench cards height; width can be similar
    val SHOP_SLOT_WIDTH = 70.dp
    val SHOP_SLOT_HEIGHT = 90.dp
}


