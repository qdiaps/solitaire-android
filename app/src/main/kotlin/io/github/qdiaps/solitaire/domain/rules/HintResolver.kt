package io.github.qdiaps.solitaire.domain.rules

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.CardLocation
import io.github.qdiaps.solitaire.domain.model.Move
import io.github.qdiaps.solitaire.domain.model.Rank

/**
 * Priority classification of a suggested hint.
 * Lower [level] values correspond to higher strategic precedence.
 */
enum class HintPriority(val level: Int) {
    UNCOVER_FACE_DOWN(1),
    FOUNDATION_PROMOTION(2),
    TABLEAU_PROGRESS(3),
    STOCK_DRAW(4)
}

/**
 * Represents a structured recommendation for the player's next productive action.
 *
 * @property move The domain [Move] recommended.
 * @property priority The [HintPriority] ranking of this recommendation.
 * @property description Human-readable explanation of why this move is advantageous.
 */
data class Hint(
    val move: Move,
    val priority: HintPriority,
    val description: String
) {
    val from: CardLocation get() = move.source
    val to: CardLocation get() = move.destination
    val cards: List<Card> get() = move.cards
}

/**
 * Pure domain Hint Resolver Engine.
 *
 * Evaluates valid productive moves in strict accordance with Solitaire gameplay priorities:
 * 1. Uncovering hidden face-down tableau cards (moves to tableau or foundation that reveal a card).
 * 2. Foundation promotions (moving cards to foundation without uncovering a face-down card).
 * 3. Tableau progress (moving waste to tableau, or reorganizing tableau sequences without lateral useless King shifts).
 * 4. Productive stock draw (drawing from stock when cards in stock cycle are playable, or recycling).
 */
object HintResolver {

    /**
     * Resolves the highest-priority productive [Hint] for [state], or `null` if no productive moves exist.
     */
    fun findHint(
        state: BoardState,
        drawMode: DrawMode = DrawMode.DRAW_ONE
    ): Hint? {
        if (KlondikeRules.isGameWon(state)) return null

        val allHints = findAllHints(state, drawMode)
        return allHints.minWithOrNull(
            compareBy<Hint> { it.priority.level }
                .thenBy {
                    // Tie-breaker: prioritize tableau columns with more face-down cards
                    when (val from = it.from) {
                        is CardLocation.Tableau -> -state.tableau[from.columnIndex].count { card -> !card.isFaceUp }
                        else -> 0
                    }
                }
        )
    }

