# Project State & Session Memory

## Project Overview
- **App:** Solitaire (Klondike)
- **Package:** `io.github.qdiaps.solitaire`
- **Current Milestone:** Phase 5 - Game Loop, Scoring, Auto-Complete & Persistence
- **Active Branch:** `feature/phase-5-game-loop-and-customization`

---

## Current Focus & Status
- **Phase:** 5 / 7 (Game Loop, Scoring, Auto-Complete & Persistence)
- **Completed Milestones Summary:**
  - **Phase 1: Pure Domain Engine** — 159 unit tests (100% pass), models, rules, scoring, smart tap, undo. (Complete)
  - **Phase 2: Solvability Engine & Background Generator** — 80 unit tests (100% pass), A* solver, deadlock detector, buffered deal generator. (Complete)
  - **Phase 3: Compose Board Layout & Static Presentation** — 31 unit tests (100% pass), full vector board, themes, dimensions, 38 previews. (Complete)
  - **Phase 4: Drag-and-Drop & Interactive Gameplay** — 154 unit tests (100% pass, 423 total suite tests), MVI contract, `GameViewModel`, timer, smart tap, hitboxes, drag overlay, snap-back physics, universal haptics (ERM + LRA), flight animations, 3D card flips, low-latency SoundPool audio feedback engine, and `MainActivity` wiring. (Complete)
  - *(Full historical task breakdown archived in [docs/archive/STATE_HISTORY.md](archive/STATE_HISTORY.md))*
- **Current Focus:** Completed `T-5.8` (`SettingsBottomSheet`). Ready to proceed to `T-5.9` (`StatsRepository`).
- **Blockers / Technical Debt:** None.

---

## Recent Progress Log
- **2026-09-26 (Bug Fixes & Polish):**
  - **Fix 1 (Audio & Haptics Muting):** Added `isEnabled: Boolean` to `SolitaireHaptics` and `AndroidSolitaireHaptics`, passed `enabled` through `rememberSolitaireHaptics` and `rememberSolitaireAudio` in `SolitaireGameScreen` to strictly mute all sound and haptic feedback when disabled. (`356c98c`)
  - **Fix 2 (Settings Timer Pause):** Paused elapsed timer upon `openSettings()` and resumed on `closeSettings()` if `autoStartTimer` is enabled and game is not won. Added unit test in `GameViewModelTest`. (`fc2d789`)
  - **Fix 3 (Auto-Hint Inactivity Timer):** Implemented 10-second idle timer in `GameViewModel` triggering `requestHint()` on player inactivity when `autoHintEnabled` is true. Any player action, settings sheet opening, or game victory resets or cancels the timer cleanly. Added comprehensive unit tests in `GameViewModelTest`. (`47bb0fc`)
  - **Fix 4 (Draw 3 Waste Horizontal Fan):** Implemented horizontal 3-card fanning for the waste pile in `WastePileView` and `TopRowView` with left-handed mode mirroring. Updated card flight animations (`animatedStockClick` and `animatedWasteClick`) to correctly target the fanned position. Added `WastePileOffsetTest` with 8 unit tests and `TopRowDrawThreePreview`. (`d4342ba`)
  - Full suite verified: 522 unit tests passing (100% pass), 0 Android lint errors (`./gradlew check`).
