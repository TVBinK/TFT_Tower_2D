package com.baothanhbin.game2d.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.baothanhbin.game2d.game.model.Season
import com.baothanhbin.game2d.game.datastore.GameDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

/**
 * SplashScreen mới: background bg_splash, selector mùa ở giữa với mũi tên trái/phải, nút Play, 2 nút dưới (Characters, Survival)
 */
@Composable
fun SplashScreen(
    onPlay: (Season) -> Unit = {},
    onOpenCharacters: () -> Unit = {},
    onOpenSurvival: () -> Unit = {},
    onSeasonChanged: (Season) -> Unit = {}
) {
    val context = LocalContext.current
    val gameDataStore = remember { GameDataStore(context) }
    val coroutineScope = rememberCoroutineScope()
    val soundOn by gameDataStore.soundEnabled.collectAsState(initial = true)

    var showLoading by remember { mutableStateOf(false) }
    var currentSeason by remember { mutableStateOf(Season.SPRING) }
    var pendingNavigate by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Music is global now; SplashScreen no longer owns a MediaPlayer

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background splash image
        Image(
            painter = painterResource(id = com.baothanhbin.game2d.R.drawable.bg_splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        val soundIcon = if (soundOn)
            com.baothanhbin.game2d.R.drawable.ic_sound_on
        else
            com.baothanhbin.game2d.R.drawable.ic_sound_off
        // Sound toggle in the top-right corner
        Image(
            painter = painterResource(id = soundIcon),
            contentDescription = "Sound",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 16.dp)
                .size(40.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                    coroutineScope.launch {
                        gameDataStore.setSoundEnabled(!soundOn)
                    }
                },
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .background(Color.Transparent)
            ) {
                //Fairy Bloom
                Spacer(Modifier.height(24.dp))
                val iconTag = when (currentSeason) {
                    Season.SPRING -> com.baothanhbin.game2d.R.drawable.ic_spring_tag
                    Season.SUMMER -> com.baothanhbin.game2d.R.drawable.ic_summner_tag
                    Season.AUTUMN -> com.baothanhbin.game2d.R.drawable.ic_autumn_tag
                    Season.WINTER -> com.baothanhbin.game2d.R.drawable.ic_winter_tag
                }
                Image(
                    painter = painterResource(id = iconTag),
                    contentDescription = "spring tag",
                    modifier = Modifier
                        .width(200.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.baothanhbin.game2d.R.drawable.ic_prev),
                        contentDescription = "Previous",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(8.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }) {
                                currentSeason = currentSeason.prev()
                                onSeasonChanged(currentSeason)
                            }
                    )
                    // Season card
                    Card(
                        modifier = Modifier.size(180.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val icon = when (currentSeason) {
                                Season.SPRING -> com.baothanhbin.game2d.R.drawable.ic_spring
                                Season.SUMMER -> com.baothanhbin.game2d.R.drawable.ic_summner
                                Season.AUTUMN -> com.baothanhbin.game2d.R.drawable.ic_autumn
                                Season.WINTER -> com.baothanhbin.game2d.R.drawable.ic_winter
                            }
                            Image(
                                painter = painterResource(id = icon),
                                contentDescription = "Season icon",
                                modifier = Modifier.size(180.dp)
                            )
                        }
                    }
                    Image(
                        painter = painterResource(id = com.baothanhbin.game2d.R.drawable.ic_next),
                        contentDescription = "Next",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(8.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }) {
                                currentSeason = currentSeason.next()
                                onSeasonChanged(currentSeason)
                            }
                    )
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Play button
            Button(
                onClick = {
                    pendingNavigate = { onPlay(currentSeason) }
                    showLoading = true
                },
                modifier = Modifier
                    .width(160.dp)
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            ) {
                Icon(
                    painter = painterResource(id = com.baothanhbin.game2d.R.drawable.ic_play),
                    contentDescription = "Play",
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom buttons (Characters / Survival) sát đáy, 2 ảnh sát 2 bên
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = com.baothanhbin.game2d.R.drawable.ic_survival),
                modifier = Modifier
                    .height(100.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        pendingNavigate = { onOpenSurvival() }
                        showLoading = true
                    },
                contentDescription = "Survival"
            )
            Image(
                painter = painterResource(id = com.baothanhbin.game2d.R.drawable.ic_high_score),
                modifier = Modifier
                    .height(100.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) { onOpenCharacters() },
                contentDescription = "Characters"
            )
        }

        if (showLoading) {
            LoadingDialog(
                onDismiss = { showLoading = false; pendingNavigate = null },
                onNavigate = { (pendingNavigate ?: { onPlay(currentSeason) }).invoke() }
            )
        }
    }
}

@Composable
private fun LoadingDialog(
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = { }) {
        // Transparent backdrop dialog content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.baothanhbin.game2d.R.raw.waitting))

            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    iterations = Int.MAX_VALUE,
                    modifier = Modifier.size(140.dp),
                    speed = 2.0f // Faster speed
                )
            } else {
                // Fallback nếu JSON không load được
                Text(
                    text = "Loading...",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
    }

    // Handle navigation and auto dismiss
    LaunchedEffect(Unit) {
        delay(1000)
        onNavigate() // Start navigation
        delay(100)
        onDismiss() // Hide loading dialog
    }
}


@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen(
        onPlay = {},
        onOpenCharacters = {},
        onOpenSurvival = {},
        onSeasonChanged = {}
    )
}
