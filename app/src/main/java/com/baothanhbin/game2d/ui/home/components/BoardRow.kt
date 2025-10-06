package com.baothanhbin.game2d.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.baothanhbin.game2d.game.model.*

/**
 * Hàng board - 5 slot theo hệ tướng
 */
@Composable
fun BoardRow(
    player: Player,
    canManageUnits: Boolean,
    selectedUnit: com.baothanhbin.game2d.game.model.Unit?,
    draggingUnit: com.baothanhbin.game2d.game.model.Unit?,
    season: Season,
    isDragging: Boolean = false,
    dragOffset: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    onRecallUnit: (BoardSlot) -> kotlin.Unit,
    onDeployUnit: (String, BoardSlot) -> kotlin.Unit,
    onDragOver: (BoardSlot) -> kotlin.Unit = {},
    onDragLeave: () -> kotlin.Unit = {},
    onDrop: (String, BoardSlot) -> kotlin.Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // Track slot positions for hit testing
    val slotPositions = remember { mutableMapOf<BoardSlot, androidx.compose.ui.geometry.Rect>() }
    
    // Debug: Log dragOffset changes
    LaunchedEffect(dragOffset, isDragging) {
        android.util.Log.d("BoardRow", "🔄 DRAG STATE CHANGE: isDragging=$isDragging, dragOffset=$dragOffset")
    }
    
    // Nhận slot đang hover khi kéo thả Board nhận dragOffset từ parent
    val hoveredSlot = remember(dragOffset, isDragging) {
        if (isDragging && dragOffset != androidx.compose.ui.geometry.Offset.Zero) {
            // Tìm ô nào chứa vị trí kéo
            val foundSlot = slotPositions.entries.find { (slot, rect) ->
                rect.contains(dragOffset)
            }?.key
            android.util.Log.d("BoardRow", "  → Hovered slot: ${foundSlot?.name ?: "NONE"}")
            foundSlot
        } else {
            null
        }
    }
    
    // Track previous drag state to detect drag end
    var wasDragging by remember { mutableStateOf(false) }
    var lastHoveredSlot by remember { mutableStateOf<BoardSlot?>(null) }
    var lastDraggingUnit by remember { mutableStateOf<com.baothanhbin.game2d.game.model.Unit?>(null) }
    
    // Handle drop when drag ends - capture both hoveredSlot and draggingUnit before they get reset
    LaunchedEffect(isDragging, hoveredSlot, draggingUnit) {

        // Kiểm tra nếu vừa kết thúc kéo thả
        if (wasDragging && !isDragging) {
            // Kiểm tra: có hoveredSlot hợp lệ không
            val currentHoveredSlot = hoveredSlot ?: lastHoveredSlot
            val currentDraggingUnit = draggingUnit ?: lastDraggingUnit

            if (currentHoveredSlot != null && currentDraggingUnit != null) {
                val targetSlot = currentHoveredSlot
                // Nếu có: gọi onDrop(unitId, slot)
                if (targetSlot.position < player.deployCap && player.board[targetSlot] == null) {
                    onDrop(currentDraggingUnit.id, targetSlot)
                }
            }
        }
        wasDragging = isDragging
        lastHoveredSlot = hoveredSlot
        lastDraggingUnit = draggingUnit
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BoardSlot.values().forEach { slot ->
                DraggableBoardSlot(
                    slot = slot,
                    unit = player.board[slot],
                    isActive = slot.position < player.deployCap,
                    canManage = canManageUnits,
                    isDragTarget = isDragging && draggingUnit != null && slot.position < player.deployCap && player.board[slot] == null && hoveredSlot == slot,
                    selectedUnit = if (isDragging) draggingUnit else selectedUnit,
                    season = season,
                    onRecall = { onRecallUnit(slot) },
                    onDeploy = { unitId -> 
                        onDeployUnit(unitId, slot)
                    },
                    onDragOver = { onDragOver(slot) },
                    onDragLeave = { onDragLeave() },
                    onDrop = { unitId, targetSlot -> onDrop(unitId, targetSlot) },
                    onPositionChanged = { position, size ->
                        slotPositions[slot] = androidx.compose.ui.geometry.Rect(position, size)
                    },
                    modifier = Modifier
                        .width(UiDimens.SHOP_SLOT_WIDTH)
                        .height(UiDimens.SHOP_SLOT_HEIGHT)
                )
            }
        }
    }
}

@Preview
@Composable
private fun BoardRowPreview() {
    val player = remember {
        Player(
            gold = 50,
            level = 3,
            bench = listOf(),
            board = mapOf(
                BoardSlot.SLOT_1 to Unit(
                    id = "u2",
                    type = HeroType.METAL,
                    baseDamage = 20f,
                    baseFireRateMs = 1000L
                ),
                BoardSlot.SLOT_2 to null,
                BoardSlot.SLOT_3 to null,
                BoardSlot.SLOT_4 to null
            )
        )
    }

    BoardRow(
        player = player,
        canManageUnits = true,
        selectedUnit = Unit(
            id = "u2",
            type = HeroType.METAL,
            baseDamage = 20f,
            baseFireRateMs = 1000L
        ),
        draggingUnit = null,
        season = Season.SPRING,
        onRecallUnit = {},
        onDeployUnit = { _, _ -> },
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    )
}