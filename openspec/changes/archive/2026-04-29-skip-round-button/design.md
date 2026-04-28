## Context

The game engine (`GameEngine`) is an event-sourced state machine. Phases and state advance by processing `GameEvent` objects. Round completion today is detected passively: after `AnswerShown`, if `GameState.isRoundComplete` is true, the phase moves to `RoundEnd`. There is no way to force a round end from mid-round.

The host UI (`PhaseControls.kt`) renders phase-specific controls; the `DesktopSessionController` owns the engine and exposes typed action methods.

## Goals / Non-Goals

**Goals:**
- Allow the host to skip all remaining questions in a round and enter `RoundEnd` from `ChoosingQuestion` phase
- Reuse the existing `RoundEnd` → `NextRound` flow (no new phases)
- Keep the engine change minimal and consistent with existing event patterns

**Non-Goals:**
- Skipping from inside a question (revealing/showing/answering) — question must be idle first
- Skipping the last round directly to `GameOver` (existing `NextRound` logic already handles this)
- Undo or confirmation dialog

## Decisions

### 1. New `SkipRound` event (not repurposing `NextRound`)

`NextRound` is only valid in `RoundEnd`. Repurposing it would require conditional logic across several phases. A dedicated `SkipRound` event keeps validation and state transition logic clean and explicit.

**Alternative considered:** Allow `NextRound` in `ChoosingQuestion`. Rejected because it skips `RoundEnd` entirely, breaking the existing score-review moment.

### 2. Transition to `RoundEnd`, not directly to `ChoosingPlayer`

Going through `RoundEnd` preserves the existing pause for the host to review scores. It also means zero changes to the `NextRound` → `ChoosingPlayer`/`GameOver` path.

**Alternative considered:** Transition directly to `ChoosingPlayer`. Simpler code, but removes the score-review screen and diverges from normal round-end UX.

### 3. Mark all round questions as played in `applyEvent`

`isRoundComplete` is computed from `playedQuestionIds`. Setting all question IDs of the current round into `playedQuestionIds` ensures the engine's invariants hold — the `RoundEnd` phase is legitimately reached and `NextRound` will advance correctly.

### 4. Allow `SkipRound` in both `ChoosingQuestion` and `ChoosingPlayer`

`ChoosingPlayer` is the idle state at the start of a round. Allowing skip there too lets the host bail immediately after a round starts without needing to pick a question first.

## Risks / Trade-offs

- **Accidental skip** → No confirmation dialog (non-goal). The `RoundEnd` screen acts as a soft checkpoint before the round actually advances.
- **Questions skipped without being shown** → Scores are unaffected; questions are marked played. This is intentional and matches user expectation of "skip".

## Migration Plan

No persistence, no network protocol changes. All changes are in-process within a single session. No rollback strategy needed.

## Open Questions

None.
