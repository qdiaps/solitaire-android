# Solitaire (Klondike) - Architecture & Technical Design

## 1. Architectural Pattern: Clean Architecture + MVI
The project follows strict layered Clean Architecture principles coupled with unidirectional data flow (MVI) in the presentation layer.

```text
       ┌────────────────────────────────────────────────────────┐
       │                   UI Layer (Presentation)              │
       │  Jetpack Compose Screen  ◄── StateFlow<UiState> ──┐     │
       │          │                                        │     │
       │       GameIntent                             GameViewModel
       │          └────────────────────────────────────────┘     │
       └──────────────────────────┬──────────────────────────────┘
                                   │ Invokes UseCases / Engine
       ┌──────────────────────────▼──────────────────────────────┐
       │                   Domain Layer (Pure Kotlin)           │
       │  Models: Card, Suit, Rank, BoardState, Move            │
       │  Rules: KlondikeRules, MoveValidator, SmartTapResolver │
       │  Engine: SolitaireEngine, UndoStack                    │
       │  Solver: SolverStateKey, SolverMoveGenerator,          │
       │          SafePromotion, SolvabilityChecker, ...        │
       └──────────────────────────┬──────────────────────────────┘
                                   │ Depends on abstractions
       ┌──────────────────────────▼──────────────────────────────┐
       │                   Data Layer                           │
       │  PreferencesRepository (Jetpack DataStore)             │
       │  GamePersistence (JSON serialization of BoardState)    │
       └────────────────────────────────────────────────────────┘
```

### Architectural Guardrails
1. Domain Purity: Zero dependencies on Android SDK (android.*, androidx.*) or Compose. Testable with vanilla JUnit 6.
2. UI Decoupling: Composable functions must NOT contain rule calculations. They only render UiState and emit GameIntent.
3. Immutability: All domain state changes produce a new instance of BoardState. No mutable collections inside models.

---

## 2. Package Structure (`app/src/main/kotlin/io/github/qdiaps/solitaire/`)
```text
io.github.qdiaps.solitaire/
├── domain/
│   ├── model/
│   │   ├── Suit.kt               // Enum (Hearts, Diamonds, Clubs, Spades)
│   │   ├── Rank.kt               // Enum (Ace = 1 to King = 13)
│   │   ├── Card.kt               // Data class (id, suit, rank, isFaceUp)
│   │   ├── PileType.kt           // Sealed interface / enum (Stock, Waste, Foundation, Tableau)
│   │   ├── CardLocation.kt       // Where a card currently resides
│   │   ├── BoardState.kt         // Immutable snapshot of all piles
│   │   └── Move.kt               // Source, destination, cards moved, score delta
│   ├── rules/
│   │   ├── KlondikeRules.kt      // Move legality validation
│   │   └── SmartTapResolver.kt   // Logic for auto-moving card on tap
│   ├── engine/
│   │   ├── SolitaireEngine.kt    // Applies moves, exposes cards, manages score & undo
│   │   └── UndoManager.kt        // Manages ArrayDeque of BoardState snapshots
│   └── solver/
│       ├── SolverStateKey.kt     // Bit-packed canonical state key for visited pruning
│       ├── SolverMoveGenerator.kt// Legal non-redundant successor transition generator
│       ├── SafePromotion.kt      // Provably safe foundation promotion heuristic
│       ├── SolvabilityChecker.kt // BFS / A* heuristic solver
│       ├── DeadlockDetector.kt   // Real-time unplayable deadlock detector
│       └── DealGenerator.kt      // Generates and buffers solvable deck seeds
│
├── data/
│   ├── local/
│   │   ├── DataStoreManager.kt   // Preferences DataStore wrapper
│   │   └── GameStateSerializer.kt// kotlinx.serialization for autosave
│   └── repository/
│       ├── SettingsRepository.kt // Draw mode, left-handed, themes
│       └── StatsRepository.kt    // Wins, streaks, best time, high score
│
└── ui/
    ├── game/
    │   ├── GameContract.kt       // GameUiState, GameIntent, GameEvent (single-shot)
    │   ├── GameViewModel.kt      // Holds StateFlow<GameUiState>, processes intents
    │   ├── GameScreen.kt         // Main portrait board layout
    │   ├── components/           // StockPile, WastePile, FoundationRow, TableauColumn, CardView
    │   ├── gesture/              // DragDropState, DragOverlay
    │   └── win/                  // VictoryCanvas (particle physics bounce animation)
    ├── settings/                 // SettingsBottomSheet
    ├── stats/                    // StatsDialog
    └── theme/                    // Color palettes, Felt surfaces, Typography
```

