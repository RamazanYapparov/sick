## ADDED Requirements

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
The browser buzzer page SHALL render a SKIP button alongside the BUZZ button after a player has joined. The SKIP button SHALL POST the joined player's ID to `/skip` and reflect the response in the page status area.

#### Scenario: Player taps SKIP and the vote is recorded
- **WHEN** a joined player clicks the SKIP button and the server responds `200 OK`
- **THEN** the page shows a "Skipped!" status and the SKIP button is left disabled

#### Scenario: Player taps SKIP and the vote is rejected
- **WHEN** a joined player clicks the SKIP button and the server responds `400`
- **THEN** the page shows a "Too late!" status and the SKIP button is re-enabled so the player can try again on a future question

#### Scenario: SKIP button hidden before joining
- **WHEN** the page first loads and the player has not yet joined
- **THEN** the SKIP button is not visible (it is part of the same buzz section that is hidden until join succeeds)
