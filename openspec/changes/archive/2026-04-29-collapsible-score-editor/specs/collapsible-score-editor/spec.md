## ADDED Requirements

### Requirement: Score editor collapsed by default
Each player row in the host screen SHALL show only the player name, current score, and an "Edit" button when first rendered. Score delta input and "Apply" button SHALL NOT be visible in the collapsed state.

#### Scenario: Initial render
- **WHEN** the host screen displays a player row
- **THEN** only the player name, current score, and "Edit" button are visible
- **THEN** the score delta input and "Apply" button are not visible

### Requirement: Expand score editor on demand
The player row SHALL expand to show the score delta input and "Apply" button when the host clicks the "Edit" button.

#### Scenario: Edit button clicked
- **WHEN** the host clicks the "Edit" button on a player row
- **THEN** the score delta input and "Apply" button become visible for that player
- **THEN** the "Edit" button is no longer visible

#### Scenario: Multiple rows can expand independently
- **WHEN** the host clicks "Edit" on more than one player row
- **THEN** all clicked rows show their score inputs independently

### Requirement: Collapse score editor after apply
The player row SHALL return to collapsed state after the host clicks "Apply".

#### Scenario: Apply clicked
- **WHEN** the host enters a score delta and clicks "Apply"
- **THEN** the score change is submitted
- **THEN** the score delta input and "Apply" button are hidden
- **THEN** the updated score is shown in the collapsed row
