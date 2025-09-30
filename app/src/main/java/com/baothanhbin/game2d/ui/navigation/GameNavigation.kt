package com.baothanhbin.game2d.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.baothanhbin.game2d.game.model.Difficulty
import com.baothanhbin.game2d.ui.home.HomeScreen
import com.baothanhbin.game2d.ui.splash.SplashScreen

/**
 * Navigation cho game
 */
@Composable
fun GameNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onDifficultySelected = { difficulty ->
                    navController.navigate(Screen.Home.createRoute(difficulty)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.Home.route,
            arguments = Screen.Home.arguments
        ) { backStackEntry ->
            val difficultyName = backStackEntry.arguments?.getString("difficulty") ?: "NORMAL"
            val difficulty = try {
                Difficulty.valueOf(difficultyName)
            } catch (e: Exception) {
                Difficulty.NORMAL
            }
            
            HomeScreen(
                difficulty = difficulty,
                onBackToSplash = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Định nghĩa các màn hình
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    
    object Home : Screen("home/{difficulty}") {
        fun createRoute(difficulty: Difficulty) = "home/${difficulty.name}"
        
        val arguments = listOf(
            androidx.navigation.navArgument("difficulty") {
                type = androidx.navigation.NavType.StringType
                defaultValue = "NORMAL"
            }
        )
    }
}
