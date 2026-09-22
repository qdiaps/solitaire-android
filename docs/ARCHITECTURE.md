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
       └───────────────────────────┬────────────────────────────┘
                                   │ Invokes UseCases / Engine
       ┌───────────────────────────▼────────────────────────────┐
       │                   Domain Layer (Pure Kotlin)           │
       │  Models: Card, Suit, Rank, BoardState, Move            │
       │  Rules: KlondikeRules, MoveValidator, SmartTapResolver │
       │  Engine: SolitaireEngine, UndoStack                    │
       │  Solver: SolvabilityChecker, DealGenerator             │
       └───────────────────────────┬────────────────────────────┘
                                   │ Depends on abstractions
       ┌───────────────────────────▼────────────────────────────┐
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
│       ├── SolvabilityChecker.kt // BFS / A* heuristic solver
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
- **Strict Immutability:** Never expose `MutableStateFlow` outside `ViewModel`. Expose `asStateFlow()`.
- **Coroutines:** Use `Dispatchers.Default` for game engine computations and solver; `Dispatchers.Main` for UI state collection.
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
