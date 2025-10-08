package com.baothanhbin.game2d.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AcUnit
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.util.Log
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.res.painterResource
import com.baothanhbin.game2d.game.model.Season
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import com.baothanhbin.game2d.R
import com.baothanhbin.game2d.game.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AnimatedUnitImage(
    unit: com.baothanhbin.game2d.game.model.Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    frameDurationMs: Long = 200L, // Tăng từ 120L lên 200L để animation chậm hơn
    isShooting: Boolean
) {
    // Only animate when shooting; otherwise show idle frame
    var frameIndex by remember(unit.id) { mutableStateOf(0) }

    LaunchedEffect(unit.id, isShooting) {
        if (!isShooting) {
            frameIndex = 0
            return@LaunchedEffect
        }
        while (isActive && isShooting) {
            delay(frameDurationMs)
            frameIndex = (frameIndex + 1) % 2
        }
    }

    val painter = when (unit.type) {
        HeroType.METAL -> {
            val resId = if (isShooting) {
                if (frameIndex == 0) R.drawable.hero_metal1 else R.drawable.hero_metal2
            } else {
                R.drawable.hero_metal1
            }
            painterResource(id = resId)
        }

        HeroType.FLOWER -> {
            val resId = if (isShooting) {
                if (frameIndex == 0) R.drawable.hero_flower1 else R.drawable.hero_flower2
            } else {
                R.drawable.hero_flower1
            }
            painterResource(id = resId)
        }

        HeroType.FIRE -> {
            val resId = if (isShooting) {
                if (frameIndex == 0) R.drawable.hero_fire1 else R.drawable.hero_fire2
            } else {
                R.drawable.hero_fire1
            }
            painterResource(id = resId)
        }

        HeroType.WATER -> {
            val resId = if (isShooting) {
                if (frameIndex == 0) R.drawable.hero_water1 else R.drawable.hero_water2
            } else {
                R.drawable.hero_water1
            }
            painterResource(id = resId)
        }

        HeroType.ICE -> {
            val resId = if (isShooting) {
                if (frameIndex == 0) R.drawable.hero_ice1 else R.drawable.hero_ice2
            } else {
                R.drawable.hero_ice1
            }
            painterResource(id = resId)
        }
    }

    Image(
        painter = painter,
        contentDescription = unit.type.displayName,
        modifier = modifier,
        contentScale = contentScale
    )
}

