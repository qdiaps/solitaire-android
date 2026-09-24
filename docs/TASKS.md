# Current Sprint Tasks - Phase 2: Solvability Engine & Background Generator

> **Current Phase:** Phase 2 (Solvability Engine & Background Generator)
> **Branch:** `feature/phase-2-solver`

- [x] **T-2.1: State Canonicalization & Visited Pruning (`SolverStateKey`)**
  - Implement compact/canonical state key representation normalizing symmetric tableau columns and stock-cycle states to prevent cyclic exploration.
  - *TDD:* `SolverStateKeyTest` checking symmetry handling, hash equality, and memory efficiency.
- [x] **T-2.2: Successor Move Enumerator (`SolverMoveGenerator`)**
  - Implement legal move generator producing all non-redundant successor `BoardState` transitions (Stock draw, Waste moves, Tableau sequence moves, Foundation promotions).
  - *TDD:* `SolverMoveGeneratorTest` checking exhaustive yet non-redundant move generation across varied board positions.
- [x] **T-2.3: Safe Foundation Auto-Promotion Heuristic (`SafePromotion`)**
  - Implement rule-based pruning for safe foundation promotions (e.g., Aces, Twos, and cards whose lower-ranked opposite-color cards are already banked), collapsing unnecessary search branching.
  - *TDD:* `SafePromotionTest` validating correct identification of safe vs. unsafe foundation moves.
- [x] **T-2.4: Core A* / Heuristic Search Engine (`SolvabilityChecker`)**
  - Implement heuristic search (A* / Best-First) prioritizing unexposed card reveals, foundation advancements, and minimal wasteful cycling with configurable timeout/depth limits.
  - Return structured `SolvabilityResult` (`Solvable(moves, path)`, `Unsolvable`, `Timeout`).
  - *TDD:* `SolvabilityCheckerTest` verifying solvability resolution on known hand positions and timeout guardrails.
- [x] **T-2.5: Deadlock Detector (`DeadlockDetector`)**
  - Implement real-time board analyzer detecting when no legal productive moves remain (checking stock cycle, waste moves, and tableau shifts).
  - Return `DeadlockStatus` (`ActiveGame`, `Deadlock(reason)`).
  - *TDD:* `DeadlockDetectorTest` validating detection across exhausted stock, locked tableaus, and active playable boards.
- [x] **T-2.6: Background Deal Generator (`DealGenerator`)**
  - Implement coroutine-based background deal generator running on `Dispatchers.Default` using a buffered Kotlin `Channel` (capacity 2–3) of pre-verified solvable deals.
  - Expose suspend `getSolvableDeal(): BoardState` for instant new game provisioning.
  - *TDD:* `DealGeneratorTest` testing buffer pre-filling, cancellation responsiveness, and non-blocking deal consumption.
- [ ] **T-2.7: Solvability Benchmarks & Validation on Known Deals**
  - Validate solver performance on benchmark seeds ensuring resolution meets the Definition of Done (< 300ms on benchmark seed).
  - Test on known unsolvable deals to ensure graceful exhaustion/timeout without memory leaks or infinite loops.
  - *TDD/Benchmark:* `SolvabilityBenchmarkTest`.
- [ ] **T-2.8: Phase 2 Review, Code Cleanliness & State Sync**
  - Architectural review of solver package, idiomatic Kotlin checks, `./gradlew check` & `./gradlew test` verification.
  - Update `docs/STATE.md` and commit final Phase 2 delivery.

---

# Completed Sprint Tasks - Phase 1: Pure Domain Engine

> **Phase 1 Status:** Complete (159 unit tests passing, 0 lint warnings)
> **Branch:** `feature/phase-1-domain` (merged into `master`)

- [x] **Task 1.1:** Setup project structure, Kotlin source sets (`app/src/main/kotlin/io/github/qdiaps/solitaire/`), and configure JUnit 6 testing dependencies.
- [x] **Task 1.2:** Implement core domain models: `Suit`, `Rank`, `Card`, `PileType`, `CardLocation`, and `BoardState`.
- [x] **Task 1.3:** Implement `Deck` generator, shuffling utility, and initial Klondike 7-column deal logic with unit tests.
- [x] **Task 1.4:** Implement `KlondikeRules`: Stock draw & recycling logic (Draw 1 / Draw 3) with unit tests.
- [x] **Task 1.5:** Implement `KlondikeRules`: Tableau-to-Tableau and Waste-to-Tableau movement validation with unit tests.
- [x] **Task 1.6:** Implement `KlondikeRules`: Foundation building validation (Ace to King by suit) with unit tests.
- [x] **Task 1.7:** Implement auto-exposing face-down cards and scoring calculation on moves.
- [x] **Task 1.8:** Implement `SmartTapResolver` (Priority: Foundation > Expose hidden > Leftmost valid tableau) with unit tests.
- [x] **Task 1.9:** Implement `UndoManager` (state snapshot rollback for board, score, moves) with unit tests.
- [x] **Task 1.10:** Phase 1 review, refactoring to idiomatic Kotlin, completion of `Move` model, and verification that all tests pass.
