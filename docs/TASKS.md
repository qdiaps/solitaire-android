# Current Sprint Tasks - Phase 1: Pure Domain Engine

> **Current Phase:** Phase 1 (Pure Domain Engine & Core Rules) — Complete
> **Branch:** `feature/phase-1-domain`

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

---

# Upcoming Phase 2: Solvability Engine & Background Generator

- [ ] **Task 2.1:** Implement fast Klondike solvability solver (`SolvabilityChecker`) with heuristic search and state pruning.
- [ ] **Task 2.2:** Implement deadlock detector (`DeadlockDetector`) for detecting when no legal productive moves remain.
- [ ] **Task 2.3:** Implement background deal generator (`DealGenerator`) buffering 2–3 pre-verified solvable seeds.
- [ ] **Task 2.4:** Benchmark and test solver on known solvable and unsolvable test hands.