/**
 * Draggable component for bench units
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableBenchSlot(
    unit: com.baothanhbin.game2d.game.model.Unit?,
    canManage: Boolean,
    isSelected: Boolean,
    isDragging: Boolean,
    onSelect: () -> kotlin.Unit,
    onSell: () -> kotlin.Unit,
    onDragStart: (com.baothanhbin.game2d.game.model.Unit, androidx.compose.ui.geometry.Offset) -> kotlin.Unit,
    onDragEnd: () -> kotlin.Unit,
    onDragUpdate: (androidx.compose.ui.geometry.Offset) -> kotlin.Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var globalPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var startDragPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var cumulativeOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isPositioned by remember { mutableStateOf(false) }

    // Debug: Log when globalPosition changes
    LaunchedEffect(globalPosition) {
        if (globalPosition != androidx.compose.ui.geometry.Offset.Zero) {
            isPositioned = true
        }
    }

    Card(
        modifier = modifier
            .size(
                width = UiDimens.BENCH_SLOT_WIDTH, height = UiDimens.BENCH_SLOT_HEIGHT
            )
            .onGloballyPositioned { coordinates ->
                val newGlobalPosition = coordinates.positionInWindow()
                globalPosition = newGlobalPosition
                isPositioned = true
            }
            .pointerInput(unit?.id, canManage) {
                if (unit != null && canManage) {
                    // Người chơi chạm và kéo unit trên bench -> DraggableBenchSlot phát hiện gesture
                    detectDragGestures(onDragStart = { offset ->
                        // Calculate center of bench slot (70dp x 90dp)
                        val cardSizeInPx = with(density) {
                            androidx.compose.ui.geometry.Size(
                                UiDimens.BENCH_SLOT_WIDTH.toPx(), UiDimens.BENCH_SLOT_HEIGHT.toPx()
                            )
                        }

                        // Tính vị trí trung tâm của card
                        val centerPosition =
                            if (globalPosition != androidx.compose.ui.geometry.Offset.Zero) {
                                // sử dụng vị trí đã đo được để tính toán chính xác hơn
                                globalPosition + androidx.compose.ui.geometry.Offset(
                                    cardSizeInPx.width / 2f, cardSizeInPx.height / 2f
                                )
                            } else {
                                offset
                            }

                        startDragPosition = centerPosition
                        cumulativeOffset = androidx.compose.ui.geometry.Offset.Zero
                        //Báo cho parent: "Đang kéo unit X tại vị trí Y"
                        onDragStart(unit, startDragPosition)
                    }, onDragEnd = {
                        onDragEnd()
                    }, onDrag = { change, dragAmount ->
                        // Cộng dồn khoảng cách di chuyển từ lúc bắt đầu drag
                        cumulativeOffset += dragAmount
                        val currentPosition = startDragPosition + cumulativeOffset
                        // Báo vị trí mới cho parent
                        onDragUpdate(currentPosition)
                        change.consume() // Consume the gesture to prevent conflicts
                    })
                }
            }
            .then(
                if (unit != null && canManage) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSelect() }
            } else {
                Modifier
            })
            .scale(if (isDragging) 1.1f else 1f)
            .alpha(if (isDragging) 0.8f else 1f)
            .zIndex(if (isDragging) 10f else 1f), colors = CardDefaults.cardColors(
        containerColor = if (unit != null) Color(unit.type.color).copy(0.4f) else Color.Transparent
    ), border = if (isSelected) {
        androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3))
    } else {
        null
    }, shape = RoundedCornerShape(8.dp)) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (unit != null) {
                // Use UnitCard like Shop for consistent visuals
                UnitCard(
                    unit = unit, modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            } else {
                // Empty slot with add icon
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "Empty",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Draggable component for board slots
 */
