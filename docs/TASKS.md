# Current Sprint Tasks - Phase 6: Victory Screen & Polish

> **Current Phase:** Phase 6 (Victory Screen & Polish)
> **Branch:** `feature/phase-6-victory-screen-and-polish`

- [x] **T-6.FIX: SplashScreen API & Zero-Flicker Cold Start Initialization (`MainActivity`, `GameViewModel`, `SolitaireGameScreen`)**
  - Integrated Android 12+ SplashScreen API (`androidx.core:core-splashscreen`) with custom starting theme (`Theme.App.Starting`) and animated vector spinner loader (`ic_splash_spinner.xml`).
  - Implemented `installSplashScreen().setKeepOnScreenCondition { gameViewModel.uiState.value.isLoading }` in `MainActivity`.
  - Updated `GameViewModel` to initialize with `isLoading = true` when settings or session repositories are present, asynchronously pre-loading settings and saved session before setting `isLoading = false` in a single atomic update.
  - Added loading guard with `CircularProgressIndicator` in `SolitaireGameScreen` rendering centered spinner while `isLoading == true` to guarantee zero visual flash or theme blinking on cold start.
  - *TDD/Unit Tests:* Added `ZeroFlickerAsyncInitTests` in `GameViewModelTest` verifying `isLoading` lifecycle during asynchronous initialization.

- [x] **T-6.FIX2: Configurable Deal Difficulty & Guaranteed Solvable Deal Pre-Generation (`DealDifficulty`, `DealGenerator`, `SettingsBottomSheet`)**
  - [x] **T-6.FIX2.1 (Domain Engine):** Implement `DealDifficulty` enum (`EASY`, `MEDIUM`, `RANDOM`), `DealDifficultyClassifier` heuristics (open mobility $\ge 2$ initial moves, A* states threshold $\le 250$ for Easy, standard solvable for Medium), and update `DealGenerator` to maintain buffered deals per difficulty with instant retrieval. *TDD/Unit Tests:* `DealDifficultyClassifierTest` and `DealGeneratorDifficultyTest`.
  - [x] **T-6.FIX2.2 (Data & ViewModel):** Extend `GameSettings` and `SettingsRepository` with `dealDifficulty: DealDifficulty` (persisted in DataStore, default `EASY`), update `GameViewModel` to observe difficulty, wire `DealGenerator` in `MainActivity.provideFactory`, and handle `RANDOM` bypass or `EASY`/`MEDIUM` buffered deals on cold start and new game. *TDD/Unit Tests:* `SettingsRepositoryTest` and `GameViewModelDealDifficultyTest`.
  - [x] **T-6.FIX2.3 (UI & Verification):** Add Deal Difficulty selector (`SegmentedChoiceRow`: Easy / Medium / Random) to `SettingsBottomSheet` under Gameplay section, update `SettingsBottomSheetPreview`, verify full test suite (`./gradlew test`), and validate zero-regression integration.

- [x] **T-6.FIX3: Fix Duplicate Card Flight Animation during Auto-Complete Cascade (`SolitaireGameScreen`)**
  - Fixed a state desynchronization bug in `animatedAutoCompleteClick` where `val current = currentBoardState` was re-read every loop iteration from un-recomposed Compose state, causing cards to replay their flight animation twice.
  - Retained sequential board state mutation across iterations (`var current = currentBoardState` outside loop, updated to `current = move.resultingState` per step), guaranteeing unique card flight paths.
  - *TDD/Unit Tests:* Added `step by step auto complete loop advances state sequentially with unique cards` in `SolitaireGameScreenTest` verifying unique sequential moves without duplicate card animations.

- [x] **T-6.FIX4: Record Victory Stats and Clear Session on Auto-Complete Win (`GameViewModel`)**
  - Fixed a missing stats recording bug in `GameViewModel.applyAutoCompleteMove`: previously, when winning through auto-complete cascade moves, `recordVictoryInStats()` and `cancelIdleHintTimer()` were not invoked, leaving victory statistics unrecorded in `StatsRepository` and the active session un-cleared in `GamePersistenceRepository`.
  - Added calls to `recordVictoryInStats()` and `cancelIdleHintTimer()` upon `isWon` detection in `applyAutoCompleteMove`.
  - *TDD/Unit Tests:* Added `recordGameWon is invoked when winning via ApplyAutoCompleteMove` in `GameViewModelTest` verifying that auto-complete victory properly persists win statistics and increments win counters.

