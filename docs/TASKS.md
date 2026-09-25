# Current Sprint Tasks - Phase 4: Drag-and-Drop & Interactive Gameplay

> **Current Phase:** Phase 4 (Drag-and-Drop & Interactive Gameplay)
> **Branch:** `feature/phase-4-interactive-gameplay`

- [x] **T-4.1: MVI Contract Definitions (`GameContract.kt`)**
  - Define `CardLocation` hierarchy: `Stock`, `Waste`, `Foundation(index)`, `Tableau(columnIndex, cardIndex)`.
  - Define immutable `GameUiState` (`@Immutable`) containing `boardState`, `isGameWon`, `isDeadlocked`, `canUndo`, `elapsedTimeSeconds`, `feltTheme`, `isLeftHanded`, `activeHint`.
  - Define `GameIntent` sealed interface and single-shot `GameEvent` sealed interface (haptics, messages).
  - *TDD/Unit Tests:* `GameContractTest` verifying state immutability, default properties, and helper methods.
- [x] **T-4.2: GameViewModel Lifecycle, Deal Initialization & Timer**
  - Implement `GameViewModel` with `StateFlow<GameUiState>`, coroutine timer loop, and deal initialization from `KlondikeDealer` / `DealGenerator`.
  - Handle `StartNewGame`, `RestartGame`, and timer pause/resume lifecycle.
  - *TDD/Unit Tests:* `GameViewModelTest` verifying state initialization, new game deal, and timer ticks.
- [x] **T-4.3: Stock Draw, Waste Extraction & Undo Processing in ViewModel**
  - Implement `DrawStockCard`, `RecycleStock`, and `UndoMove` intent processing in `GameViewModel`.
  - Connect `UndoManager` snapshot management and score tracking across draw/undo operations.
  - *TDD/Unit Tests:* `GameViewModelTest` verifying stock cycle, waste card extraction, and undo state restoration.
- [x] **T-4.4: Smart Tap Move Execution & Auto-Flip Uncovered Cards**
  - Implement `OnCardTapped(card, location)` intent in `GameViewModel` backed by `SmartTapResolver`.
  - Auto-flip uncovered face-down cards when top card of a tableau column is vacated.
  - Check win condition (`KlondikeRules.isGameWon`) and deadlock status after every move.
  - *TDD/Unit Tests:* `GameViewModelTest` verifying tap promotions to foundation, cross-tableau sequence moves, and face-down reveals.
- [x] **T-4.5: Drop Target Hitbox Registry (`DropTargetRegistry`)**
  - Implement `DropTargetRegistry` storing absolute screen bounds (`Rect`) of all 4 Foundation slots and 7 Tableau columns via `Modifier.onGloballyPositioned`.
  - Implement pure helper `findBestDropTarget(draggedBounds, targets)` calculating bounding box overlap area to select the intended destination.
  - *TDD/Unit Tests:* `DropTargetRegistryTest` testing geometry overlap matching and tolerance edge cases.
- [x] **T-4.6: Drag & Drop State Management (`DragDropState`)**
  - Implement `DragDropState` managing active drag lifecycle: `sourceLocation`, `draggedCards`, current drag `Offset`, and source visibility flags.
  - Support sub-stack dragging in tableau (dragging from index k lifts cards k..N).
  - *TDD/Unit Tests:* `DragDropStateTest` validating sub-stack slicing, active dragging state flags, and touch offset tracking.
- [x] **T-4.7: Global Drag Overlay Layer (`DragOverlay`)**
  - Implement `DragOverlay` composable at the root layout layer floating above all board elements (ADR 003).
  - Render moving card stacks with elevated shadow (`12.dp`) and vertical cascade spacing matching `CardDimensions`.
  - Hide source cards on the board while drag is in progress to prevent duplicate ghost cards.
  - *Compose Previews:* `DragOverlayPreview` showing single card and multi-card stacks floating over felt.
- [x] **T-4.8: Drop Validation, Snap-Back Animation & Haptic Feedback**
  - Implement drop gesture release handling: validate destination using `KlondikeRules.canMoveCards`.
  - If valid: dispatch `OnCardDropped` intent, update board state, auto-flip uncovered cards, and trigger haptic snap.
  - If invalid: animate dragged cards smoothly back to origin position using `Animatable` spring physics before clearing drag state.
  - Integrate `LocalHapticFeedback` for card pickup and placement clicks.
- [x] **T-4.9: Activity & Screen Wiring, End-to-End Gameplay & Phase 4 Review**
  - Created `CardDragModifier.kt` with `Modifier.cardDragTarget` detecting drag gestures, capturing root bounds, triggering haptics on pickup, tracking continuous displacement, and resolving drops.
  - Wired `cardDragTarget` and `onCardDropped` across `TableauColumnView`, `TableauAreaView`, `WastePileView`, `FoundationPileView`, and `TopRowView`.
  - Integrated `DragOverlay` and provided `LocalDragDropState` and `LocalDropTargetRegistry` in `SolitaireGameScreen`.
  - Added stateful `SolitaireGameScreen(viewModel: GameViewModel)` collecting `uiState` and routing user actions/events.
  - Wired `MainActivity` with `gameViewModel by viewModels()` rendering the connected `SolitaireGameScreen`.
  - Added unit and screen wiring tests in `SolitaireGameScreenTest` verifying end-to-end MVI loop and haptic feedback.
  - Full suite passed: 413 unit tests passing (100%), 0 failures, 0 Android lint errors.
