# Project State & Session Memory

## Project Overview
- **App:** Solitaire (Klondike)
- **Package:** `io.github.qdiaps.solitaire`
- **Current Milestone:** Phase 2 - Solvability Engine & Background Generator
- **Active Branch:** `feature/phase-2-solver`

---

## Current Focus & Status
- **Phase:** 2 / 6
- **Completed Tasks (Phase 1):**
  - `Task 1.1`: Setup project structure, Kotlin source sets, and configure JUnit 6 testing dependencies.
  - `Task 1.2`: Implement core domain models: `Suit`, `Rank`, `Card`, `PileType`, `CardLocation`, and `BoardState`.
  - `Task 1.3`: Implement `Deck` generator, shuffling utility, and initial Klondike 7-column deal logic with unit tests.
  - `Task 1.4`: Implement `KlondikeRules`: Stock draw & recycling logic (Draw 1 / Draw 3) with unit tests.
  - `Task 1.5`: Implement `KlondikeRules`: Tableau-to-Tableau and Waste-to-Tableau movement validation with unit tests.
  - `Task 1.6`: Implement `KlondikeRules`: Foundation building validation (Ace to King by suit) with unit tests.
  - `Task 1.7`: Implement auto-exposing face-down cards and scoring calculation on moves.
  - `Task 1.8`: Implement `SmartTapResolver` (Priority: Foundation > Expose hidden > Leftmost valid tableau) with unit tests.
  - `Task 1.9`: Implement `UndoManager` (state snapshot rollback for board, score, moves) with unit tests.
  - `Task 1.10`: Phase 1 review, refactoring to idiomatic Kotlin, completion of `Move` model, and verification of all 159 tests passing.
- **Current Focus:** Phase 2: Solvability Engine & Background Generator (Tasks T-2.1 through T-2.8).
- **Blockers / Technical Debt:** None.

---

## Recent Progress Log
- **2026-09-23 (Phase 2 Microtasks Decomposition):** Structured Phase 2 in `TASKS.md` into 8 atomic tasks (T-2.1 .. T-2.8) covering canonical state pruning, move enumeration, safe foundation heuristic, A* search loop, deadlock detection, coroutine deal buffering, and DoD benchmarks.
- **2026-09-23 (Phase 2 Initialization):** Transitioned project to Phase 2 (Solvability Engine & Background Generator):
  - Merged Phase 1 PR #1 into `master`.
  - Rebased `feature/phase-2-solver` on `master`.
  - Updated `STATE.md` and `TASKS.md` milestones.
  - Formulated high-level architectural plan for Phase 2 components (`SolvabilityChecker`, `DeadlockDetector`, `DealGenerator`).
- **2026-09-23 (Task 1.10):** Completed Phase 1 comprehensive review, refactoring, and model completion:
  - Implemented the final domain model entity `Move` (`source`, `destination`, `cards`, `scoreDelta`) with `@Serializable` and non-empty preconditions.
  - Added unit and serialization tests in `ModelsTest`, bringing the suite to 159 unit tests (100% pass).
  - Executed full project verification: `./gradlew check` and `./gradlew lintDebug` passing cleanly with zero errors or warnings.
  - Verified architectural boundaries: pure Kotlin domain layer with 0% Android framework leaks (`android.*`, `androidx.*`).
  - Marked Phase 1 as completely fulfilled in `ROADMAP.md` and prepared Phase 2 sprint backlog in `TASKS.md`.
- **2026-09-23 (Task 1.9):** Implemented `UndoManager` for step-by-step game undo/redo state rollback:
  - Backed by LIFO `ArrayDeque<BoardState>` snapshot stacks with configurable/unbounded history (`maxHistorySize`).
  - Implemented `record(state)`, `undo(currentState)`, `redo(currentState)`, `peekUndo()`, `peekRedo()`, and `clear()`.
  - Added full rollback validation ensuring card positions, face-up/down orientations, scores, and moves counts are cleanly preserved and restored.
  - Added serialization and state persistence helpers `getUndoHistory()`, `getRedoHistory()`, and `restoreHistory()`.
  - Invalidation of redo stack upon branching with a new move.
  - Verified with 14 new unit tests (`UndoManagerTest`), bringing total test suite to 155 unit tests (100% pass).
- **2026-09-23 (Task 1.8):** Implemented `SmartTapResolver` for auto-moving cards on single tap following Klondike specification:
  - Priority 1: Move to Foundation (if valid).
  - Priority 2: Move to Tableau column that reveals a hidden face-down card.
  - Priority 3: Move to leftmost valid Tableau column (0..6).
  - Prevented meaningless lateral moves (King already at base `cardIndex == 0` of a column does not move to another empty column).
  - Implemented `resolveDestination`, `resolveAndApply`, `resolveMove`, and global `findBestMove(state)`.
  - Added overloads accepting either `CardLocation` or `Card` instance.
  - Verified with 23 new unit tests (`SmartTapResolverTest`), bringing total test suite to 141 unit tests (100% pass).
