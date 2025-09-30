package com.baothanhbin.game2d.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import android.util.Log
import com.baothanhbin.game2d.game.model.BoardSlot
import com.baothanhbin.game2d.game.model.GameState

@Composable
private fun CollapseHeader(
    title: String,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onDrag: (dy: Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(collapsed) {
                detectVerticalDragGestures { _, dragAmount ->
                    onDrag(dragAmount)
                }
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = Color.White)
        TextButton(onClick = onToggle) {
            Text(if (collapsed) "Hiển thị" else "Ẩn", color = Color.White)
        }
    }
}

@Composable
fun BottomPanel(
    gameState: GameState,
    onBuyUnit: (Int) -> Unit,
    onSellUnit: (String) -> Unit,
    onRerollShop: () -> Unit,
    onBuyXP: () -> Unit,
    onDeployUnit: (String, BoardSlot) -> Unit,
    onRecallUnit: (BoardSlot) -> Unit,
    onSwapUnit: (String, BoardSlot) -> Unit,
    onStartCombat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedUnit by remember { mutableStateOf<com.baothanhbin.game2d.game.model.Unit?>(null) }
    var draggingUnit by remember { mutableStateOf<com.baothanhbin.game2d.game.model.Unit?>(null) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Simple direct callback
    val dragUpdateCallback: (androidx.compose.ui.geometry.Offset) -> Unit = { offset ->
        Log.d("BottomPanel", "📍 DIRECT DRAG UPDATE: offset = $offset")
        dragOffset = offset
        Log.d("BottomPanel", "📍 DIRECT DRAG UPDATE: dragOffset updated to $dragOffset")
    }
    
    // Debug: Log dragOffset changes
    LaunchedEffect(dragOffset, isDragging) {
        Log.d("BottomPanel", "🔄 BOTTOM PANEL STATE: isDragging=$isDragging, dragOffset=$dragOffset")
    }
    var benchCollapsed by remember { mutableStateOf(false) }
    var shopCollapsed by remember { mutableStateOf(false) }
    var bottomPanelPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                bottomPanelPosition = coordinates.positionInWindow()
                Log.d("BottomPanel", "BottomPanel position: $bottomPanelPosition")
            }
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFF121212).copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        // Board row - Đội hình ở trên cùng
        BoardRow(
            player = gameState.player,
            canManageUnits = gameState.canManageUnits(),
            selectedUnit = selectedUnit,
            draggingUnit = draggingUnit,
            isDragging = isDragging,
            dragOffset = dragOffset,
            onRecallUnit = onRecallUnit,
            onDeployUnit = { unitId, slot ->
                val occupied = gameState.player.board[slot] != null
                if (occupied) {
                    onSwapUnit(unitId, slot)
                } else {
                    onDeployUnit(unitId, slot)
                }
                // Clear all state after deploy
                selectedUnit = null
                draggingUnit = null
                isDragging = false
                dragOffset = androidx.compose.ui.geometry.Offset.Zero
            },
            onDragOver = { /* no-op visual handled inside */ },
            onDragLeave = { /* no-op */ },
            onDrop = { unitId, slot ->
                val occupied = gameState.player.board[slot] != null
                if (occupied) onSwapUnit(unitId, slot) else onDeployUnit(unitId, slot)
                // Clear drag state after drop
                selectedUnit = null
                draggingUnit = null
                isDragging = false
                dragOffset = androidx.compose.ui.geometry.Offset.Zero
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Bench header (có thể kéo xuống để ẩn/hiện)
        CollapseHeader(
            title = "Hàng chờ tướng",
            collapsed = benchCollapsed,
            onToggle = { benchCollapsed = !benchCollapsed },
            onDrag = { dy ->
                if (dy > 16f) benchCollapsed = true
                if (dy < -16f) benchCollapsed = false
            }
        )

        if (!benchCollapsed) {
            BenchRow(
                player = gameState.player,
                canManageUnits = gameState.canManageUnits(),
                selectedUnit = selectedUnit,
                draggingUnit = draggingUnit,
                isDragging = isDragging,
                dragOffset = dragOffset,
                onSellUnit = onSellUnit,
                onSwapUnit = onSwapUnit,
                onSelectedUnitChange = { selectedUnit = it },
                onDragStart = { unit, position ->
                    selectedUnit = null  // Clear selection when starting drag
                    draggingUnit = unit
                    isDragging = true
                    dragOffset = position  // Set initial position to center of bench slot
                    Log.d("BottomPanel", "=== DRAG START ===")
                    Log.d("BottomPanel", "Unit: ${unit.type.displayName} ${unit.star.symbol}")
                    Log.d("BottomPanel", "Position from bench: $position")
                    Log.d("BottomPanel", "Setting dragOffset to: $dragOffset")
                    Log.d("BottomPanel", "==================/")
                },
                onDragEnd = { 
                    draggingUnit = null
                    isDragging = false
                    dragOffset = androidx.compose.ui.geometry.Offset.Zero
                },
                onDragUpdate = dragUpdateCallback,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Shop header (có thể kéo xuống để ẩn/hiện)
        CollapseHeader(
            title = "Shop mua tướng",
            collapsed = shopCollapsed,
            onToggle = { shopCollapsed = !shopCollapsed },
            onDrag = { dy ->
                if (dy > 16f) shopCollapsed = true
                if (dy < -16f) shopCollapsed = false
            }
        )

        if (!shopCollapsed) {
            ShopRow(
                shop = gameState.shop,
                playerGold = gameState.player.gold,
                canBuy = { index -> gameState.canBuyUnit(index) },
                onBuyUnit = onBuyUnit,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onRerollShop,
                enabled = gameState.canReroll(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Reroll") }

            Button(
                onClick = onBuyXP,
                enabled = gameState.canBuyXP(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BC34A)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Mua XP") }

            if (gameState.isInPrep) {
                Button(
                    onClick = onStartCombat,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Chiến đấu") }
            }
        }
        }
        
        // Drag overlay - shows dragged unit following cursor
        val currentDraggingUnit = draggingUnit // Local copy to avoid smart cast issues
        if (isDragging && currentDraggingUnit != null) {
            val density = LocalDensity.current
            
            // Center the overlay on the drag position
            val cardSizeInPx = with(density) {
                androidx.compose.ui.geometry.Size(70.dp.toPx(), 90.dp.toPx())
            }
            
            // Convert from root coordinates to BottomPanel-relative coordinates
            val relativeDragOffset = dragOffset - bottomPanelPosition
            val overlayX = (relativeDragOffset.x - cardSizeInPx.width / 2f).toInt()
            val overlayY = (relativeDragOffset.y - cardSizeInPx.height / 2f).toInt()
            
            Log.d("BottomPanel", "=== OVERLAY DEBUG ===")
            Log.d("BottomPanel", "dragOffset (root) = $dragOffset")
            Log.d("BottomPanel", "bottomPanelPosition = $bottomPanelPosition")
            Log.d("BottomPanel", "relativeDragOffset = $relativeDragOffset")
            Log.d("BottomPanel", "cardSize = $cardSizeInPx") 
            Log.d("BottomPanel", "overlayX = $overlayX, overlayY = $overlayY")
            Log.d("BottomPanel", "Final overlay position: ($overlayX, $overlayY)")
            Log.d("BottomPanel", "====================/")
            
            Card(
                modifier = Modifier
                    .offset { 
                        androidx.compose.ui.unit.IntOffset(overlayX, overlayY)
                    }
                    .size(width = 70.dp, height = 90.dp)
                    .zIndex(10f)
                    .scale(1.2f)
                    .alpha(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UnitCard(unit = currentDraggingUnit)
                }
            }
        }
    }
}


