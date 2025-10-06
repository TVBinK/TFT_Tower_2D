package com.baothanhbin.game2d.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.baothanhbin.game2d.game.model.ColorTheme
import com.baothanhbin.game2d.game.model.GameMode
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baothanhbin.game2d.game.GameViewModel
import com.baothanhbin.game2d.game.model.Season
import com.baothanhbin.game2d.ui.home.components.*
import android.media.MediaPlayer
import androidx.compose.ui.platform.LocalContext
import com.baothanhbin.game2d.game.datastore.GameDataStore
import androidx.compose.runtime.rememberCoroutineScope
import com.baothanhbin.game2d.game.model.SoundEvent
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode

/**
 * Màn hình chính của game
 */
@Composable
fun HomeScreen(
    season: Season,
    onBackToSplash: () -> kotlin.Unit,
    viewModel: GameViewModel = viewModel(),
    mode: GameMode = GameMode.CAMPAIGN
) {
    val gameState by viewModel.gameState.collectAsState()
    val context = LocalContext.current
    val gameDataStore = remember { GameDataStore(context) }
    val coroutineScope = rememberCoroutineScope()
    val soundOn by gameDataStore.soundEnabled.collectAsState(initial = true)
    
    // Map season to ColorTheme
    val colorTheme = when (season) {
        Season.SPRING -> ColorTheme.SPRING
        Season.SUMMER -> ColorTheme.SUMMER
        Season.AUTUMN -> ColorTheme.AUTUMN
        Season.WINTER -> ColorTheme.WINTER
    }
    
    // Use gameState directly to ensure UI updates properly
    
    // Auto-start game khi vào màn hình
    LaunchedEffect(mode) {
        try {
            viewModel.startGame(mode)
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error starting game: ${e.message}", e)
        }
    }
    
    var showShopOverlay by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E1A0A)) // Background sa mạc
    ) {
        // One-shot SFX players
        var gameOverPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
        var victoryPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

        // Play game over sound once when isGameOver turns true
        LaunchedEffect(gameState.isGameOver, soundOn) {
            if (gameState.isGameOver && soundOn) {
                try {
                    // Stop any existing instance first
                    gameOverPlayer?.let { p ->
                        try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                        try { p.release() } catch (_: Exception) {}
                    }
                    val player = MediaPlayer.create(context, com.baothanhbin.game2d.R.raw.sound_game_over)
                    gameOverPlayer = player
                    player.setOnCompletionListener {
                        try { it.release() } catch (_: Exception) {}
                        gameOverPlayer = null
                    }
                    player.start()
                } catch (_: Exception) { }
            }
            if (!soundOn) {
                gameOverPlayer?.let { p ->
                    try { if (p.isPlaying) p.pause() } catch (_: Exception) {}
                }
            }
        }

        // Play victory sound once when isVictory turns true
        LaunchedEffect(gameState.isVictory, soundOn) {
            if (gameState.isVictory && soundOn) {
                try {
                    victoryPlayer?.let { p ->
                        try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                        try { p.release() } catch (_: Exception) {}
                    }
                    val player = MediaPlayer.create(context, com.baothanhbin.game2d.R.raw.sound_win)
                    victoryPlayer = player
                    player.setOnCompletionListener {
                        try { it.release() } catch (_: Exception) {}
                        victoryPlayer = null
                    }
                    player.start()
                } catch (_: Exception) { }
            }
            if (!soundOn) {
                victoryPlayer?.let { p ->
                    try { if (p.isPlaying) p.pause() } catch (_: Exception) {}
                }
            }
        }

        // Hero skill sound players
        var iceSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
        var waterSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
        var fireSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
        var bossWarnPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
        // One-shot red flash overlay driver
        val bossFlashAlpha = remember { Animatable(0f) }
        
        // Process sound events from game state
        LaunchedEffect(gameState.pendingSoundEvents, soundOn) {
            if (soundOn && gameState.pendingSoundEvents.isNotEmpty()) {
                gameState.pendingSoundEvents.forEach { event ->
                    when (event) {
                        SoundEvent.ICE_SKILL -> {
                            iceSoundPlayer?.let { p ->
                                try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                                try { p.release() } catch (_: Exception) {}
                            }
                            iceSoundPlayer = MediaPlayer.create(context, com.baothanhbin.game2d.R.raw.sound_ice).apply {
                                setOnCompletionListener { 
                                    try { release() } catch (_: Exception) {}
                                    iceSoundPlayer = null
                                }
                                start()
                            }
                        }
                        SoundEvent.WATER_SKILL -> {
                            waterSoundPlayer?.let { p ->
                                try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                                try { p.release() } catch (_: Exception) {}
                            }
                            waterSoundPlayer = MediaPlayer.create(context, com.baothanhbin.game2d.R.raw.sound_water).apply {
                                setOnCompletionListener { 
                                    try { release() } catch (_: Exception) {}
                                    waterSoundPlayer = null
                                }
                                start()
                            }
                        }
                        SoundEvent.FIRE_SKILL -> {
                            fireSoundPlayer?.let { p ->
                                try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                                try { p.release() } catch (_: Exception) {}
                            }
                            fireSoundPlayer = MediaPlayer.create(context, com.baothanhbin.game2d.R.raw.sound_fire).apply {
                                setOnCompletionListener { 
                                    try { release() } catch (_: Exception) {}
                                    fireSoundPlayer = null
                                }
                                start()
                            }
                        }
                        SoundEvent.BEFORE_BOSS -> {
                            bossWarnPlayer?.let { p ->
                                try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                                try { p.release() } catch (_: Exception) {}
                            }
                            bossWarnPlayer = MediaPlayer.create(context, com.baothanhbin.game2d.R.raw.sound_before_boss).apply {
                                setOnCompletionListener { 
                                    try { release() } catch (_: Exception) {}
                                    bossWarnPlayer = null
                                }
                                start()
                            }
                            // Trigger two slow red flashes
                            try {
                                bossFlashAlpha.snapTo(0f)
                                repeat(2) {
                                    bossFlashAlpha.animateTo(0.7f, tween(durationMillis = 160, easing = LinearEasing))
                                    bossFlashAlpha.animateTo(0f, tween(durationMillis = 220, easing = LinearEasing))
                                    // brief gap between flashes
                                    kotlinx.coroutines.delay(120L)
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }
                // Clear processed sound events
                viewModel.clearSoundEvents()
            }
        }
        

        // Release players when leaving this screen
        DisposableEffect(Unit) {
            onDispose {
                try {
                    gameOverPlayer?.let { p ->
                        try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                        try { p.release() } catch (_: Exception) {}
                    }
                } catch (_: Exception) { } finally {
                    gameOverPlayer = null
                }
                try {
                    victoryPlayer?.let { p ->
                        try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                        try { p.release() } catch (_: Exception) {}
                    }
                } catch (_: Exception) { } finally {
                    victoryPlayer = null
                }
                try {
                    iceSoundPlayer?.let { p ->
                        try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                        try { p.release() } catch (_: Exception) {}
                    }
                } catch (_: Exception) { } finally {
                    iceSoundPlayer = null
                }
                try {
                    waterSoundPlayer?.let { p ->
                        try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                        try { p.release() } catch (_: Exception) {}
                    }
                } catch (_: Exception) { } finally {
                    waterSoundPlayer = null
                }
                try {
                    fireSoundPlayer?.let { p ->
                        try { if (p.isPlaying) p.stop() } catch (_: Exception) {}
                        try { p.release() } catch (_: Exception) {}
                    }
                } catch (_: Exception) { } finally {
                    fireSoundPlayer = null
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // HUD - thông tin game ở trên
            GameHUD(
                gameState = gameState,
                onPauseToggle = viewModel::togglePause,
                onRestart = viewModel::restartGame,
                onBackToSplash = onBackToSplash,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Play Area - khu vực game chính
            PlayArea(
                gameState = gameState,
                season = season,
                onForceCombat = viewModel::forceCombatPhase,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            // Bottom Panel - Shop, Bench, Board
            BottomPanel(
                gameState = gameState,
                season = season,
                onBuyUnit = viewModel::buyUnit,
                onSellUnit = viewModel::sellUnit,
                onRerollShop = viewModel::rerollShop,
                onBuyXP = viewModel::buyXP,
                onDeployUnit = viewModel::deployUnit,
                onRecallUnit = viewModel::recallUnit,
                onSwapUnit = viewModel::swapUnit,
                onStartCombat = viewModel::startCombat,
                onOpenShop = { showShopOverlay = true },
                colorTheme = colorTheme,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Game Over Dialog
        if (gameState.isGameOver) {
            GameOverDialog(
                score = gameState.player.score,
                day = gameState.player.day,
                onRestart = viewModel::restartGame,
                onBackToSplash = onBackToSplash
            )
        }
        // Draw red flash overlay on top of everything
        if (bossFlashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = bossFlashAlpha.value))
            ) {}
        }
        // Victory Dialog (Campaign win)
        if (gameState.isVictory) {
            VictoryDialog(
                score = gameState.player.score,
                day = gameState.player.day,
                onBackToSplash = onBackToSplash,
                onPlayAgain = viewModel::restartGame
            )
        }
        
        // Pause Overlay
        if (gameState.isPaused && !gameState.isGameOver) {
            PauseOverlay(
                onResume = viewModel::togglePause,
                onRestart = viewModel::restartGame,
                onBackToSplash = onBackToSplash
            )
        }

        // Shop bottom bar overlay drawn above everything
        ShopBottomBar(
            visible = showShopOverlay,
            gameState = gameState,
            onReroll = viewModel::rerollShop,
            onBuyUnit = viewModel::buyUnit,
            onClose = { showShopOverlay = false }
        )
    }
}
