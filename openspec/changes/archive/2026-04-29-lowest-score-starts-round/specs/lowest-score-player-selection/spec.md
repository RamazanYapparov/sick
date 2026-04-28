## ADDED Requirements

### Requirement: Lowest-score candidates are computed from player scores
The system SHALL compute the set of lowest-score candidates as all players whose score equals the minimum score among all players in the game.

#### Scenario: All players have distinct scores
- **WHEN** players have distinct scores
- **THEN** the single player with the minimum score is the only candidate

#### Scenario: Multiple players share the minimum score
- **WHEN** two or more players share the same minimum score
- **THEN** all players with that minimum score are candidates

#### Scenario: First round with all scores at zero
- **WHEN** the game starts and all player scores are 0
- **THEN** all players are candidates

### Requirement: Single candidate is auto-selected at round start
The system SHALL automatically select the active player and advance to the ChoosingQuestion phase without host input when exactly one player is the lowest-score candidate.

#### Scenario: Unique lowest-score player at round start
- **WHEN** the phase transitions to ChoosingPlayer
- **AND** exactly one player has the minimum score
- **THEN** that player is automatically set as the active player
- **AND** the phase advances to ChoosingQuestion immediately

#### Scenario: Tied lowest-score players at round start
- **WHEN** the phase transitions to ChoosingPlayer
- **AND** two or more players share the minimum score
- **THEN** the system waits for host selection and does NOT auto-advance

### Requirement: Host player picker is restricted to lowest-score candidates
The system SHALL display only the lowest-score candidates in the host's player selection UI during the ChoosingPlayer phase.

#### Scenario: Host sees only tied candidates
- **WHEN** the phase is ChoosingPlayer
- **AND** multiple players share the minimum score
- **THEN** the host UI shows only those players in the picker
- **AND** players with higher scores are not shown as selectable options
