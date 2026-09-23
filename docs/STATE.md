# Project State & Session Memory

## Project Overview
- **App:** Solitaire (Klondike)
- **Package:** `io.github.qdiaps.solitaire`
- **Current Milestone:** Phase 2 - Solvability Engine & Background Generator
- **Active Branch:** `feature/phase-2-solver`

---

## Current Focus & Status
- **Phase:** 2 / 6
- **Completed Tasks (Phase 2):**
  - `T-2.1`: Implement compact/canonical state key representation (`SolverStateKey`) normalizing symmetric tableau columns and stock-cycle states to prevent cyclic exploration.
  - `T-2.2`: Implement legal move generator (`SolverMoveGenerator`) producing all non-redundant successor `BoardState` transitions with empty column and lateral move pruning.
  - `T-2.3`: Implement rule-based pruning for safe foundation promotions (`SafePromotion`) collapsing unnecessary search branching.
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
- **Current Focus:** Phase 2: Solvability Engine & Background Generator (Task T-2.4).
- **Blockers / Technical Debt:** None.

---

## Recent Progress Log
- **2026-09-23 (Task T-2.3):** Implemented safe foundation auto-promotion heuristic `SafePromotion`:
  - Mathematical proof implementation: Aces & Twos are unconditionally safe; Ranks $\ge 3$ are safe once both opposite-color foundation piles have reached at least rank $R - 1$.
  - Added `findSafeTransitions` and `applyAllSafePromotions` for greedy fixed-point search space collapse.
  - Added ADR 005 in `docs/ARCHITECTURE.md`.
  - Verified with 12 unit tests in `SafePromotionTest`, bringing total unit tests to 200 (100% pass, 0 lint warnings).
- **2026-09-23 (Task T-2.2):** Implemented successor move enumerator `SolverMoveGenerator`:
  - Generates exhaustive non-redundant transitions (`SolverTransition(move, state)`) from any board state.
  - Pruning heuristics: terminal check (won state), empty column symmetry pruning (targeting only first empty column for Kings), useless lateral King move pruning (King at index 0 prohibited from jumping between empty columns), and equivalent parent rank/color pruning.
  - Optional support for foundation-to-tableau demotions (disabled by default).
  - Verified with 15 unit tests in `SolverMoveGeneratorTest`, expanding test suite to 188 passing unit tests (100% pass, 0 lint warnings).
- **2026-09-23 (Task T-2.1):** Implemented compact/canonical state key representation `SolverStateKey`:
  - Normalized tableau column order via lexicographical `ByteArray` sorting to prune symmetric column permutations and lateral King shifts.
  - Packed 4 foundation top card ranks into a 16-bit integer indexed by suit (`Suit.entries`), ensuring foundation pile ordering invariance.
  - Bit-packed individual cards into single bytes (rank in bits 0..3, suit in bits 4..5, face-up in bit 6).
  - Ensured score and movesCount invariance for pure board state equality.
  - Added visited set cyclic stock draw pruning verification and performance benchmarks (< 500ms for 10,000 keys).
  - Documented ADR 004 in `docs/ARCHITECTURE.md`.
  - Verified with 14 new unit tests (`SolverStateKeyTest`), bringing test suite to 173 unit tests (100% pass, 0 lint warnings).
- **2026-09-23 (Phase 2 Microtasks Decomposition):** Structured Phase 2 in `TASKS.md` into 8 atomic tasks (T-2.1 .. T-2.8) covering canonical state pruning, move enumeration, safe foundation heuristic, A* search loop, deadlock detection, coroutine deal buffering, and DoD benchmarks.
- **2026-09-23 (Phase 2 Initialization):** Transitioned project to Phase 2 (Solvability Engine & Background Generator):
  - Merged Phase 1 PR #1 into `master`.
  - Rebased `feature/phase-2-solver` on `master`.
  - Updated `STATE.md` and `TASKS.md` milestones.
  - Formulated high-level architectural plan for Phase 2 components (`SolvabilityChecker`, `DeadlockDetector`, `DealGenerator`).
- **2026-09-23 (Task 1.10):** Completed Phase 1 comprehensive review, refactoring, and model completion (159 unit tests, 0 errors/warnings).
- **2026-09-23 (Task 1.9):** Implemented `UndoManager` with state snapshot stacks and history limits (14 tests).
- **2026-09-23 (Task 1.8):** Implemented `SmartTapResolver` for smart card auto-moves (23 tests).
- **2026-09-23 (Task 1.7):** Implemented auto-exposing face-down cards and scoring calculation (19 tests).
- **2026-09-23 (Task 1.6):** Implemented foundation building rules and game won check (22 tests).
- **2026-09-23 (Task 1.5):** Implemented tableau placement and sequence moves (25 tests).
- **2026-09-23 (Agent Guidelines & Best Practices):** Updated `AGENTS.md` and `tdd-workflow` skill.
- **2026-09-23 (Agent Skills & Session Protocol):** Added `session-startup` skill.
- **2026-09-23 (Task 1.4):** Implemented `KlondikeRules` stock draw and recycling logic (15 tests).
- **2026-09-22 (Task 1.3):** Implement `Deck` generator, shuffling utility, and initial Klondike 7-column deal logic with unit tests.
- **2026-09-22 (Task 1.2):** Implemented core domain models in pure Kotlin.
- **2026-09-22 (Task 1.1):** Verified Android scaffold, Compose setup, and JUnit 6 test runner.

---

## Next Immediate Step
- **Target Task:** `T-2.4: Core A* / Heuristic Search Engine (SolvabilityChecker)`
