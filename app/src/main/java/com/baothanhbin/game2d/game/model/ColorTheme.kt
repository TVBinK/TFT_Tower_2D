package com.baothanhbin.game2d.game.model

import androidx.compose.ui.graphics.Color

/**
 * Enum chứa các theme màu cho các map khác nhau
 */
enum class ColorTheme(
    val mapName: String,
    val backgroundResource: String,
    val bottomPanelGradientStart: Color,
    val bottomPanelGradientEnd: Color,
    val description: String
) {
    WINTER(
        mapName = "Winter",
        backgroundResource = "background_winter",
        bottomPanelGradientStart = Color(0xFF008AA4),
        bottomPanelGradientEnd = Color(0xFF37D5E3),
        description = "Mùa đông với tuyết và màu xanh lạnh"
    ),
    
    SPRING(
        mapName = "Spring", 
        backgroundResource = "background_spring",
        bottomPanelGradientStart = Color(0xFF4CAF50),
        bottomPanelGradientEnd = Color(0xFF8BC34A),
        description = "Mùa xuân với màu xanh lá tươi mát"
    ),
    
    SUMMER(
        mapName = "Summer",
        backgroundResource = "background_summer", 
        bottomPanelGradientStart = Color(0xFFFF9800),
        bottomPanelGradientEnd = Color(0xFFFFC107),
        description = "Mùa hè với màu vàng cam nắng"
    ),
    
    AUTUMN(
        mapName = "Autumn",
        backgroundResource = "background_autumn",
        bottomPanelGradientStart = Color(0xFF8D6E63),
        bottomPanelGradientEnd = Color(0xFFD7CCC8),
        description = "Mùa thu với màu nâu vàng lá rụng"
    );
    
    /**
     * Lấy danh sách tất cả các map có sẵn
     */
    companion object {
        fun getDefault(): ColorTheme = WINTER
    }
}
