## ADDED Requirements

### Requirement: Shared display header shows only round progress
The shared display header SHALL show only a compact single-line round indicator ("Round X / Y" or "Lobby"). It SHALL NOT show the pack name or game phase label.

#### Scenario: Round indicator shown during active round
- **WHEN** the shared display renders with an active round
- **THEN** the header contains exactly one line reading "Round X / Y" where X is the current round index and Y is the total rounds

#### Scenario: Lobby indicator shown before game starts
- **WHEN** the shared display renders with no active round
- **THEN** the header contains exactly one line reading "Lobby"

#### Scenario: Pack name is not shown
- **WHEN** the shared display renders
- **THEN** the pack name is not visible in the header area

#### Scenario: Phase label is not shown
- **WHEN** the shared display renders
- **THEN** the game phase name is not visible in the header area

### Requirement: Board fills all available height with themes evenly distributed
When the board overview is visible (no question is active), all themes SHALL be distributed evenly across the full available height of the board area. Each theme row SHALL receive an equal share of the vertical space.

#### Scenario: All themes visible simultaneously
- **WHEN** the shared display shows the board overview with N themes
- **THEN** all N theme rows are visible on screen without scrolling

#### Scenario: Themes share height equally
- **WHEN** there are multiple themes
- **THEN** each theme row occupies the same vertical space (1/N of the board area)

### Requirement: Questions rendered in a single row per theme
Within the board overview, each theme's question buttons SHALL be displayed in a single horizontal `Row`. The buttons SHALL fill the available width evenly.

#### Scenario: Questions appear on one line
- **WHEN** a theme has multiple questions
- **THEN** all question price buttons appear on a single horizontal row, not wrapped to a second line

#### Scenario: Question buttons fill row width
- **WHEN** a theme row is rendered
- **THEN** the question buttons distribute evenly across the full width of the row

### Requirement: Theme names truncate gracefully
Theme names that are too long to fit in their row SHALL be truncated with an ellipsis on a single line rather than wrapping.

#### Scenario: Long theme name is truncated
- **WHEN** a theme name exceeds the available width
- **THEN** the name is shown on one line ending with "..."