@Composable
fun DraggableBoardSlot(
    slot: BoardSlot,
    unit: com.baothanhbin.game2d.game.model.Unit?,
    isActive: Boolean,
    canManage: Boolean,
    isDragTarget: Boolean,
    selectedUnit: com.baothanhbin.game2d.game.model.Unit?,
    season: Season,
    onRecall: () -> Unit,
    onDeploy: (String) -> Unit,
    onDragOver: (BoardSlot) -> Unit,
    onDragLeave: () -> Unit,
    onDrop: (String, BoardSlot) -> Unit,
    onPositionChanged: (androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Size) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var slotPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var slotSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Card(modifier = modifier
        .onGloballyPositioned { coordinates ->
            slotPosition = coordinates.positionInWindow()
            slotSize = coordinates.size.toSize()
            val rect = androidx.compose.ui.geometry.Rect(slotPosition, slotSize)
            onPositionChanged(slotPosition, slotSize)
        }
        .pointerInput(unit, canManage) {
            // Only setup drag gestures for units already on board
            if (unit != null && canManage) {
                detectDragGestures(onDragStart = { offset ->
                    Log.d("BoardSlot", "🔄 DRAG START: From board slot ${slot.name}")
                }, onDragEnd = {
                    Log.d("BoardSlot", "🔄 DRAG END: From board slot ${slot.name}")
                }, onDrag = { change, _ ->
                    // Handle dragging units from board
                })
            } else {
                // For empty slots or non-manageable units, only handle tap
                detectTapGestures(
                    onTap = {

                        // Check if this is a normal deploy operation
                        if (selectedUnit != null && canManage && isActive && unit == null) {
                            onDeploy(selectedUnit.id)
                        }
                        // Check if this is a recall operation
                        else if (unit != null && canManage) {
                            onRecall()
                        }
                    })
            }
        }
        .then(
            when {
            unit != null && canManage -> {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onRecall() }
            }

            selectedUnit != null && canManage && isActive && unit == null -> {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDeploy(selectedUnit.id)
                }
            }

            else -> Modifier
        }), colors = CardDefaults.cardColors(
        containerColor = Color.Transparent
    ),
        // No thick border to match Shop/Bench (UnitCard is clear enough)
        border = null, shape = RoundedCornerShape(8.dp)) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background slot image based on season
            val slotImageResId = when (season) {
                Season.SPRING -> R.drawable.bg_slot_spring
                Season.SUMMER -> R.drawable.bg_slot_autumn // Fallback to winter for summer
                Season.AUTUMN -> R.drawable.bg_slot_autumn
                Season.WINTER -> R.drawable.bg_slot_winter
            }
            Image(
                painter = painterResource(id = slotImageResId),
                contentDescription = "Slot background",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isActive) 1f else 0.5f),
                contentScale = ContentScale.FillBounds
            )
            if (unit != null) {
                // Show UnitCard with shooting state to switch frames when firing
                val shootingWindowMs = 250L
                var nowMs by remember(unit.id) { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(unit.id) {
                    while (true) {
                        nowMs = System.currentTimeMillis()
                        delay(50L)
                    }
                }
                val isShooting = (nowMs - unit.lastShotAtMs) <= shootingWindowMs

                UnitCard(
                    unit = unit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    isShooting = isShooting,
                    showBorder = false // Không hiển thị viền trong DraggableBoardSlot
                )

                // Cooldown indicator overlay at bottom
                if (unit.cooldownRemainingMs > 0) {
                    val progress =
                        1f - (unit.cooldownRemainingMs.toFloat() / unit.actualFireRateMs.toFloat())
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = Color(unit.type.color),
                            trackColor = Color.Black.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                // Empty slot - show slot number and status on top of background
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    if (!isActive) {
                        Text(
                            text = "Need level ${slot.position + 1}",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            modifier = Modifier
                                .background(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Component displaying unit with golden image instead of text
 */
@Composable
fun UnitCard(
    unit: com.baothanhbin.game2d.game.model.Unit,
    modifier: Modifier = Modifier,
    isShooting: Boolean = false,
    showBorder: Boolean = true
) {
    Box(
        modifier = modifier.size(32.dp).let { baseModifier ->
                val withBackground = if (showBorder) {
                    baseModifier.background(
                        color = Color.Transparent, shape = RoundedCornerShape(6.dp)
                    )
                } else {
                    baseModifier
                }

                if (showBorder) {
                    withBackground.border(
                        width = 2.dp,
                        color = if (unit.isFrozen) Color(0xFF00FFFF) else Color(unit.type.color), // Cyan border khi frozen
                        shape = RoundedCornerShape(6.dp)
                    )
                } else {
                    withBackground
                }
            }) {
        // Animated image fills the entire card
        AnimatedUnitImage(
            unit = unit,
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp),
            contentScale = ContentScale.FillBounds,
            isShooting = isShooting
        )

        // Stars overlay at top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                repeat(unit.star.value) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = Color.Yellow,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }


        // Freeze overlay khi tướng bị đóng băng - hiển thị freeze effect
        if (unit.isFrozen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFF00FFFF).copy(alpha = 0.2f), // Cyan tint
                        shape = RoundedCornerShape(6.dp)
                    )
            ) {
                // Freeze icon ở giữa
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = "Frozen",
                    tint = Color(0xFF00FFFF),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun DraggableBenchSlotPreview() {
    DraggableBenchSlot(
        unit = com.baothanhbin.game2d.game.model.Unit.create(
        type = HeroType.METAL
    ),
        canManage = true,
        isSelected = false,
        isDragging = false,
        onSelect = {},
        onSell = {},
        onDragStart = { _, _ -> },
        onDragEnd = {})
}