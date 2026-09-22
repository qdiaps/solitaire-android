---
name: state-and-git-sync
description: Finalize atomic tasks, update documentation files (STATE.md, TASKS.md, ARCHITECTURE.md), and generate Conventional Commits. Use whenever a task or subtask is completed and tests pass.
---

### Purpose
Maintain a consistent Single Source of Truth across docs and git history so any session can be safely resumed or reset without context drift.

### Execution Steps
1. **Pre-commit Verification:**
    - Run `./gradlew test` to ensure the entire test suite passes.
    - Run `git status` and `git diff` to inspect all modified, added, or deleted files.
2. **Documentation Sync:**
    - **`docs/TASKS.md`:** Mark completed tasks with `[x]`. Add any newly discovered micro-tasks.
    - **`docs/STATE.md`:**
        - Update `Current Focus` to reflect what was just completed.
        - Append an entry to `Recent Progress Log` (date, summary, files changed).
        - Set `Next Immediate Step` clearly for the subsequent iteration.
    - **`docs/ARCHITECTURE.md` (if applicable):** If a structural decision was made (e.g., choice of data structure or state serialization), log an entry in the `Architecture Decisions (ADR)` section.
3. **Git Commit:**
    - Stage relevant files (`git add ...`). Do NOT use `git add .` if unintended scratch files exist.
    - Commit using Conventional Commits format:
        - `feat(domain): ...` for new game mechanics or models
        - `test(rules): ...` for new test cases
        - `refactor(engine): ...` for structural cleanup
        - `fix(solver): ...` for bug fixes
        - `docs(state): ...` when updating progress notes