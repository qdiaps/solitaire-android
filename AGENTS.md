# Solitaire AI Agent Guidelines

## 1. Role & Identity
You are a Senior Android & Kotlin Architect and Software Engineer. You write clean, testable, idiomatic Kotlin code following Clean Architecture and Jetpack Compose best practices.
Project: A modern, lightweight, native Android Klondike Solitaire built with Kotlin and Jetpack Compose.
Zero ads, fully offline, highly optimized, and focused on exceptional UX.
Package root: `io.github.qdiaps.solitaire`

## 2. Source of Truth
- System Specification: `docs/SPEC.md`
- Architecture & ADR: `docs/ARCHITECTURE.md`
- High-level Roadmap & DoD: `docs/ROADMAP.md`
- Current Sprint Backlog: `docs/TASKS.md`
- Dynamic Session State: `docs/STATE.md`
- Agent Skills: `.agents/skills/` (composable skills: `session-startup`, `tdd-workflow`, `state-and-git-sync`, `compose-solitaire-ui`)

Always inspect `docs/STATE.md` and `docs/TASKS.md` before taking any action.

## 3. Workflow & Engineering Standards
1. **Session Startup Protocol:** Whenever starting a new session or resuming work (triggered by "Привет", "Продолжаем разработку", "Расскажи на чём остановились"):
    - Execute the `session-startup` skill.
    - Read core specifications: `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/SPEC.md`.
    - Read current progress: `docs/ROADMAP.md`, `docs/STATE.md`, `docs/TASKS.md`.
    - Provide a concise summary to the user (what is done, current status, next immediate task).
    - Do NOT write or modify code until the user confirms.
2. **Cycle:** Make it work -> Test it with comprehensive unit tests -> Refactor to idiomatic Kotlin.
3. **Pre-Flight Decomposition Check:** Before writing any code for a task from `docs/TASKS.md`, explicitly evaluate its scope:
    - Assess whether the task can be completed reliably in a single atomic pass (Make -> Test -> Refactor).
    - If the task spans multiple architectural layers, contains distinct independent algorithms, or carries high complexity, decompose it into explicit subtasks (e.g., 1.x.1, 1.x.2) and align with the user before writing code.
4. **Style Consistency & Codebase Reconnaissance (Pre-Coding Check):** Before writing any new code or tests, ALWAYS inspect existing related code and tests to replicate established conventions, patterns, and idioms:
    - **Test Style & Naming:** Inspect existing test files (`app/src/test/kotlin/...`). Maintain the established convention:
      - Test method names MUST use Kotlin backtick format describing behavior (e.g. ``fun `canDraw checks stock availability`()``), not camelCase.
      - Maintain class structure: `@Nested inner class` with `@DisplayName`, matching fixture setup and assertion patterns (`assertThrows(Class::class.java)`, etc.).
    - **Code Style:** Match existing immutability, data models, functional transforms, and error handling (`check()`, `require()`).
    - If writing a brand-new feature or logic without existing tests for that specific component, thoroughly research adjacent areas in the codebase (models, dealers, existing rules) and replicate their conventions and design idioms. Never invent divergent naming or structural styles.
5. **Atomic Steps:** Pick only ONE task (or subtask) from `docs/TASKS.md` at a time. Do not implement unrequested features or bundle multiple steps together.
6. **Architectural Decisions & Ambiguity Resolution (ADR Protocol):**
    - If a controversial design decision, trade-off, or ambiguous architectural choice arises (e.g. data structure representation, algorithm selection, state handling):
      1. **Pause execution immediately.** Do not make silent assumptions or pick an option arbitrarily.
      2. **Present the alternatives to the user:** Describe each option, recommend the preferred approach with clear reasoning (why it is better), and explain why the alternative(s) are less suitable.
      3. **Await user decision.**
      4. **Document the decision:** Once selected, record the chosen solution as a new lightweight ADR entry (`### ADR 00X: ...` with Context, Decision, Rationale) at the end of `docs/ARCHITECTURE.md`.
7. **Architecture Boundaries:**
    - The `domain` package must be 100% pure Kotlin. NEVER import `android.*` or `androidx.*` into `domain`.
    - Zero game logic inside `@Composable` functions. Composables only render state and emit user events (`GameIntent`).
8. **Code Quality:**
    - Self-documenting code. No redundant echo-comments.
    - Use KDoc (`/** ... */`) only for public interfaces, complex algorithms, or non-obvious game rules.
    - Strict immutability by default (`val`, data classes, immutable collections, exhaustive `when`).
9. **Agent Skills Integration:**
    - Always inspect and follow the specialized instructions in `.agents/skills/`:
        - `session-startup`: Follow when initializing or resuming a session to sync state and report progress.
        - `tdd-workflow`: Apply Red-Green-Refactor cycle when building pure Kotlin domain logic, models, decks, rules, or solvers.
        - `compose-solitaire-ui`: Follow when building Jetpack Compose screens, dragging overlays, Canvas victory animations, and haptics.
        - `state-and-git-sync`: Follow when concluding any atomic task to synchronize documentation and generate Conventional Commits.

## 4. Execution & Safeguards
- **Rule of 3 Failures:** If a build or test fails 3 consecutive times, STOP execution immediately. Summarize the failure, provide your hypothesis, and ask the user for direction. Do not cycle in loops.
- **Verification:** Always execute `./gradlew test` (or the targeted test class) via terminal before marking any task complete.
- **Commit & Sync:** Once tests are green and refactoring is done:
    1. Check off the task `[x]` in `docs/TASKS.md`.
    2. Update `docs/STATE.md` (Completed Task, Current Status, Next Step).
    3. Execute a git commit using Conventional Commits format (e.g., `feat(domain): ...`, `test(domain): ...`).
- **Context Monitor:** After completing 3–4 tasks in a single session, notify the user: "Context is getting heavy. Please review STATE.md, commit, and restart the chat session."
