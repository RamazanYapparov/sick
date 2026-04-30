## ADDED Requirements

### Requirement: Toggle button appears when themes are hidden
The question board SHALL display a toggle button only in the host screen when at least one theme is fully played. The shared display screen SHALL never render the toggle button.

#### Scenario: One fully-played theme exists — host screen
- **WHEN** at least one theme is fully played
- **THEN** a "Show completed" button is visible in the host screen above the theme list

#### Scenario: One fully-played theme exists — shared display
- **WHEN** at least one theme is fully played
- **THEN** no toggle button is rendered in the shared display screen

#### Scenario: No fully-played themes
- **WHEN** no theme is fully played
- **THEN** no toggle button is rendered in either screen

### Requirement: Toggle reveals hidden themes on both screens
Clicking the toggle in the host screen SHALL show all fully-played themes in both the host screen and the shared display screen simultaneously.

#### Scenario: Host clicks "Show completed"
- **WHEN** the host clicks the toggle button while completed themes are hidden
- **THEN** all fully-played themes become visible in the host screen
- **AND** all fully-played themes become visible in the shared display screen
- **AND** the button label in the host screen changes to "Hide completed"

#### Scenario: Host clicks "Hide completed"
- **WHEN** the host clicks the toggle button while completed themes are visible
- **THEN** fully-played themes are hidden in the host screen
- **AND** fully-played themes are hidden in the shared display screen
- **AND** the button label in the host screen changes to "Show completed"
