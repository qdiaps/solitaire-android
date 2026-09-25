package io.github.qdiaps.solitaire.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import io.github.qdiaps.solitaire.domain.deck.KlondikeDealer
import io.github.qdiaps.solitaire.ui.game.SolitaireGameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sampleState = remember { KlondikeDealer.dealShuffled() }
            SolitaireGameScreen(
                boardState = sampleState,
                timeSeconds = 0L,
                canUndo = false
            )
        }
    }
}