- [x] **T-6.FIX5: Fast-Fail Solver Optimization, Pre-Seeded Seed Bank (200 Deals), Dynamic Scaling & Developer Debug Panel**
  - [x] **T-6.FIX5.1 (A* Solver Fast-Fail & Realistic Difficulty Thresholds):**
    - Relax `maxEasyStates` from 250 to 1000 in `DealDifficultyClassifier` for `DealDifficulty.EASY`; classify `> 1000` states as `DealDifficulty.MEDIUM`.
    - Implement aggressive Fast-Fail in `SolvabilityChecker`: introduce `fastFailTimeoutMs = 150L` and `fastFailMaxStates = 2000` for background deal screening, eliminating 1.5s stalls on unpromising or deadlocked deals.
    - *TDD/Unit Tests:* Update `DealDifficultyClassifierTest` and add `SolvabilityCheckerFastFailTest`.
  - [x] **T-6.FIX5.2 (Seed Pre-Generation Script & Initial Assets Catalog):**
    - Create CLI/Gradle script `scripts/generate_seed_bank.kts` generating 200 guaranteed solvable seeds (100 Easy, 100 Medium) verified by `SolvabilityChecker` and `DealDifficultyClassifier`.
    - Bundle the pre-verified seeds into an initial asset catalog (`app/src/main/assets/deals/seed_bank.json`).
    - *TDD/Unit Tests:* `SeedBankAssetTest` verifying JSON parsing, card completeness (52 distinct cards per deal), and determinism.
  - [x] **T-6.FIX5.3 (Persistent Rotating Seed Bank & Dynamic Worker Scaling):**
    - Implement `PersistentSeedBank`: initialize from assets on cold start, persist active seeds in local app storage (`DataStore`/files).
    - Implement rotation semantics: whenever a deal is started or skipped, its seed is consumed and removed from the bank to ensure each game is fresh and non-repeating.
    - Implement dynamic coroutine replenishment in `DealGenerator`: 0 workers active when buffer is full (100/100, 0% CPU), 1 background worker on mild dip (>= 70%), scaling up to 3-4 parallel workers on heavy depletion (< 50%).
    - Expose reactive telemetry via `StateFlow<GeneratorDebugStats>` (Easy/Medium bank counts, active worker count, rejection rate, last solve duration).
    - *TDD/Unit Tests:* `PersistentSeedBankTest` (rotation, persistence, seed depletion/replenishment) and `DealGeneratorDynamicScalingTest`.
  - [x] **T-6.FIX5.4 (Developer Debug Tools in SettingsBottomSheet under BuildConfig.DEBUG):**
    - Add **"Developer / Debug Tools"** section to `SettingsBottomSheet` rendered exclusively when `BuildConfig.DEBUG == true` (zero release overhead).
    - Telemetry UI: live seed bank counts (`Easy [X/100]`, `Medium [Y/100]`), active generator workers (`Idle` / `Refilling (N active)`), average candidate solve time, current session diagnostics (session ID, moves, score, deadlock / auto-complete flags), system DPI & card dimensions.
    - Dev Actions:
      - **"Stress Refill / Flush 90%"**: trims 90% of currently buffered seeds to trigger deep depletion and observe multi-worker dynamic scaling in real time. Guarded by a safety threshold: if total seeds < 20 (or < 10 in Easy / Medium), displays a warning banner/toast preventing over-depletion.
      - **"View / Export Seeds"**: opens dialog or copies active seed bank list to clipboard for instant inspection.
      - **"Instant Win"**: instantly forces foundation completion to easily test victory celebration animations.
    - *TDD & Previews:* Unit tests for dev intents (`GameIntent.DevStressRefill`, `GameIntent.DevInstantWin`, `GameIntent.DevExportSeeds`), and Compose Previews in `SettingsBottomSheetPreview`.

