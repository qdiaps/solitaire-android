# Current Sprint Tasks - Phase 4: Drag-and-Drop & Interactive Gameplay

> **Current Phase:** Phase 4 (Drag-and-Drop & Interactive Gameplay)
> **Branch:** `feature/phase-4-interactive-gameplay`

- [x] **T-4.1: MVI Contract Definitions (`GameContract.kt`)**
  - Define `CardLocation` hierarchy: `Stock`, `Waste`, `Foundation(index)`, `Tableau(columnIndex, cardIndex)`.
  - Define immutable `GameUiState` (`@Immutable`) containing `boardState`, `isGameWon`, `isDeadlocked`, `canUndo`, `elapsedTimeSeconds`, `feltTheme`, `isLeftHanded`, `activeHint`.
  - Define `GameIntent` sealed interface and single-shot `GameEvent` sealed interface (haptics, messages).
  - *TDD/Unit Tests:* `GameContractTest` verifying state immutability, default properties, and helper methods.
- [x] **T-4.2: GameViewModel Lifecycle, Deal Initialization & Timer**
  - Implement `GameViewModel` with `StateFlow<GameUiState>`, coroutine timer loop, and deal initialization from `KlondikeDealer` / `DealGenerator`.
  - Handle `StartNewGame`, `RestartGame`, and timer pause/resume lifecycle.
  - *TDD/Unit Tests:* `GameViewModelTest` verifying state initialization, new game deal, and timer ticks.
- [x] **T-4.3: Stock Draw, Waste Extraction & Undo Processing in ViewModel**
  - Implement `DrawStockCard`, `RecycleStock`, and `UndoMove` intent processing in `GameViewModel`.
  - Connect `UndoManager` snapshot management and score tracking across draw/undo operations.
  - *TDD/Unit Tests:* `GameViewModelTest` verifying stock cycle, waste card extraction, and undo state restoration.
- [x] **T-4.4: Smart Tap Move Execution & Auto-Flip Uncovered Cards**
  - Implement `OnCardTapped(card, location)` intent in `GameViewModel` backed by `SmartTapResolver`.
  - Auto-flip uncovered face-down cards when top card of a tableau column is vacated.
  - Check win condition (`KlondikeRules.isGameWon`) and deadlock status after every move.
  - *TDD/Unit Tests:* `GameViewModelTest` verifying tap promotions to foundation, cross-tableau sequence moves, and face-down reveals.
- [x] **T-4.5: Drop Target Hitbox Registry (`DropTargetRegistry`)**
  - Implement `DropTargetRegistry` storing absolute screen bounds (`Rect`) of all 4 Foundation slots and 7 Tableau columns via `Modifier.onGloballyPositioned`.
  - Implement pure helper `findBestDropTarget(draggedBounds, targets)` calculating bounding box overlap area to select the intended destination.
  - *TDD/Unit Tests:* `DropTargetRegistryTest` testing geometry overlap matching and tolerance edge cases.
- [ ] **T-4.6: Drag & Drop State Management (`DragDropState`)**
  - Implement `DragDropState` managing active drag lifecycle: `sourceLocation`, `draggedCards`, current drag `Offset`, and source visibility flags.
  - Support sub-stack dragging in tableau (dragging from index k lifts cards k..N).
  - *TDD/Unit Tests:* `DragDropStateTest` validating sub-stack slicing, active dragging state flags, and touch offset tracking.
- [ ] **T-4.7: Global Drag Overlay Layer (`DragOverlay`)**
  - Implement `DragOverlay` composable at the root layout layer floating above all board elements (ADR 003).
  - Render moving card stacks with elevated shadow (`12.dp`) and vertical cascade spacing matching `CardDimensions`.
  - Hide source cards on the board while drag is in progress to prevent duplicate ghost cards.
  - *Compose Previews:* `DragOverlayPreview` showing single card and multi-card stacks floating over felt.
- [ ] **T-4.8: Drop Validation, Snap-Back Animation & Haptic Feedback**
  - Implement drop gesture release handling: validate destination using `KlondikeRules.canMoveCards`.
  - If valid: dispatch `OnCardDropped` intent, update board state, auto-flip uncovered cards, and trigger haptic snap.
  - If invalid: animate dragged cards smoothly back to origin position using `Animatable` spring physics before clearing drag state.
  - Integrate `LocalHapticFeedback` for card pickup and placement clicks.
- [ ] **T-4.9: Activity & Screen Wiring, End-to-End Gameplay & Phase 4 Review**
  - Wire `GameViewModel` into `SolitaireGameScreen` and `MainActivity`.
  - End-to-end verification: playable game loop with both smart tap and smooth drag-and-drop.
  - Run `./gradlew check` and `./gradlew test` (ensuring 100% pass rate).
  - Update `docs/STATE.md` and `docs/TASKS.md`.

---

# Upcoming Sprint Backlog - Phase 5 (Preview)

- [ ] **T-5.x: Card Themes & Visual Customization (Back & Face Styles)**
  - Implement `CardBackStyle` enum: `ClassicLattice`, `CrimsonVintage`, `EmeraldArtDeco`, `ObsidianMinimal`.
  - Implement `CardFaceStyle` enum: `ModernClean`, `ClassicSerif`, `LargePrint`.
  - Integrate selection with `SolitaireTheme`, settings persistence (`DataStore`), and interactive `CardThemesGalleryPreview`.

---

> **Historical Archive:** Completed tasks from Phase 1, Phase 2, and Phase 3 are preserved in [docs/archive/TASKS_HISTORY.md](archive/TASKS_HISTORY.md).
