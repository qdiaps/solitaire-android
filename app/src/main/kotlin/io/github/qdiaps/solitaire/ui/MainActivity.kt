package io.github.qdiaps.solitaire.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.qdiaps.solitaire.ui.game.GameViewModel
import io.github.qdiaps.solitaire.ui.game.SolitaireGameScreen

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels { GameViewModel.provideFactory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            gameViewModel.uiState.value.isLoading
        }
        enableEdgeToEdge()
        setContent {
            SolitaireGameScreen(viewModel = gameViewModel)
        }
    }

    override fun onPause() {
        super.onPause()
        gameViewModel.pauseTimer()
        gameViewModel.saveCurrentSession()
    }

    override fun onResume() {
        super.onResume()
        gameViewModel.resumeTimer()
    }
}
