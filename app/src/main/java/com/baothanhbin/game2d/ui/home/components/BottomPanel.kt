package com.baothanhbin.game2d.ui.home.components

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
import com.baothanhbin.game2d.game.model.ColorTheme
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import android.util.Log
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.baothanhbin.game2d.R
import com.baothanhbin.game2d.game.model.BoardSlot
import com.baothanhbin.game2d.game.model.GameState
import com.baothanhbin.game2d.game.model.Season


@Composable
fun BottomPanel(
    gameState: GameState,
    season: Season,
    onBuyUnit: (Int) -> Unit,
    onSellUnit: (String) -> Unit,
    onRerollShop: () -> Unit,
    onBuyXP: () -> Unit,
    onDeployUnit: (String, BoardSlot) -> Unit,
    onRecallUnit: (BoardSlot) -> Unit,
    onSwapUnit: (String, BoardSlot) -> Unit,
    onStartCombat: () -> Unit,
    onOpenShop: () -> Unit = {},
    colorTheme: ColorTheme = ColorTheme.getDefault(),
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

    var benchCollapsed by remember { mutableStateOf(false) }
    var bottomPanelPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                bottomPanelPosition = coordinates.positionInWindow()
                Log.d("BottomPanel", "BottomPanel position: $bottomPanelPosition")
            }
    ) {
        // Background image based on season
        val backgroundResId = when (season) {
            Season.SPRING -> R.drawable.bg_bottom_spring
            Season.SUMMER -> R.drawable.bg_bottom_summer
            Season.AUTUMN -> R.drawable.bg_bottom_autumn
            Season.WINTER -> R.drawable.bg_bottom_winter
        }
        
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )
        
        Column(
            modifier = Modifier
                .padding(PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        // Board row - Đội hình ở trên cùng
        BoardRow(
            player = gameState.player,
            canManageUnits = gameState.canManageUnits(),
            selectedUnit = selectedUnit,
            draggingUnit = draggingUnit,
            season = season,
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

        // Bench header (can drag down to hide/show)
        CollapseHeader(
            title = "Bench",
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
                // Parent nhận thông tin từ bench sau đó chuyển tiếp xuống BottomPanel
                onDragStart = { unit, position ->
                    selectedUnit = null  // Clear selection when starting drag
                    draggingUnit = unit // Lưu unit đang kéo
                    isDragging = true  // Đánh dấu đang kéo
                    dragOffset = position   // Lưu vị trí
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

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onOpenShop,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_shop),
                    contentDescription = "Toggle",
                    modifier = Modifier.size(37.dp)
                )
            }

            Button(
                onClick = { 
                    if (gameState.player.canBuyXP) {
                        onBuyXP()
                    }
                },
                enabled = gameState.player.canBuyXP,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_buyxp),
                    contentDescription = "Buy XP",
                    modifier = Modifier
                        .size(45.dp)
                        .alpha(if (gameState.player.canBuyXP) 1f else 0.3f)
                )
            }

            if (gameState.isInPrep) {
                Button(
                    onClick = onStartCombat,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_fight),
                        contentDescription = "Toggle",
                        modifier = Modifier.size(37.dp)
                    )
                }
            }
        }
        }
        
        // Overlay moved to HomeScreen layer

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
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_show),
                contentDescription = "Toggle",
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Preview
@Composable
fun BottomPanelPreview() {
    BottomPanel(
        gameState = GameState.sample(),
        season = Season.WINTER,
        onBuyUnit = { index -> },
        onSellUnit = { unitId -> },
        onRerollShop = { },
        onBuyXP = { },
        onDeployUnit = { unitId, slot -> },
        onRecallUnit = { slot -> },
        onSwapUnit = { unitId, slot -> },
        onStartCombat = { },
        colorTheme = ColorTheme.WINTER,
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    )
}


