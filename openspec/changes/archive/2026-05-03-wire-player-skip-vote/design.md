## Context

The engine accepts `PlayerSkipped(playerId)` during `ShowingQuestion`, validated and clearance handled identically to existing skip-vote rules. The browser buzzer page (`PageRoute.kt`) currently exposes only `/join` and `/buzz`. Players already have a `playerId` after joining (stored in JS as `playerId`), so transport for skip is symmetrical with `/buzz` — same form parameter, same validation pattern, same gating behind `buzzAllowed`.

## Goals / Non-Goals

**Goals:**
- Provide an HTTP endpoint that maps cleanly to `engine.process(PlayerSkipped(playerId))`
- Add a SKIP button beside the BUZZ button with consistent styling and feedback
- Reuse `buzzAllowed` gating so the timer-pause behavior matches BUZZ (skip votes shouldn't accumulate while paused)
- Tests for the new route mirror `BuzzRouteTest`

**Non-Goals:**
- No real-time push to the player UI (no WebSocket, no SSE). Players don't yet see live game state; reconciling that is a separate concern.
- No re-vote / un-vote support. The engine forbids it; the UI simply disables the button after a successful vote.
- No core or composeApp changes.

## Decisions

### New `/skip` route, parallel to `/buzz`
A separate `installSkipRoute(engine, buzzAllowed)` keeps each route file small and matches the existing per-route file convention (`BuzzRoute.kt`, `JoinRoute.kt`).

**Alternative considered**: a generic `/event` endpoint that accepts an event-type discriminator. Rejected — it leaks the event sealed-interface to HTTP and complicates validation.

### Reuse `buzzAllowed` for skip gating
Skip votes are also irrelevant while the timer is paused, and the existing predicate already encodes that exact condition (`!engine.state.isTimerPaused`). Renaming the parameter would churn `BuzzRoute.kt` and the controller for no real semantic gain.

**Alternative considered**: rename to `eventsAllowed` or pass a separate `skipAllowed`. Rejected — the predicate is identical; renaming is cosmetic noise.

### SKIP button: disable on success, re-enable on failure
The engine returns `GameError.InvalidEvent` for a duplicate vote, wrong phase, or a player in `failedBuzzPlayerIds`. Mapping the response 1:1 to button state is simple and matches BUZZ's pattern. After a successful vote we leave the button disabled until the page reloads or the player re-joins — there is no live state feed to flip it back on a new question, but a refused vote (HTTP 400) re-enables it so a player who fat-fingers a skip during the wrong phase isn't locked out.

**Alternative considered**: client-side phase tracking (re-enable when a new question starts). Rejected — without server push we'd be polling, which is a much bigger lift than the feature warrants.

### HTTP semantics: 200 on success, 400 on `InvalidEvent`, 503 when not allowed
Same as `/buzz`. Consistency keeps the JS handler trivially symmetric with `doBuzz()`.

## Risks / Trade-offs

- **Risk**: Player who voted to skip can't see what happened (their vote was recorded vs. unanimous → answer revealed). → Mitigation: status text differentiates "Skipped!" (200) from "Too late!" (400). Acceptable for v1; richer feedback needs live state.
- **Risk**: A user opens the page in two tabs and double-votes. → Mitigation: engine deduplicates on `playerId`, so the second tab's vote returns 400. Already handled.
- **Risk**: Button styling sits in inline CSS in `PageRoute.kt` which is already verbose. → Mitigation: reuse the existing `#buzz` selector group with an additional `#skip` rule sharing most properties.

## Migration Plan

Pure additive: new route + new HTML element. No breaking changes, no persistence, no migration. Old clients (cached HTML) won't have the SKIP button but will continue to work; a refresh picks up the new UI.
