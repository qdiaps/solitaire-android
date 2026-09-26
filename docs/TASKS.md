# Current Sprint Tasks - Phase 5: Game Loop, Scoring, Auto-Complete & Persistence

> **Current Phase:** Phase 5 (Game Loop, Scoring, Auto-Complete & Persistence)
> **Branch:** `feature/phase-5-game-loop-and-customization`

- [x] **T-5.1: Card Visual Styles & Custom Back Designs (`CardBackStyle`)**
  - Implement `CardBackStyle` enum: `ClassicLattice`, `CrimsonVintage`, `EmeraldArtDeco`, `ObsidianMinimal`.
  - Implement vector/canvas pattern renderers for all 4 card backs in `CardBackView` / `CardView`.
  - Support theme switching in `SolitaireTheme`.
  - *TDD/Unit Tests:* `CardBackStyleTest` verifying style enum properties, names, and color bindings.
  - *Compose Previews:* `CardBackPreview` showing all 4 card back styles side-by-side.

- [x] **T-5.2: Card Face Typography Architecture (`CardFaceStyle`)**
  - Implement extensible `CardFaceStyle` enum with canonical `ModernClean` style.
  - Update `CardView` and typography styling (`createCardTypography`) to provide modular architecture for future face themes.
  - *TDD/Unit Tests:* `CardFaceStyleTest` verifying font families, index scale factors, and formatting.
  - *Compose Previews:* `CardThemesGalleryPreview` rendering canonical face style across all 4 card backs.

- [x] **T-5.3: Pure Domain Hint Resolver Engine (`HintResolver`)**
  - Implement pure Kotlin `HintResolver` evaluating valid productive moves following SPEC priorities:
    1. Tableau move uncovering a hidden face-down card.
    2. Foundation promotion from Tableau or Waste.
    3. Productive tableau sequence reorganization.
    4. Productive stock draw when stock cycle contains accessible playable cards.
  - Return structured `Hint` / `Move` candidate or null when no productive move is available.
  - *TDD/Unit Tests:* `HintResolverTest` verifying priority resolution across known board configurations.

- [x] **T-5.4: Hint Pulsing UI Highlighting & ViewModel Integration**
  - Connect `GameIntent.RequestHint` and `GameIntent.DismissHint` in `GameViewModel`.
  - Implement smooth pulsing border highlight animation (`Modifier.drawBehind` / `Animatable`) for source card and destination slot.
  - Dismiss hint highlight automatically when player touches any card, draws from stock, or makes a move.
  - *TDD/Unit Tests:* `GameViewModelTest` verifying hint request, dismiss triggers, and auto-dismiss on interaction.

- [x] **T-5.4b: Fix Ghost Card Flip Turnover Animation on New Game / Deal Reset**
  - Resolved spurious 3D flip turnover animation and turnover audio playing on face-up cards when starting a new game or restarting a deal.
  - Root cause: Compose node slot reuse across deals preserved stale `previousFaceUp = false` state for static card IDs (`card.id`), causing `LaunchedEffect(card.isFaceUp)` to erroneously trigger turnover animation on newly dealt face-up cards.
  - Introduced `gameSessionId` in `GameUiState` and `LocalGameSessionId` composition local, incremented on every `StartNewGame` and `RestartGame`.
  - Keyed tableau card composables with `"${gameSessionId}_${card.id}"` and `remember(card.id, gameSessionId)` in `CardView`.
  - Added `animateFlip: Boolean = true` parameter to `CardView`, disabling redundant turnover animations in `AnimatedMoveOverlay`, `StockPileView`, `WastePileView`, `FoundationRowView`, and `DragOverlay`.
  - Disambiguated `StockPileView` top card ID to `STOCK_PILE_TOP` to eliminate ID collisions with the King of Spades.
  - *TDD/Unit Tests:* `GameViewModelTest` and `GameContractTest` verifying session ID incrementation and default state.

- [x] **T-5.4c: Fix Stock Draw Flying Card Morphing / Desync Bug**
  - Resolved bug where clicking the stock pile animated a different card than the one that actually landed on the waste pile.
  - Root cause: `KlondikeRules.draw` takes cards from the head of the stock pile (`state.stock.take(count)`), whereas `SolitaireGameScreen.kt` mistakenly used `boardState.stock.last()` as `movingCard`.
  - Added `drawMode: DrawMode = DrawMode.DRAW_ONE` to `GameUiState` and `SolitaireGameScreen`.
  - Calculated `movingCard = boardState.stock.take(drawCount).last()` matching domain draw logic for both Draw 1 and Draw 3 modes.
  - Guarded `isFlipping` inside `CardView` with `animateFlip` to prevent 1-frame glitches, and wrapped flying card in `key(card.id)` in `AnimatedMoveOverlay`.
  - *TDD/Unit Tests:* `GameContractTest` and suite tests (457 tests, 100% pass).

