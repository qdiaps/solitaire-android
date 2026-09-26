# Project State & Session Memory

## Project Overview
- **App:** Solitaire (Klondike)
- **Package:** `io.github.qdiaps.solitaire`
- **Current Milestone:** Phase 4 - Drag-and-Drop & Interactive Gameplay (Complete, Polished & Audio Feedback Added)
- **Active Branch:** `feature/phase-4-interactive-gameplay`

---

## Current Focus & Status
- **Phase:** 4 / 6 (Phase 4 Completed, Polished & Audio-Enhanced)
- **Completed Milestones Summary:**
  - **Phase 1: Pure Domain Engine** — 159 unit tests (100% pass), models, rules, scoring, smart tap, undo. (Complete)
  - **Phase 2: Solvability Engine & Background Generator** — 80 unit tests (100% pass), A* solver, deadlock detector, buffered deal generator. (Complete)
  - **Phase 3: Compose Board Layout & Static Presentation** — 31 unit tests (100% pass), full vector board, themes, dimensions, 38 previews. (Complete)
  - **Phase 4: Drag-and-Drop & Interactive Gameplay** — 154 unit tests (100% pass, 424 total suite tests), MVI contract, `GameViewModel`, timer, smart tap, hitboxes, drag overlay, snap-back physics, universal haptics (ERM + LRA), flight animations, 3D card flips, low-latency SoundPool audio feedback engine, and `MainActivity` wiring. (Complete)
  - *(Full historical task breakdown archived in [docs/archive/STATE_HISTORY.md](archive/STATE_HISTORY.md))*
- **Current Focus:** Phase 4 completed, polished with universal ERM/MediaTek haptics, 60/120 FPS flight & 3D flip animations, and low-latency acoustic SoundPool audio feedback. Ready for Phase 5 (Visual Polish, Themes & Customization).
- **Blockers / Technical Debt:** None.

---

## Recent Progress Log
- **2026-09-25 (T-4.13: Acoustic Audio Feedback Engine via SoundPool):**
  - Designed and implemented low-latency acoustic card audio engine using Android `SoundPool` configured with `AudioAttributes.USAGE_GAME` and `CONTENT_TYPE_SONIFICATION`.
  - Generated and bundled 4 uncompressed 16-bit 44.1kHz PCM WAV audio assets in `app/src/main/res/raw/`:
    - `card_slide.wav` (75ms, subtle paper friction on card lift / drag start).
    - `card_snap.wav` (65ms, crisp placement click with felt table impact resonance).
    - `card_flip.wav` (80ms, card turnover flick & flutter on reveals and stock draws).
    - `card_deal.wav` (160ms, multi-card cascading riffle deal on new game deals).
  - Created `SolitaireAudio` interface and `AndroidSolitaireAudio` implementation with async sample preload tracking and fallback guards.
  - Provided `LocalSolitaireAudio` and `rememberSolitaireAudio()` composables.
  - Integrated `playSlide()` and `playSnap()` into `CardDragModifier.kt` for tactile pickup and drop sound.
  - Integrated `playFlip()` into `CardView.kt` when face-down cards are turned face-up.
  - Added `GameEvent.PlayDealSound` in `GameContract.kt` and wired it in `GameViewModel.kt` on new deals and restarts.
  - Added unit test suite `SolitaireAudioTest`, updated `GameContractTest` and `SolitaireGameScreenTest`.
  - Full suite passed: 424 unit tests passing (100%), 0 failures, 0 Android lint errors.
