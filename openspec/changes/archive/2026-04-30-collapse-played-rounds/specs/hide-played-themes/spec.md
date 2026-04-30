## ADDED Requirements

### Requirement: Fully-played themes hidden by default
The question board SHALL hide themes where every question is played, by default.

#### Scenario: All questions in a theme are played
- **WHEN** a theme has all questions marked as played
- **THEN** that theme card is not visible in the board

#### Scenario: Theme with at least one unplayed question
- **WHEN** a theme has one or more unplayed questions
- **THEN** that theme card remains visible regardless of toggle state

### Requirement: Toggle button appears when themes are hidden
The question board SHALL display a toggle button when at least one theme is fully played.

#### Scenario: One fully-played theme exists
- **WHEN** at least one theme is fully played
- **THEN** a "Show completed" button is visible above the theme list

#### Scenario: No fully-played themes
- **WHEN** no theme is fully played
- **THEN** no toggle button is rendered

### Requirement: Toggle reveals hidden themes
Clicking the toggle SHALL show all fully-played themes inline in the list.

#### Scenario: Host clicks "Show completed"
- **WHEN** the host clicks the toggle button while completed themes are hidden
- **THEN** all fully-played themes become visible and the button label changes to "Hide completed"

#### Scenario: Host clicks "Hide completed"
- **WHEN** the host clicks the toggle button while completed themes are visible
- **THEN** fully-played themes are hidden again and the button label changes to "Show completed"

### Requirement: Toggle state resets when a new question is answered
The toggle SHALL reset to hidden when game state changes cause recomposition.

#### Scenario: Question answered while completed themes visible
- **WHEN** a question is answered (board recomposes)
- **THEN** the toggle resets to hidden state
