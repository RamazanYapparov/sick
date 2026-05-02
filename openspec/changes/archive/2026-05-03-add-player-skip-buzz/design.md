## Context

The game engine (`GameEngine.kt`) is an event-driven state machine. All state mutations flow through `GameEngine.process(event)`, which validates the event against the current phase, applies a state update, and optionally transitions the phase. Players already have a buzz mechanism (`PlayerBuzzed`) with analogous per-player tracking via `failedBuzzPlayerIds`.

The existing `SkipQuestion` event (host-only) already drives the correct outcome: it transitions through `ShowingAnswer` and then to the next question. The player skip vote should reuse this outcome rather than duplicating the transition logic.

## Goals / Non-Goals

**Goals:**
- Add `PlayerSkipped(playerId: UUID)` event to core
- Track skip votes in `GameState.skipVotePlayerIds`
- Auto-trigger the skip outcome when all *eligible* players have voted (eligible = not in `failedBuzzPlayerIds`)
- Only valid during `ShowingQuestion` phase; one-way votes (no un-vote)
- Clear skip votes when a new question starts

**Non-Goals:**
- Buzz button UI or any `composeApp` changes (separate phase)
- Changing buzz behavior (`PlayerBuzzed` logic is untouched)

## Decisions

### Skip threshold: players NOT in failedBuzzPlayerIds
Only players who haven't yet given a wrong answer must vote to skip. Rationale: once a player has failed a buzz, they're "out" for this question — requiring their skip vote would let a single already-eliminated player block the rest. The eligible set is `players.ids - failedBuzzPlayerIds`.

**Alternative considered**: unanimous among all players regardless of buzz history. Rejected per user requirement — eliminated players should not block a skip.

### Auto-skip fires internally in GameEngine, not via a separate event
When `PlayerSkipped` causes the eligible vote count to reach the threshold, `GameEngine.process()` applies the same state transition as `SkipQuestion` directly, in the same call. Rationale: keeps the state machine single-step and avoids race conditions from emitting a synthetic event back into the engine.

**Alternative considered**: emit a synthetic `SkipQuestion` event. Rejected — self-posting events complicate replay and testing.

### Eligible set is computed dynamically, not cached
At the moment `PlayerSkipped` is processed, compute `eligibleIds = state.players.map { it.id }.toSet() - state.failedBuzzPlayerIds`. If `skipVotePlayerIds` contains all of `eligibleIds`, auto-skip fires. This is O(n) on player count (always small) and always reflects current state.

### Skip vote stored as `Set<UUID>` on GameState (same pattern as failedBuzzPlayerIds)
Consistent with existing pattern. The set is cleared in the same place as `failedBuzzPlayerIds` (when a new question is selected).

### Validation: a player already in failedBuzzPlayerIds cannot vote to skip
Keeps the two sets semantically clean and avoids ambiguity about whether an eliminated player "needs" to vote.

## Risks / Trade-offs

- **Risk**: All remaining eligible players vote to skip, but the last eligible player also has `answeringPlayerId` set (is currently answering) → Mitigation: `PlayerSkipped` is only valid during `ShowingQuestion`, not `PlayerAnswering`. Skip votes can only accumulate while no one is currently answering.
- **Risk**: If a player leaves mid-question, the eligible set shrinks and might immediately reach threshold → Acceptable; `PlayerLeft` during a question is an edge case and the result (skip fires) is reasonable.

## Migration Plan

Pure additive change to core. No existing events, states, or fields are modified. No persistence layer exists (game state is in-memory), so no migration is required.
