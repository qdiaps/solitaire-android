# 🃏 Klondike Solitaire for Android

A clean, minimalist, and lightweight native Android Klondike Solitaire built with Kotlin and Jetpack Compose.

Designed for quick, distraction-free sessions: **no ads, no subscriptions, fully offline, and zero clutter.**

---

## ✨ Features

- **Classic Gameplay:** Standard Klondike rules with support for both **Draw 1** and **Draw 3** modes, plus infinite deck recycling.
- **Smart Tap (Auto-Move):** One tap automatically finds the optimal legal slot (prioritizing foundation placement and uncovering face-down cards).
- **Smooth Drag & Drop:** Fluid touch gestures with an overlay layer preventing card clipping and sorting glitches.
- **Guaranteed Solvable Deals:** Background generation pipeline checks deal seeds so you never waste time on dead hands.
- **Deadlock Detection:** Alerts you when no productive legal moves remain on the board.
- **Unlimited Undo:** Step-by-step history rollback with score and move counter synchronization.
- **Auto-Complete:** Automatically gathers remaining cards into foundations once all tableau cards are face-up.
- **Haptic Feedback:** Subtle tactile ticks on card pickup and slot snap.
- **Left-Handed Mode:** Mirror top layout for comfortable single-handed play.
- **Customization:** Multiple clean felt backgrounds and minimalist card back styles.
- **Classic Victory Screen:** Hardware-accelerated canvas cascade animation with tap-to-skip.
- **Offline & Private:** Zero network permissions, no telemetry, no tracking.

---

## 🛠️ Tech Stack & Architecture

- **Language:** 100% pure Kotlin
- **UI Framework:** Jetpack Compose (Material 3) + Hardware-accelerated Compose Canvas
- **Architecture:** Layered Clean Architecture + MVI (Unidirectional Data Flow)
- **Concurrency:** Kotlin Coroutines & StateFlow
- **Persistence:** Jetpack DataStore (Preferences) + `kotlinx.serialization`
- **Testing:** JUnit 6 for engine and move validation rules

```text
app/
├── domain/     # Pure Kotlin: Game rules, solver, deck generation (0 Android dependencies)
├── data/       # Preferences, persistence, autosave
└── ui/         # Jetpack Compose screens, MVI ViewModel, gestures, canvas animations
```

## 🚀 Building & Running
### Prerequisites
- **JDK:** 17 or higher
- **Android SDK:** 37 (Build Tools & Platform API 37)
- **Minimum OS Support:** Android 8.0 (API 26+)
- **IDE:** IntelliJ IDEA or Android Studio

### Build from source
Clone the repository:
```bash
git clone https://github.com/qdiaps/solitaire-android.git
cd solitaire-android
```

Run unit tests:
```bash
./gradlew test
```

Assemble debug APK:
```bash
./gradlew assembleDebug
```
The APK will be available under `app/build/outputs/apk/debug/`.

---

## 🤝 Contributing
Contributions are welcome! Please check out [CONTRIBUTING.md](CONTRIBUTING.md) before submitting pull requests.

## 📄 License
This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.