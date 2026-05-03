## Context

The SKIP button in `PageRoute.kt`'s inline `doSkip()` JavaScript function disables itself on a successful `200 OK` response and never re-enables. Because the button state is never reset between questions, players can only cast a single skip vote per session. All vote-validity logic already lives in the game engine (`GameError.InvalidEvent` on duplicate vote or wrong phase), making the client-side disable purely cosmetic — and broken.

## Goals / Non-Goals

**Goals:**
- Players can vote to skip on every question, not just the first one.
- UX feedback ("Skipped!" text) is preserved after a successful vote.

**Non-Goals:**
- Adding client-side state tracking or polling to mirror server state.
- Changing engine or server skip logic.
- Visual distinction between "already voted this question" and "not yet voted".

## Decisions

**Remove `skipBtn.disabled = true` on success** — The server returns `400` for any invalid skip attempt (duplicate vote, wrong phase, failed-buzz player), so the only reason to disable the button was to prevent the user from spamming. Keeping the button enabled is correct because on the next question a new skip vote is entirely valid. The "Skipped!" status text alone is sufficient feedback without locking the control.

Alternatives considered:
- Re-enable the button via an SSE/polling event when a new question starts — unnecessary complexity; the server rejects invalid votes already.
- Keep button disabled but reset it on question change — requires client–server state sync; more moving parts for no gain.

## Risks / Trade-offs

- A player can tap SKIP multiple times per question; the second tap gets a `400` and shows "Too late!" — acceptable, since the spec already handles this scenario.

## Migration Plan

Single-line JS change in `PageRoute.kt`. No server restart coordination or data migration required.
