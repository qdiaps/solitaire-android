# Project State & Session Memory

## Project Overview
- **App:** Solitaire (Klondike)
- **Package:** `io.github.qdiaps.solitaire`
- **Current Milestone:** Phase 4 - Drag-and-Drop & Interactive Gameplay
- **Active Branch:** `feature/phase-4-interactive-gameplay`

---

## Current Focus & Status
- **Phase:** 4 / 6
- **Completed Milestones Summary:**
  - **Phase 1: Pure Domain Engine** — 159 unit tests (100% pass), models, rules, scoring, smart tap, undo. (Complete)
  - **Phase 2: Solvability Engine & Background Generator** — 80 unit tests (100% pass), A* solver, deadlock detector, buffered deal generator. (Complete)
  - **Phase 3: Compose Board Layout & Static Presentation** — 31 unit tests (100% pass), full vector board, themes, dimensions, 38 previews. (Complete)
  - *(Full historical task breakdown archived in [docs/archive/STATE_HISTORY.md](archive/STATE_HISTORY.md))*
- **Current Focus:** Phase 4: Drag-and-Drop & Interactive Gameplay (Task T-4.8).
- **Blockers / Technical Debt:** None.

---

## Recent Progress Log
- **2026-09-25 (T-4.7: Global Drag Overlay Layer (DragOverlay)):**
  - Implemented top-level `DragOverlay` and pure presentation `DragOverlayContent` floating above all board elements (ADR 003).
  - Applied elevated shadow (`12.dp`) and vertical cascade spacing matching `CardDimensions.faceUpPeek`.
  - Added layout/draw phase offset lambda `{ dragPosition() }` ensuring 60/120 FPS performance without triggering recomposition during continuous drag gestures.
  - Added source card alpha masking (`graphicsLayer { if (isCardDragged) alpha = 0f }`) across `TableauColumnView`, `FoundationPileView`, and `WastePileView` to prevent duplicate ghost cards on the board while cards are floating.
  - Added elevation parameter to `CardView` (defaults to `2.dp`, configurable to `12.dp`).
  - Added interactive Compose previews: `DragOverlaySingleCardPreview` and `DragOverlayCardStackPreview`.
  - Added unit test suite `DragOverlayTest` validating stack height calculations and cascade offsets (8 unit tests, 100% pass across all 371 suite tests).
- **2026-09-25 (T-4.6: Drag & Drop State Management (DragDropState)):**
  - Implemented `DragDropState` managing active drag-and-drop gesture lifecycle, `sourceLocation`, `draggedCards`, `originPosition`, `dragPosition`, and displacement `dragOffset`.
  - Added tableau sub-stack slicing (`sliceTableauStack`) ensuring face-down card protection and multi-card stack extraction ($k \dots N$).
  - Added drag initiation handlers `startTableauDrag`, `startWasteDrag`, and `startFoundationDrag` with coordinates binding.
  - Implemented source card visibility flags (`isCardHidden` and `isCardDragged`) to prevent duplicate ghost cards on the board during active dragging.
  - Provided `LocalDragDropState` and `@Composable rememberDragDropState()`.
  - Added unit test suite `DragDropStateTest` with 19 unit tests covering state lifecycle, coordinate offsets, sub-stack slicing, and card masking (100% pass across all 363 suite tests).
- **2026-09-25 (T-4.5: Drop Target Hitbox Registry (DropTargetRegistry)):**
  - Created `DropTargetRegistry` managing root-relative screen bounds (`Rect`) of Foundation slots and Tableau columns.
  - Implemented `calculateOverlapArea` and `calculateOverlapRatio` functions handling boundary, edge-touching, and degenerate conditions.
  - Implemented generic `findBestDropTarget` selecting the destination with maximum intersection area and supporting `minOverlapArea` tolerance thresholds.
  - Added `Modifier.dropTarget(location, registry)` and ambient `@Composable Modifier.dropTarget(location)` backed by `LocalDropTargetRegistry`.
  - Wired drop target modifier attachments into `FoundationRowView` (`Foundation(0..3)`) and `TableauAreaView` (`Tableau(0..6)`).
  - Added unit test suite `DropTargetRegistryTest` with 21 unit tests covering state management, overlap calculations, ratios, and multi-target priority selection (100% pass across all 344 suite tests).
