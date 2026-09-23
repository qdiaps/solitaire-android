---
name: tdd-workflow
description: Enforce Test-Driven Development (TDD) for pure Kotlin domain logic. Use when creating or modifying domain entities, rules, decks, solvers, or state management.
---

### Purpose
Ensure all game rules, models, and domain logic are thoroughly tested, deterministic, and free of Android framework dependencies, while maintaining absolute stylistic and structural consistency with existing codebase tests.

### Constraints
1. **Zero Android Imports:** No imports from `android.*` or `androidx.*` are allowed in the `domain` module/package. Use pure Kotlin only.
2. **Rule of 3 Failures:** If `./gradlew test` fails 3 times consecutively for the same issue, STOP immediately. Do not guess or add hacks. Present the error log and ask the user for direction.
3. **Style Consistency:** Match existing test conventions: Kotlin backticks for test method names (e.g. ``fun `canDraw checks stock availability`()``), `@Nested inner class` with `@DisplayName`, and JUnit 5 assertions (`assertEquals`, `assertTrue`, `assertThrows(Class::class.java)`).

### Execution Steps
1. **Reconnaissance & Style Alignment (Pre-flight):**
    - Inspect existing tests in the target package or adjacent modules (e.g. `KlondikeRulesStockTest.kt`, `KlondikeDealerTest.kt`, `DeckTest.kt`, `ModelsTest.kt`).
    - Identify and strictly match conventions:
      - Test naming: Kotlin backticks with descriptive sentence (e.g. ``fun `canDraw checks stock availability`()``).
      - Test grouping: `@Nested inner class` with `@DisplayName`.
      - Assertion style: JUnit 5 assertions (`assertEquals`, `assertTrue`, `assertThrows(Class::class.java) { ... }`).
    - If no tests exist for this component, study other test suites in the codebase and mirror their style.
2. **Red Phase (Test First):**
    - Create or update a test class under `app/src/test/kotlin/.../domain/` following the identified style.
    - Write targeted JUnit 6 test cases covering normal flows and boundary conditions (e.g., empty tableau, illegal rank/suit drops, king-only empty slots).
    - Execute `./gradlew test` in the terminal and verify the build fails or cannot compile.
3. **Green Phase (Minimal Implementation):**
    - Write the simplest, most direct Kotlin code in `app/src/main/kotlin/.../domain/` to make all tests pass.
    - Run `./gradlew test` to verify all tests are green.
4. **Refactor Phase:**
    - Refactor code for idiomatic Kotlin (immutability, data classes, expression bodies).
    - Add concise KDoc comments strictly for non-obvious Klondike rules or complex calculations.
    - Run `./gradlew test` once more to guarantee zero regressions.
