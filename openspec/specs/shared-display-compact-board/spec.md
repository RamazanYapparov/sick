## ADDED Requirements

### Requirement: Question buttons visible at any theme count
When the compact board is shown with `fillHeight=true`, every theme's question price buttons SHALL be visible regardless of how many themes are in the round. Card-internal padding and spacing SHALL adapt so buttons are never clipped or hidden.

#### Scenario: Questions visible with few themes
- **WHEN** the shared display shows the board overview with 3 or fewer themes
- **THEN** all theme name texts and all question price buttons are visible inside their respective cards

#### Scenario: Questions visible with many themes
- **WHEN** the shared display shows the board overview with 8 or more themes
- **THEN** all theme name texts and all question price buttons are visible inside their respective cards

### Requirement: Compact internal padding when fillHeight is enabled
When `fillHeight=true`, each theme card's inner column SHALL use reduced vertical padding (no more than 4 dp top and 4 dp bottom) to minimise wasted space within weighted-height rows.

#### Scenario: Compact padding applied in fill-height mode
- **WHEN** `QuestionBoard` renders with `fillHeight=true`
- **THEN** the vertical padding inside each theme card is 4 dp or less on top and bottom

#### Scenario: Default padding preserved in non-fill-height mode
- **WHEN** `QuestionBoard` renders with `fillHeight=false` (the default)
- **THEN** the vertical padding inside each theme card is unchanged (12 dp)

### Requirement: Space within compact theme card distributed between name and buttons
When `fillHeight=true`, the vertical space inside each theme card SHALL be distributed between the theme name and the question row with no fixed-height spacer between them.

#### Scenario: No fixed spacer between name and buttons in compact mode
- **WHEN** `QuestionBoard` renders with `fillHeight=true`
- **THEN** there is no fixed-height spacer element between the theme name text and the question buttons row

#### Scenario: Inner column fills card height
- **WHEN** `QuestionBoard` renders with `fillHeight=true`
- **THEN** the inner column of each theme card fills the full card height so available space is distributed

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
