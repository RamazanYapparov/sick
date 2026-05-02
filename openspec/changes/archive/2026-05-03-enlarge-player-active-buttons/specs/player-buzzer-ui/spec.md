## ADDED Requirements

### Requirement: Active buzzer screen fills the full viewport
After a player joins, the buzzer screen SHALL occupy the entire viewport with no containing card or padding constraints.

#### Scenario: Buzz section covers full screen
- **WHEN** a player has successfully joined the game
- **THEN** the `#buzz-section` fills the full viewport width and height

#### Scenario: Join card is hidden after joining
- **WHEN** a player has successfully joined the game
- **THEN** the `main` join card is no longer visible

### Requirement: BUZZ and SKIP buttons each occupy half the viewport height
The BUZZ button SHALL fill the top half of the viewport and the SKIP button SHALL fill the bottom half, each spanning the full viewport width.

#### Scenario: BUZZ button fills top half
- **WHEN** the buzzer screen is active
- **THEN** the BUZZ button has a height of approximately 50% of the viewport height and spans full width

#### Scenario: SKIP button fills bottom half
- **WHEN** the buzzer screen is active
- **THEN** the SKIP button has a height of approximately 50% of the viewport height and spans full width

### Requirement: Player name and status are visible on the active buzzer screen
The player's name and the last status message SHALL be visible without obscuring the tap targets.

#### Scenario: Player name shown on active screen
- **WHEN** the buzzer screen is active
- **THEN** the player's name is displayed in a non-interactive strip or overlay

#### Scenario: Status message shown on active screen
- **WHEN** a buzz or skip action receives a server response
- **THEN** the status message is displayed without covering the full button area
