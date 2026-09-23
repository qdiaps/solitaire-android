---
name: session-startup
description: Initialize or resume a development session. Automatically triggered when the user greets, starts, or resumes work (e.g. "Привет", "Продолжаем разработку", "Расскажи на чём остановились"). Reads project guidelines (AGENTS.md, ARCHITECTURE.md, SPEC.md), inspects current progress (ROADMAP.md, STATE.md, TASKS.md), and provides a concise status report before writing code.
---

### Purpose
Ensure the agent is fully synchronized with project architecture, rules, constraints, and current backlog state at the beginning of each session or after context resumption, without requiring manual prompts from the user.

### Trigger Phrases
Trigger automatically when the user says phrases such as:
- "Привет, продолжаем разработку..."
- "Расскажи, на чём мы остановились"
- "Старт сессии / начинаем"
- Any greeting or session restart request.

### Execution Steps
1. **Read Core Guidelines & Rules (Foundations):**
   - Read `AGENTS.md`: Role, TDD standards, ADR protocol, immutability, zero-Android-in-domain rule, and safeguards.
   - Read `docs/ARCHITECTURE.md`: Layering, package structure, MVI presentation patterns, and existing ADRs.
   - Read `docs/SPEC.md`: Klondike rules, scoring, touch controls, and specifications.

2. **Read Progress & Session Memory (State):**
   - Read `docs/ROADMAP.md`: Current milestone, phase scope, and Definition of Done (DoD).
   - Read `docs/STATE.md`: Active branch, last completed task, and recent progress log.
   - Read `docs/TASKS.md`: Current sprint backlog and active task checklist.

3. **Status Confirmation (Report):**
   - Present a concise report to the user:
     - **Готово:** Последняя завершённая задача и проверенные тесты.
     - **Текущий статус:** Активная фаза и текущая ветка.
     - **Следующий шаг:** Ближайшая задача из `docs/TASKS.md`.
   - **Crucial Rule:** Do NOT write or modify code in this step. Await user confirmation before proceeding with implementation.
