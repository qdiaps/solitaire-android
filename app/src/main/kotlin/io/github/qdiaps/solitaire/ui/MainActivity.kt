package io.github.qdiaps.solitaire.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.qdiaps.solitaire.ui.game.GameViewModel
import io.github.qdiaps.solitaire.ui.game.SolitaireGameScreen

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolitaireGameScreen(viewModel = gameViewModel)
        }
    }
}