---

## 3. Core Domain Models (Reference Contracts)
### Card & Identifiers
```kotlin
enum class Suit(val isRed: Boolean) {
    HEARTS(true), DIAMONDS(true), CLUBS(false), SPADES(false)
}

enum class Rank(val value: Int) {
    ACE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6),
    SEVEN(7), EIGHT(8), NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13)
}

data class Card(
    val suit: Suit,
    val rank: Rank,
    val isFaceUp: Boolean = false,
    val id: String = "${rank.name}_${suit.name}"
)
```

### Immutable Board State
```kotlin
data class BoardState(
    val stock: List<Card> = emptyList(),
    val waste: List<Card> = emptyList(),
    val foundations: List<List<Card>> = List(4) { emptyList() },
    val tableau: List<List<Card>> = List(7) { emptyList() },
    val score: Int = 0,
    val movesCount: Int = 0
)
```

---

## 4. Presentation Pattern (MVI)
### Intent / Event / State Contract
```kotlin
sealed interface GameIntent {
    data object DrawCard : GameIntent
    data class OnCardTapped(val card: Card, val location: CardLocation) : GameIntent
    data class OnCardDropped(val cards: List<Card>, val source: CardLocation, val target: CardLocation) : GameIntent
    data object UndoMove : GameIntent
    data object RequestHint : GameIntent
    data object AutoComplete : GameIntent
    data object StartNewGame : GameIntent
    data object SkipWinAnimation : GameIntent
}

data class GameUiState(
    val board: BoardState = BoardState(),
    val isAutoCompleteAvailable: Boolean = false,
    val isGameWon: Boolean = false,
    val isDeadlocked: Boolean = false,
    val activeHint: Hint? = null,
    val elapsedTimeSeconds: Long = 0L,
    val isLeftHanded: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface GameEvent {
    data object PlayHapticTick : GameEvent
    data object PlayHapticSnap : GameEvent
    data class ShowMessage(val message: String) : GameEvent
}
```

---

## 5. Coding & Documentation Standards
- **KDoc:** Required for public APIs, non-obvious game rules, and complex solver heuristics.
- **Strict Immutability:** Never expose `MutableStateFlow` outside `ViewModel`. Expose `asStateFlow()`.\n- **Coroutines:** Use `Dispatchers.Default` for game engine computations and solver; `Dispatchers.Main` for UI state collection.
- **Testing:** Every rule in `KlondikeRules` must have a corresponding test case covering both valid and invalid scenarios.

---

## 6. Architecture Decision Records (ADR Light)
### ADR 001: DataStore Preferences over Room Database
- **Context:** We need to store user settings, game stats, and active board state snapshots.
- **Decision:** Use Jetpack `DataStore` (Preferences) + `kotlinx.serialization` (JSON string for board snapshot) instead of Room.
- **Rationale:** The application has no complex relational queries. SQLite/Room introduces unnecessary boilerplate and schema management overhead for key-value settings and single-record session dumps.

### ADR 002: Canvas-Based Victory Screen over Compose Widgets
- **Context:** Classical victory cascade requires rendering 52 bouncing card sprites with gravity and trails at 60/120 fps.
- **Decision:** Use a single `Canvas` drawing loop via `withFrameNanos`.
- **Rationale:** Managing 52 animated Composable widgets with coordinate physics triggers massive recomposition overhead and frame drops on mid-range devices. Canvas provides raw hardware-accelerated drawing.

### ADR 003: Dedicated Drag Overlay Layer
- **Context:** Dragging cards between columns can cause clipping artifacts due to container bounds and Z-index ordering.
- **Decision:** Render dragged cards in a top-level `Box` overlay using global window offsets.
- **Rationale:** Isolates drag gestures from column hierarchy, preventing card clipping and guaranteeing dragged cards always float above all other board elements.