- [x] **T-6.FIX6: Smart Cross-Difficulty Deal Harvesting & Eager Cold-Start Replenishment**
  - **Cold-Start Eager Replenishment:** Automatically trigger replenishment checks (`checkAndReplenish(EASY)` & `checkAndReplenish(MEDIUM)`) on game startup via `seedBank.state` collector in `DealGenerator`, eliminating the requirement for the player to deal a game before workers wake up.
  - **Cross-Difficulty Deal Harvesting:** When a worker searches for candidate seeds, don't discard solvable seeds of the "other" difficulty. If an Easy worker finds a Medium seed and Medium has `< 100` seeds, deposit it into Medium (and vice versa), doubling generation efficiency and avoiding wasted solver CPU cycles.
  - **Dynamic Worker Contraction:** When seed count crosses from `< 50` (4 workers) back to `>= 50` (1 worker), cancel excess running workers cleanly.
  - *TDD/Unit Tests:* Update `DealGeneratorDynamicScalingTest` to verify cold-start eager replenishment and cross-difficulty harvesting.

- [x] **T-6.FIX7: Real-Time Worker Discovery Log in Generator Telemetry & Debug Tools**
  - **Worker Discovery Event Model:** Define `@Serializable` `WorkerLogEntry(timestampMs, workerId, seed, difficulty, durationMs)` and add ring-buffered `recentWorkerLogs: List<WorkerLogEntry>` to `GeneratorDebugStats`.
  - **Telemetry Logging in DealGenerator:** Assign incremental/thread IDs to spawned background workers, logging every discovered solvable seed with worker ID, classified difficulty, seed number, and solve duration into `recentWorkerLogs` (max 20 entries).
  - **Worker Discovery Log UI in SettingsBottomSheet:** Render a styled monospace activity feed card in the Developer Tools section displaying `[HH:mm:ss] W#id ➔ Difficulty #seed (ms)`, providing real-time visibility into worker contributions and parallel harvesting.
  - *TDD/Unit Tests:* Add unit tests in `DealGeneratorDynamicScalingTest` asserting worker log creation and ring buffer capping; verify Compose preview.

- [x] **T-6.FIX8: Guard Against Duplicate Victory Recording on Undo & Re-Win (`GameViewModel`, `StatsRepository`)**
  - Resolved a statistical corruption bug where undoing after a win and re-playing the winning move invoked `recordVictoryInStats()` again, improperly incrementing `gamesWon` and `currentWinStreak` on the same game session.
  - Introduced `isVictoryRecorded: Boolean` session-scoped guard flag in `GameViewModel`: set to `true` upon first victory recording, and reset to `false` exclusively on fresh deals (`applyNewDeal`), game restarts (`restartGame`), or saved session restorations (`restoreGameSession`).
  - Added session persistence update in `undoMove()` (`if (!isWonNow && hasMoved) saveCurrentSession()`) guaranteeing undone active games are safely persisted if backgrounded.
  - *TDD/Unit Tests:* Added test cases in `GameViewModelTest` asserting that undoing a won game and completing the winning move again maintains `gameWonCount == 1`, and restarting the game allows subsequent victory recording.

- [x] **T-6.1: Victory Animation Extensible Architecture & Bouncing Physics Engine (`VictoryAnimator`, `BouncingCardsPhysics`)**
  - Designed extensible `VictoryAnimationType` enum (`CLASSIC_BOUNCE`) and `VictoryAnimator` abstraction with `VictoryAnimatorFactory`.
  - Implemented pure math `BouncingCardsPhysics` simulation engine: models card particles with position, velocity ($v_x, v_y$), gravity ($1800\\,\\text{px/s}^2$), floor restitution coefficient ($e \\approx -0.85$), minimal bounce clamping to prevent floor jitter, and classic screen exit termination (as well as optional wall bouncing).
  - Implemented deterministic `CascadeSequencer`: orders foundation cards from top to bottom (King to Ace) cycling across foundations (3 down to 0), releases the first card immediately at $t = 0$, and sequences subsequent cards at configurable intervals (`releaseIntervalMs = 150L`), tracking queued, active, and completed/terminated particles.
  - Implemented `ClassicBounceAnimator` wrapping `CascadeSequencer` and `BouncingCardsPhysics` with sub-stepping for numerical stability under variable frame delta times.
  - *TDD/Unit Tests:* Added 17 unit tests in `BouncingCardsPhysicsTest`, `CascadeSequencerTest`, and `VictoryAnimatorTest` (100% pass, 638 total suite tests).

