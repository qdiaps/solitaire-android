---
name: state-and-git-sync
description: Finalize atomic tasks, update documentation files (STATE.md, TASKS.md, ARCHITECTURE.md), and generate Conventional Commits. Use whenever a task or subtask is completed and tests pass.
---

### Purpose
Maintain a consistent Single Source of Truth across docs and git history so any session can be safely resumed or reset without context drift.

### Core Safeguards
1. **Single-Task Boundary (1 цикл = 1 таска):** Strictly finish only the approved task. Never bundle multiple tasks or begin the next task automatically.
2. **Phase & Branch Integrity:** Never autonomously switch git branches, change phase, or add new backlog tasks. If completing a phase review task, report completion, specify the next phase as the next goal in `docs/STATE.md`, and stop.
3. **Autonomous Verification:** Run `git status`, `git diff`, and `./gradlew test` directly without asking user confirmation.
4. **Project-Wide Style Uniformity:** Maintain strict style, naming, casing, and formatting consistency across ALL touched files (code, tests, configs, docs). Never invent new styles or alter existing formats mid-phase; project-wide refactoring occurs strictly at the end of each phase.
5. **Minimal Diff & Zero Unrelated Edits:** Never modify anything outside the direct scope of the assigned task without user instructions. Never alter past entries, change historical verb tenses in `docs/STATE.md`, or reword unrelated code/comments.
6. **Multiline Integrity & No `\n` Collapsing:** Never collapse multi-line structures into a single line with literal `\n` characters or escaped breaks. Always ensure natural line formatting.

### Execution Steps
1. **Pre-commit Verification & Self-Audit:**
    - Run `./gradlew test` (or the targeted test suite) autonomously to ensure all tests pass.
    - Run `git status` and `git diff` autonomously to inspect all modified, added, or deleted files.
    - Audit `git diff` against safeguards: no unrelated edits, no formatting collapses/`\n` artifacts, and adherence to file-level style precedents.
2. **Documentation Sync:**
    - **`docs/TASKS.md`:** Mark completed task with `[x]`. Do NOT add or execute new tasks unless explicitly instructed by the user.
    - **`docs/STATE.md`:**
        - Update `Completed Tasks` to reflect the finished task.
        - Append a new entry to `Recent Progress Log` matching the exact formatting and style of previous entries.
        - Set `Next Immediate Step` clearly for the subsequent iteration.
        - If finishing the final task of a phase (e.g., Phase review/wrap-up): report all phase tasks complete and set the next phase as the next goal, without changing branch or editing phase scope.
    - **`docs/ARCHITECTURE.md` (if applicable):** If a structural decision was made (ADR), log an entry in the `Architecture Decisions (ADR)` section.
3. **Git Commit:**
    - Stage relevant files (`git add ...`). Do NOT use `git add .` if unintended scratch files exist.
    - Commit using Conventional Commits format:
        - `feat(domain): ...` for new game mechanics or models
        - `test(rules): ...` for new test cases
        - `refactor(engine): ...` for structural cleanup
        - `fix(solver): ...` for bug fixes
        - `docs(state): ...` when updating progress notes
4. **Stop & Await Confirmation:**
    - Do NOT proceed to the next task. Report the completion and await explicit user confirmation.
