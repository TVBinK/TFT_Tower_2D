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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
    frameDurationMs: Long = 120L,
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
        HeroType.KIM -> {
            val resId = if (isShooting) {
                if (frameIndex == 0) R.drawable.hero_kim1 else R.drawable.hero_kim2
            } else {
                R.drawable.hero_kim1
            }
            painterResource(id = resId)
        }
        HeroType.HOA -> {
            val resId = if (isShooting) {
                if (frameIndex == 0) R.drawable.hero_hoa1 else R.drawable.hero_hoa2
            } else {
                R.drawable.hero_hoa1
            }
            painterResource(id = resId)
        }
        else -> painterResource(id = R.drawable.golden)
    }

    Image(
        painter = painter,
        contentDescription = unit.type.displayName,
        modifier = modifier,
        contentScale = contentScale
    )
}

/**
 * Component kéo thả cho bench units
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
    
    // Test onDragUpdate callback
    LaunchedEffect(Unit) {
        Log.d("DragBench", "🔧 SETUP: onDragUpdate callback = ${onDragUpdate}")
    }
    
    Card(
        modifier = modifier
            .size(width = UiDimens.BENCH_SLOT_WIDTH, height = UiDimens.BENCH_SLOT_HEIGHT)
            .onGloballyPositioned { coordinates ->
                val newGlobalPosition = coordinates.positionInWindow()
                globalPosition = newGlobalPosition
                isPositioned = true
            }
            .pointerInput(unit?.id, canManage) {
                if (unit != null && canManage) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Calculate center of bench slot (70dp x 90dp)
                            val cardSizeInPx = with(density) {
                                androidx.compose.ui.geometry.Size(UiDimens.BENCH_SLOT_WIDTH.toPx(), UiDimens.BENCH_SLOT_HEIGHT.toPx())
                            }
                            
                            // Use global position if available, otherwise calculate from offset
                            val centerPosition = if (globalPosition != androidx.compose.ui.geometry.Offset.Zero) {
                                // Use global position and add center offset
                                globalPosition + androidx.compose.ui.geometry.Offset(
                                    cardSizeInPx.width / 2f,
                                    cardSizeInPx.height / 2f
                                )
                            } else {
                                // Fallback: use the touch offset directly (less accurate)
                                offset
                            }
                            
                            startDragPosition = centerPosition
                            cumulativeOffset = androidx.compose.ui.geometry.Offset.Zero
                            
                            Log.d("DragBench", "Start position: $startDragPosition")
                            onDragStart(unit, startDragPosition)
                        },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onDrag = { change, dragAmount ->
                            // Update cumulative offset and current position
                            cumulativeOffset += dragAmount
                            val currentPosition = startDragPosition + cumulativeOffset
                            onDragUpdate(currentPosition)
                            change.consume() // Consume the gesture to prevent conflicts
                        }
                    )
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
                }
            )
            .scale(if (isDragging) 1.1f else 1f)
            .alpha(if (isDragging) 0.8f else 1f)
            .zIndex(if (isDragging) 10f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = if (unit != null) {
                if (isSelected) Color(0xFF2196F3) else Color(0xFF1E1E1E)
            } else {
                Color(0xFF424242)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3))
        } else {
            null
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (unit != null) {
				// Ảnh động: thay khung hình theo thời gian để tạo chuyển động
				AnimatedUnitImage(
					unit = unit,
					modifier = Modifier
						.fillMaxSize()
						.padding(2.dp),
					contentScale = ContentScale.FillBounds,
					isShooting = false
				)
                
                // Stars overlay ở góc trên bên phải
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        repeat(unit.star.value) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                }
                
                // Tier overlay ở góc dưới bên trái
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "T${unit.tier.name.last()}",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            } else {
                // Empty slot với icon add
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "Trống",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Component kéo thả cho board slots
 */