- **2026-09-23 (Task 1.7):** Implemented auto-exposing face-down cards and standard Klondike scoring calculation on game moves:
  - Added scoring constants: `SCORE_WASTE_TO_TABLEAU (+5)`, `SCORE_WASTE_TO_FOUNDATION (+10)`, `SCORE_TABLEAU_TO_FOUNDATION (+10)`, `SCORE_TURNOVER_TABLEAU_CARD (+5)`, `SCORE_FOUNDATION_TO_TABLEAU (-15)`.
  - Implemented `calculateScore(currentScore, delta)` clamping score to 0 minimum (`coerceAtLeast(0)`).
  - Implemented standalone helpers `autoExposeTableauCard` and `autoExposeAllTableauColumns`.
  - Updated `moveWasteToTableau` (+5), `moveWasteToFoundation` (+10), `moveTableauToFoundation` (+10, +5 if top hidden card exposed), `moveTableauToTableau` (+5 if top hidden card exposed), and `moveFoundationToTableau` (-15).
  - Added parameter `autoExpose: Boolean = true` to `moveTableauToTableau` and `moveTableauToFoundation`.
  - Verified with 19 new unit tests (`KlondikeRulesScoringAndExposeTest`) and updated existing tableau/foundation tests, bringing total test suite to 118 unit tests (100% pass).
- **2026-09-23 (Task 1.6):** Implemented foundation building rules: `canPlaceOnFoundation`, `findTargetFoundationIndex`, `canMoveWasteToFoundation`, `moveWasteToFoundation`, `canMoveTableauToFoundation`, `moveTableauToFoundation`, `canMoveFoundationToTableau`, `moveFoundationToTableau`, and `isGameWon`. Verified with 22 unit tests (`KlondikeRulesFoundationTest`), bringing total tests to 99 (100% pass).
- **2026-09-23 (Task 1.5):** Implemented tableau placement (`canPlaceOnTableau`), multi-card sequence validation (`isValidTableauSequence`), Waste-to-Tableau moves (`canMoveWasteToTableau`, `moveWasteToTableau`), and Tableau-to-Tableau single and multi-card moves (`canMoveTableauToTableau`, `moveTableauToTableau`) in pure Kotlin `KlondikeRules`. Verified with 25 unit tests following backtick naming style (`KlondikeRulesTableauTest`), bringing total tests to 77 (100% pass).
- **2026-09-23 (Agent Guidelines & Best Practices):** Updated `AGENTS.md` and `tdd-workflow` skill with strict requirements:
  - Codebase reconnaissance and style matching (test method backtick naming, JUnit 5/6 assertions, structure replication).
  - Modern idiomatic Kotlin best practices (immutability, exhaustive `when`, expressive stdlib, defensive preconditions with `require`/`check`).
  - Clean Architecture & pure JVM domain testability (zero Android leaks in domain).
  - Jetpack Compose modern patterns (UDF, state hoisting, stability, canvas performance).
- **2026-09-23 (Agent Skills & Session Protocol):** Added `session-startup` skill (`.agents/skills/session-startup/SKILL.md`) and updated `AGENTS.md` with explicit Session Startup Protocol to auto-read foundational docs and project state upon greeting/session start.
- **2026-09-23 (Task 1.4):** Implemented `KlondikeRules` stock draw and infinite recycling logic supporting `DrawMode.DRAW_ONE` and `DrawMode.DRAW_THREE`. Verified with 15 unit tests covering draw order, boundary conditions, state immutability, and full multi-cycle invariance (`KlondikeRulesStockTest`).
- **2026-09-22 (Task 1.3):** Implemented `Deck` generator, shuffling utility, and `KlondikeDealer` dealing logic. Verified with 100% test coverage (`DeckTest`, `KlondikeDealerTest`).
- **2026-09-22 (Task 1.2):** Implemented core domain models: `Suit`, `Rank`, `Card`, `PileType`, `CardLocation`, and `BoardState` in pure Kotlin with JUnit 6 tests (`ModelsTest`).
- **2026-09-22 (Task 1.1):** Verified Android scaffold, Compose setup, and JUnit 6 test suite runner (`InitializationTest`).
- **2026-09-22 (Agent Skills & Guidelines):** Added agent skills (`tdd-workflow`, `state-and-git-sync`, `compose-solitaire-ui`), integrated skills into `AGENTS.md`, and added ADR protocol.

---

## Next Immediate Step
- **Target Task:** `T-2.1: State Canonicalization & Visited Pruning (SolverStateKey)`
