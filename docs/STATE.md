# Project State & Session Memory

## Project Overview
- **App:** Solitaire (Klondike)
- **Package:** `io.github.qdiaps.solitaire`
- **Current Milestone:** Phase 1 - Pure Domain Engine & Core Rules
- **Active Branch:** `feature/phase-1-domain`

---

## Current Status
- **Phase:** 1 / 6
- **Completed Tasks:**
  - `Task 1.1`: Setup project structure, Kotlin source sets, and configure JUnit 6 testing dependencies.
  - `Task 1.2`: Implement core domain models: `Suit`, `Rank`, `Card`, `PileType`, `CardLocation`, and `BoardState`.
  - `Task 1.3`: Implement `Deck` generator, shuffling utility, and initial Klondike 7-column deal logic with unit tests.
- **In Progress:** Ready to start Task 1.4.
- **Blockers / Technical Debt:** None.

---

## Last Session Summary
- Added pre-flight decomposition check rule to `AGENTS.md` and `.clinerules`.
- Implemented `Deck` generator and shuffling utility (`Deck.createStandard52()`, `Deck.shuffle()`) in pure Kotlin.
- Implemented `KlondikeDealer.deal()` and `KlondikeDealer.dealShuffled()` constructing valid initial `BoardState` with 7 tableau columns (28 cards, top card face-up) and 24 stock cards.
- Added comprehensive unit tests in `DeckTest` and `KlondikeDealerTest` verifying 52-card distribution, deterministic seed shuffling, and layout invariants.
- Verified test pipeline passing cleanly with `./gradlew test`.

---

## Next Step
- **Target Task:** `Task 1.4: Implement KlondikeRules: Stock draw & recycling logic (Draw 1 / Draw 3) with unit tests.`
