## Why

Players currently have no agency to skip a question — only the host can. Adding a player-driven skip vote makes gameplay more interactive and reduces host micromanagement when a question is clearly unanswerable.

## What Changes

- New `PlayerSkipped(playerId: UUID)` event players emit when they want to skip
- New `skipVotePlayerIds: Set<UUID>` field tracked in `GameState`
- When every player has voted to skip, the engine automatically triggers the existing skip flow (reveal answer, then next question)
- Skip votes are one-way (no un-vote) and only valid during `ShowingQuestion` phase
- Buzz button (`PlayerBuzzed`) requires no core changes — it already exists and works correctly; only a UI button is needed
- `skipVotePlayerIds` is cleared when a new question begins (alongside `failedBuzzPlayerIds`)

## Capabilities

### New Capabilities

- `player-skip-vote`: Players can individually vote to skip the current question; when all players have voted the question is automatically skipped (answer revealed, then next question)

### Modified Capabilities

<!-- No existing spec-level behavior is changing -->

## Impact

- `core/event/Events.kt` — add `PlayerSkipped(playerId: UUID)`
- `core/model/game.kt` — add `skipVotePlayerIds: Set<UUID>` field
- `core/engine/GameEngine.kt` — handle `PlayerSkipped`: validate phase + duplicate vote, update state, auto-skip when vote is unanimous
- `composeApp/` — player screen UI (out of scope for this core-first phase)
