package com.baothanhbin.game2d.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baothanhbin.game2d.game.model.GameState
import com.baothanhbin.game2d.game.model.RoundPhase


/**
 * HUD hiển thị thông tin game ở trên màn hình
 */
@Composable
fun GameHUD(
    gameState: GameState,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onBackToSplash: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier,
        color = Color(0xFF1A1A1A).copy(alpha = 0.9f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Gold, Score, Wave, Lives
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HUDItem(
                    icon = Icons.Default.AttachMoney,
                    label = "Gold",
                    value = gameState.player.gold.toString(),
                    color = Color(0xFFFFD700)
                )
                
                HUDItem(
                    icon = Icons.Default.Star,
                    label = "Score",
                    value = gameState.player.score.toString(),
                    color = Color(0xFFFFA726)
                )
                
                HUDItem(
                    icon = Icons.Default.Favorite,
                    label = "Wave",
                    value = gameState.player.wave.toString(),
                    color = Color(0xFF00BCD4)
                )
                
                HUDItem(
                    icon = Icons.Default.Favorite,
                    label = "Lives",
                    value = gameState.player.lives.toString(),
                    color = if (gameState.player.lives <= 20) Color(0xFFFF5722) else Color(0xFF4CAF50)
                )
            }
            
            // Center - Round info
            RoundInfo(
                phase = gameState.roundPhase,
                roundNumber = gameState.roundNumber,
                enemiesKilled = gameState.enemiesKilled,
                totalEnemies = gameState.totalEnemiesPerRound
            )
            
            // Right side - Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause button
                IconButton(
                    onClick = onPauseToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (gameState.isPaused) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                        contentDescription = if (gameState.isPaused) "Resume" else "Pause",
                        tint = Color.White
                    )
                }
                
                // Menu button
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }
            }
        }
    }
    
    // Menu dropdown
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("Menu") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showMenu = false
                            onRestart()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restart Game")
                    }
                    
                    TextButton(
                        onClick = {
                            showMenu = false
                            onBackToSplash()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Menu")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun HUDItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        
        Column {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun RoundInfo(
    phase: RoundPhase,
    roundNumber: Int,
    enemiesKilled: Int,
    totalEnemies: Int
) {
    val phaseColor = when (phase) {
        RoundPhase.PREP -> Color(0xFF4CAF50)
        RoundPhase.COMBAT -> Color(0xFFFF5722)
    }
    
    val phaseText = when (phase) {
        RoundPhase.PREP -> "CHUẨN BỊ"
        RoundPhase.COMBAT -> "CHIẾN ĐẤU"
    }
    
    Card(
        modifier = Modifier
            .background(
                color = phaseColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp)
        ) {
            Text(
                text = "Round $roundNumber",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Text(
                text = phaseText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = phaseColor
            )
            
            if (phase == RoundPhase.COMBAT) {
                Text(
                    text = "$enemiesKilled/$totalEnemies",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Vô hạn",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun GameHUDPreview() {
    GameHUD(
        gameState = GameState(
            player = com.baothanhbin.game2d.game.model.Player(
                gold = 150,
                score = 1200,
                wave = 5,
                lives = 18
            ),
            roundPhase = RoundPhase.COMBAT,
            roundNumber = 5,
            enemiesKilled = 12,
            totalEnemiesPerRound = 20,
            isPaused = false
        ),
        onPauseToggle = {},
        onRestart = {},
        onBackToSplash = {}
    )
}