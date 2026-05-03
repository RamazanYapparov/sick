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