- [x] **T-5.5: Pure Domain Auto-Complete Resolver (`AutoCompleteResolver`)**
  - Implement pure Kotlin `AutoCompleteResolver` detecting when all tableau columns contain zero face-down cards and stock/waste can be safely cleared.
  - Calculate the next immediate safe foundation promotion move until board reaches victory state (`KlondikeRules.isGameWon`).
  - *TDD/Unit Tests:* `AutoCompleteResolverTest` verifying readiness conditions and sequential promotion step generation.

- [x] **T-5.6: Auto-Complete Cascade Execution in ViewModel & UI**
  - Update `GameUiState.isAutoCompleteAvailable` to reactively flag when auto-complete conditions are met.
  - Implement `GameIntent.AutoComplete` coroutine loop in `GameViewModel`: sequentially applies foundation promotions with 120–150ms delay, triggering tactile ticks/snaps and sound effects until victory.
  - Add auto-complete trigger banner / button in `SolitaireGameScreen` when available.
  - *TDD/Unit Tests:* `GameViewModelTest` verifying cascade loop execution, state updates, and win event trigger.

- [x] **T-5.7: DataStore Manager & Settings Repository (`SettingsRepository`)**
  - Implement `DataStoreManager` wrapping Jetpack `DataStore<Preferences>` with type-safe preference keys.
  - Implement `SettingsRepository` exposing `Flow<GameSettings>`: `drawMode`, `isLeftHanded`, `feltTheme`, `cardBackStyle`, `cardFaceStyle`, `soundEnabled`, `hapticsEnabled`, and `autoHintEnabled`.
  - *TDD/Unit Tests:* `SettingsRepositoryTest` verifying default settings, persistence mutations, and flow emissions.

- [x] **T-5.8: Settings Bottom Sheet UI (`SettingsBottomSheet`)**
  - Implement modal `SettingsBottomSheet` with sections: Gameplay (Draw 1 / Draw 3, Left-handed mode), Appearance (Felt cloth themes, Card backs, Card faces), and Feedback (Sound FX, Haptic feedback).
  - Connect settings triggers to `GameViewModel` / `SettingsRepository` for immediate live updates without restarting games.
  - *Compose Previews:* `SettingsBottomSheetPreview` in compact and large heights across felt table themes.

- [ ] **T-5.9: Statistics Repository (`StatsRepository`)**
  - Implement `StatsRepository` storing player records: `gamesPlayed`, `gamesWon`, `winPercentage`, `currentStreak`, `bestStreak`, `bestTimeSeconds`, `fewestMoves`, and `highScore`.
  - Implement record updating methods on game start and game victory, plus `resetStats()`.
  - *TDD/Unit Tests:* `StatsRepositoryTest` verifying win streak calculation, best time updates, and reset logic.

- [ ] **T-5.10: Statistics Dialog UI (`StatsDialog`)**
  - Implement modal `StatsDialog` displaying formatted statistics grid (Games, Wins, Win %, Streaks, Best Time in `mm:ss`, Fewest Moves, High Score) with a confirmation dialog for reset.
  - Wire stats dialog action in top status bar and bottom action bar.
  - *Compose Previews:* `StatsDialogPreview` with empty and populated statistics.

- [ ] **T-5.11: Game Session Persistence & Lifecycle Restoration (`GamePersistenceRepository`)**
  - Implement `GamePersistenceRepository` serializing active session (`BoardState`, `elapsedTimeSeconds`, `score`, `movesCount`, `drawMode`, and undo snapshots) to JSON via `kotlinx.serialization` in DataStore.
  - Wire Android lifecycle `ON_PAUSE` in `MainActivity` / `SolitaireGameScreen` to trigger autosave.
  - Restore active game on app startup if a saved session exists, or initialize fresh deal if none.
  - *TDD/Unit Tests:* `GamePersistenceRepositoryTest` and `GameViewModelTest` verifying save/restore and clean state after win.

- [ ] **T-5.12: Phase 5 Review, State Synchronization & Comprehensive Polish**
  - Architectural review of `data/` layer, pure domain separation, and lifecycle hygiene.
  - Full suite verification: `./gradlew test` (100% pass) and `./gradlew check` (0 Android lint errors).
  - Synchronize `docs/STATE.md`, `docs/TASKS.md`, and prepare milestone transition to Phase 6.

---

> **Historical Archive:** Completed tasks from Phases 1 through 4 are preserved in [docs/archive/TASKS_HISTORY.md](archive/TASKS_HISTORY.md).
