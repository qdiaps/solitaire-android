---
name: compose-solitaire-ui
description: Guide implementation of Jetpack Compose UI, card dragging, custom canvas animations, and reactive state management for Solitaire. Use when working on UI screens, overlays, or animations.
---

### Purpose
Ensure performant, 60/120 FPS UI rendering without unnecessary recompositions, visual glitches, or clipped drag layers.

### UI & Performance Rules
1. **No Logic in Composables:** Composable functions must only emit UI and route user events (`GameIntent`) to the ViewModel/StateHolder. Never evaluate Klondike move validity inside a `@Composable`.
2. **Z-Index & Drag Overlay:**
    - Do NOT drag cards inside localized column/stack hierarchies, as parent bounds will clip them.
    - Maintain a top-level `Box` overlay for actively dragged cards using global offsets. Render the origin card at 30-50% alpha while dragged.
3. **Smart Recomposition:**
    - Pass immutable state holders (`@Immutable` / primitives / stable lists).
    - Use `derivedStateOf` for derived calculations (e.g., whether auto-complete is available).
4. **Victory & Heavy Animations via Canvas:**
    - Do NOT create dozens of Composable nodes for falling cards during victory sequences.
    - Use a dedicated `Canvas` driven by `withFrameNanos` to calculate sprite coordinates (`x`, `y`, `velocity`, `gravity`, `bounce`) on the fly. Provide an immediate screen-tap listener to skip.
5. **Haptics:**
    - Trigger `LocalHapticFeedback` on drag pickup and valid drop completion.