- **2026-09-26 (T-5.8: Settings Bottom Sheet UI):**
  - Implemented `SettingsBottomSheet.kt` and `SettingsSheetContent` in `io.github.qdiaps.solitaire.ui.game.components`:
    - **Gameplay section:** Segmented choice row for Draw Mode (`Draw 1 Card` vs `Draw 3 Cards`), toggle switch for Left-Handed Mode (mirrored layout), toggle switch for Auto-Hints.
    - **Appearance section:** 4 selectable felt cloth swatches (`Classic Green`, `Deep Navy`, `Dark Charcoal`, `Wine Red`) with active checkmark indicators; 4 card back style miniature vector previews (`Classic Lattice`, `Crimson Vintage`, `Emerald Art Deco`, `Obsidian Minimal`); card face style selector (`Modern Clean`).
    - **Feedback section:** Toggle switches for Sound Effects (`soundEnabled`) and Haptic Feedback (`hapticsEnabled`).
    - **Reset section:** Styled "Reset Settings to Defaults" action button.
  - Implemented live immediate application: setting changes update `GameUiState` synchronously for instant feedback and persist asynchronously to `SettingsRepository` in DataStore without restarting games.
  - Connected `drawStockCard()`, `requestHint()`, and `undoMove()` in `GameViewModel` to reactive `_uiState.value.drawMode` for live draw mode changes.
  - Gated audio and haptics in `SolitaireGameScreen` with `uiState.soundEnabled` and `uiState.hapticsEnabled`.
  - Added `GameViewModel.provideFactory(context)` and updated `MainActivity` to instantiate `GameViewModel` with it.
  - Added unit test coverage: `GameContractTest` and `GameViewModelTest` (`SettingsIntentsTests` covering all 8 settings intents, state toggles, and DataStore synchronization).
  - Added Compose Previews in `SettingsBottomSheetPreview.kt` across felt themes and device heights (compact 600dp, standard Pixel 7, and full in-game modal preview).
  - Full suite verified: 506 unit tests passing (100% pass), 0 Android lint errors (`./gradlew check`).
- **2026-09-26 (T-5.7: DataStore Manager & Settings Repository):**
  - Implemented `DataStoreManager` in `io.github.qdiaps.solitaire.data.local`: thread-safe and resilient wrapper around Jetpack `DataStore<Preferences>` with `IOException` error boundary, type-safe getters/setters, preference removal, and clear helpers.
  - Implemented `@Immutable` `GameSettings` in `io.github.qdiaps.solitaire.data.model` covering all game customization options and toggles: `drawMode`, `isLeftHanded`, `feltTheme`, `cardBackStyle`, `cardFaceStyle`, `soundEnabled`, `hapticsEnabled`, and `autoHintEnabled`.
  - Implemented `SettingsRepository` interface and `DataStoreSettingsRepository` in `io.github.qdiaps.solitaire.data.repository`:
    - Reactive `settingsFlow: Flow<GameSettings>` with `distinctUntilChanged()`.
    - Granular type-safe mutators (`setDrawMode`, `setLeftHanded`, `setFeltTheme`, `setCardBackStyle`, `setCardFaceStyle`, `setSoundEnabled`, `setHapticsEnabled`, `setAutoHintEnabled`).
    - Atomic batch updating (`updateSettings`).
    - Resilient fallback handling for corrupted/unknown enum values.
    - Non-destructive `resetToDefaults()` preserving stats and game persistence.
  - Added comprehensive test suites: `DataStoreManagerTest` (4 unit tests) and `SettingsRepositoryTest` (13 unit tests).
  - Verified test suite: 498 unit tests passing (100% pass), 0 Android lint errors (`./gradlew check`).
