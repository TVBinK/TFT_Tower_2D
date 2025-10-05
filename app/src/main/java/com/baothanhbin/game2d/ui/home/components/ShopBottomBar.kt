package com.baothanhbin.game2d.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.baothanhbin.game2d.R
import com.baothanhbin.game2d.game.model.GameState
import com.baothanhbin.game2d.game.model.HeroType
import com.baothanhbin.game2d.game.model.ShopSlot
import com.baothanhbin.game2d.ui.theme.tangoRegular
import com.baothanhbin.game2d.game.model.HeroDescription
import kotlin.math.min

@Composable
fun ShopBottomBar(
    visible: Boolean,
    gameState: GameState,
    onReroll: () -> Unit,
    onBuyUnit: (Int) -> Unit,
    onClose: () -> Unit
) {
    if (!visible) return
    
    // Auto reroll when new day starts
    LaunchedEffect(gameState.dayNumber) {
        if (gameState.dayNumber > 1 && gameState.player.freeRerollsRemaining > 0) {
            onReroll()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(bottom = 20.dp)
            .zIndex(50f)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color(0xFF1E1E1E).copy(alpha = 0.5f),
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shop",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 50.dp, height = 50.dp)
                                .clip(RectangleShape)
                                .clickable(
                                    enabled = gameState.canReroll(),
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = rememberRipple(bounded = true),
                                    onClick = onReroll
                                )
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_refesh),
                                contentDescription = "Reroll",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        }


                        TextButton(
                            onClick = onClose,
                            modifier = Modifier.size(width = 30.dp, height = 30.dp),
                            enabled = true,
                            contentPadding = PaddingValues(0.dp),
                            shape = RectangleShape,
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Unspecified,
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = Color.Unspecified
                            )
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Close",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    }
                }

                (0 until min(3, gameState.shop.slots.size)).forEach { idx ->
                    val slot = gameState.shop.slots.getOrNull(idx)
                    if (slot != null) {
                        if (slot.isEmpty) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            ) { }
                        } else {
                            ShopEntryRow(
                                slot = slot,
                                slotIndex = idx,
                                playerGold = gameState.player.gold,
                                canBuy = gameState.canBuyUnit(idx),
                                onBuy = { onBuyUnit(idx) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopEntryRow(
    slot: ShopSlot,
    slotIndex: Int,
    playerGold: Int,
    canBuy: Boolean,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image for the card
            Image(
                painter = painterResource(id = R.drawable.bg_full_tag_shop),
                contentDescription = "Shop entry background",
                modifier = Modifier
                    .fillMaxWidth(),
                contentScale = ContentScale.FillBounds
            )

             // Content box that overlays on top of the background image
             Box(
                 modifier = Modifier
                     .fillMaxSize()
                     .padding(top = 16.dp)
                     .clickable(
                         enabled = slot.unit != null && canBuy,
                         interactionSource = remember { MutableInteractionSource() },
                         indication = LocalIndication.current
                     ) { onBuy() },
                 contentAlignment = Alignment.Center
             ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left framed artwork - whole row acts as purchase button
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                         Image(
                             painter = painterResource(
                                 id = when (slot.unit?.type) {
                                     HeroType.METAL -> R.drawable.hero_metal
                                     HeroType.FLOWER -> R.drawable.hero_flower
                                     HeroType.WATER -> R.drawable.hero_water
                                     HeroType.FIRE -> R.drawable.hero_fire
                                     HeroType.ICE -> R.drawable.hero_ice
                                     null -> R.drawable.hero_metal
                                 }
                             ),
                             contentDescription = slot.unit?.type?.displayName ?: "Unit",
                             modifier = Modifier
                                 .width(90.dp)
                                 .height(120.dp),
                             contentScale = ContentScale.FillBounds
                         )
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        val heroDescription = HeroDescription.getByHeroType(slot.unit?.type) ?: HeroDescription.getDefault()
                        
                        Text(
                            text = heroDescription.displayName,
                            fontFamily = tangoRegular,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (!slot.isEmpty) {
                            Text(
                                text = heroDescription.star1Description,
                                fontFamily = tangoRegular,
                                color = Color(0xFFCFCFCF),
                                fontSize = 17.sp
                            )
                            Text(
                                text = heroDescription.star2Description,
                                fontFamily = tangoRegular,
                                color = Color(0xFFCFCFCF),
                                fontSize = 17.sp
                            )
                            Text(
                                text = heroDescription.star3Description,
                                fontFamily = tangoRegular,
                                color = Color(0xFFCFCFCF),
                                fontSize = 17.sp
                            )
                        }
                    }
                    // Bỏ nút Mua riêng, dùng click toàn hàng
                }
            }
            // Price badge on top-right (only show when slot has a unit)
            if (!slot.isEmpty) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${slot.price}",
                        fontFamily = tangoRegular,
                        color = if (playerGold >= slot.price) Color(0xFF000000) else Color(
                            0xFFFF6E40
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ShopBottomBarPreview() {
    ShopBottomBar(
        visible = true,
        gameState = GameState.sample(),
        onReroll = {},
        onBuyUnit = {},
        onClose = {}
    )
}
