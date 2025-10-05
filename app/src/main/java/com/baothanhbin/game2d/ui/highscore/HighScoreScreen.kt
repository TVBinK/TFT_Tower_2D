package com.baothanhbin.game2d.ui.highscore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baothanhbin.game2d.R
import com.baothanhbin.game2d.game.model.Season
import com.baothanhbin.game2d.game.repo.GameDataStore
import com.baothanhbin.game2d.game.repo.HighScoreEntry

@Composable
fun HighScoreScreen(
    gameDataStore: GameDataStore,
    onBack: () -> Unit,
    season: Season = Season.SPRING
) {
    val top6Scores by gameDataStore.top6Scores.collectAsState(initial = emptyList())
    val bestScore by gameDataStore.bestScore.collectAsState(initial = 0L)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background based on season
        val backgroundResId = when (season) {
            Season.SPRING -> R.drawable.bg_splash
            Season.SUMMER -> R.drawable.bg_splash
            Season.AUTUMN -> R.drawable.bg_splash
            Season.WINTER -> R.drawable.bg_splash
        }
        
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
               Image(
                     painter = painterResource(id = R.drawable.high_score),
                     contentDescription = "Game Logo",
                     modifier = Modifier
                          .height(48.dp)
                          .clip(RoundedCornerShape(12.dp)),
                     contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
               )
                
                // Spacer to balance the layout
                Spacer(modifier = Modifier.size(48.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Best Score Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFD700).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BEST SCORE",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bestScore.toString(),
                        color = Color.Black,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Top 6 Scores List
            Text(
                text = "TOP 6 SCORES",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Create a list of 6 items (actual scores + empty slots)
                val displayItems = (0 until 6).map { index ->
                    if (index < top6Scores.size) {
                        top6Scores[index] to false // actual score, not empty
                    } else {
                        HighScoreEntry(0L, 0) to true // empty slot
                    }
                }
                
                itemsIndexed(displayItems) { index, (entry, isEmpty) ->
                    HighScoreItem(
                        rank = index + 1,
                        score = entry.score,
                        day = entry.day,
                        isTop3 = index < 3,
                        isEmpty = isEmpty
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer info
            Text(
                text = "Survive as many days as possible!\nDefeat bosses for higher scores!",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun HighScoreItem(
    rank: Int,
    score: Long,
    day: Int,
    isTop3: Boolean = false,
    isEmpty: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isEmpty -> Color.White.copy(alpha = 0.1f)
                isTop3 -> when (rank) {
                    1 -> Color(0xFFFFD700).copy(alpha = 0.8f) // Gold
                    2 -> Color(0xFFC0C0C0).copy(alpha = 0.8f) // Silver
                    3 -> Color(0xFFCD7F32).copy(alpha = 0.8f) // Bronze
                    else -> Color.White.copy(alpha = 0.3f)
                }
                else -> Color.White.copy(alpha = 0.2f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isTop3) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when {
                            isEmpty -> Color.Transparent
                            isTop3 -> Color.Black.copy(alpha = 0.3f)
                            else -> Color.Black.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEmpty) "-" else rank.toString(),
                    color = if (isEmpty) Color.White.copy(alpha = 0.5f) else Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (isTop3) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            // Score
            Text(
                text = if (isEmpty) "No Score" else score.toString(),
                color = if (isEmpty) Color.White.copy(alpha = 0.5f) else Color.White,
                fontSize = 18.sp,
                fontWeight = if (isTop3) FontWeight.Bold else FontWeight.Normal
            )
            
            // Days survived
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (isEmpty) "-" else "Day $day",
                    color = if (isEmpty) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = if (isTop3) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "Survived",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
@Preview
@Composable
fun HighScoreItemPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(16.dp)
    ) {
        HighScoreItem(rank = 1, score = 1500, day = 15, isTop3 = true)
        HighScoreItem(rank = 2, score = 1200, day = 12, isTop3 = true)
        HighScoreItem(rank = 3, score = 1000, day = 10, isTop3 = true)
        HighScoreItem(rank = 4, score = 800, day = 8)
        HighScoreItem(rank = 5, score = 600, day = 6)
        HighScoreItem(rank = 6, score = 0, day = 0, isEmpty = true)
    }
}
