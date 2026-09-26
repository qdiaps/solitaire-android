# Project State & Session Memory

## Project Overview
- **App:** Solitaire (Klondike)
- **Package:** `io.github.qdiaps.solitaire`
- **Current Milestone:** Phase 5 - Game Loop, Scoring, Auto-Complete & Persistence
- **Active Branch:** `feature/phase-5-game-loop-and-customization`

---

## Current Focus & Status
- **Phase:** 5 / 6 (Game Loop, Scoring, Auto-Complete & Persistence)
- **Completed Milestones Summary:**
  - **Phase 1: Pure Domain Engine** — 159 unit tests (100% pass), models, rules, scoring, smart tap, undo. (Complete)
  - **Phase 2: Solvability Engine & Background Generator** — 80 unit tests (100% pass), A* solver, deadlock detector, buffered deal generator. (Complete)
  - **Phase 3: Compose Board Layout & Static Presentation** — 31 unit tests (100% pass), full vector board, themes, dimensions, 38 previews. (Complete)
  - **Phase 4: Drag-and-Drop & Interactive Gameplay** — 154 unit tests (100% pass, 423 total suite tests), MVI contract, `GameViewModel`, timer, smart tap, hitboxes, drag overlay, snap-back physics, universal haptics (ERM + LRA), flight animations, 3D card flips, low-latency SoundPool audio feedback engine, and `MainActivity` wiring. (Complete)
  - *(Full historical task breakdown archived in [docs/archive/STATE_HISTORY.md](archive/STATE_HISTORY.md))*
- **Current Focus:** Completed `T-5.3` (Pure Domain Hint Resolver Engine). Ready to proceed to `T-5.4` (Hint UI Highlighting & ViewModel Integration).
- **Blockers / Technical Debt:** None.

---

## Recent Progress Log
- **2026-09-26 (T-5.3: Pure Domain Hint Resolver Engine):**
  - Implemented pure Kotlin `HintResolver` (`io.github.qdiaps.solitaire.domain.rules`) evaluating valid productive moves in strict accordance with SPEC priorities:
    1. `UNCOVER_FACE_DOWN` (level 1): Tableau moves or foundation promotions uncovering hidden cards underneath.
    2. `FOUNDATION_PROMOTION` (level 2): Direct promotions from tableau or waste to foundation.
    3. `TABLEAU_PROGRESS` (level 3): Waste-to-tableau placements and non-lateral sequence reorganizations.
    4. `STOCK_DRAW` (level 4): Drawing from stock or recycling waste when accessible playable cards exist in the cycle.
  - Implemented `HintPriority` enum and structured `Hint` model (`move`, `priority`, `description`, `from`, `to`, `cards`).
  - Added comprehensive unit test suite `HintResolverTest` (10 tests) covering all 4 priority tiers, tie-breaker columns, Draw 1 / Draw 3 modes, cycle lookahead, and terminal/deadlock states.
  - Full suite verified: 444 unit tests passing (100%), 0 Android lint errors (`./gradlew check`).
- **2026-09-26 (T-5.2: Card Face Typography Architecture):**
  - Implemented extensible `CardFaceStyle` enum with canonical `MODERN_CLEAN` style (SansSerif Bold), designed to easily accommodate future face themes without breaking API contracts.
  - Added `createCardTypography(faceStyle)` helper in `Type.kt`.
  - Updated `SolitaireTheme` with `LocalCardFaceStyle`, `SolitaireTheme.cardFaceStyle`, and reactive typography generation.
  - Updated `CardView.kt` (`FaceUpCardContent`) to accept `cardFaceStyle: CardFaceStyle`.
  - Added unit test suite `CardFaceStyleTest` (6 tests) and Compose preview suite `CardThemesGalleryPreview.kt`.
  - Verified test suite: all 434 unit tests pass (100% pass), 0 Android lint errors (`./gradlew check`).
- **2026-09-26 (T-5.1: Card Visual Styles & Custom Back Designs):**
  - Implemented `CardBackStyle` enum with 4 distinct visual styles: `CLASSIC_LATTICE`, `CRIMSON_VINTAGE`, `EMERALD_ART_DECO`, and `OBSIDIAN_MINIMAL`.
  - Implemented vector Canvas renderer `CardBackView.kt` in `ui/game/components/`.
  - Added unit test suite `CardBackStyleTest` (5 tests) and Compose preview suite `CardBackPreview.kt` (3 previews).
  - Verified test suite: all 428 unit tests pass (100% pass), 0 Android lint errors (`./gradlew check`).

---

## Next Immediate Step
- **Target Task:** `T-5.4: Hint Pulsing UI Highlighting & ViewModel Integration`