- **2026-09-25 (T-4.4: Smart Tap Move Execution & Auto-Flip Uncovered Cards):**
  - Connected `OnCardTapped(card, location)` intent in `GameViewModel` backed by `SmartTapResolver`.
  - Added tap routing: tapping stock delegates to `drawStockCard()`, cards in tableau or waste evaluate prioritized foundation moves, revealing moves, or sequence shifts.
  - Automatically exposed newly uncovered face-down cards on tableau with turnover scoring (+5 pts).
  - Integrated `DeadlockDetector.detect` updating `isDeadlocked` state reactively after every move and undo action.
  - Connected victory detection and win celebration flow triggering `GameEvent.TriggerWinCelebration` and halting the elapsed timer.
  - Added test suite `SmartTapIntents` in `GameViewModelTest` covering waste-to-foundation promotions, tableau sequence moves, face-down auto-exposure, unmovable taps, win triggers, deadlock updates, and undo reversibility (8 unit tests, 100% pass across all 323 suite tests).
- **2026-09-25 (T-4.3: Stock Draw, Waste Extraction & Undo Processing in ViewModel):**
  - Configured `DrawMode` (`DRAW_ONE` / `DRAW_THREE`) parameter support in `GameViewModel`.
  - Implemented `drawStockCard()` handling standard card draw from stock to waste and automatic recycling fallback when stock is empty.
  - Implemented `recycleStock()` restoring waste cards face-down into stock with state snapshot recording.
  - Implemented `undoMove()` popping previous board state from `UndoManager`, updating `canUndo` flag, win status, and resuming timer if rolled back from a victory.
  - Added single-shot `GameEvent.PlayHapticTick` on card draw/undo actions and `GameEvent.TriggerWinCelebration` on victory.
  - Added test suite `StockAndUndoIntents` in `GameViewModelTest` covering 9 comprehensive test scenarios (100% pass).
- **2026-09-25 (T-4.2: GameViewModel Lifecycle, Deal Initialization & Timer):**
  - Implemented `GameViewModel` exposing reactive `StateFlow<GameUiState>` and `SharedFlow<GameEvent>`.\
  - Added deal initialization supporting standard shuffled deals via `dealProvider` and background solvable deals via `DealGenerator`.
  - Implemented coroutine stopwatch timer loop with `startTimer`, `pauseTimer`, `resumeTimer`, and `stopTimer` methods, guarded against ticks when game is won.
  - Added `StartNewGame`, `RestartGame`, `ToggleLeftHanded`, `SelectFeltTheme`, and `DismissHint` intent handling.
  - Designed timer coroutine scope injection (`coroutineScope: CoroutineScope?`) enabling test integration via `backgroundScope` to eliminate scheduler deadlocks and infinite continuation re-dispatching during tests.
  - Added full test suite in `GameViewModelTest` with 10 unit tests covering initial deal, custom deal, timer incrementing, pause/resume, win guard, reset/restart, and `DealGenerator` integration (100% pass in ~5s).
- **2026-09-25 (T-4.1: MVI Contract Definitions):**
  - Defined immutable `@Immutable` `GameUiState` in `GameContract.kt` encapsulating `boardState`, `isGameWon`, `isDeadlocked`, `canUndo`, `elapsedTimeSeconds`, `feltTheme`, `isLeftHanded`, `activeHint`, `isLoading`, and `isAutoCompleteAvailable`.
  - Added derived visual properties `highlightedCard` and `isHintActive` directly bound to `activeHint`.
  - Defined comprehensive `GameIntent` sealed interface covering draw, recycle, tap, drop, undo, hint, auto-complete, deal start/restart, theme switch, and left-handed toggles.
  - Defined single-shot `GameEvent` sealed interface for UI haptic feedbacks, snackbar messages, and win celebration trigger.
  - Added unit test suite `GameContractTest` verifying state immutability, defaults, hint derivation, and exhaustive intent/event handling (100% pass).
- **2026-09-25 (Phase 4 Task Decomposition & Setup):**
  - Created working branch `feature/phase-4-interactive-gameplay` branched off clean `master` (PR #3 merged).
  - Decomposed Phase 4 into 9 atomic tasks (T-4.1 .. T-4.9) covering MVI contract, `GameViewModel`, stock/waste moves, smart tap, drop target hitboxes, drag overlay, snap-back animations, and screen wiring.
  - Archived completed task checklists and historical logs from Phases 1-3 into `docs/archive/TASKS_HISTORY.md` and `docs/archive/STATE_HISTORY.md`.

---

## Next Immediate Step
- **Target Task:** `T-4.8: Drop Validation, Snap-Back Animation & Haptic Feedback`
