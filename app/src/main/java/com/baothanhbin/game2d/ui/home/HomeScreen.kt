package com.baothanhbin.game2d.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.baothanhbin.game2d.game.model.ColorTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baothanhbin.game2d.game.GameViewModel
import com.baothanhbin.game2d.game.model.Difficulty
import com.baothanhbin.game2d.ui.home.components.*

/**
 * Màn hình chính của game
 */
@Composable
fun HomeScreen(
    difficulty: Difficulty,
    onBackToSplash: () -> kotlin.Unit,
    viewModel: GameViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    
    // Use gameState directly to ensure UI updates properly
    
    // Auto-start game khi vào màn hình
    LaunchedEffect(difficulty) {
        try {
            viewModel.startGame(difficulty)
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
                onForceCombat = viewModel::forceCombatPhase,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            // Bottom Panel - Shop, Bench, Board
            BottomPanel(
                gameState = gameState,
                onBuyUnit = viewModel::buyUnit,
                onSellUnit = viewModel::sellUnit,
                onRerollShop = viewModel::rerollShop,
                onBuyXP = viewModel::buyXP,
                onDeployUnit = viewModel::deployUnit,
                onRecallUnit = viewModel::recallUnit,
                onSwapUnit = viewModel::swapUnit,
                onStartCombat = viewModel::startCombat,
                onOpenShop = { showShopOverlay = true },
                colorTheme = ColorTheme.WINTER, // TODO: Sẽ được thay đổi động dựa trên map được chọn
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Game Over Dialog
        if (gameState.isGameOver) {
            GameOverDialog(
                score = gameState.player.score,
                wave = gameState.player.wave,
                onRestart = viewModel::restartGame,
                onBackToSplash = onBackToSplash
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
