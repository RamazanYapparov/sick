## ADDED Requirements

### Requirement: Shared display shows a player card bar at the bottom
The shared display window SHALL render a horizontal row of player cards at the bottom of the screen. Each card SHALL display the player's name and current score.

#### Scenario: Player cards are rendered when players exist
- **WHEN** the game has one or more players
- **THEN** the shared display shows one card per player at the bottom, each with the player's name and score

#### Scenario: No cards rendered when no players exist
- **WHEN** the game has no players
- **THEN** the player card bar is empty (no cards shown)

### Requirement: Answering player card turns green
The card of the player who is currently answering (i.e., whose ID matches `answeringPlayerId`) SHALL have a green background.

#### Scenario: Answering player card is green
- **WHEN** a player's ID matches `answeringPlayerId`
- **THEN** that player's card background is green

#### Scenario: Non-answering player card is not green
- **WHEN** a player's ID does not match `answeringPlayerId`
- **THEN** that player's card background is not green (unless another state applies)

### Requirement: Skipping player card turns gray
The card of any player who has voted to skip the current question (i.e., whose ID is in `skipVotePlayerIds`) SHALL have a gray background.

#### Scenario: Skipping player card is gray
- **WHEN** a player's ID is in `skipVotePlayerIds`
- **THEN** that player's card background is gray

#### Scenario: Answering state takes priority over skipping state
- **WHEN** a player's ID is both `answeringPlayerId` and in `skipVotePlayerIds` (edge case)
- **THEN** the card background is green (answering takes priority)

#### Scenario: Skip state clears between questions
- **WHEN** `skipVotePlayerIds` is empty (new question or lobby)
- **THEN** no player cards are shown in gray due to a previous skip vote

### Requirement: Choosing player name is visually marked
The card of the player who is currently choosing a question (i.e., whose ID matches `activePlayerId`) SHALL visually distinguish the player's name (e.g., bold text in an accent color).

#### Scenario: Choosing player name is marked
- **WHEN** a player's ID matches `activePlayerId`
- **THEN** that player's name is rendered bold in the accent color on their card

#### Scenario: Non-choosing player name is not marked
- **WHEN** a player's ID does not match `activePlayerId`
- **THEN** that player's name is rendered in the default style

### Requirement: Existing scoreboard is removed from the shared display
The `Scoreboard` widget that previously appeared in the top-right corner of `SharedDisplayScreen` SHALL be removed. The `Scoreboard.kt` component file SHALL be deleted.

#### Scenario: Scoreboard is absent from the shared display
- **WHEN** the shared display renders
- **THEN** the top row does not contain a scoreboard widget

### Requirement: `skipVotePlayerIds` is exposed in `DesktopUiState`
`DesktopUiState` SHALL include a `skipVotePlayerIds: Set<UUID>` field populated from the engine's game state so that the UI can reflect per-player skip state.

#### Scenario: Skip vote IDs are present in UI state
- **WHEN** one or more players have voted to skip the current question
- **THEN** `DesktopUiState.skipVotePlayerIds` contains those player IDs

#### Scenario: Skip vote IDs are empty when no votes are cast
- **WHEN** no player has voted to skip (lobby, new question, or after phase transition)
- **THEN** `DesktopUiState.skipVotePlayerIds` is empty
