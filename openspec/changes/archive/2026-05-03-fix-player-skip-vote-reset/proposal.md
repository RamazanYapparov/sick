## Why

After a player votes to skip a question the SKIP button on their buzzer page is left permanently disabled, making it impossible to vote on any subsequent question. The server and engine already enforce all skip-vote rules (duplicate votes, wrong phase, failed-buzz exclusion), so the client-side disable is redundant and harmful.

## What Changes

- Remove the permanent disable of the SKIP button after a successful skip vote.
- The SKIP button remains enabled at all times; the server will reject any invalid re-vote with `400 Bad Request`.
- "Skipped!" status text is still shown after a successful vote so the player gets feedback.

## Capabilities

### New Capabilities

### Modified Capabilities
- `player-skip-vote`: The SKIP button MUST NOT be permanently disabled after a successful vote. The button stays enabled so the player can vote on future questions.

## Impact

- `server/src/main/kotlin/com/sick/server/routes/PageRoute.kt` — remove `skipBtn` disable on success in `doSkip()`.