- **2026-09-26 (Fix: Auto-Complete Stock/Waste Promotion & UI Hang Resolution):**
  - Identified and resolved the bug where auto-completing with remaining stock cards caused the cascade to stall after tableau moves, locking the UI with an active timer and unresponsive touch barrier.
  - **Root Cause 1 (Stock evaluation ignored):** In `AutoCompleteResolver.kt`, `KlondikeRules.findTargetFoundationIndex(state, stockCard)` checked `if (!card.isFaceUp) return null`. Because all stock cards have `isFaceUp = false`, it returned `null` for every stock card, causing `nextMove` to report no available moves while cards remained in stock.
  - **Root Cause 2 (UI lock in un-reset auto-complete state):** When `nextMove` returned `null` or when the animation loop terminated, `animatedAutoCompleteClick` broke out of its coroutine loop without resetting `isAutoCompleting = false`, leaving the transparent full-screen touch interceptor permanently blocking touches while the timer continued to run.
  - **Root Cause 3 (Competing concurrent loops):** `SolitaireGameScreen` was triggering `onAutoCompleteClick()` (launching headless `viewModel.autoComplete()` in parallel) while simultaneously running its own flight animation loop via `applyAutoCompleteMove(move)`.
  - **Resolution:**
    - Updated `AutoCompleteResolver.kt` to evaluate stock cards with `stockCard.copy(isFaceUp = true)` and check all accessible cards in waste and stock, properly cascading them to foundations with rank-first ordering.
    - Added `GameIntent.StartAutoComplete` and `GameIntent.FinishAutoComplete` to `GameContract` and `GameViewModel`.
    - Updated `SolitaireGameScreen.kt` to decouple the UI animation loop from the headless ViewModel loop, wrapping flight execution in `try { ... } finally { latestOnAutoCompleteFinished() }` ensuring `isAutoCompleting` is guaranteed to reset to `false` on any exit.
  - Verified suite: 481 unit tests passing (100% pass), 0 Android lint errors (`./gradlew check`).
- **2026-09-26 (Fix: Highlight Stock Recycle Placeholder instead of Waste Card on Stock Recycle Hint):**
  - Identified and fixed bug where requesting a hint when stock is empty and recycling is recommended erroneously highlighted the top face-up waste card instead of the empty stock recycle placeholder (`CardSlotPlaceholder`).
  - Root cause: `HintResolver` emitted recycle move with `source = CardLocation.Waste`, causing `SolitaireGameScreen` to treat the top waste card as `highlightedCard`. The user saw a glowing card in waste with nowhere legal to move it, while the recycle slot remained unhighlighted.
  - In `HintResolver.kt`: set recycle move `source = CardLocation.Stock` (matching user tap interaction on the stock slot to trigger recycle).
  - In `GameContract.kt`: ensured `highlightedCard = null` and `highlightedCards = emptyList()` whenever `hintSourceLocation is CardLocation.Stock`.
  - In `SolitaireGameScreen.kt`: updated `isWasteHighlighted = isHintActive && (hintSourceLocation is CardLocation.Waste)` and guarded `TableauAreaView` source card highlights so only tableau hints highlight tableau cards.
  - Result: when recycling is recommended, only the empty stock placeholder with the circular recycle icon pulses in radiant amber-gold, clearly prompting the user to tap the placeholder to recycle the deck.
  - Verified suite: 479 unit tests passing (100% pass), 0 Android lint errors (`./gradlew check`).
- **2026-09-26 (Fix: Refine Auto-Complete Conditions, Flight Animations, and Touch Interception):**
  - **Condition (Point 1):** Updated `AutoCompleteResolver.isAutoCompleteReady(state)` to require only that all tableau cards are face-up (`state.tableau.all { col -> col.all { it.isFaceUp } } && !KlondikeRules.isGameWon(state)`). Auto-complete activates regardless of remaining stock or waste cards. Updated `nextMove` and `resolveAllMoves` to sequentially promote cards from stock and waste in addition to tableau without deadlock.
  - **Haptics/Audio Muting (Point 2):** Removed per-card `GameEvent.PlayHapticSnap` emissions during the auto-complete cascade loop in `GameViewModel`. The cascade runs smoothly and quietly without vibrations on every step, emitting only the final `TriggerWinCelebration` upon victory.
  - **Animated Card Flight (Point 3):** Replaced instantaneous board teleportation with smooth flight animations (`cardFlightState.startFlight` with 130ms duration) in `SolitaireGameScreen`. Cards fly smoothly from Tableau, Waste, or Stock directly to their target foundation pile, with 3D flip for stock cards, matching the smart tap flight experience.
  - **Touch & Drag Interception (Point 4):** Added `isAutoCompleting` flag to `GameUiState`. When auto-complete starts, `SolitaireGameScreen` immediately resets `dragDropState` (returning any held cards), displays a full-screen transparent touch barrier intercepting all pointer events (`PointerEventPass.Initial`), and guards all click handlers against interaction during the cascade.
  - Full suite verified: 478 unit tests passing (100% pass), 0 Android lint errors (`./gradlew check`).
