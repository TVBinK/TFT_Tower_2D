package com.baothanhbin.game2d.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    
    // Determine which slot is being hovered over during drag
    val hoveredSlot = remember(dragOffset, isDragging) {
        if (isDragging && dragOffset != androidx.compose.ui.geometry.Offset.Zero) {
            android.util.Log.d("BoardRow", "🔍 HIT TEST: dragOffset=$dragOffset, slotCount=${slotPositions.size}")
            slotPositions.entries.forEach { (slot, rect) ->
                android.util.Log.d("BoardRow", "  Slot ${slot.name}: $rect")
                android.util.Log.d("BoardRow", "    → Contains dragOffset? ${rect.contains(dragOffset)}")
            }
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
        android.util.Log.d("BoardRow", "🔄 DRAG STATE: wasDragging=$wasDragging, isDragging=$isDragging, lastHoveredSlot=${lastHoveredSlot?.name}, hoveredSlot=${hoveredSlot?.name}, draggingUnit=${draggingUnit?.id}")
        
        // Detect drag end (was dragging, now not dragging) and capture current values
        if (wasDragging && !isDragging) {
            val currentHoveredSlot = hoveredSlot ?: lastHoveredSlot
            val currentDraggingUnit = draggingUnit ?: lastDraggingUnit
            android.util.Log.d("BoardRow", "🎯 DRAG END: currentHoveredSlot=${currentHoveredSlot?.name}, currentDraggingUnit=${currentDraggingUnit?.id}")
            
            if (currentHoveredSlot != null && currentDraggingUnit != null) {
                val targetSlot = currentHoveredSlot
                android.util.Log.d("BoardRow", "🎯 DRAG END DETECTED: Checking drop to ${targetSlot.name}")
                android.util.Log.d("BoardRow", "  → targetSlot.position=${targetSlot.position}, player.deployCap=${player.deployCap}")
                android.util.Log.d("BoardRow", "  → player.board[targetSlot]=${player.board[targetSlot]?.id}")
                
                // Check if drop is valid
                if (targetSlot.position < player.deployCap && player.board[targetSlot] == null) {
                    android.util.Log.d("BoardRow", "🎯 AUTO DROP: Unit ${currentDraggingUnit.id} to slot ${targetSlot.name}")
                    onDrop(currentDraggingUnit.id, targetSlot)
                } else {
                    android.util.Log.d("BoardRow", "❌ DROP INVALID: Cannot drop to ${targetSlot.name}")
                }
            } else {
                android.util.Log.d("BoardRow", "❌ NO DROP: currentHoveredSlot=${currentHoveredSlot?.name}, currentDraggingUnit=${currentDraggingUnit?.id}")
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BoardSlot.values().forEach { slot ->
                DraggableBoardSlot(
                    slot = slot,
                    unit = player.board[slot],
                    isActive = slot.position < player.deployCap,
                    canManage = canManageUnits,
                    isDragTarget = isDragging && draggingUnit != null && slot.position < player.deployCap && player.board[slot] == null && hoveredSlot == slot,
                    selectedUnit = if (isDragging) draggingUnit else selectedUnit,
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
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BoardSlotComponent(
    slot: BoardSlot,
    unit: com.baothanhbin.game2d.game.model.Unit?,
    isActive: Boolean,
    canManage: Boolean,
    selectedUnit: com.baothanhbin.game2d.game.model.Unit?,
    onRecall: () -> kotlin.Unit,
    onDeploy: (String) -> kotlin.Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .then(
                when {
                    unit != null && canManage -> {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onRecall() }
                    }
                    selectedUnit != null && canManage && isActive -> {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDeploy(selectedUnit.id) }
                    }
                    else -> Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isActive -> Color(0xFF424242).copy(alpha = 0.5f)
                selectedUnit != null && unit == null -> {
                    Color(0xFF4CAF50).copy(alpha = 0.3f) // Xanh lá cho slot có thể deploy
                }
                unit != null -> Color(unit.type.color).copy(alpha = 0.2f) // Màu theo tướng đang đặt
                else -> Color(0xFF2196F3).copy(alpha = 0.2f) // Màu xanh dương mặc định cho slot trống
            }
        ),
        border = when {
            !isActive -> androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
            selectedUnit != null && unit == null -> {
                androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50)) // Border xanh lá
            }
            unit != null -> androidx.compose.foundation.BorderStroke(2.dp, Color(unit.type.color)) // Border theo màu tướng
            else -> androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (unit != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    UnitCard(unit = unit)
                    
                    // Cooldown indicator
                    if (unit.cooldownRemainingMs > 0) {
                        val progress = 1f - (unit.cooldownRemainingMs.toFloat() / unit.actualFireRateMs)
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = Color(unit.type.color),
                            trackColor = Color.Gray
                        )
                    }
                    
                    // Recall button
                    if (canManage) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Thu về",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Slot ${slot.position + 1}",
                        color = if (isActive) Color.White else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (!isActive) {
                        Text(
                            text = "Cần level ${slot.position + 1}",
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                    }
                }
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
            maxMana = 100f,
            bench = listOf(),
            board = mapOf(
                BoardSlot.SLOT_1 to Unit(
                    id = "u2",
                    type = HeroType.KIM,
                    tier = Tier.T1,
                    baseDamage = 20f,
                    baseFireRateMs = 1000L,
                    manaCost = 15f
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
            type = HeroType.KIM,
            tier = Tier.T1,
            baseDamage = 20f,
            baseFireRateMs = 1000L,
            manaCost = 15f
        ),
        draggingUnit = null,
        onRecallUnit = {},
        onDeployUnit = { _, _ -> },
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    )
}