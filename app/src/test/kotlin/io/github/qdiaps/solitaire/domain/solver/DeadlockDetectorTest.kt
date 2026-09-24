package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DeadlockDetectorTest {

    private val aceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val twoHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val threeHearts = Card(Suit.HEARTS, Rank.THREE, isFaceUp = true)
    private val fourHearts = Card(Suit.HEARTS, Rank.FOUR, isFaceUp = true)
    private val kingHearts = Card(Suit.HEARTS, Rank.KING, isFaceUp = true)

    private val aceClubs = Card(Suit.CLUBS, Rank.ACE, isFaceUp = true)
    private val twoClubs = Card(Suit.CLUBS, Rank.TWO, isFaceUp = true)
    private val threeClubs = Card(Suit.CLUBS, Rank.THREE, isFaceUp = true)
    private val fourClubs = Card(Suit.CLUBS, Rank.FOUR, isFaceUp = true)
    private val fiveClubs = Card(Suit.CLUBS, Rank.FIVE, isFaceUp = true)
    private val sevenClubs = Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = true)
    private val kingClubs = Card(Suit.CLUBS, Rank.KING, isFaceUp = true)

    private val aceSpades = Card(Suit.SPADES, Rank.ACE, isFaceUp = true)
    private val twoSpades = Card(Suit.SPADES, Rank.TWO, isFaceUp = true)
    private val fiveSpades = Card(Suit.SPADES, Rank.FIVE, isFaceUp = true)
    private val sixSpades = Card(Suit.SPADES, Rank.SIX, isFaceUp = true)

    private val aceDiamonds = Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true)
    private val twoDiamonds = Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = true)
    private val sevenDiamonds = Card(Suit.DIAMONDS, Rank.SEVEN, isFaceUp = true)
    private val eightDiamonds = Card(Suit.DIAMONDS, Rank.EIGHT, isFaceUp = true)

    @Nested
    @DisplayName("Active Playable Game States")
    inner class ActiveGameTests {

        @Test
        @DisplayName("Returns ActiveGame when game is already won")
        fun `game won is not deadlocked`() {
            val fullFoundations = Suit.entries.map { suit ->
                Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
            }
            val wonState = BoardState(foundations = fullFoundations)

            val status = DeadlockDetector.detect(wonState)

            assertEquals(DeadlockStatus.ActiveGame, status)
            assertFalse(DeadlockDetector.isDeadlocked(wonState))
        }

        @Test
        @DisplayName("Returns ActiveGame when tableau card can move to foundation")
        fun `tableau to foundation move keeps game active`() {
            val state = BoardState(
                tableau = listOf(
                    listOf(aceHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state)

            assertEquals(DeadlockStatus.ActiveGame, status)
            assertFalse(status.isDeadlocked)
        }

        @Test
        @DisplayName("Returns ActiveGame when waste card can move to foundation")
        fun `waste to foundation move keeps game active`() {
            val state = BoardState(
                waste = listOf(aceSpades),
                tableau = listOf(
                    listOf(fourHearts),
                    listOf(sixSpades),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state)

            assertEquals(DeadlockStatus.ActiveGame, status)
        }

        @Test
        @DisplayName("Returns ActiveGame when waste card can move to tableau")
        fun `waste to tableau move keeps game active`() {
            // fourHearts in waste can move onto fiveSpades in tableau
            val state = BoardState(
                waste = listOf(fourHearts),
                tableau = listOf(
                    listOf(fiveSpades),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state)

            assertEquals(DeadlockStatus.ActiveGame, status)
        }

        @Test
        @DisplayName("Returns ActiveGame when tableau sequence move reveals a hidden card")
        fun `tableau sequence uncovers face down card keeps game active`() {
            val hiddenCard = Card(Suit.SPADES, Rank.TEN, isFaceUp = false)
            // Column 0: hiddenCard, fourHearts. Column 1: fiveClubs. fourHearts moves to fiveClubs revealing hiddenCard.
            val state = BoardState(
                tableau = listOf(
                    listOf(hiddenCard, fourHearts),
                    listOf(fiveClubs),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state)

            assertEquals(DeadlockStatus.ActiveGame, status)
        }

        @Test
        @DisplayName("Returns ActiveGame when tableau move empties a column")
        fun `tableau move emptying column is productive`() {
            // Column 0 has fourHearts (single card). Column 1 has fiveClubs.
            // Moving fourHearts to Column 1 empties Column 0.
            val state = BoardState(
                tableau = listOf(
                    listOf(fourHearts),
                    listOf(fiveClubs),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state)

            assertEquals(DeadlockStatus.ActiveGame, status)
        }

        @Test
        @DisplayName("Returns ActiveGame when King with face-down cards underneath moves to empty column")
        fun `king uncovering hidden card to empty column is productive`() {
            val hiddenCard = Card(Suit.SPADES, Rank.TEN, isFaceUp = false)
            val state = BoardState(
                tableau = listOf(
                    listOf(hiddenCard, kingHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state)

            assertEquals(DeadlockStatus.ActiveGame, status)
        }

        @Test
        @DisplayName("Returns ActiveGame when playable card is reachable in stock (Draw 1)")
        fun `reachable stock card in Draw 1 keeps game active`() {
            // Stock has fourHearts; tableau has fiveClubs. In Draw 1, fourHearts will be drawn and can be played.
            val state = BoardState(
                stock = listOf(Card(Suit.SPADES, Rank.EIGHT, isFaceUp = false), fourHearts.copy(isFaceUp = false)),
                tableau = listOf(
                    listOf(fiveClubs),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state, drawMode = DrawMode.DRAW_ONE)

            assertEquals(DeadlockStatus.ActiveGame, status)
        }

        @Test
        @DisplayName("Returns ActiveGame when playable card is reachable in stock (Draw 3)")
        fun `reachable stock card in Draw 3 keeps game active`() {
            // In Draw 3, drawing 3 cards brings the 3rd card to the top of waste.
            // If the 3rd card is fourHearts, it can be placed on fiveClubs.
            val card1 = Card(Suit.SPADES, Rank.EIGHT, isFaceUp = false)
            val card2 = Card(Suit.SPADES, Rank.NINE, isFaceUp = false)
            val playableCard = fourHearts.copy(isFaceUp = false)

            val state = BoardState(
                stock = listOf(card1, card2, playableCard),
                tableau = listOf(
                    listOf(fiveClubs),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state, drawMode = DrawMode.DRAW_THREE)

            assertEquals(DeadlockStatus.ActiveGame, status)
        }
    }

    @Nested
    @DisplayName("Exhausted Stock Deadlock Detection")
    inner class ExhaustedStockDeadlockTests {

        @Test
        @DisplayName("Detects deadlock when stock and waste are empty and no tableau moves exist")
        fun `exhausted stock and locked tableau triggers EXHAUSTED_STOCK deadlock`() {
            // Two cards that cannot move to foundation (Rank 4 and Rank 6), cannot move between columns (different colors, but rank difference != 1: 4 on 6 is illegal)
            val state = BoardState(
                stock = emptyList(),
                waste = emptyList(),
                tableau = listOf(
                    listOf(fourHearts),
                    listOf(sixSpades),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state)

            val deadlock = assertInstanceOf(DeadlockStatus.Deadlock::class.java, status)
            assertEquals(DeadlockReason.EXHAUSTED_STOCK, deadlock.reason)
            assertTrue(status.isDeadlocked)
            assertTrue(DeadlockDetector.isDeadlocked(state))
        }

        @Test
        @DisplayName("Useless King shift between empty columns does not prevent deadlock")
        fun `useless lateral king shift between empty columns does not prevent deadlock`() {
            // King is at index 0 of column 0. All other columns are empty.
            // Moving King to column 1 leaves column 0 empty and column 1 with King - symmetric, uncovers nothing.
            val state = BoardState(
                stock = emptyList(),
                waste = emptyList(),
                tableau = listOf(
                    listOf(kingHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state)

            val deadlock = assertInstanceOf(DeadlockStatus.Deadlock::class.java, status)
            assertEquals(DeadlockReason.EXHAUSTED_STOCK, deadlock.reason)
        }

        @Test
        @DisplayName("Useless equivalent parent shift does not prevent deadlock")
        fun `useless equivalent parent shift does not prevent deadlock`() {
            // Column 0: eightDiamonds, sevenClubs
            // Column 1: eightHearts
            // Moving sevenClubs from eightDiamonds (red 8) to eightHearts (red 8) uncovers no hidden card, does not empty column.
            val eightHearts = Card(Suit.HEARTS, Rank.EIGHT, isFaceUp = true)
            val state = BoardState(
                stock = emptyList(),
                waste = emptyList(),
                tableau = listOf(
                    listOf(eightDiamonds, sevenClubs),
                    listOf(eightHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state)

            val deadlock = assertInstanceOf(DeadlockStatus.Deadlock::class.java, status)
            assertEquals(DeadlockReason.EXHAUSTED_STOCK, deadlock.reason)
        }
    }

    @Nested
    @DisplayName("Stock Cycle Exhausted Deadlock Detection")
    inner class StockCycleExhaustedDeadlockTests {

        @Test
        @DisplayName("Detects deadlock when stock has cards but none can be played (Draw 1)")
        fun `unplayable stock in Draw 1 triggers STOCK_CYCLE_EXHAUSTED deadlock`() {
            // Stock has fourClubs and sixSpades.
            // Tableau has fourHearts and sevenClubs.
            // Neither fourClubs nor sixSpades can be placed on fourHearts or sevenClubs or empty foundation.
            val state = BoardState(
                stock = listOf(fourClubs.copy(isFaceUp = false), sixSpades.copy(isFaceUp = false)),
                waste = emptyList(),
                tableau = listOf(
                    listOf(fourHearts),
                    listOf(sevenClubs),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state, drawMode = DrawMode.DRAW_ONE)

            val deadlock = assertInstanceOf(DeadlockStatus.Deadlock::class.java, status)
            assertEquals(DeadlockReason.STOCK_CYCLE_EXHAUSTED, deadlock.reason)
        }

        @Test
        @DisplayName("Detects deadlock when stock cycles infinitely without moves across multiple recycles")
        fun `stock and waste cycle without moves triggers STOCK_CYCLE_EXHAUSTED`() {
            val state = BoardState(
                stock = listOf(fourClubs.copy(isFaceUp = false)),
                waste = listOf(sixSpades),
                tableau = listOf(
                    listOf(fourHearts),
                    listOf(sevenClubs),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state, drawMode = DrawMode.DRAW_ONE)

            val deadlock = assertInstanceOf(DeadlockStatus.Deadlock::class.java, status)
            assertEquals(DeadlockReason.STOCK_CYCLE_EXHAUSTED, deadlock.reason)
        }

        @Test
        @DisplayName("Buried card in Draw 3 mode is inaccessible and triggers deadlock")
        fun `buried card in Draw 3 inaccessible triggers deadlock`() {
            // In Draw 3 mode, with stock of 3 cards, only the 3rd card is at the top of waste.
            // Cards at index 0 and 1 are buried under index 2.
            // If card 0 could legally move (fourHearts on fiveClubs), BUT cards 1 and 2 cannot move,
            // then card 0 is NEVER accessible at the top of waste!
            val playableBuriedCard = fourHearts.copy(isFaceUp = false) // index 0 (buried)
            val unplayableCard1 = Card(Suit.SPADES, Rank.EIGHT, isFaceUp = false) // index 1 (buried)
            val unplayableCard2 = Card(Suit.SPADES, Rank.TEN, isFaceUp = false) // index 2 (exposed on top of waste)

            val state = BoardState(
                stock = listOf(playableBuriedCard, unplayableCard1, unplayableCard2),
                waste = emptyList(),
                tableau = listOf(
                    listOf(fiveClubs),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val status = DeadlockDetector.detect(state, drawMode = DrawMode.DRAW_THREE)

            val deadlock = assertInstanceOf(DeadlockStatus.Deadlock::class.java, status)
            assertEquals(DeadlockReason.STOCK_CYCLE_EXHAUSTED, deadlock.reason)
        }
    }
}
