## Why

The `PlayerSkipped` event and skip-vote logic landed in `core` (see `player-skip-vote` spec), but players have no way to actually emit it. The browser buzzer page only exposes a BUZZ button, and the server has no endpoint to forward skip votes. The feature is effectively dormant until the wiring is added.

## What Changes

- Add a `POST /skip` HTTP route to the server that processes a `PlayerSkipped(playerId)` event against the engine
- Add a SKIP button on the player buzzer page (`PageRoute.kt` HTML) that posts to `/skip` with the joined player's ID
- Disable / re-enable the SKIP button based on the response (so a player can't re-vote after a successful skip vote and gets clear feedback when their vote is rejected)
- Reuse the same engine and `buzzAllowed` gating already used by `/buzz` (skip is also blocked while the timer is paused)

## Capabilities

### New Capabilities
<!-- None — the player-skip-vote spec already covers the engine-level behavior -->

### Modified Capabilities
- `player-skip-vote`: Add requirements describing the HTTP transport (POST /skip) and player UI (SKIP button) so the existing engine event has an actual delivery path

## Impact

- `server/src/main/kotlin/com/sick/server/routes/SkipRoute.kt` — new file, mirrors `BuzzRoute.kt`
- `server/src/main/kotlin/com/sick/server/GameServer.kt` — install the new route
- `server/src/main/kotlin/com/sick/server/routes/PageRoute.kt` — add SKIP button + `doSkip()` JS handler
- `server/src/test/kotlin/com/sick/server/SkipRouteTest.kt` — new tests parallel to `BuzzRouteTest`
- No `core` or `composeApp` changes required
