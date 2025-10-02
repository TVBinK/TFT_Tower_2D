package com.baothanhbin.game2d.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baothanhbin.game2d.game.model.Difficulty

/**
 * Màn hình splash - chọn độ khó
 */
@Composable
fun SplashScreen(
    onDifficultySelected: (Difficulty) -> kotlin.Unit
) {
    var showConfirmDialog by remember { mutableStateOf<Difficulty?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3C72),
                        Color(0xFF2A5298),
                        Color(0xFF1E3C72)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo/Title
            Text(
            text = "TOWER DEFENSE\nTFT STYLE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Difficulty buttons
            Text(
                text = "Chọn độ khó:",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DifficultyButton(
                    difficulty = Difficulty.EASY,
                    color = Color(0xFF4CAF50),
                    onClick = { showConfirmDialog = Difficulty.EASY }
                )
                
                DifficultyButton(
                    difficulty = Difficulty.NORMAL,
                    color = Color(0xFFFF9800),
                    onClick = { showConfirmDialog = Difficulty.NORMAL }
                )
                
                DifficultyButton(
                    difficulty = Difficulty.HARD,
                    color = Color(0xFFF44336),
                    onClick = { showConfirmDialog = Difficulty.HARD }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Game info
            Text(
                text = "• 5 hệ tướng: Kim, Mộc, Thủy, Hỏa, Thổ\n" +
                      "• Mua tướng, reroll shop, nâng sao\n" +
                      "• Kinh tế và level system\n" +
                      "• Bắn đạn thẳng đứng, enemy rơi xuống",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
    
    // Confirm dialog
    showConfirmDialog?.let { difficulty ->
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            title = {
                Text(
                    text = "Xác nhận",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Bắt đầu game với độ khó ${difficulty.displayName}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = null
                        onDifficultySelected(difficulty)
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DifficultyButton(
    difficulty: Difficulty,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = difficulty.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = getDifficultyDescription(difficulty),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

private fun getDifficultyDescription(difficulty: Difficulty): String {
    return when (difficulty) {
        Difficulty.EASY -> "HP -20%, Tốc độ -10%"
        Difficulty.NORMAL -> "Cân bằng"
        Difficulty.HARD -> "HP +30%, Tốc độ +10%"
    }
}
