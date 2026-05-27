## MODIFIED Requirements

### Requirement: Player can vote to skip the current question
During the `ShowingQuestion` phase, any eligible player (one who has not previously given a wrong answer on the current question AND has not already voted to skip) SHALL be able to submit a `PlayerSkipped` vote. A player who is already in `failedBuzzPlayerIds` OR `skipVotePlayerIds` SHALL NOT be allowed to vote to skip.

#### Scenario: Eligible player votes to skip
- **WHEN** a player who is NOT in `failedBuzzPlayerIds` and NOT in `skipVotePlayerIds` sends `PlayerSkipped` during `ShowingQuestion`
- **THEN** the player's ID is added to `skipVotePlayerIds` and the phase remains `ShowingQuestion`

#### Scenario: Failed player cannot vote to skip
- **WHEN** a player who IS in `failedBuzzPlayerIds` sends `PlayerSkipped`
- **THEN** the engine returns a `GameError.InvalidEvent` and state is unchanged

#### Scenario: Skip vote is rejected outside ShowingQuestion
- **WHEN** `PlayerSkipped` is sent during any phase other than `ShowingQuestion`
- **THEN** the engine returns a `GameError.InvalidEvent` and state is unchanged

#### Scenario: A player cannot vote to skip twice
- **WHEN** a player who is already in `skipVotePlayerIds` sends `PlayerSkipped` again
- **THEN** the engine returns a `GameError.InvalidEvent` and state is unchanged

### Requirement: Unanimous skip vote triggers auto-skip including failed players
When every player is either in `skipVotePlayerIds` (voted to skip) or `failedBuzzPlayerIds` (gave wrong answer), the engine SHALL automatically apply the skip outcome: transition to `ShowingAnswer` (clearing `answeringPlayerId`, timer, and vote sets) as if `SkipQuestion` had been issued by the host.

#### Scenario: Last eligible player votes to skip
- **WHEN** a player casts a `PlayerSkipped` vote such that all players are now in either `skipVotePlayerIds` or `failedBuzzPlayerIds`
- **THEN** the phase transitions to `ShowingAnswer`, `timerRemaining` is reset to 0, `answeringPlayerId` is cleared, and both `failedBuzzPlayerIds` and `skipVotePlayerIds` are cleared

#### Scenario: Non-unanimous vote does not auto-skip
- **WHEN** `PlayerSkipped` is cast but at least one eligible player has not yet voted AND is not in `failedBuzzPlayerIds`
- **THEN** the vote is recorded and the phase remains `ShowingQuestion`

#### Scenario: Auto-skip when only one player remains eligible
- **WHEN** two out of three players are in `failedBuzzPlayerIds` and the one remaining eligible player votes to skip
- **THEN** the skip threshold is met (all players accounted for) and the phase transitions to `ShowingAnswer`

#### Scenario: Auto-skip triggers after player answers wrong and others already voted to skip
- **WHEN** Player A voted to skip (in `skipVotePlayerIds`), Player B answers wrong (added to `failedBuzzPlayerIds`), and now all players are in either `skipVotePlayerIds` or `failedBuzzPlayerIds`
- **THEN** the phase transitions to `ShowingAnswer` automatically

#### Scenario: Auto-skip triggers when skip voter is the last eligible player after others failed
- **WHEN** Player A answers wrong (added to `failedBuzzPlayerIds`), then Player B votes to skip, and now all players are in either `skipVotePlayerIds` or `failedBuzzPlayerIds`
- **THEN** the phase transitions to `ShowingAnswer` automatically

### Requirement: Players who voted to skip cannot buzz
During the `ShowingQuestion` phase, a player who is in `skipVotePlayerIds` SHALL NOT be allowed to buzz. The engine SHALL reject `PlayerBuzzed` events from players in `skipVotePlayerIds` with a `GameError.InvalidEvent`.

#### Scenario: Skip voter cannot buzz
- **WHEN** a player who IS in `skipVotePlayerIds` sends `PlayerBuzzed`
- **THEN** the engine returns a `GameError.InvalidEvent` and state is unchanged

#### Scenario: Eligible player can buzz
- **WHEN** a player who is NOT in `skipVotePlayerIds` and NOT in `failedBuzzPlayerIds` sends `PlayerBuzzed`
- **THEN** the player's ID is set as `answeringPlayerId` and the phase transitions to `PlayerAnswering`

### Requirement: Skip vote state is cleared between questions
The `skipVotePlayerIds` set SHALL be cleared whenever a new question begins (when `QuestionSelected` is processed), alongside the existing `failedBuzzPlayerIds` reset.

#### Scenario: Skip votes do not carry over to the next question
- **WHEN** a new question is selected after a round of skip votes
- **THEN** `skipVotePlayerIds` is empty at the start of the new question

### Requirement: Server exposes a skip-vote endpoint
The server SHALL expose `POST /skip` that accepts a `playerId` form parameter and forwards it to the game engine as a `PlayerSkipped` event. The endpoint SHALL be gated by the same predicate as `/buzz` so that skip votes are rejected while the timer is paused.

#### Scenario: Eligible player submits a skip vote during ShowingQuestion
- **WHEN** an eligible player POSTs to `/skip` with their `playerId` while the engine is in `ShowingQuestion` and the timer is not paused
- **THEN** the server responds `200 OK` and the player's ID is recorded in `skipVotePlayerIds`

#### Scenario: Skip vote rejected by the engine
- **WHEN** a player POSTs to `/skip` and the engine returns `GameError.InvalidEvent` (wrong phase, player already voted, or player in `failedBuzzPlayerIds`)
- **THEN** the server responds `400 Bad Request` with the engine error message and the engine state is unchanged

#### Scenario: Skip vote during paused timer
- **WHEN** a player POSTs to `/skip` while the timer is paused
- **THEN** the server responds `503 Service Unavailable` and does NOT call `engine.process`

#### Scenario: Missing or invalid playerId
- **WHEN** a player POSTs to `/skip` with no `playerId`, or with a value that is not a valid UUID
- **THEN** the server responds `400 Bad Request` and the engine state is unchanged

### Requirement: Player UI exposes a SKIP control
The browser buzzer page SHALL render a SKIP button alongside the BUZZ button after a player has joined. The SKIP button SHALL POST the joined player's ID to `/skip` and reflect the response in the page status area. The SKIP button SHALL remain enabled at all times; the server is responsible for rejecting invalid votes.

#### Scenario: Player taps SKIP and the vote is recorded
- **WHEN** a joined player clicks the SKIP button and the server responds `200 OK`
- **THEN** the page shows a "Skipped!" status and the SKIP button remains enabled

#### Scenario: Player taps SKIP and the vote is rejected
- **WHEN** a joined player clicks the SKIP button and the server responds `400`
- **THEN** the page shows a "Too late!" status and the SKIP button remains enabled

#### Scenario: SKIP button hidden before joining
- **WHEN** the page first loads and the player has not yet joined
- **THEN** the SKIP button is not visible (it is part of the same buzz section that is hidden until join succeeds)