- [x] **T-6.2: High-Performance Card Sprite Cache (`CardSpriteCache`)**
  - Designed `CardSpriteRenderer` interface and `DefaultCardSpriteRenderer` utilizing Compose `CanvasDrawScope`, vector `drawSuitEmblem`, and typography `TextMeasurer` / `drawText`.
  - Implemented `CardSpriteCache` managing hardware-backed `ImageBitmap` sprites for all 52 card faces: supports instant retrieval by `Card` or `(Suit, Rank)`, idempotent population, and forced re-rendering upon style or dimension updates.
  - Implemented `@Composable fun rememberCardSpriteCache(...)` with `LaunchedEffect` for automatic population and `DisposableEffect` for lifecycle-safe memory eviction when animation finishes or leaves composition.
  - *TDD/Unit Tests:* Added `CardSpriteCacheTest` validating initial empty state, 52-sprite population, dimension verification, idempotence, and clean cache eviction (100% pass, 642 total suite tests).

- [x] **T-6.3: Hardware-Accelerated Victory Canvas Overlay (`VictoryOverlay`, `ClassicBounceRenderer`)**
  - Implemented pluggable `VictoryRenderer` interface and `ClassicBounceRenderer` drawing to hardware-accelerated Compose `Canvas`.
  - Implemented authentic persistent motion trails (Windows Solitaire classic card ghosting trail) via offscreen hardware-backed trail buffer without full canvas clears or excessive allocations.
  - Implemented high-performance frame loop driving ticks via `withFrameNanos` and draw-phase invalidation skipping Composable recomposition overhead for 60/120 FPS rendering.
  - Implemented full-screen touch barrier with tap-to-skip gesture handling (`Modifier.pointerInput` and `detectTapGestures`).
  - *TDD/Unit Tests:* Added `ClassicBounceRendererTest` validating type mapping, offscreen buffer allocation, particle stamping, and lifecycle memory eviction (100% pass, 644 total suite tests).
  - *Compose Previews:* Created `VictoryOverlayPreview` (`VictoryOverlayBouncingCascadePreview`) demonstrating active particle cascade and motion trails.

- [x] **T-6.4: Victory Audio Fanfare & Celebration Haptics (`SolitaireAudio`, `SolitaireHaptics`)**
  - Synthesized and bundled crisp uncompressed 16-bit 44.1kHz PCM WAV audio asset `victory_fanfare.wav` in `app/src/main/res/raw/` (bright C Major ascending arpeggio and triumphant brass/chime chord).
  - Extended `SolitaireAudio` and `AndroidSolitaireAudio` with `playWinFanfare()`, loaded into low-latency `SoundPool` with `USAGE_GAME` sonification and muted when audio is disabled.
  - Extended `SolitaireHaptics` and `AndroidSolitaireHaptics` with `playWinCelebration()`, driving a multi-pulse celebratory waveform (`0, 70, 60, 70, 60, 140` ms) on supported vibrators with graceful Compose fallback.
  - *TDD/Unit Tests:* Added tests in `SolitaireAudioTest` and `SolitaireHapticsTest` validating invocation tracking, muting guards when disabled, and no-op contract compliance (100% pass, 644 total suite tests).

- [x] **T-6.5: Victory Summary Data Model & Records Calculation (`VictorySummary`, `GameContract`)**
  - Defined `@Immutable` `VictorySummary` data model encapsulating `timeSeconds`, `movesCount`, `score`, breakthrough flags (`isNewBestTime`, `isNewFewestMoves`, `isNewHighScore`, `isNewBestStreak`), and composite `hasAnyNewRecord`.
  - Implemented `VictorySummary.calculate()` calculating personal records against baseline `GameStats`.
  - Extended `GameUiState` with `isVictoryAnimationActive: Boolean`, `victorySummary: VictorySummary?`, and `selectedVictoryAnimation: VictoryAnimationType`.
  - Added MVI intents `StartVictoryAnimation`, `SkipWinAnimation`, and `DismissVictorySummary` in `GameIntent` and wired exhaustive branches in `GameViewModel`.
  - *TDD/Unit Tests:* Added `VictorySummaryTest` (100% pass) and updated `GameContractTest` covering state immutability, default state verification, and exhaustive intent handling (651 total suite tests).

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
