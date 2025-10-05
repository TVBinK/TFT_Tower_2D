package com.baothanhbin.game2d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.baothanhbin.game2d.ui.navigation.GameNavigation
import com.baothanhbin.game2d.ui.theme.Game2DTheme
import com.baothanhbin.game2d.media.MusicManager
import com.baothanhbin.game2d.game.repo.GameDataStore
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Game2DTheme {
                val dataStore = GameDataStore(this@MainActivity)
                val soundOn by dataStore.soundEnabled.collectAsState(initial = true)
                LaunchedEffect(Unit) {
                    MusicManager.initialize(this@MainActivity)
                }
                LaunchedEffect(soundOn) {
                    MusicManager.setEnabled(this@MainActivity, soundOn)
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameNavigation(context = this@MainActivity)
                }
            }
        }
    }
}