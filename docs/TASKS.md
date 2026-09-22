# Current Sprint Tasks - Phase 1: Pure Domain Engine

> **Current Phase:** Phase 1 (Pure Domain Engine & Core Rules)
> **Branch:** `feature/phase-1-domain`

- [x] **Task 1.1:** Setup project structure, Kotlin source sets (`app/src/main/kotlin/io/github/qdiaps/solitaire/`), and configure JUnit 6 testing dependencies.
- [ ] **Task 1.2:** Implement core domain models: `Suit`, `Rank`, `Card`, `PileType`, `CardLocation`, and `BoardState`.
- [ ] **Task 1.3:** Implement `Deck` generator, shuffling utility, and initial Klondike 7-column deal logic with unit tests.
- [ ] **Task 1.4:** Implement `KlondikeRules`: Stock draw & recycling logic (Draw 1 / Draw 3) with unit tests.
- [ ] **Task 1.5:** Implement `KlondikeRules`: Tableau-to-Tableau and Waste-to-Tableau movement validation with unit tests.
- [ ] **Task 1.6:** Implement `KlondikeRules`: Foundation building validation (Ace to King by suit) with unit tests.
- [ ] **Task 1.7:** Implement auto-exposing face-down cards and scoring calculation on moves.
- [ ] **Task 1.8:** Implement `SmartTapResolver` (Priority: Foundation > Expose hidden > Leftmost valid tableau) with unit tests.
- [ ] **Task 1.9:** Implement `UndoManager` (state snapshot rollback for board, score, moves) with unit tests.
- [ ] **Task 1.10:** Phase 1 review, refactoring to idiomatic Kotlin, and verification that all tests pass.
