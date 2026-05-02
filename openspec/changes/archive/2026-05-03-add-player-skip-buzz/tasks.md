## 1. Domain Event

- [x] 1.1 Add `PlayerSkipped(playerId: UUID)` data class to `core/event/Events.kt`

## 2. Game State

- [x] 2.1 Add `skipVotePlayerIds: Set<UUID> = emptySet()` field to `GameState` in `core/model/game.kt`

## 3. Engine: Event Validation

- [x] 3.1 Add `PlayerSkipped` to the allowed events set for `GamePhase.ShowingQuestion` in `GameEngine.kt`
- [x] 3.2 In `processEvent`, validate that the player is NOT in `failedBuzzPlayerIds` (return `GameError.InvalidEvent` if they are)
- [x] 3.3 Validate that the player is NOT already in `skipVotePlayerIds` (no duplicate votes)

## 4. Engine: State Update & Auto-Skip

- [x] 4.1 On valid `PlayerSkipped`, add the player's ID to `skipVotePlayerIds` in the updated state
- [x] 4.2 After adding the vote, compute eligible players (`players.ids - failedBuzzPlayerIds`) and check if `skipVotePlayerIds` covers all of them
- [x] 4.3 If unanimous: apply the skip outcome — transition phase to `ShowingAnswer`, reset `timerRemaining` to 0, clear `answeringPlayerId`, clear `failedBuzzPlayerIds` and `skipVotePlayerIds`

## 5. State Reset

- [x] 5.1 Clear `skipVotePlayerIds` alongside `failedBuzzPlayerIds` when `QuestionSelected` is processed (new question starts)

## 6. Tests

- [x] 6.1 Test: eligible player's vote is recorded, phase stays `ShowingQuestion`
- [x] 6.2 Test: player in `failedBuzzPlayerIds` cannot vote to skip
- [x] 6.3 Test: player cannot vote to skip twice
- [x] 6.4 Test: skip vote rejected outside `ShowingQuestion` phase
- [x] 6.5 Test: last eligible player's vote triggers auto-skip (phase → `ShowingAnswer`, state cleared)
- [x] 6.6 Test: auto-skip threshold accounts for failed players (2 of 3 failed → 1 eligible vote suffices)
- [x] 6.7 Test: `skipVotePlayerIds` is empty after `QuestionSelected`