- **2026-09-25 (T-4.12: Universal ERM Haptics Engine & MediaTek HAL Fallback):**
  - Identified silent vibration drop on MediaTek Helio G99 / rugged devices (Hotwav Cyber X) where vendor HAL ignores `createPredefined(EFFECT_CLICK)` without throwing exceptions and Compose view haptics get silenced.
  - Added `vib.areAllEffectsSupported(effect)` guard before using `createPredefined`.
  - Added calibrated one-shot pulse fallbacks designed for heavy rugged phones (~380g) with ERM motors: 45ms tick, 65ms pickup, 90ms snap with `VibrationEffect.DEFAULT_AMPLITUDE` overcoming motor rotor inertia.
  - Upgraded vibration attributes to `VibrationAttributes.createForUsage(USAGE_HARDWARE_FEEDBACK)` (API 33+) and `AudioAttributes.USAGE_GAME` (API 26+) ensuring game tactile feedback is never muted by system keyboard/touch toggle.
  - Added unit test suite `SolitaireHapticsTest`.
  - Full suite passed: 420 unit tests passing (100%), 0 failures, 0 Android lint errors.
- **2026-09-25 (T-4.11: Smooth Flight Animations & 3D Flips):**
  - Implemented `CardFlightState` managing in-flight card interpolation, 3D flip rotation, and source card masking without triggering recomposition loops.
  - Implemented `AnimatedMoveOverlay` floating overlay layer rendering flying cards with elevation shadow (10.dp) and 3D Y-axis rotation with perspective camera distance.
  - Added smart tap flight animation: when tapping a card that can move to Foundation or Tableau, it smoothly flies to the target destination over 180ms before completing the move and playing tactile haptics.
  - Added Stock draw flip animation: 3D rotation and flight from Stock to Waste pile (0°..90° back -> 90°..0° face) over 180ms.
  - Added 3D card reveal flip in `CardView`: uncovering face-down cards animates rotation around Y-axis with perspective camera distance over 200ms.
  - Mounted permanent baseline placeholders in `TableauColumnView`, `FoundationPileView`, and `WastePileView` with `underCard` rendering so empty slots never pop visually.
  - Registered `Stock` and `Waste` bounds in `DropTargetRegistry` for reliable trajectory calculation in normal and left-handed modes.
  - Added unit test suite `CardFlightStateTest` with 5 unit tests covering state lifecycle, linear offset interpolation, 3D angle calculations, cancellation, and callbacks.
  - Full suite passed: 418 unit tests passing (100%), 0 failures, 0 Android lint errors.
- **2026-09-25 (T-4.10: Drag-and-Drop Polish, Stale Gesture Recomposition & Haptics Engine Bugfixes):**
  - Resolved stale card closure bug where cards dragged from Waste or Foundation retained the identity of the first card pulled: added `rememberUpdatedState` for `currentOnStartDrag`, `currentBoardState`, and `currentOnValidDrop` in `CardDragModifier.kt`.
  - Resolved tableau stack slicing synchronization issue when moving sequences onto columns: wrapped card renderers in `key(card.id)` across `TableauColumnView.kt`, `WastePileView.kt`, and `FoundationPileView.kt` so item compositions preserve gesture identity when card lists update.
  - Replaced sluggish spring snap-back animation (`DampingRatioMediumBouncy`) with snappy 160ms easing curve (`tween(160, FastOutSlowInEasing)`), removing trailing oscillations and delays.
  - Added race condition protection in `DragDropState`: `startDrag` resets `isSnappingBack` immediately, interrupting any ongoing return animation and allowing new card pickups with zero delay.
  - Fixed coordinate offset in `SolitaireGameScreen`: moved `statusBarsPadding()` and `navigationBarsPadding()` from the outer constraint box to the inner content column, perfectly aligning `DragOverlay` coordinates with `boundsInRoot()`.
  - Added hardware vibrator engine `SolitaireHaptics` and `rememberSolitaireHaptics()`, added `<uses-permission android:name="android.permission.VIBRATE" />` to `AndroidManifest.xml`, ensuring crisp tactile feedback on pickup, snap drop, and card dealing across physical devices.
  - Full suite passed: 413 unit tests passing (100%), 0 failures, 0 Android lint errors.

---

## Next Immediate Step
- **Target Task:** Phase 4 completed. Ready for Phase 5 (Visual Polish, Themes & Customization).
