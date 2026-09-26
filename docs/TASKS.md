# Current Sprint Tasks - Phase 6: Victory Screen & Polish

> **Current Phase:** Phase 6 (Victory Screen & Polish)
> **Branch:** `feature/phase-6-victory-screen-and-polish`

- [ ] **T-6.1: Victory Animation Extensible Architecture & Bouncing Physics Engine (`VictoryAnimator`, `BouncingCardsPhysics`)**
  - Design extensible `VictoryAnimationType` enum (`CLASSIC_BOUNCE`, expandable to future effects like `FIREWORKS`, `CARD_SPIRAL`, etc.) and `VictoryAnimator` abstraction.
  - Implement pure math `BouncingCardsPhysics` engine: particles with position, velocity ($v_x, v_y$), gravity, restitution coefficient ($e \approx -0.85$), and screen boundary bouncing.
  - Implement deterministic `CascadeSequencer` queuing and releasing all 52 foundation cards in sequence with configurable intervals.
  - *TDD/Unit Tests:* `BouncingCardsPhysicsTest` and `CascadeSequencerTest` validating step calculus, gravity, floor bounces, lateral momentum, and termination.

- [ ] **T-6.2: High-Performance Card Sprite Cache (`CardSpriteCache`)**
  - Implement `CardSpriteCache` pre-rendering or capturing all 52 card faces (styled with active `CardFaceStyle`) into reusable hardware-backed `ImageBitmap` / `Picture` instances.
  - Eliminate vector `Path` and typography recalculations during the 60/120 FPS animation loop.
  - Provide lifecycle-safe memory reclamation when animation completes or is dismissed.
  - *TDD/Unit Tests:* `CardSpriteCacheTest` validating cache population, dimensions matching `CardDimensions`, and clean memory eviction.

- [ ] **T-6.3: Hardware-Accelerated Victory Canvas Overlay (`VictoryOverlay`, `ClassicBounceRenderer`)**
  - Implement pluggable `VictoryRenderer` interface and `ClassicBounceRenderer` drawing to hardware-accelerated Compose `Canvas`.
  - Implement persistent motion trails (Windows Solitaire classic card ghosting trail) without full canvas clears or excessive allocations.
  - Implement game loop driving frame ticks via `withFrameNanos` ensuring smooth 60/120 FPS rendering.
  - Implement full-screen touch barrier with tap-to-skip gesture handling (`Modifier.pointerInput`).
  - *Compose Previews:* `VictoryOverlayPreview` demonstrating active particle cascade and motion trails.

- [ ] **T-6.4: Victory Audio Fanfare & Celebration Haptics (`SolitaireAudio`, `SolitaireHaptics`)**
  - Create and bundle crisp uncompressed 16-bit 44.1kHz PCM WAV audio asset `victory_fanfare.wav` in `app/src/main/res/raw/`.
  - Add `playWinFanfare()` to `SolitaireAudio` and `AndroidSolitaireAudio`, respecting `soundEnabled`.
  - Add celebratory victory haptic sequence `playWinCelebration()` to `SolitaireHaptics` and `AndroidSolitaireHaptics`, respecting `hapticsEnabled`.
  - *TDD/Unit Tests:* `SolitaireAudioTest` and `SolitaireHapticsTest` validating victory sound/vibration triggering and muting guards.

- [ ] **T-6.5: Victory Summary Data Model & Records Calculation (`VictorySummary`, `GameContract`)**
  - Define `@Immutable` `VictorySummary` data model: `timeSeconds`, `movesCount`, `score`, and record breakthrough flags (`isNewBestTime`, `isNewFewestMoves`, `isNewHighScore`, `isNewBestStreak`).
  - Update `GameUiState` with `isVictoryAnimationActive: Boolean`, `victorySummary: VictorySummary?`, and `selectedVictoryAnimation: VictoryAnimationType`.
  - Add intents: `StartVictoryAnimation`, `SkipWinAnimation`, `DismissVictorySummary`.
  - *TDD/Unit Tests:* `GameContractTest` and `VictorySummaryTest` checking record comparisons against current `GameStats`.

- [ ] **T-6.6: Victory Summary Dialog UI (`VictorySummaryDialog`)**
  - Implement celebratory modal dialog `VictorySummaryDialog.kt` displaying finished game time, moves, score, and glowing golden badges for new personal records.
  - Provide quick action buttons: "New Game", "Play Again" (restart same deal), and "Close / View Board".
  - *Compose Previews:* `VictorySummaryDialogPreview` showcasing standard win, new record break, and dark felt themes.

- [ ] **T-6.7: ViewModel & Screen Integration (`GameViewModel`, `SolitaireGameScreen`)**
  - Connect win detection (`isGameWon`) to trigger `StartVictoryAnimation`, compute `VictorySummary`, and update `StatsRepository`.
  - Wire `VictoryOverlay` into `SolitaireGameScreen` layout hierarchy above board components.
  - Wire `SkipWinAnimation` on screen tap to immediately halt the canvas loop and present `VictorySummaryDialog`.
  - *TDD/Unit Tests:* `GameViewModelTest` and `SolitaireGameScreenTest` verifying win flow, tap-to-skip, audio/haptic dispatch, and dialog actions.

- [ ] **T-6.8: Performance Profiling, Memory Leak Audit & Final Polish**
  - Profile frame rendering times using Android Profiler / Compose Tracing, verifying stable 60/120 FPS with 0 jank frames.
  - Audit heap allocations: ensure 0 object allocations during the continuous `Canvas` animation draw loop.
  - Verify lifecycle resilience: backgrounding the app (`ON_PAUSE` / `ON_STOP`) during victory cascade safely pauses/cancels loops without memory leaks.
  - Verify full test suite: `./gradlew check` and `./gradlew test` (100% pass, 0 lint warnings).
  - Document ADR 010 in `docs/ARCHITECTURE.md` covering extensible victory animations and canvas motion trails.

---

> **Historical Archive:** Completed tasks from Phases 1 through 5 are preserved in [docs/archive/TASKS_HISTORY.md](archive/TASKS_HISTORY.md).
