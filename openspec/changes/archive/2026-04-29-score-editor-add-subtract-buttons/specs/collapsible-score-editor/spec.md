## MODIFIED Requirements

### Requirement: Score editor collapsed by default
Each player row in the host screen SHALL show only the player name, current score, and an "Edit" button when first rendered. Score delta input, "+" button, and "−" button SHALL NOT be visible in the collapsed state.

#### Scenario: Initial render
- **WHEN** the host screen displays a player row
- **THEN** only the player name, current score, and "Edit" button are visible
- **THEN** the score delta input, "+" button, and "−" button are not visible

### Requirement: Expand score editor on demand
The player row SHALL expand to show the score delta input, "+" button, and "−" button when the host clicks the "Edit" button.

#### Scenario: Edit button clicked
- **WHEN** the host clicks the "Edit" button on a player row
- **THEN** the score delta input, "+" button, and "−" button become visible for that player
- **THEN** the "Edit" button is no longer visible

#### Scenario: Multiple rows can expand independently
- **WHEN** the host clicks "Edit" on more than one player row
- **THEN** all clicked rows show their score inputs independently

### Requirement: Collapse score editor after score change
The player row SHALL return to collapsed state after the host clicks "+" or "−".

#### Scenario: Plus button clicked
- **WHEN** the host enters a positive score delta and clicks "+"
- **THEN** the score is increased by the entered amount
- **THEN** the score delta input, "+" button, and "−" button are hidden
- **THEN** the updated score is shown in the collapsed row

#### Scenario: Minus button clicked
- **WHEN** the host enters a positive score delta and clicks "−"
- **THEN** the score is decreased by the entered amount
- **THEN** the score delta input, "+" button, and "−" button are hidden
- **THEN** the updated score is shown in the collapsed row

## ADDED Requirements

### Requirement: Score delta input accepts only positive integers
The score delta input field SHALL accept only positive integer values. Negative numbers and non-numeric characters SHALL NOT be enterable.

#### Scenario: Non-digit characters rejected
- **WHEN** the host types a non-digit character into the score delta input
- **THEN** the character is not added to the input value

#### Scenario: Negative sign rejected
- **WHEN** the host types a minus sign into the score delta input
- **THEN** the character is not added to the input value

#### Scenario: Buttons disabled for empty or zero input
- **WHEN** the score delta input is empty or contains "0"
- **THEN** both "+" and "−" buttons are disabled