### ADR 004: Bit-Packed Canonical Solver State Key (`SolverStateKey`)
- **Context:** Solvability search (BFS/A*) explores tens of thousands of game states. Using naive `BoardState` graphs in visited sets causes high memory overhead (~1.5 KB per state), slow GC, and misses symmetric redundancies (e.g. swapping identical tableau columns, king moves to empty columns, or foundation pile ordering).
- **Decision:** Implement `SolverStateKey` with:
  1. Lexicographically sorted tableau column byte arrays for column permutation invariance.
  2. 16-bit packed integer for foundation top card ranks indexed by suit (HEARTS, DIAMONDS, CLUBS, SPADES).
  3. Single-byte bit-packed card encoding (rank in bits 0..3, suit in bits 4..5, face-up in bit 6).
  4. Omission of ephemeral scoring and move count metadata.
  5. Pre-computed hash code and SIMD-backed `contentEquals`.
- **Rationale:** Reduces memory consumption from ~1.5 KB to under 100 bytes per state (>15x reduction), normalizes symmetric board branches to prune cyclic or equivalent states, and yields sub-microsecond hash table lookups.

### ADR 005: Provably Safe Foundation Auto-Promotion Heuristic (`SafePromotion`)
- **Context:** A naive search engine branches on every legal foundation promotion. However, in Klondike, prematurely promoting a card can dead-end a winning line if that card was needed in the tableau to receive a descending card of opposite color. Conversely, exploring every possible foundation move creates an enormous branching factor.
- **Decision:** Implement mathematically safe foundation promotion heuristics:
  1. Rank 1 (Aces) and Rank 2 (Twos) are unconditionally safe (never needed to receive cards in tableau).
  2. Rank $R \ge 3$ is provably safe if both opposite-color foundation piles have already reached rank $\ge R - 1$.
  3. Safe promotions are applied greedily to reach a reduced canonical state without branching, collapsing search state spaces by an order of magnitude.
- **Rationale:** Guarantees no winning solution paths are severed while avoiding combinatorial explosion in the solver.

### ADR 006: Core A* Search Engine with Priority-Queue Expansion (`SolvabilityChecker`)
- **Context:** Solitaire deals must be verified for solvability before being offered to players, and hint/solution paths must be available upon request. Search algorithms must run within tight mobile time and memory budgets (< 300 ms on benchmark seeds, 0 GC spikes).
- **Decision:** Implement an A* priority queue search engine using:
  1. Admissible heuristic function $h(s) = (52 - \sum foundation) + 2 \cdot faceDown + (stock + waste)$.
  2. Tie-breaking on lower heuristic cost $h$ to favor paths advancing foundations and card reveals.
  3. Seamless integration with `SafePromotion` to collapse safe promotion chains into single nodes with full move parent pointer traceability.
  4. Guardrails with configurable `timeoutMs` and `maxStates` limits.
- **Rationale:** Produces optimal and near-optimal solution paths, guarantees deterministic termination on mobile hardware, and provides full move history reconstruction.

### ADR 007: Real-Time Deadlock Detection via Stock Cycle and Tableau Invariants (`DeadlockDetector`)
- **Context:** Players and UI need instant feedback when a board reaches a state where no legal productive moves remain (e.g. stock exhausted, stock cycles without producing playable cards, tableaus locked with non-productive lateral King hops or equivalent parent moves).
- **Decision:** Implement `DeadlockDetector` analyzing:
  1. Win check (`ActiveGame`).
  2. Direct foundation promotions from tableau or waste.
  3. Direct tableau placements from waste.
  4. Productive tableau sequence moves (pruning lateral King moves to empty columns and equivalent parent rank/color swaps).
  5. Stock cycle simulation tracking visited `(stock, waste)` pairs under active `DrawMode` (Draw 1 / Draw 3) to verify if any accessible waste card can be placed onto tableau or foundations.
  6. Structured status `DeadlockStatus.ActiveGame` vs `DeadlockStatus.Deadlock(DeadlockReason)`.
- **Rationale:** Provides sub-millisecond deadlock evaluation without full tree search, enabling real-time UI "No moves left" alerts and fast solver pruning.
