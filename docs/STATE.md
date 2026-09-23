# Project State & Session Memory

## Project Overview
- **App:** Solitaire (Klondike)
- **Package:** `io.github.qdiaps.solitaire`
- **Current Milestone:** Phase 1 - Pure Domain Engine & Core Rules
- **Active Branch:** `feature/phase-1-domain`

---

## Current Focus & Status
- **Phase:** 1 / 6
- **Completed Tasks:**
  - `Task 1.1`: Setup project structure, Kotlin source sets, and configure JUnit 6 testing dependencies.
  - `Task 1.2`: Implement core domain models: `Suit`, `Rank`, `Card`, `PileType`, `CardLocation`, and `BoardState`.
  - `Task 1.3`: Implement `Deck` generator, shuffling utility, and initial Klondike 7-column deal logic with unit tests.
  - `Task 1.4`: Implement `KlondikeRules`: Stock draw & recycling logic (Draw 1 / Draw 3) with unit tests.
  - `Task 1.5`: Implement `KlondikeRules`: Tableau-to-Tableau and Waste-to-Tableau movement validation with unit tests.
  - `Task 1.6`: Implement `KlondikeRules`: Foundation building validation (Ace to King by suit) with unit tests.
  - `Task 1.7`: Implement auto-exposing face-down cards and scoring calculation on moves.
  - `Task 1.8`: Implement `SmartTapResolver` (Priority: Foundation > Expose hidden > Leftmost valid tableau) with unit tests.
- **Current Focus:** Ready to start Task 1.9.
- **Blockers / Technical Debt:** None.

---

## Recent Progress Log
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
- **Target Task:** `Task 1.9: Implement UndoManager (state snapshot rollback for board, score, moves) with unit tests.`