@Composable
fun DraggableBoardSlot(
    slot: BoardSlot,
    unit: com.baothanhbin.game2d.game.model.Unit?,
    isActive: Boolean,
    canManage: Boolean,
    isDragTarget: Boolean,
    selectedUnit: com.baothanhbin.game2d.game.model.Unit?,
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
    
    Card(
        modifier = modifier
            .aspectRatio(0.9f)
            .onGloballyPositioned { coordinates ->
                slotPosition = coordinates.positionInWindow()
                slotSize = coordinates.size.toSize()
                val rect = androidx.compose.ui.geometry.Rect(slotPosition, slotSize)
                onPositionChanged(slotPosition, slotSize)
            }
            .pointerInput(unit, canManage) {
                // Only setup drag gestures for units already on board
                if (unit != null && canManage) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            Log.d("BoardSlot", "🔄 DRAG START: From board slot ${slot.name}")
                        },
                        onDragEnd = {
                            Log.d("BoardSlot", "🔄 DRAG END: From board slot ${slot.name}")
                        },
                        onDrag = { change, _ ->
                            // Handle dragging units from board
                        }
                    )
                } else {
                    // For empty slots or non-manageable units, only handle tap
                    detectTapGestures(
                        onTap = {
                            Log.d("BoardSlot", "🖱️ TAP: Slot ${slot.name}")
                            Log.d("BoardSlot", "🔍 TAP CHECK: selectedUnit=${selectedUnit?.id}, canManage=$canManage, isActive=$isActive, unit=${unit?.id}")
                            
                            // Check if this is a normal deploy operation
                            if (selectedUnit != null && canManage && isActive && unit == null) {
                                Log.d("BoardSlot", "✅ DEPLOY: Unit ${selectedUnit.id} to slot ${slot.name}")
                                onDeploy(selectedUnit.id)
                            }
                            // Check if this is a recall operation
                            else if (unit != null && canManage) {
                                Log.d("BoardSlot", "✅ RECALL: From slot ${slot.name}")
                                onRecall()
                            }
                            else {
                                Log.d("BoardSlot", "❌ NO TAP ACTION: Conditions not met")
                            }
                        }
                    )
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
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isActive -> Color(0xFF424242).copy(alpha = 0.5f)
                selectedUnit != null && unit == null -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                unit != null -> Color(unit.type.color).copy(alpha = 0.2f) // Màu theo tướng đang đặt
                else -> Color(0xFF2196F3).copy(alpha = 0.2f) // Màu xanh dương mặc định cho slot trống
            }
        ),
        border = when {
            !isActive -> BorderStroke(1.dp, Color.Gray)
            selectedUnit != null && unit == null -> BorderStroke(3.dp, Color(0xFF4CAF50))
            unit != null -> BorderStroke(2.dp, Color(unit.type.color)) // Border theo màu tướng
            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (unit != null) {
				// Ảnh động: thay khung hình theo thời gian để tạo chuyển động
				// Chỉ animate khi đang bắn: tạo một cửa sổ ngắn ngay sau khi bắn
                val shootingWindowMs = 250L
                var nowMs by remember(unit.id) { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(unit.id) {
                    while (true) {
                        nowMs = System.currentTimeMillis()
                        delay(50L)
                    }
                }
                val isShooting = (nowMs - unit.lastShotAtMs) <= shootingWindowMs
				AnimatedUnitImage(
					unit = unit,
					modifier = Modifier
						.fillMaxSize()
						.padding(2.dp),
					contentScale = ContentScale.FillBounds,
					isShooting = isShooting
				)
                
                // Stars overlay ở góc trên bên phải
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        repeat(unit.star.value) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                }
                
                // Tier overlay ở góc dưới bên trái
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "T${unit.tier.name.last()}",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
                
                // Cooldown indicator overlay ở dưới cùng
                if (unit.cooldownRemainingMs > 0) {
                    val progress = 1f - (unit.cooldownRemainingMs.toFloat() / unit.actualFireRateMs.toFloat())
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
                    } else if (selectedUnit != null && unit == null) {
                        Text(
                            text = "Click để deploy",
                            color = Color(0xFF4CAF50),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Component hiển thị unit đang được kéo
 */
@Composable
fun DraggingUnitOverlay(
    unit: com.baothanhbin.game2d.game.model.Unit?,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    if (unit != null && isDragging) {
        Box(
            modifier = modifier
                .zIndex(20f)
                .alpha(0.9f)
                .scale(1.2f)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1976D2).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 100.dp)
                ) {
					// Ảnh động cho overlay kéo thả
					AnimatedUnitImage(
						unit = unit,
						modifier = Modifier
							.fillMaxSize()
							.padding(4.dp),
						contentScale = ContentScale.FillBounds,
						isShooting = false
					)
                    
                    // Stars overlay ở góc trên bên phải
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            repeat(unit.star.value) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                    
                    // Tier overlay ở góc dưới bên trái
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "T${unit.tier.name.last()}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Component hiển thị drop zone
 */
@Composable
fun DropZoneIndicator(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF4CAF50).copy(alpha = 0.3f))
                .zIndex(5f)
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Thả tướng vào đây",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * Component hiển thị unit với ảnh golden thay vì text
 */
@Composable
fun UnitCard(
    unit: com.baothanhbin.game2d.game.model.Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(
                color = Color(unit.type.color).copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 2.dp,
                color = Color(unit.type.color),
                shape = RoundedCornerShape(6.dp)
            )
    ) {
		// Ảnh động lấp đầy toàn bộ card
		AnimatedUnitImage(
			unit = unit,
			modifier = Modifier
				.fillMaxSize()
				.padding(1.dp),
			contentScale = ContentScale.FillBounds,
			isShooting = false
		)
        
        // Stars overlay ở góc trên bên phải
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
                        tint = Color.Unspecified,
                        modifier = Modifier.size(6.dp)
                    )
                }
            }
        }
        
        // Tier overlay ở góc dưới bên trái
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(2.dp)
        ) {
            Text(
                text = "T${unit.tier.name.last()}",
                color = Color.White,
                fontSize = 6.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(horizontal = 2.dp, vertical = 1.dp)
            )
        }
    }
}

@Preview
@Composable
fun DraggableBenchSlotPreview() {
    DraggableBenchSlot(
        unit = com.baothanhbin.game2d.game.model.Unit.create(
            type = HeroType.KIM,
            tier = Tier.T2
        ),
        canManage = true,
        isSelected = false,
        isDragging = false,
        onSelect = {},
        onSell = {},
        onDragStart = { _, _ -> },
        onDragEnd = {}
    )
}