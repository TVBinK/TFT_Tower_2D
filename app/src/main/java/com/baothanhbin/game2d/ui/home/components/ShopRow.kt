package com.baothanhbin.game2d.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baothanhbin.game2d.game.model.Shop
import com.baothanhbin.game2d.game.model.ShopSlot


/**
 * Hàng shop - 5 ô shop slots
 */
@Composable
fun ShopRow(
    shop: Shop,
    playerGold: Int,
    canBuy: (Int) -> Boolean,
    onBuyUnit: (Int) -> kotlin.Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Shop - Mua tướng (Roll #${shop.rollCount})",
            color = Color(0xFF2196F3),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            shop.slots.forEachIndexed { index, slot ->
                ShopSlot(
                    slot = slot,
                    slotIndex = index,
                    canBuy = canBuy(index),
                    playerGold = playerGold,
                    onBuy = { onBuyUnit(index) },
                    modifier = Modifier
                        .width(UiDimens.SHOP_SLOT_WIDTH)
                        .height(UiDimens.SHOP_SLOT_HEIGHT)
                )
            }
        }
    }
}

@Composable
private fun ShopSlot(
    slot: ShopSlot,
    slotIndex: Int,
    canBuy: Boolean,
    playerGold: Int,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(UiDimens.SHOP_SLOT_WIDTH)
            .height(UiDimens.SHOP_SLOT_HEIGHT)
            .then(
                if (slot.unit != null && canBuy) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBuy() }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (slot.unit != null) {
                Color(0xFF1E1E1E) // Màu xám đậm giống bench khi có tướng
            } else {
                Color(0xFF424242) // Màu xám nhạt giống bench khi trống
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (slot.isEmpty) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Shop ${slotIndex + 1}",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                slot.unit?.let { unit ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Sử dụng UnitCard với kích thước phù hợp cho ShopSlot
                        UnitCard(
                            unit = unit,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Hiển thị giá
                        Text(
                            text = "${slot.price}g",
                            color = if (playerGold >= slot.price) Color(0xFFFFD700) else Color(0xFFFF5722),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun ShopRowPreview() {
    val shop = remember {
        Shop(
            slots = listOf(
                ShopSlot(
                    unit = null,
                    price = 5
                ),
                ShopSlot(
                    unit = null,
                    price = 5
                ),
                ShopSlot(
                    unit = null,
                    price = 0
                ),
                ShopSlot(
                    unit = null,
                    price = 5
                ),
                ShopSlot(
                    unit = null,
                    price = 5
                )
            ),
            rollCount = 1
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        ShopRow(
            shop = shop,
            playerGold = 10,
            canBuy = { index -> shop.slots[index].unit != null },
            onBuyUnit = { index -> /* Xử lý mua tướng */ }
        )
    }
}
