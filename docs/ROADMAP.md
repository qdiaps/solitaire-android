# Solitaire (Klondike) - Development Roadmap

## Phase 1: Pure Domain Engine & Core Rules
**Goal:** Implement the complete game engine in pure Kotlin with zero Android dependencies, fully verified by unit tests.
- **Scope:**
    - Card models (`Card`, `Suit`, `Rank`, `PileType`, `CardLocation`, `BoardState`, `Move`).
    - Standard deck generation, shuffling, and Klondike dealing logic.
    - Core move validations: Waste to Foundation/Tableau, Tableau to Tableau/Foundation.
    - Stock draw logic (Draw 1 / Draw 3) with infinite recycling.
    - Auto-expose top card of tableau when uncovering.
    - Smart Tap priority resolution.
    - Undo manager (state snapshot stack) with scoring & move counter rollback.
- **Definition of Done (DoD):**
    - 100% pure Kotlin (`android.*` imports strictly forbidden).
    - Comprehensive unit test suite covering valid moves, invalid moves, edge cases, and undo rollback.
    - All tests green via `./gradlew test`.

---

## Phase 2: Solvability Engine & Background Generator
**Goal:** Guarantee solvable deals and provide deadlock detection.
- **Scope:**
    - Fast Klondike solver algorithm (BFS / heuristic A*).
    - Deadlock detector (flags when zero legal productive moves remain).
    - Background deal generator running on `Dispatchers.Default` with a 2–3 seed buffer.
- **Definition of Done (DoD):**
    - Solvability checker resolves deals within reasonable execution time (< 300ms on benchmark seed).
    - Unit tests validating solver on known solvable and unsolvable test hands.

---

## Phase 3: Compose Board Layout & Static Presentation
**Goal:** Build the complete portrait UI structure with Jetpack Compose.
- **Scope:**
    - Dynamic card sizing math based on screen width (7-column portrait constraint).
    - Top row: Stock, Waste, and 4 Foundations (with left-handed mirroring support).
    - Tableau area: 7 overlapping vertical columns.
    - Bottom action bar: Undo, Hint, New Game, Settings.
    - Felt background themes and custom card faces/backs rendering.
- **Definition of Done (DoD):**
    - UI renders an initial board state cleanly across standard mobile resolutions.
    - Zero game logic inside `@Composable` functions.
    - `@Preview` annotations working for individual components and full board.

---

## Phase 4: Interactivity (Smart Tap, Drag & Drop Overlay, Haptics)
**Goal:** Bring the board to life with responsive touch controls.
- **Scope:**
    - Connect `GameViewModel` with `GameIntent` and `StateFlow<GameUiState>`.
    - Smart Tap interaction: tapping card triggers animated auto-move.
    - Drag & Drop: top-level overlay layer handling card stack drag gestures.
    - Drop hitbox detection and snapping/revert logic.
    - Haptic feedback integration on card pickup and placement.
- **Definition of Done (DoD):**
    - Player can play a full game smoothly via either tap-to-move or drag-and-drop.
    - Dragged cards float above all columns without clipping artifacts.

---

## Phase 5: Game Loop, Scoring, Auto-Complete & Persistence
**Goal:** Complete the meta-game systems and lifecycle handling.
- **Scope:**
    - Active timer and Standard Klondike scoring system.
    - Auto-complete cascade trigger and animation when all tableau cards are face-up.
    - Hint system with pulsing UI highlight.
    - `DataStore` persistence: autosave current game state on app pause, restore on launch.
    - Statistics tracking (games played, win %, best time, streaks) with reset option.
    - Card theme customization: Multiple card back designs (`ClassicLattice`, `CrimsonVintage`, `EmeraldArtDeco`, `ObsidianMinimal`) and face typography styles (`ModernClean`, `ClassicSerif`, `LargePrint`) with Compose previews and settings persistence.
- **Definition of Done (DoD):**
    - Game state persists across app kill/restart.
    - Auto-complete finishes game automatically and correctly.
    - Settings (Draw 1/3, Left-hand, Themes, Card Styles) persist and apply instantly.

---

## Phase 6: Victory Screen & Polish
**Goal:** Delightful finish and final optimization.
- **Scope:**
    - Hardware-accelerated `Canvas` bouncing card victory cascade.
    - Tap-to-skip victory animation to show final stats dialog.
    - Performance profiling (guarantee 60/120 fps, memory leak verification).
- **Definition of Done (DoD):**
    - Smooth victory animation without frame drops.
    - Release APK build verified and playable on physical device.

---

## Phase 7: Production Release Readiness & CI/CD Pipeline
**Goal:** Prepare a production-grade optimized release build with adaptive branding, automated matrix CI/CD, and store-ready packaging.
- **Scope:**
    - App Identity & Branding: Adaptive launcher icon (`ic_launcher`) with background, foreground, round, and monochrome (Material You themed icon) variants, localized application strings (`values/strings.xml`, `values-ru/strings.xml`), and Android 12+ Splash Screen API integration.
    - ProGuard / R8 Optimization: Code shrinking, resource shrinking, and obfuscation rules (`proguard-rules.pro`) for Kotlin serialization, Compose runtime, and Coroutines.
    - Release Signing & Packaging: Configured release signing with environment variable fallback to debug keys for local builds; APK (`assembleRelease`) and Android App Bundle (`bundleRelease`) artifact generation.
    - CI/CD Matrix Pipeline (GitHub Actions): Split monolithic workflow into 3 parallel blocking jobs: Unit Tests, Android Lint / Static Analysis, and Release Build (`assembleRelease` + `bundleRelease`).
    - Branch Protection: Enforce all CI status checks as required preconditions for pull request merges.
- **Definition of Done (DoD):**
    - Production APK and AAB build cleanly with R8 shrinking and 0 errors via `./gradlew assembleRelease bundleRelease`.
    - GitHub Actions pipeline runs all 3 jobs in parallel and blocks failing PRs.
    - Adaptive icon and splash screen render properly on Android 8.0+ through 14+.