- **2026-09-26 (T-5.6: Auto-Complete Cascade Execution in ViewModel & UI):**
  - Updated `GameUiState.isAutoCompleteAvailable` to reactively flag when auto-complete conditions are met (`AutoCompleteResolver.isAutoCompleteReady(state)`).
  - Implemented `GameIntent.AutoComplete` coroutine loop in `GameViewModel`: sequentially applies foundation promotions with 120ms delay (configurable via `autoCompleteDelayMs`), updating moves/score, and emitting `GameEvent.TriggerWinCelebration` upon victory.
  - Guarded user interactions (`onCardTapped`, `drawStockCard`, `recycleStock`, `onCardDropped`) by ignoring user input while cascade job is active.
  - Automatically cancels auto-complete loop on user interrupt actions (`undoMove`, `startNewGame`, `restartGame`, `onCleared`).
  - Implemented `AutoCompleteBannerView.kt` in `io.github.qdiaps.solitaire.ui.game.components` with animated slide/fade entry, gold border styling, and vector fast-forward chevrons.
  - Integrated auto-complete banner into `SolitaireGameScreen` layout right above `BottomActionBarView`, delegating clicks to `GameIntent.AutoComplete`.
  - Added unit test suite in `GameViewModelTest` and end-to-end wiring tests in `SolitaireGameScreenTest` verifying cascade loop execution, state updates, cancellation on restart, and win event trigger.
  - Full suite verified: 478 unit tests passing (100% pass), 0 Android lint errors (`./gradlew check`).
- **2026-09-26 (T-5.5: Pure Domain Auto-Complete Resolver):**
  - Implemented pure Kotlin `AutoCompleteResolver` (`io.github.qdiaps.solitaire.domain.rules`) detecting when all tableau columns contain zero face-down cards, stock is exhausted, and all remaining cards across tableau and waste can be safely cascaded to foundations without deadlock.
  - Implemented `AutoCompleteMove` model (`card`, `from`, `to`, `resultingState`, with aliases `source` and `destination`).
  - Implemented `nextMove(state: BoardState)` generating the next immediate foundation promotion, prioritized strictly by rank (lowest rank first: Aces before 2s, 2s before 3s) and deterministic location/suit ordering for a balanced victory cascade.
  - Implemented `resolveAllMoves(state: BoardState)` performing end-to-end non-destructive simulation of foundation promotions from tableau and waste until all cards are cleared, returning the complete sequential move list.
  - Implemented `isAutoCompleteReady(state: BoardState)` / `canAutoComplete(state: BoardState)` validating readiness conditions and verifying that full clearance is mathematically achievable.
  - Added comprehensive unit test suite `AutoCompleteResolverTest` (17 tests) covering readiness conditions, waste/tableau step generation, rank-first ordering, partial deals, 52-card victory cascade, and property aliases.
  - Full suite verified: 472 unit tests passing (100% pass), 0 Android lint errors (`./gradlew check`).
- **2026-09-26 (T-5.4c: Fix Stock Draw Flying Card Morphing / Desync Bug):**
  - Identified and fixed bug where clicking on the stock pile displayed one card during the 3D flight animation, but upon landing in the waste pile, the card became a different card.
  - Root cause: `KlondikeRules.draw()` extracts cards from the beginning of the stock list (`state.stock.take(count)`), whereas `SolitaireGameScreen.kt` took `boardState.stock.last()` (the 24th/bottom card of the stock pile) as the animated `movingCard`.
  - Added `drawMode: DrawMode = DrawMode.DRAW_ONE` to `GameUiState` and `SolitaireGameScreen`.
  - Updated `SolitaireGameScreen.kt` to compute `movingCard = boardState.stock.take(drawCount).last()`, perfectly matching the domain rules for both Draw 1 and Draw 3 modes.
  - Guarded `isFlipping = animateFlip && flipAnimatable.value < 1f && card.isFaceUp` in `CardView.kt` to eliminate 1-frame face-down flashes when `animateFlip = false`.
  - Added `key(card.id)` to `AnimatedMoveOverlay.kt` to prevent composable slot reuse glitches across flights.
  - Verified suite: 457 unit tests pass (100%), 0 Android lint errors.
