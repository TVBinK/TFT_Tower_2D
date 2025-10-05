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