- [x] **T-4.10: Drag-and-Drop Polish, Stale Gesture Recomposition & Haptics Engine Bugfixes**
  - Fixed stale card closures in `CardDragModifier` via `rememberUpdatedState` for drag callbacks and board state.
  - Added `key(card.id)` across `TableauColumnView`, `WastePileView`, and `FoundationPileView` preserving item identity during stack re-ordering.
  - Replaced sluggish spring snap-back physics with fast, responsive `DefaultSnapBackSpec` (`tween(160ms, FastOutSlowInEasing)`).
  - Added mid-animation cancel guard in `startDrag` and `snapBack` to prevent coordinate corruption when quickly grabbing new cards.
  - Aligned `DragOverlay` coordinates to absolute root by scoping status and navigation bar insets to child content.
  - Added hardware vibrator engine `SolitaireHaptics` and `VIBRATE` permission in `AndroidManifest.xml` for tactile feedback on pickup, snap, and ticks.
- [x] **T-4.11: Smooth Flight Animations & 3D Flips (`AnimatedMoveOverlay`, `CardFlightState`)**
  - Implemented `CardFlightState` managing in-flight card interpolation, 3D flip rotation, and source card masking.
  - Implemented `AnimatedMoveOverlay` floating overlay layer rendering flying cards with elevation shadow and 3D Y-axis rotation.
  - Added smart tap flight animation: when tapping a card that can move to Foundation or Tableau, it smoothly flies to the target destination over 180ms before completing the move.
  - Added Stock draw flip animation: 3D rotation and flight from Stock to Waste pile (0°..90° back -> 90°..0° face).
  - Added 3D card reveal flip in `CardView`: uncovering face-down cards animates rotation around Y-axis with perspective camera distance.
  - Mounted permanent baseline placeholders in `TableauColumnView`, `FoundationPileView`, and `WastePileView` with `underCard` rendering so empty slots never pop visually.
  - Registered `Stock` and `Waste` bounds in `DropTargetRegistry` for reliable trajectory calculation in normal and left-handed modes.
  - *TDD/Unit Tests:* `CardFlightStateTest` verifying interpolation, rotation, cancellation, and callbacks.
  - Full suite passed: 418 unit tests passing (100%), 0 failures, 0 Android lint errors.
- [x] **T-4.12: Universal ERM Haptics Engine & MediaTek HAL Fallback (`SolitaireHaptics`)**
  - Fixed silent failure of `VibrationEffect.createPredefined` on MediaTek / rugged devices (Hotwav Cyber X) where HAL returns unsupported without throwing exceptions.
  - Added `vib.areAllEffectsSupported(effect)` hardware check guarding `createPredefined`.
  - Implemented calibrated one-shot pulse fallbacks for ERM motors in heavy rugged phones (~380g): 45ms tick, 65ms pickup, 90ms snap at `DEFAULT_AMPLITUDE`.
  - Upgraded vibration attributes to `VibrationAttributes.USAGE_HARDWARE_FEEDBACK` (API 33+) and `AudioAttributes.USAGE_GAME` (API 26+) so tactile feedback is not muted by system touch/keyboard toggle.
  - Added `SolitaireHapticsTest` unit tests.
  - Full suite passed: 420 unit tests passing (100%), 0 failures, 0 Android lint errors.
- [x] **T-4.13: Acoustic Audio Feedback Engine (`SolitaireAudio`, `SoundPool`)**
  - Designed lightweight low-latency acoustic card audio engine using Android `SoundPool` with `AudioAttributes.USAGE_GAME` and `CONTENT_TYPE_SONIFICATION`.
  - Generated and embedded 4 crisp, uncompressed 16-bit 44.1kHz PCM WAV audio assets in `app/src/main/res/raw/`: `card_slide.wav` (pickup), `card_snap.wav` (placement), `card_flip.wav` (turnover / stock draw), `card_deal.wav` (riffle deal).
  - Added `SolitaireAudio` interface, `AndroidSolitaireAudio`, `LocalSolitaireAudio`, and `rememberSolitaireAudio()` composable helper.
  - Integrated `playSlide()` and `playSnap()` into `CardDragModifier.kt` for tactile pickup and drop sound.
  - Integrated `playFlip()` into `CardView.kt` when face-down cards are turned face-up.
  - Added `GameEvent.PlayDealSound` in `GameContract.kt` and wired it in `GameViewModel.kt` on new deals and game restarts.
  - Provided `LocalSolitaireAudio` in `SolitaireGameScreen` with event mapping.
  - *TDD/Unit Tests:* `SolitaireAudioTest`, `GameContractTest`, and `SolitaireGameScreenTest` verifying audio contracts, event handling, and deal sound emissions.
  - Full suite passed: 424 unit tests passing (100%), 0 failures, 0 Android lint errors.

---

# Upcoming Sprint Backlog - Phase 5 (Preview)

- [ ] **T-5.x: Card Themes & Visual Customization (Back & Face Styles)**
  - Implement `CardBackStyle` enum: `ClassicLattice`, `CrimsonVintage`, `EmeraldArtDeco`, `ObsidianMinimal`.
  - Implement `CardFaceStyle` enum: `ModernClean`, `ClassicSerif`, `LargePrint`.
  - Integrate selection with `SolitaireTheme`, settings persistence (`DataStore`), and interactive `CardThemesGalleryPreview`.

---

> **Historical Archive:** Completed tasks from Phase 1, Phase 2, and Phase 3 are preserved in [docs/archive/TASKS_HISTORY.md](archive/TASKS_HISTORY.md).