- **2026-09-26 (T-5.4b: Fix Ghost Card Flip Turnover Animation on New Game / Deal Reset):**
  - Identified and fixed intermittent 3D card flip turnover animation and audio playing on face-up tableau cards when dealing a new game or restarting.
  - Root cause: Compose node slot reuse across deals preserved stale `previousFaceUp = false` state for static card IDs (`card.id`), causing `LaunchedEffect(card.isFaceUp)` to erroneously interpret freshly dealt face-up cards as having just been uncovered.
  - Added `gameSessionId: Long` to `GameUiState`, automatically incremented by `GameViewModel` on each `StartNewGame` and `RestartGame`.
  - Exposed `LocalGameSessionId` composition local and keyed tableau column cards with `"${gameSessionId}_${card.id}"`, resetting all remembered flip states on every deal.
  - Added `animateFlip: Boolean = true` parameter to `CardView`, explicitly disabling turnover animations in `AnimatedMoveOverlay`, `StockPileView`, `WastePileView`, `FoundationRowView`, and `DragOverlay`.
  - Fixed `StockPileView` static card ID from colliding with the real King of Spades by assigning dedicated ID `"STOCK_PILE_TOP"`.
  - Cancelled any active card flights or drag gestures on deal reset.
  - Suite verification: 457 unit tests passing (100%), 0 Android lint errors.
- **2026-09-26 (T-5.4: Hint Pulsing UI Highlighting & ViewModel Integration):**
  - Integrated `HintResolver` with `GameViewModel`: implemented `requestHint()` and `dismissHint()`, connected `GameIntent.RequestHint` and `GameIntent.DismissHint`.
  - Updated `GameContract`: `GameUiState.activeHint` is now `Hint?`, exposing `highlightedCard`, `hintSourceLocation`, `hintTargetLocation`, and `isHintActive`.
  - Implemented high-contrast, radiant amber-gold hint highlighting (`HintHighlight = Color(0xFFFFB300)`):
    - Replaced low-contrast emerald green with vibrant amber-gold, delivering crisp visibility across all felt themes (especially Classic Green).
    - `CardView`: 3.dp gold pulsing border, 3D elevation shadow lift (6..10dp), gentle breathing scale (1.00x..1.035x), and luminous warm gold surface wash (14%..30% alpha) illuminating the entire card.
    - `CardSlotPlaceholder`: 3.dp gold pulsing border, glowing amber background fill (16%..32% alpha), and luminous golden watermark symbols.
    - Multi-card stack highlighting: all cards in a moving cascade sequence (e.g. 6-5-4 moving onto 7) now simultaneously highlight and pulse in unison with the warm golden aura, clearly indicating the whole sub-stack move.
    - Fixed Compose Preview rendering in `SolitaireGameScreenPreview` by guarding `rememberSolitaireAudio` and `rememberSolitaireHaptics` with `LocalInspectionMode.current` and adding safe width calculation fallback in `SolitaireGameScreen`.
  - Wired hint source and destination coordinates to `SolitaireGameScreen`, `TopRowView`, `StockPileView`, `FoundationRowView`, `TableauAreaView`, and `TableauColumnView`.
  - Added auto-dismiss of active hint upon any player touch: tapping card, drawing/recycling stock, dropping card, or undoing move.
  - Added unit test coverage in `GameViewModelTest.kt` (7 tests) and `GameContractTest.kt` (all 455 suite tests passing 100%).
  - Updated Compose preview in `CardViewPreview.kt` showcasing empty slot hint destination highlighting.
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
- **Target Task:** `T-5.9: Statistics Repository (StatsRepository)`
