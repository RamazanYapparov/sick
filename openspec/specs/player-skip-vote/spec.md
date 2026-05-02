## ADDED Requirements

### Requirement: Player can vote to skip the current question
During the `ShowingQuestion` phase, any eligible player (one who has not previously given a wrong answer on the current question) SHALL be able to submit a `PlayerSkipped` vote. A player who is already in `failedBuzzPlayerIds` SHALL NOT be allowed to vote to skip.

#### Scenario: Eligible player votes to skip
- **WHEN** a player who is NOT in `failedBuzzPlayerIds` sends `PlayerSkipped` during `ShowingQuestion`
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

### Requirement: Unanimous eligible skip vote triggers auto-skip
When every eligible player (all players not in `failedBuzzPlayerIds`) has voted to skip, the engine SHALL automatically apply the skip outcome: transition to `ShowingAnswer` (clearing `answeringPlayerId`, timer, and vote sets) as if `SkipQuestion` had been issued by the host.

#### Scenario: Last eligible player votes to skip
- **WHEN** a player casts the final `PlayerSkipped` vote such that `skipVotePlayerIds` covers all players not in `failedBuzzPlayerIds`
- **THEN** the phase transitions to `ShowingAnswer`, `timerRemaining` is reset to 0, `answeringPlayerId` is cleared, and both `failedBuzzPlayerIds` and `skipVotePlayerIds` are cleared

#### Scenario: Non-unanimous vote does not auto-skip
- **WHEN** `PlayerSkipped` is cast but at least one eligible player has not yet voted
- **THEN** the vote is recorded and the phase remains `ShowingQuestion`

#### Scenario: Auto-skip threshold accounts for failed players
- **WHEN** two out of three players are in `failedBuzzPlayerIds` and the one remaining eligible player votes to skip
- **THEN** the skip threshold is met (1 eligible player, 1 vote) and the phase transitions to `ShowingAnswer`

### Requirement: Skip vote state is cleared between questions
The `skipVotePlayerIds` set SHALL be cleared whenever a new question begins (when `QuestionSelected` is processed), alongside the existing `failedBuzzPlayerIds` reset.

#### Scenario: Skip votes do not carry over to the next question
- **WHEN** a new question is selected after a round of skip votes
- **THEN** `skipVotePlayerIds` is empty at the start of the new question
