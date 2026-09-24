package io.github.qdiaps.solitaire.domain.solver

import io.github.qdiaps.solitaire.domain.model.BoardState
import io.github.qdiaps.solitaire.domain.model.Card
import io.github.qdiaps.solitaire.domain.model.Suit

/**
 * Compact, canonical representation of a [BoardState] for solvability search and cycle detection.
 *
 * Optimizations:
 * 1. **Tableau Column Symmetry Normalization:** Tableau columns are sorted lexicographically,
 *    meaning any lateral movement of card stacks between symmetric empty columns or column swaps
 *    maps to the identical canonical state key, preventing redundant branch exploration.
 * 2. **Foundation Pile Invariance:** Foundations are canonically indexed by [Suit] and packed
 *    into a single 16-bit integer, regardless of which physical foundation pile holds each suit.
 * 3. **Stock & Waste Cycle Tracking:** Preserves the exact cyclic ordering of cards in stock
 *    and waste, allowing visited sets to recognize and prune cyclic stock draws.
 * 4. **Score & Move Invariance:** Solvability depends purely on card configurations; meta-fields
 *    like [BoardState.score] and [BoardState.movesCount] are intentionally omitted.
 * 5. **Memory Efficiency:** Cards are bit-packed into single bytes. The entire board configuration
 *    is serialized into a compact byte array (~30–60 bytes), resulting in sub-100-byte footprint
 *    per visited state.
 */
class SolverStateKey private constructor(
    val foundationsPacked: Int,
    private val data: ByteArray
) {

    private val cachedHashCode: Int = run {
        var result = foundationsPacked
        result = 31 * result + data.contentHashCode()
        result
    }

    /**
     * Secondary constructor creating a [SolverStateKey] directly from a [BoardState].
     */
    constructor(state: BoardState) : this(
        foundationsPacked = packFoundations(state),
        data = packData(state)
    )

    /**
     * Returns the highest rank (0..13) currently banked on the foundation for the given [suit].
     */
    fun getFoundationRank(suit: Suit): Int = when (suit) {
        Suit.HEARTS -> (foundationsPacked shr 12) and 0x0F
        Suit.DIAMONDS -> (foundationsPacked shr 8) and 0x0F
        Suit.CLUBS -> (foundationsPacked shr 4) and 0x0F
        Suit.SPADES -> foundationsPacked and 0x0F
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SolverStateKey) return false
        if (this.cachedHashCode != other.cachedHashCode) return false
        if (this.foundationsPacked != other.foundationsPacked) return false
        return this.data.contentEquals(other.data)
    }

    override fun hashCode(): Int = cachedHashCode

    override fun toString(): String {
        return "SolverStateKey(" +
            "H=${getFoundationRank(Suit.HEARTS)}, " +
            "D=${getFoundationRank(Suit.DIAMONDS)}, " +
            "C=${getFoundationRank(Suit.CLUBS)}, " +
            "S=${getFoundationRank(Suit.SPADES)}, " +
            "bytes=${data.size})"
    }

    companion object {
        /** Bit mask for face-up flag in card byte encoding. */
        private const val FACE_UP_FLAG: Int = 0x40

        /**
         * Lexicographical comparator for encoded tableau column byte arrays.
         * Treats bytes as unsigned for consistent total ordering.
         */
        private val ColumnComparator = Comparator<ByteArray> { a, b ->
            val minLen = minOf(a.size, b.size)
            for (i in 0 until minLen) {
                val diff = (a[i].toInt() and 0xFF).compareTo(b[i].toInt() and 0xFF)
                if (diff != 0) return@Comparator diff
            }
            a.size.compareTo(b.size)
        }

        /**
         * Creates a canonical [SolverStateKey] from the given [state].
         */
        fun from(state: BoardState): SolverStateKey = SolverStateKey(state)

        /**
         * Packs foundation top card ranks into a 16-bit integer:
         * - Bits 12..15: HEARTS rank (0..13)
         * - Bits 8..11: DIAMONDS rank (0..13)
         * - Bits 4..7: CLUBS rank (0..13)
         * - Bits 0..3: SPADES rank (0..13)
         */
        private fun packFoundations(state: BoardState): Int {
            var heartsRank = 0
            var diamondsRank = 0
            var clubsRank = 0
            var spadesRank = 0

            for (pile in state.foundations) {
                if (pile.isNotEmpty()) {
                    val top = pile.last()
                    when (top.suit) {
                        Suit.HEARTS -> heartsRank = top.rank.value
                        Suit.DIAMONDS -> diamondsRank = top.rank.value
                        Suit.CLUBS -> clubsRank = top.rank.value
                        Suit.SPADES -> spadesRank = top.rank.value
                    }
                }
            }

            return (heartsRank shl 12) or (diamondsRank shl 8) or (clubsRank shl 4) or spadesRank
        }

        /**
         * Encodes a [Card] into a single byte:
         * - Bits 0..3: Rank value (1..13)
         * - Bits 4..5: Suit ordinal (0..3)
         * - Bit 6: Face-up boolean flag
         */
        private fun encodeCard(card: Card): Byte {
            val faceUpBit = if (card.isFaceUp) FACE_UP_FLAG else 0
            return (card.rank.value or (card.suit.ordinal shl 4) or faceUpBit).toByte()
        }

        /**
         * Serializes normalized tableau columns, waste, and stock into a single compact [ByteArray].
         */
        private fun packData(state: BoardState): ByteArray {
            // Encode and sort all tableau columns to achieve permutation invariance
            val sortedColumns = Array(state.tableau.size) { i ->
                val col = state.tableau[i]
                ByteArray(col.size) { j -> encodeCard(col[j]) }
            }
            sortedColumns.sortWith(ColumnComparator)

            var totalBytes = 0
            for (col in sortedColumns) {
                totalBytes += 1 + col.size // 1 byte for length + card bytes
            }
            totalBytes += 1 + state.waste.size // waste length + cards
            totalBytes += 1 + state.stock.size // stock length + cards

            val buffer = ByteArray(totalBytes)
            var offset = 0

            // 1. Tableau columns
            for (col in sortedColumns) {
                buffer[offset++] = col.size.toByte()
                System.arraycopy(col, 0, buffer, offset, col.size)
                offset += col.size
            }

            // 2. Waste pile
            buffer[offset++] = state.waste.size.toByte()
            for (card in state.waste) {
                buffer[offset++] = encodeCard(card)
            }

            // 3. Stock pile
            buffer[offset++] = state.stock.size.toByte()
            for (card in state.stock) {
                buffer[offset++] = encodeCard(card)
            }

            return buffer
        }
    }
}
