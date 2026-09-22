# Solitaire AI Agent Guidelines

## 1. Project Overview
A modern, lightweight, native Android Klondike Solitaire built with Kotlin and Jetpack Compose.
Zero ads, fully offline, highly optimized, and focused on exceptional UX.
Package root: `io.github.qdiaps.solitaire`

## 2. Source of Truth
- System Specification: `docs/SPEC.md`
- Architecture & ADR: `docs/ARCHITECTURE.md`
- High-level Roadmap & DoD: `docs/ROADMAP.md`
- Current Sprint Backlog: `docs/TASKS.md`
- Dynamic Session State: `docs/STATE.md`

Always inspect `docs/STATE.md` and `docs/TASKS.md` before taking any action.

## 3. Workflow & Engineering Standards
1. **Cycle:** Make it work -> Test it with comprehensive unit tests -> Refactor to idiomatic Kotlin.
2. **Atomic Steps:** Pick only ONE task from `docs/TASKS.md` at a time. Do not implement unrequested features or bundle multiple steps together.
3. **Architecture Boundaries:**
    - The `domain` package must be 100% pure Kotlin. NEVER import `android.*` or `androidx.*` into `domain`.
    - Zero game logic inside `@Composable` functions. Composables only render state and emit user events (`GameIntent`).
4. **Code Quality:**
    - Self-documenting code. No redundant echo-comments.
    - Use KDoc (`/** ... */`) only for public interfaces, complex algorithms, or non-obvious game rules.
    - Strict immutability by default (`val`, data classes, immutable collections, exhaustive `when`).

## 4. Execution & Safeguards
- **Rule of 3 Failures:** If a build or test fails 3 consecutive times, STOP execution immediately. Summarize the failure, provide your hypothesis, and ask the user for direction. Do not cycle in loops.
- **Verification:** Always execute `./gradlew test` (or the targeted test class) via terminal before marking any task complete.
- **Commit & Sync:** Once tests are green and refactoring is done:
    1. Check off the task `[x]` in `docs/TASKS.md`.
    2. Update `docs/STATE.md` (Completed Task, Current Status, Next Step).
    3. Execute a git commit using Conventional Commits format (e.g., `feat(domain): ...`, `test(domain): ...`).
- **Context Monitor:** After completing 3–4 tasks in a single session, notify the user: "Context is getting heavy. Please review STATE.md, commit, and restart the chat session."