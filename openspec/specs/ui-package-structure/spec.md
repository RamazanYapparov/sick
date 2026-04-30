## Requirements

### Requirement: UI files organized into subpackages
The `app.ui` package SHALL be split into four subpackages: `window`, `media`, `components`, and `theme`. Each file SHALL reside in exactly one subpackage matching its role.

#### Scenario: Window entry-points in window subpackage
- **WHEN** developer navigates to `app.ui.window`
- **THEN** they find `HostWindow.kt` and `SharedDisplay.kt` and no other files

#### Scenario: Media players in media subpackage
- **WHEN** developer navigates to `app.ui.media`
- **THEN** they find `AudioPlayer.kt` and `VideoPlayer.kt` and no other files

#### Scenario: Game components in components subpackage
- **WHEN** developer navigates to `app.ui.components`
- **THEN** they find `PhaseControls.kt`, `PlayerComponents.kt`, `QuestionBoard.kt`, `Scoreboard.kt`, and `SectionCard.kt`

#### Scenario: Theme constants in theme subpackage
- **WHEN** developer navigates to `app.ui.theme`
- **THEN** they find `Palette.kt` and no other files

#### Scenario: App compiles after refactor
- **WHEN** `./gradlew :composeApp:build` is run after the refactor
- **THEN** build succeeds with no compilation errors
