# Solitaire (Klondike) - Functional Specification

## 1. Game Overview
A clean, native, ad-free, portrait-first Android Klondike Solitaire designed for seamless offline play with high visual polish, tactile feedback, and intuitive controls.

---

## 2. Core Game Rules & Layout
- **Deck:** Standard 52-card deck (no Jokers). 4 suits (Hearts, Diamonds, Clubs, Spades), 13 ranks (Ace to King).
- **Layout:**
    - **Stock (Draw Pile):** Remaining cards face-down.
    - **Waste (Discard Pile):** Cards drawn from Stock, face-up.
    - **Foundations (4 piles):** Built up by suit from Ace (1) to King (13).
    - **Tableau (7 columns):** Columns 1 through 7 containing 1 to 7 cards respectively. Only the top card in each column is initially face-up; cards beneath remain face-down until exposed.
- **Stock Draw Mode:** Configurable setting: **Draw 1** (casual) or **Draw 3** (classic).
- **Stock Recycling:** Infinite recycling of Waste back into Stock without penalty or limits.
- **Tableau Building:**
    - Build down in alternating colors (e.g., Red 6 on Black 7).
    - Multiple face-up cards can be moved as a unit if they form a valid sequence.
    - Empty tableau columns can ONLY be filled by a King (rank 13) or a valid sequence starting with a King.
- **Exposing Hidden Cards:** When a face-up card is moved away from a tableau column, the topmost face-down card immediately flips face-up.

---

## 3. Interaction & Controls
- **Smart Tap (Auto-Move by Click):**
    - Tapping a valid face-up card automatically attempts to move it to the optimal legal destination.
    - **Priority 1:** Move to Foundation (if valid).
    - **Priority 2:** Move to a Tableau column that reveals a hidden face-down card (helps uncover the board faster).
    - **Priority 3:** Move to the first valid Tableau column from left to right.
- **Drag & Drop:**
    - Press and hold to drag a single card or a valid stack of face-up cards.
    - Renders over an overlay layer above all columns to prevent clipping and Z-index sorting issues.
    - Snaps into place if dropped over a valid drop target (within tolerance hitbox); smoothly returns to source position if invalid.
- **Haptic Feedback:**
    - Subtle tactile tick when picking up a card.
    - Crisp confirmation tick when a card locks into a Foundation or Tableau slot.

---

## 4. Solvability & Game Generation
- **Guaranteed Solvable Deals:**
    - The game generates random seeds and verifies solvability via a background solver (BFS / A* heuristic).
    - A background pipeline maintains a small buffer (2–3 pre-verified solvable deals) so starting a new game is instantaneous.
- **Deadlock Detection ("No Moves Left"):**
    - The engine constantly checks available legal actions across Stock, Waste, Tableau, and Foundations.
    - If no legal moves remain that could alter board state, show a non-intrusive banner or dialog notifying the player.

---

## 5. Game Session & Lifecycle
- **Undo System:**
    - Unlimited step-by-step undo (`ArrayDeque<SolitaireState>`).
    - Correctly rolls back card states (turns cards face-down if they were exposed by that move), score, and move counter.
- **Auto-Complete:**
    - Triggers when all 7 Tableau columns contain **zero hidden (face-down) cards** and the Stock is empty/exhausted.
    - Automatically cascades remaining cards from Tableau to Foundations with sequential animations (100–150ms per card).
- **Lifecycle & Persistence:**
    - State serializes to local storage (`DataStore`) on app pause/stop.
    - Game timer pauses automatically on lifecycle `ON_PAUSE`.
    - Resuming the app restores the exact board state, timer, and undo history.

---

## 6. Scoring, Time & Moves
- **Scoring System (Standard Klondike):**
    - Waste to Tableau: +5 points
    - Waste to Foundation: +10 points
    - Tableau to Foundation: +10 points
    - Turn over Tableau card: +5 points
    - Foundation to Tableau: -15 points
- **Moves Counter:** Increments by 1 on every valid move made by player.
- **Timer:** Elapsed active play time in `mm:ss` format.

---

## 7. Statistics (Reset-enabled)
Stored in DataStore with an option to reset:
- Games Played
- Games Won
- Win Percentage (%)
- Current Win Streak / Best Win Streak
- Best Time (fastest victory)
- Minimum Moves (most efficient victory)
- High Score

---

## 8. Hints
- Tapping "Hint" evaluates all valid moves and highlights the best candidate with a soft pulsing border on the source card and destination slot.
- Priority: Expose hidden card > Move to Foundation > Productive tableau reorganization > Draw from Stock.

---

## 9. Victory Screen & Animation
- When all 4 Foundations reach King (52 cards placed):
    - Transition to a dedicated Compose `Canvas`.
    - Classic bouncing cards animation: cards drop from foundations with gravity, bounce off the bottom of the screen, and leave motion trails.
    - **Skip Animation:** Tapping anywhere on screen instantly skips the animation and presents the Victory Stats summary.

---

## 10. Settings & Customization
- **Deck Draw Mode:** Draw 1 / Draw 3.
- **Left-Handed Mode:** Mirrors the top row (Stock/Waste on the right, Foundations on the left).
- **Themes:** Choice of 3–4 classic felt colors (Classic Green, Deep Navy, Dark Charcoal, Wine Red).
- **Card Backs:** Selection of 3–4 clean, minimal card back designs.
- **Toggles:** Sound FX, Haptic Feedback, Auto-Hint, Auto-Complete.