    /**
     * Finds all valid, non-redundant [Hint] suggestions available for [state].
     */
    fun findAllHints(
        state: BoardState,
        drawMode: DrawMode = DrawMode.DRAW_ONE
    ): List<Hint> {
        if (KlondikeRules.isGameWon(state)) return emptyList()

        val hints = mutableListOf<Hint>()
        val firstEmptyColIndex = state.tableau.indexOfFirst { it.isEmpty() }

        // --- Priority 1 & 2 & 3: Tableau moves ---
        for (fromCol in state.tableau.indices) {
            val sourceCol = state.tableau[fromCol]
            if (sourceCol.isEmpty()) continue

            // Check Foundation promotion of top card
            val topCard = sourceCol.last()
            if (topCard.isFaceUp) {
                val foundationIndex = KlondikeRules.findTargetFoundationIndex(state, topCard)
                if (foundationIndex != null) {
                    val uncoversFaceDown = sourceCol.size >= 2 && !sourceCol[sourceCol.size - 2].isFaceUp
                    val priority = if (uncoversFaceDown) HintPriority.UNCOVER_FACE_DOWN else HintPriority.FOUNDATION_PROMOTION
                    val desc = if (uncoversFaceDown) {
                        "Move ${topCard.rank.shortLabel} of ${topCard.suit.name} to Foundation to uncover a hidden card"
                    } else {
                        "Move ${topCard.rank.shortLabel} of ${topCard.suit.name} to Foundation"
                    }
                    hints.add(
                        Hint(
                            move = Move(
                                source = CardLocation.Tableau(fromCol, sourceCol.lastIndex),
                                destination = CardLocation.Foundation(foundationIndex),
                                cards = listOf(topCard),
                                scoreDelta = KlondikeRules.SCORE_TABLEAU_TO_FOUNDATION +
                                    (if (uncoversFaceDown) KlondikeRules.SCORE_TURNOVER_TABLEAU_CARD else 0)
                            ),
                            priority = priority,
                            description = desc
                        )
                    )
                }
            }

            // Check Tableau-to-Tableau moves
            for (cardIndex in sourceCol.indices) {
                val card = sourceCol[cardIndex]
                if (!card.isFaceUp) continue

                val movingStack = sourceCol.subList(cardIndex, sourceCol.size)
                if (!KlondikeRules.isValidTableauSequence(movingStack)) continue

                val uncoversFaceDown = cardIndex > 0 && !sourceCol[cardIndex - 1].isFaceUp

                for (toCol in state.tableau.indices) {
                    if (toCol == fromCol) continue
                    val targetCol = state.tableau[toCol]

                    if (targetCol.isEmpty()) {
                        if (card.rank != Rank.KING) continue
                        // Pruning: King at base of column moving to another empty column is useless lateral move
                        if (cardIndex == 0) continue
                        // Only target the first empty column
                        if (toCol != firstEmptyColIndex) continue
                    } else {
                        // Pruning: Equivalent parent card (moving 7 to Black 8 when already on Black 8)
                        if (cardIndex > 0 && sourceCol[cardIndex - 1].isFaceUp) {
                            val parent = sourceCol[cardIndex - 1]
                            val targetTop = targetCol.last()
                            if (parent.rank == targetTop.rank && parent.suit.isRed == targetTop.suit.isRed) {
                                continue
                            }
                        }
                    }

                    if (KlondikeRules.canMoveTableauToTableau(state, fromCol, cardIndex, toCol)) {
                        val priority = if (uncoversFaceDown) HintPriority.UNCOVER_FACE_DOWN else HintPriority.TABLEAU_PROGRESS
                        val desc = if (uncoversFaceDown) {
                            "Move ${card.rank.shortLabel} of ${card.suit.name} to column ${toCol + 1} to uncover a hidden card"
                        } else {
                            "Move ${card.rank.shortLabel} of ${card.suit.name} to column ${toCol + 1}"
                        }
                        hints.add(
                            Hint(
                                move = Move(
                                    source = CardLocation.Tableau(fromCol, cardIndex),
                                    destination = CardLocation.Tableau(toCol, targetCol.size),
                                    cards = movingStack,
                                    scoreDelta = if (uncoversFaceDown) KlondikeRules.SCORE_TURNOVER_TABLEAU_CARD else 0
                                ),
                                priority = priority,
                                description = desc
                            )
                        )
                    }
                }
            }
        }

        // --- Priority 2 & 3: Waste moves ---
        if (state.waste.isNotEmpty()) {
            val wasteCard = state.waste.last()

            // Waste to Foundation
            val targetFoundation = KlondikeRules.findTargetFoundationIndex(state, wasteCard)
            if (targetFoundation != null) {
                hints.add(
                    Hint(
                        move = Move(
                            source = CardLocation.Waste,
                            destination = CardLocation.Foundation(targetFoundation),
                            cards = listOf(wasteCard),
                            scoreDelta = KlondikeRules.SCORE_WASTE_TO_FOUNDATION
                        ),
                        priority = HintPriority.FOUNDATION_PROMOTION,
                        description = "Move ${wasteCard.rank.shortLabel} of ${wasteCard.suit.name} from Waste to Foundation"
                    )
                )
            }

            // Waste to Tableau
            for (toCol in state.tableau.indices) {
                val targetCol = state.tableau[toCol]
                if (targetCol.isEmpty() && toCol != firstEmptyColIndex) continue

                if (KlondikeRules.canMoveWasteToTableau(state, toCol)) {
                    hints.add(
                        Hint(
                            move = Move(
                                source = CardLocation.Waste,
                                destination = CardLocation.Tableau(toCol, targetCol.size),
                                cards = listOf(wasteCard),
                                scoreDelta = KlondikeRules.SCORE_WASTE_TO_TABLEAU
                            ),
                            priority = HintPriority.TABLEAU_PROGRESS,
                            description = "Move ${wasteCard.rank.shortLabel} of ${wasteCard.suit.name} from Waste to column ${toCol + 1}"
                        )
                    )
                }
            }
        }

        // --- Priority 4: Stock Draw / Recycle ---
        if (canStockProduceProgress(state, drawMode)) {
            if (KlondikeRules.canDraw(state)) {
                val drawCount = minOf(if (drawMode == DrawMode.DRAW_ONE) 1 else 3, state.stock.size)
                val drawnCards = state.stock.take(drawCount).map { it.copy(isFaceUp = true) }
                hints.add(
                    Hint(
                        move = Move(
                            source = CardLocation.Stock,
                            destination = CardLocation.Waste,
                            cards = drawnCards,
                            scoreDelta = 0
                        ),
                        priority = HintPriority.STOCK_DRAW,
                        description = "Draw cards from Stock"
                    )
                )
            } else if (KlondikeRules.canRecycle(state)) {
                val recycledCards = state.waste.map { it.copy(isFaceUp = false) }
                hints.add(
                    Hint(
                        move = Move(
                            source = CardLocation.Stock,
                            destination = CardLocation.Stock,
                            cards = recycledCards,
                            scoreDelta = 0
                        ),
                        priority = HintPriority.STOCK_DRAW,
                        description = "Recycle Waste back to Stock"
                    )
                )
            }
        }

        return hints
    }

    /**
     * Checks if drawing or recycling through the stock cycle can uncover any playable card
     * for the current board state.
     */
    private fun canStockProduceProgress(state: BoardState, drawMode: DrawMode): Boolean {
        if (state.stock.isEmpty() && state.waste.isEmpty()) return false

        val allCycleCards = state.stock + state.waste
        for (card in allCycleCards) {
            val faceUpCard = card.copy(isFaceUp = true)
            if (KlondikeRules.findTargetFoundationIndex(state, faceUpCard) != null) {
                return true
            }
            for (col in state.tableau) {
                if (KlondikeRules.canPlaceOnTableau(faceUpCard, col)) {
                    return true
                }
            }
        }
        return false
    }

    private val Rank.shortLabel: String
        get() = when (this) {
            Rank.ACE -> "A"
            Rank.TWO -> "2"
            Rank.THREE -> "3"
            Rank.FOUR -> "4"
            Rank.FIVE -> "5"
            Rank.SIX -> "6"
            Rank.SEVEN -> "7"
            Rank.EIGHT -> "8"
            Rank.NINE -> "9"
            Rank.TEN -> "10"
            Rank.JACK -> "J"
            Rank.QUEEN -> "Q"
            Rank.KING -> "K"
        }
}
