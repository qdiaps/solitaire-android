package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KlondikeRulesScoringAndExposeTest {

    private val blackKing = Card(Suit.SPADES, Rank.KING, isFaceUp = true)
    private val redKing = Card(Suit.HEARTS, Rank.KING, isFaceUp = true)
    private val redQueen = Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
    private val blackQueen = Card(Suit.CLUBS, Rank.QUEEN, isFaceUp = true)
    private val blackJack = Card(Suit.SPADES, Rank.JACK, isFaceUp = true)
    private val redJack = Card(Suit.DIAMONDS, Rank.JACK, isFaceUp = true)
    private val aceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val twoHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val aceSpades = Card(Suit.SPADES, Rank.ACE, isFaceUp = true)

    @Nested
    @DisplayName("Scoring Constants")
    inner class ScoringConstantsTests {

        @Test
        @DisplayName("Score constants match Klondike specification")
        fun `score constants have correct values according to specification`() {
            assertEquals(5, KlondikeRules.SCORE_WASTE_TO_TABLEAU)
            assertEquals(10, KlondikeRules.SCORE_WASTE_TO_FOUNDATION)
            assertEquals(10, KlondikeRules.SCORE_TABLEAU_TO_FOUNDATION)
            assertEquals(5, KlondikeRules.SCORE_TURNOVER_TABLEAU_CARD)
            assertEquals(-15, KlondikeRules.SCORE_FOUNDATION_TO_TABLEAU)
        }
    }

    @Nested
    @DisplayName("Waste to Tableau Scoring")
    inner class WasteToTableauScoringTests {

        @Test
        @DisplayName("Moving card from waste to tableau adds 5 points")
        fun `move from waste to tableau adds 5 points`() {
            val state = BoardState(
                waste = listOf(redQueen),
                tableau = List(7) { col -> if (col == 0) listOf(blackKing) else emptyList() },
                score = 0
            )

            val newState = KlondikeRules.moveWasteToTableau(state, targetColumnIndex = 0)

            assertEquals(5, newState.score)
            assertEquals(1, newState.movesCount)
        }

        @Test
        @DisplayName("Moving King from waste to empty tableau column adds 5 points")
        fun `move King from waste to empty column adds 5 points`() {
            val state = BoardState(
                waste = listOf(blackKing),
                score = 25
            )

            val newState = KlondikeRules.moveWasteToTableau(state, targetColumnIndex = 3)

            assertEquals(30, newState.score)
        }
    }

    @Nested
    @DisplayName("Waste to Foundation Scoring")
    inner class WasteToFoundationScoringTests {

        @Test
        @DisplayName("Moving card from waste to foundation adds 10 points")
        fun `move from waste to foundation adds 10 points`() {
            val state = BoardState(
                waste = listOf(aceHearts),
                score = 15
            )

            val newState = KlondikeRules.moveWasteToFoundation(state, foundationIndex = 0)

            assertEquals(25, newState.score)
            assertEquals(1, newState.movesCount)
        }
    }

    @Nested
    @DisplayName("Tableau to Foundation Scoring & Auto-Expose")
    inner class TableauToFoundationScoringTests {

        @Test
        @DisplayName("Move tableau to foundation adds 10 points when source column becomes empty")
        fun `move tableau to foundation adds 10 points when column becomes empty`() {
            val state = BoardState(
                tableau = List(7) { col -> if (col == 0) listOf(aceHearts) else emptyList() },
                score = 0
            )

            val newState = KlondikeRules.moveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 0)

            assertEquals(10, newState.score)
            assertTrue(newState.tableau[0].isEmpty())
        }

        @Test
        @DisplayName("Move tableau to foundation adds 10 points when top remaining card was already face-up")
        fun `move tableau to foundation adds 10 points when top remaining card is already face-up`() {
            val state = BoardState(
                tableau = List(7) { col ->
                    if (col == 0) listOf(twoHearts, aceHearts) else emptyList()
                },
                score = 10
            )

            val newState = KlondikeRules.moveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 0)

            assertEquals(20, newState.score)
            assertEquals(listOf(twoHearts), newState.tableau[0])
            assertTrue(newState.tableau[0].last().isFaceUp)
        }

        @Test
        @DisplayName("Move tableau to foundation auto-exposes face-down card and adds 15 points (10 + 5)")
        fun `move tableau to foundation auto-exposes hidden card and awards 15 points`() {
            val hiddenCard = Card(Suit.SPADES, Rank.SEVEN, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    if (col == 0) listOf(hiddenCard, aceHearts) else emptyList()
                },
                score = 5
            )

            val newState = KlondikeRules.moveTableauToFoundation(state, tableauIndex = 0, foundationIndex = 0)

            assertEquals(20, newState.score) // 5 + 10 (foundation) + 5 (expose)
            assertEquals(1, newState.tableau[0].size)
            assertTrue(newState.tableau[0].last().isFaceUp)
            assertEquals(hiddenCard.id, newState.tableau[0].last().id)
        }

        @Test
        @DisplayName("Move tableau to foundation with autoExpose = false leaves card face-down and adds 10 points")
        fun `move tableau to foundation with autoExpose false leaves card hidden and awards only 10 points`() {
            val hiddenCard = Card(Suit.SPADES, Rank.SEVEN, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    if (col == 0) listOf(hiddenCard, aceHearts) else emptyList()
                },
                score = 5
            )

            val newState = KlondikeRules.moveTableauToFoundation(
                state = state,
                tableauIndex = 0,
                foundationIndex = 0,
                autoExpose = false
            )

            assertEquals(15, newState.score) // 5 + 10 (foundation only)
            assertEquals(1, newState.tableau[0].size)
            assertFalse(newState.tableau[0].last().isFaceUp)
        }
    }

    @Nested
    @DisplayName("Tableau to Tableau Scoring & Auto-Expose")
    inner class TableauToTableauScoringTests {

        @Test
        @DisplayName("Move tableau to tableau auto-exposes face-down card and awards 5 points")
        fun `move tableau to tableau auto-exposes hidden card and awards 5 points`() {
            val hiddenCard = Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(hiddenCard, redQueen)
                        1 -> listOf(blackKing)
                        else -> emptyList()
                    }
                },
                score = 0
            )

            val newState = KlondikeRules.moveTableauToTableau(
                state = state,
                fromColumnIndex = 0,
                cardIndex = 1,
                toColumnIndex = 1
            )

            assertEquals(5, newState.score)
            assertEquals(1, newState.tableau[0].size)
            assertTrue(newState.tableau[0].last().isFaceUp)
            assertEquals(listOf(blackKing, redQueen), newState.tableau[1])
        }

        @Test
        @DisplayName("Move tableau stack to tableau auto-exposes hidden card and awards 5 points")
        fun `move tableau stack to tableau auto-exposes hidden card and awards 5 points`() {
            val hiddenCard = Card(Suit.DIAMONDS, Rank.THREE, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(hiddenCard, redQueen, blackJack)
                        1 -> listOf(blackKing)
                        else -> emptyList()
                    }
                },
                score = 10
            )

            val newState = KlondikeRules.moveTableauToTableau(
                state = state,
                fromColumnIndex = 0,
                cardIndex = 1,
                toColumnIndex = 1
            )

            assertEquals(15, newState.score)
            assertEquals(1, newState.tableau[0].size)
            assertTrue(newState.tableau[0].last().isFaceUp)
            assertEquals(listOf(blackKing, redQueen, blackJack), newState.tableau[1])
        }

        @Test
        @DisplayName("Move tableau to tableau does NOT award points when remaining top card was already face-up")
        fun `move tableau to tableau does not award points when remaining card is already face-up`() {
            val testState = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(blackKing, redQueen)
                        1 -> listOf(Card(Suit.CLUBS, Rank.KING, isFaceUp = true))
                        else -> emptyList()
                    }
                },
                score = 10
            )

            val newState = KlondikeRules.moveTableauToTableau(
                state = testState,
                fromColumnIndex = 0,
                cardIndex = 1,
                toColumnIndex = 1
            )

            assertEquals(10, newState.score) // No turnover, score remains 10
            assertEquals(listOf(blackKing), newState.tableau[0])
            assertTrue(newState.tableau[0].last().isFaceUp)
        }

        @Test
        @DisplayName("Move tableau to tableau does not award points when source column becomes empty")
        fun `move tableau to tableau does not award points when source column becomes empty`() {
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(blackKing, redQueen)
                        else -> emptyList()
                    }
                },
                score = 50
            )

            val newState = KlondikeRules.moveTableauToTableau(
                state = state,
                fromColumnIndex = 0,
                cardIndex = 0,
                toColumnIndex = 2
            )

            assertEquals(50, newState.score)
            assertTrue(newState.tableau[0].isEmpty())
        }

        @Test
        @DisplayName("Move tableau to tableau with autoExpose = false leaves card face-down and awards 0 points")
        fun `move tableau to tableau with autoExpose false leaves card hidden`() {
            val hiddenCard = Card(Suit.CLUBS, Rank.FIVE, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        0 -> listOf(hiddenCard, redQueen)
                        1 -> listOf(blackKing)
                        else -> emptyList()
                    }
                },
                score = 0
            )

            val newState = KlondikeRules.moveTableauToTableau(
                state = state,
                fromColumnIndex = 0,
                cardIndex = 1,
                toColumnIndex = 1,
                autoExpose = false
            )

            assertEquals(0, newState.score)
            assertFalse(newState.tableau[0].last().isFaceUp)
        }
    }

    @Nested
    @DisplayName("Foundation to Tableau Scoring")
    inner class FoundationToTableauScoringTests {

        @Test
        @DisplayName("Moving card from foundation to tableau deducts 15 points")
        fun `move from foundation to tableau deducts 15 points`() {
            val state = BoardState(
                foundations = listOf(listOf(aceHearts, twoHearts), emptyList(), emptyList(), emptyList()),
                tableau = List(7) { col ->
                    if (col == 0) listOf(Card(Suit.SPADES, Rank.THREE, isFaceUp = true)) else emptyList()
                },
                score = 30
            )

            val newState = KlondikeRules.moveFoundationToTableau(
                state = state,
                foundationIndex = 0,
                tableauIndex = 0
            )

            assertEquals(15, newState.score) // 30 - 15 = 15
            assertEquals(1, newState.movesCount)
        }

        @Test
        @DisplayName("Moving card from foundation to tableau cannot reduce score below 0")
        fun `move from foundation to tableau does not reduce score below zero`() {
            val state = BoardState(
                foundations = listOf(listOf(aceHearts, twoHearts), emptyList(), emptyList(), emptyList()),
                tableau = List(7) { col ->
                    if (col == 0) listOf(Card(Suit.SPADES, Rank.THREE, isFaceUp = true)) else emptyList()
                },
                score = 10
            )

            val newState = KlondikeRules.moveFoundationToTableau(
                state = state,
                foundationIndex = 0,
                tableauIndex = 0
            )

            assertEquals(0, newState.score) // 10 - 15 = -5 -> coerced to 0
        }
    }

    @Nested
    @DisplayName("Direct autoExpose Functions")
    inner class DirectAutoExposeTests {

        @Test
        @DisplayName("autoExposeTableauCard exposes face-down card and adds 5 points")
        fun `autoExposeTableauCard exposes hidden card and adds 5 points`() {
            val hiddenCard = Card(Suit.DIAMONDS, Rank.TEN, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col -> if (col == 2) listOf(hiddenCard) else emptyList() },
                score = 10
            )

            val newState = KlondikeRules.autoExposeTableauCard(state, columnIndex = 2)

            assertEquals(15, newState.score)
            assertTrue(newState.tableau[2].last().isFaceUp)
            assertEquals(0, newState.movesCount) // Exposing does not increment player move count
        }

        @Test
        @DisplayName("autoExposeTableauCard does nothing if top card is already face-up")
        fun `autoExposeTableauCard does nothing if top card is face-up`() {
            val state = BoardState(
                tableau = List(7) { col -> if (col == 2) listOf(blackKing) else emptyList() },
                score = 10
            )

            val newState = KlondikeRules.autoExposeTableauCard(state, columnIndex = 2)

            assertEquals(10, newState.score)
            assertEquals(state, newState)
        }

        @Test
        @DisplayName("autoExposeTableauCard does nothing if column is empty")
        fun `autoExposeTableauCard does nothing if column is empty`() {
            val state = BoardState(score = 10)

            val newState = KlondikeRules.autoExposeTableauCard(state, columnIndex = 0)

            assertEquals(10, newState.score)
            assertEquals(state, newState)
        }

        @Test
        @DisplayName("autoExposeAllTableauColumns exposes all hidden top cards across all columns")
        fun `autoExposeAllTableauColumns exposes all hidden top cards`() {
            val hiddenCard1 = Card(Suit.HEARTS, Rank.FOUR, isFaceUp = false)
            val hiddenCard2 = Card(Suit.SPADES, Rank.EIGHT, isFaceUp = false)
            val state = BoardState(
                tableau = List(7) { col ->
                    when (col) {
                        1 -> listOf(hiddenCard1)
                        3 -> listOf(blackKing, hiddenCard2)
                        5 -> listOf(redQueen) // already face-up
                        else -> emptyList()
                    }
                },
                score = 0
            )

            val newState = KlondikeRules.autoExposeAllTableauColumns(state)

            assertEquals(10, newState.score) // 2 cards exposed * 5 points
            assertTrue(newState.tableau[1].last().isFaceUp)
            assertTrue(newState.tableau[3].last().isFaceUp)
            assertTrue(newState.tableau[5].last().isFaceUp)
        }
    }
}
