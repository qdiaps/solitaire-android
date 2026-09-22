# Contributing to Solitaire

Thank you for your interest in contributing!

## 📜 Architectural Rules
1. **Domain Purity:** The `domain` package must remain 100% pure Kotlin. Do not import `android.*` or `androidx.*` packages into `domain`.
2. **Zero Game Logic in UI:** Composable functions must only render state and emit user events (`GameIntent`).
3. **Test Coverage:** Any changes or additions to game rules, moves, or solver logic must include corresponding JUnit unit tests.
4. **Code Style:** Use idiomatic Kotlin, immutability by default (`val`), and self-documenting code. Use KDoc only where necessary for public APIs.

## 🔄 Pull Request Process
1. Fork the repo and create your feature branch: `git checkout -b feature/amazing-feature`.
2. Ensure all unit tests pass: `./gradlew test`.
3. Commit your changes with Conventional Commits format (`feat:`, `fix:`, `refactor:`, `test:`).
4. Open a Pull Request with a clear description of changes.