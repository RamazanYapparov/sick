## ADDED Requirements

### Requirement: SkipRound event exists
The engine SHALL define a `SkipRound` event that the host can send to abandon the current round early.

#### Scenario: Event is defined
- **WHEN** the host triggers skip-round
- **THEN** a `SkipRound` event object is dispatched to the engine

### Requirement: SkipRound accepted in ChoosingQuestion phase
The engine SHALL accept `SkipRound` when the phase is `ChoosingQuestion`.

#### Scenario: Skip from question board
- **WHEN** phase is `ChoosingQuestion` and the host sends `SkipRound`
- **THEN** the engine processes the event without error

### Requirement: SkipRound accepted in ChoosingPlayer phase
The engine SHALL accept `SkipRound` when the phase is `ChoosingPlayer`.

#### Scenario: Skip immediately after round starts
- **WHEN** phase is `ChoosingPlayer` and the host sends `SkipRound`
- **THEN** the engine processes the event without error

### Requirement: SkipRound marks all round questions as played
On `SkipRound`, the engine SHALL add all question IDs of the current round to `playedQuestionIds`.

#### Scenario: Remaining questions marked played
- **WHEN** the host sends `SkipRound` with unplayed questions remaining
- **THEN** `GameState.playedQuestionIds` contains every question ID from the current round

### Requirement: SkipRound transitions to RoundEnd
After processing `SkipRound`, the engine SHALL transition the phase to `RoundEnd`.

#### Scenario: Phase becomes RoundEnd
- **WHEN** the host sends `SkipRound` from `ChoosingQuestion`
- **THEN** `engine.phase` is `RoundEnd`

#### Scenario: Phase becomes RoundEnd from ChoosingPlayer
- **WHEN** the host sends `SkipRound` from `ChoosingPlayer`
- **THEN** `engine.phase` is `RoundEnd`

### Requirement: NextRound still works after SkipRound
After `SkipRound` reaches `RoundEnd`, the host SHALL be able to send `NextRound` and advance normally.

#### Scenario: Advance to next round
- **WHEN** phase is `RoundEnd` following a `SkipRound`
- **THEN** sending `NextRound` transitions to `ChoosingPlayer` (or `GameOver` if last round)

### Requirement: Host UI shows Skip Round button in ChoosingQuestion
The host window SHALL display a "Skip Round" button during `ChoosingQuestion` phase.

#### Scenario: Button visible
- **WHEN** phase is `ChoosingQuestion`
- **THEN** a "Skip Round" button is rendered in the controls section

#### Scenario: Button triggers skip
- **WHEN** the host clicks "Skip Round"
- **THEN** `DesktopSessionController.skipRound()` is called
