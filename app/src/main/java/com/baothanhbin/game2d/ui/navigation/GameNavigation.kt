package com.baothanhbin.game2d.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
 
import com.baothanhbin.game2d.ui.home.HomeScreen
import com.baothanhbin.game2d.ui.splash.SplashScreen
import com.baothanhbin.game2d.ui.highscore.HighScoreScreen
import com.baothanhbin.game2d.game.model.Season
import com.baothanhbin.game2d.game.model.GameMode
import com.baothanhbin.game2d.game.repo.GameDataStore
import android.content.Context

/**
 * Navigation cho game
 */
@Composable
fun GameNavigation(
    navController: NavHostController = rememberNavController(),
    context: Context
) {
    var selectedSeason by remember { mutableStateOf(Season.SPRING) }
    val gameDataStore = remember { GameDataStore(context) }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onPlay = { season ->
                    selectedSeason = season
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onOpenCharacters = {
                    navController.navigate(Screen.HighScore.route)
                },
                onOpenSurvival = {
                    // Navigate to Home but mark Survival mode via route param
                    navController.navigate(Screen.Home.route + "?mode=survival")
                },
                onSeasonChanged = { season ->
                    selectedSeason = season
                }
            )
        }
        
        composable(
            route = Screen.Home.route + "?mode={mode}",
        ) { backStackEntry ->
            val modeArg = backStackEntry.arguments?.getString("mode")
            val gameMode = if (modeArg == "survival") GameMode.SURVIVAL else GameMode.CAMPAIGN
            HomeScreen(
                season = selectedSeason,
                onBackToSplash = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                mode = gameMode
            )
        }
        
        composable(Screen.HighScore.route) {
            HighScoreScreen(
                gameDataStore = gameDataStore,
                onBack = {
                    navController.popBackStack()
                },
                season = selectedSeason
            )
        }
    }
}

/**
 * Định nghĩa các màn hình
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object HighScore : Screen("highscore")
}
