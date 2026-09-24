package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.PileType
import io.github.qdiaps.solitaire.domain.model.Rank
import io.github.qdiaps.solitaire.domain.model.Suit
import io.github.qdiaps.solitaire.domain.rules.DrawMode
import io.github.qdiaps.solitaire.domain.rules.KlondikeRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SolverMoveGeneratorTest {

    private val aceHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val twoHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val threeHearts = Card(Suit.HEARTS, Rank.THREE, isFaceUp = true)
    private val fourHearts = Card(Suit.HEARTS, Rank.FOUR, isFaceUp = true)

    private val kingSpades = Card(Suit.SPADES, Rank.KING, isFaceUp = true)
    private val queenHearts = Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)
    private val jackClubs = Card(Suit.CLUBS, Rank.JACK, isFaceUp = true)
    private val tenDiamonds = Card(Suit.DIAMONDS, Rank.TEN, isFaceUp = true)

    private val aceClubs = Card(Suit.CLUBS, Rank.ACE, isFaceUp = true)
    private val twoClubs = Card(Suit.CLUBS, Rank.TWO, isFaceUp = true)

    @Nested
    @DisplayName("Terminal / Game Won state")
    inner class TerminalStateTests {

        @Test
        @DisplayName("Won board state produces zero transitions")
        fun `won board state produces zero transitions`() {
            val fullFoundations = Suit.entries.map { suit ->
                Rank.entries.map { rank -> Card(suit, rank, isFaceUp = true) }
            }
            val wonState = BoardState(foundations = fullFoundations)

            val transitions = SolverMoveGenerator.generateTransitions(wonState)
            assertTrue(transitions.isEmpty(), "No transitions should be generated from a won state")
        }
    }

    @Nested
    @DisplayName("Stock and Waste transitions")
    inner class StockAndWasteTests {

        @Test
        @DisplayName("Stock has cards: generates draw transition")
        fun `stock has cards generates draw transition`() {
            val state = BoardState(
                stock = listOf(
                    Card(Suit.HEARTS, Rank.FIVE, isFaceUp = false),
                    Card(Suit.SPADES, Rank.SIX, isFaceUp = false)
                ),
                waste = emptyList()
            )

            val transitions = SolverMoveGenerator.generateTransitions(state, DrawMode.DRAW_ONE)

            val drawTransitions = transitions.filter { it.move.source is CardLocation.Stock }
            assertEquals(1, drawTransitions.size)
            val draw = drawTransitions.first()
            assertEquals(CardLocation.Stock, draw.move.source)
            assertEquals(CardLocation.Waste, draw.move.destination)
            assertEquals(1, draw.state.waste.size)
            assertEquals(1, draw.state.stock.size)
            assertTrue(draw.state.waste.last().isFaceUp)
        }

        @Test
        @DisplayName("Stock is empty and waste has cards: generates recycle transition")
        fun `stock empty and waste has cards generates recycle transition`() {
            val state = BoardState(
                stock = emptyList(),
                waste = listOf(Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
            )

            val transitions = SolverMoveGenerator.generateTransitions(state)

            val recycleTransitions = transitions.filter {
                it.move.source is CardLocation.Waste && it.move.destination is CardLocation.Stock
            }
            assertEquals(1, recycleTransitions.size)
            val recycle = recycleTransitions.first()
            assertEquals(1, recycle.state.stock.size)
            assertTrue(recycle.state.waste.isEmpty())
            assertFalse(recycle.state.stock.first().isFaceUp)
        }

        @Test
        @DisplayName("Waste card can move to Foundation")
        fun `waste card can move to foundation`() {
            val state = BoardState(
                waste = listOf(aceHearts),
                foundations = List(4) { emptyList() }
            )

            val transitions = SolverMoveGenerator.generateTransitions(state)

            val foundationMoves = transitions.filter { it.move.destination is CardLocation.Foundation }
            assertEquals(1, foundationMoves.size)
            val transition = foundationMoves.first()
            assertEquals(CardLocation.Waste, transition.move.source)
            assertEquals(0, (transition.move.destination as CardLocation.Foundation).index)
            assertTrue(transition.state.waste.isEmpty())
            assertEquals(listOf(aceHearts), transition.state.foundations[0])
            assertEquals(KlondikeRules.SCORE_WASTE_TO_FOUNDATION, transition.move.scoreDelta)
        }

        @Test
        @DisplayName("Waste card can move to Tableau")
        fun `waste card can move to tableau`() {
            val state = BoardState(
                waste = listOf(queenHearts),
                tableau = listOf(
                    listOf(kingSpades),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val transitions = SolverMoveGenerator.generateTransitions(state)

            val wasteToTableau = transitions.filter {
                it.move.source is CardLocation.Waste && it.move.destination is CardLocation.Tableau
            }
            assertEquals(1, wasteToTableau.size)
            val t = wasteToTableau.first()
            assertEquals(0, (t.move.destination as CardLocation.Tableau).columnIndex)
            assertTrue(t.state.waste.isEmpty())
            assertEquals(2, t.state.tableau[0].size)
            assertEquals(queenHearts, t.state.tableau[0].last())
        }

        @Test
        @DisplayName("Waste King into multiple empty columns only targets the first empty column")
        fun `waste king only targets first empty column`() {
            val state = BoardState(
                waste = listOf(kingSpades),
                tableau = List(7) { emptyList() }
            )

            val transitions = SolverMoveGenerator.generateTransitions(state)

            val kingMoves = transitions.filter {
                it.move.source is CardLocation.Waste && it.move.destination is CardLocation.Tableau
            }
            // Must be pruned to exactly 1 move (targeting column 0), not 7 duplicate moves
            assertEquals(1, kingMoves.size)
            assertEquals(0, (kingMoves.first().move.destination as CardLocation.Tableau).columnIndex)
        }
    }

    @Nested
    @DisplayName("Tableau to Foundation transitions")
    inner class TableauToFoundationTests {

        @Test
        @DisplayName("Tableau top card promotes to foundation")
        fun `tableau top card promotes to foundation`() {
            val state = BoardState(
                tableau = listOf(
                    listOf(aceClubs),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val transitions = SolverMoveGenerator.generateTransitions(state)

            val foundMoves = transitions.filter { it.move.destination is CardLocation.Foundation }
            assertEquals(1, foundMoves.size)
            val move = foundMoves.first()
            assertEquals(CardLocation.Tableau(0, 0), move.move.source)
            assertTrue(move.state.tableau[0].isEmpty())
            assertEquals(listOf(aceClubs), move.state.foundations[0])
        }

        @Test
        @DisplayName("Tableau promotion reveals face-down card underneath")
        fun `tableau promotion reveals face down card underneath`() {
            val hiddenCard = Card(Suit.SPADES, Rank.TEN, isFaceUp = false)
            val state = BoardState(
                tableau = listOf(
                    listOf(hiddenCard, aceHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val transitions = SolverMoveGenerator.generateTransitions(state)

            val foundMoves = transitions.filter { it.move.destination is CardLocation.Foundation }
            assertEquals(1, foundMoves.size)
            val move = foundMoves.first()
            assertEquals(1, move.state.tableau[0].size)
            // Newly uncovered card must be flipped face-up
            assertTrue(move.state.tableau[0].first().isFaceUp)
            assertEquals(hiddenCard.copy(isFaceUp = true), move.state.tableau[0].first())
        }
    }

    @Nested
    @DisplayName("Tableau to Tableau transitions and pruning")
    inner class TableauToTableauTests {

        @Test
        @DisplayName("Moves valid sequence from one column to another")
        fun `moves valid sequence between columns`() {
            val state = BoardState(
                tableau = listOf(
                    listOf(kingSpades),
                    listOf(queenHearts, jackClubs),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val transitions = SolverMoveGenerator.generateTransitions(state)

            val col1ToCol0 = transitions.filter {
                it.move.source == CardLocation.Tableau(1, 0) &&
                    (it.move.destination as? CardLocation.Tableau)?.columnIndex == 0
            }
            assertEquals(1, col1ToCol0.size)
            val t = col1ToCol0.first()
            assertEquals(listOf(queenHearts, jackClubs), t.move.cards)
            assertEquals(3, t.state.tableau[0].size)
            assertTrue(t.state.tableau[1].isEmpty())
        }

        @Test
        @DisplayName("King at base of column (cardIndex 0) moving to empty column is pruned")
        fun `king at base of column to empty column is pruned`() {
            val state = BoardState(
                tableau = listOf(
                    listOf(kingSpades),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val transitions = SolverMoveGenerator.generateTransitions(state)

            val kingMoves = transitions.filter {
                it.move.source is CardLocation.Tableau && it.move.destination is CardLocation.Tableau
            }
            // Moving King from base of column 0 to empty column 1..6 is useless and must be pruned
            assertTrue(kingMoves.isEmpty(), "King at index 0 moving to empty column should be pruned")
        }

        @Test
        @DisplayName("King with hidden cards underneath moving to empty column is allowed on first empty column only")
        fun `king uncovering hidden cards to empty column only targets first empty column`() {
            val hiddenCard = Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = false)
            val state = BoardState(
                tableau = listOf(
                    listOf(hiddenCard, kingSpades),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val transitions = SolverMoveGenerator.generateTransitions(state)

            val kingMoves = transitions.filter {
                it.move.source is CardLocation.Tableau && it.move.destination is CardLocation.Tableau
            }
            // Must target column 1 only, not columns 1..6
            assertEquals(1, kingMoves.size)
            val t = kingMoves.first()
            assertEquals(CardLocation.Tableau(0, 1), t.move.source)
            assertEquals(CardLocation.Tableau(1, 0), t.move.destination)
            assertEquals(1, t.state.tableau[0].size)
            assertTrue(t.state.tableau[0].first().isFaceUp, "Hidden card should be exposed")
            assertEquals(1, t.state.tableau[1].size)
            assertEquals(kingSpades, t.state.tableau[1].first())
        }

        @Test
        @DisplayName("Moving sub-stack to an equivalent card of same rank and color is pruned")
        fun `moving sub-stack to equivalent parent rank and color is pruned`() {
            // Col 0: TenDiamonds, NineClubs
            // Col 1: TenHearts (also red Ten!)
            val tenHearts = Card(Suit.HEARTS, Rank.TEN, isFaceUp = true)
            val nineClubs = Card(Suit.CLUBS, Rank.NINE, isFaceUp = true)

            val state = BoardState(
                tableau = listOf(
                    listOf(tenDiamonds, nineClubs),
                    listOf(tenHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val transitions = SolverMoveGenerator.generateTransitions(state)

            val nineClubsMoves = transitions.filter {
                it.move.cards.first() == nineClubs &&
                    (it.move.destination as? CardLocation.Tableau)?.columnIndex == 1
            }
            // Moving 9Clubs from red TenDiamonds to red TenHearts uncovers nothing and is symmetric
            assertTrue(nineClubsMoves.isEmpty(), "Moving sub-stack to equivalent parent color must be pruned")
        }
    }

    @Nested
    @DisplayName("Foundation demotion (Foundation to Tableau)")
    inner class FoundationDemotionTests {

        @Test
        @DisplayName("Foundation to tableau is disabled by default")
        fun `foundation to tableau is disabled by default`() {
            val state = BoardState(
                foundations = listOf(listOf(aceHearts, twoHearts), emptyList(), emptyList(), emptyList()),
                tableau = listOf(
                    listOf(threeHearts),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val defaultTransitions = SolverMoveGenerator.generateTransitions(state, includeFoundationToTableau = false)
            val demotionMoves = defaultTransitions.filter { it.move.source is CardLocation.Foundation }
            assertTrue(demotionMoves.isEmpty())
        }

        @Test
        @DisplayName("Foundation to tableau generates moves when explicitly enabled")
        fun `foundation to tableau generates moves when enabled`() {
            val threeClubs = Card(Suit.CLUBS, Rank.THREE, isFaceUp = true)
            val state = BoardState(
                foundations = listOf(listOf(aceHearts, twoHearts), emptyList(), emptyList(), emptyList()),
                tableau = listOf(
                    listOf(threeClubs),
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            )

            val transitions = SolverMoveGenerator.generateTransitions(state, includeFoundationToTableau = true)
            val demotionMoves = transitions.filter { it.move.source is CardLocation.Foundation }
            assertEquals(1, demotionMoves.size)
            val move = demotionMoves.first()
            assertEquals(CardLocation.Foundation(0), move.move.source)
            assertEquals(0, (move.move.destination as CardLocation.Tableau).columnIndex)
            assertEquals(1, move.state.foundations[0].size) // only Ace remains
            assertEquals(2, move.state.tableau[0].size) // threeClubs + twoHearts
        }
    }

    @Nested
    @DisplayName("Successor states utility")
    inner class SuccessorStatesTests {

        @Test
        @DisplayName("generateSuccessorStates maps transitions to board states")
        fun `generateSuccessorStates maps transitions to board states`() {
            val state = BoardState(
                waste = listOf(aceHearts),
                foundations = List(4) { emptyList() }
            )

            val states = SolverMoveGenerator.generateSuccessorStates(state)
            // Expect 2 valid transitions: 1 to Foundation, 1 Recycle waste to stock
            assertEquals(2, states.size)
            val foundationState = states.firstOrNull { it.foundations[0].isNotEmpty() }
            assertNotNull(foundationState)
            assertEquals(listOf(aceHearts), foundationState?.foundations?.get(0))
        }
    }
}
