package com.baothanhbin.game2d.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
 * Bench row - list of units in storage
 */
@Composable
fun BenchRow(
    player: Player,
    canManageUnits: Boolean,
    selectedUnit: com.baothanhbin.game2d.game.model.Unit?,
    draggingUnit: com.baothanhbin.game2d.game.model.Unit?,
    isDragging: Boolean,
    dragOffset: androidx.compose.ui.geometry.Offset,
    onSellUnit: (String) -> kotlin.Unit,
    onSwapUnit: (String, BoardSlot) -> kotlin.Unit,
    onSelectedUnitChange: (com.baothanhbin.game2d.game.model.Unit?) -> kotlin.Unit,
    onDragStart: (com.baothanhbin.game2d.game.model.Unit, androidx.compose.ui.geometry.Offset) -> kotlin.Unit = { _, _ -> },
    onDragEnd: () -> kotlin.Unit = {},
    onDragUpdate: (androidx.compose.ui.geometry.Offset) -> kotlin.Unit = {},
    modifier: Modifier = Modifier
) {
    
    // selectedUnit is managed by parent, no need to notify back
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            // Bench slots
            itemsIndexed(
                items = player.bench + List(Player.MAX_BENCH_SIZE - player.bench.size) { null },
                key = { index, unit -> unit?.id ?: "empty_$index" }
            ) { index, unit ->
                DraggableBenchSlot(
                    unit = unit,
                    canManage = canManageUnits,
                    isSelected = selectedUnit?.id == unit?.id,
                    isDragging = draggingUnit?.id == unit?.id,
                    onSelect = { 
                        onSelectedUnitChange(if (selectedUnit?.id == unit?.id) null else unit)
                    },
                    onSell = { unit?.let { onSellUnit(it.id) } },
                    onDragStart = { unit, position ->
                        onDragStart(unit, position)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragUpdate = remember(onDragUpdate) {
                        { offset ->
                            android.util.Log.d("BenchRow", "📍 BENCH DRAG UPDATE: offset = $offset")
                            android.util.Log.d("BenchRow", "📍 CALLING PARENT onDragUpdate with: $offset")
                            onDragUpdate(offset)
                            android.util.Log.d("BenchRow", "📍 PARENT onDragUpdate CALLED")
                        }
                    },
                    modifier = Modifier
                        .size(width = UiDimens.BENCH_SLOT_WIDTH, height = UiDimens.BENCH_SLOT_HEIGHT)
                )
            }
        }
        
        // Unit actions quando selected
        selectedUnit?.let { unit ->
            UnitActionsRow(
                unit = unit,
                player = player,
                onSwapToBoard = { slot -> onSwapUnit(unit.id, slot) },
                onSell = { onSellUnit(unit.id) },
                onClose = { onSelectedUnitChange(null) }
            )
        }
    }
}

@Composable
private fun UnitActionsRow(
    unit: com.baothanhbin.game2d.game.model.Unit,
    player: Player,
    onSwapToBoard: (BoardSlot) -> kotlin.Unit,
    onSell: () -> kotlin.Unit,
    onClose: () -> kotlin.Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1976D2).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${unit.type.displayName} ${unit.star.symbol}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Deploy to board - find first empty slot
            val availableSlot = BoardSlot.values().find { slot ->
                slot.position < player.deployCap && player.board[slot] == null
            }
            val canDeploy = availableSlot != null
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { availableSlot?.let { onSwapToBoard(it) } },
                    enabled = canDeploy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(unit.type.color)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Deploy",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Deploy",
                            fontSize = 12.sp
                        )
                    }
                }
                
                Button(
                    onClick = onSell,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5722)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Sell ${unit.sellPrice}g",
                        fontSize = 12.sp
                    )
                }
            }
            
            // Unit stats
            Text(
                text = "Damage: ${unit.actualDamage.toInt()} | Fire Rate: ${unit.actualFireRateMs}ms",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }
    }
}