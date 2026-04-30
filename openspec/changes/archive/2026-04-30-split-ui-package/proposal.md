## Why

The `app.ui` package contains 10 unorganized files mixing window entry-points, reusable components, media players, and theme constants. Splitting into subpackages makes the codebase easier to navigate and clarifies each file's role.

## What Changes

- Move `HostWindow.kt` + `SharedDisplay.kt` → `app.ui.window`
- Move `AudioPlayer.kt` + `VideoPlayer.kt` → `app.ui.media`
- Move `PhaseControls.kt`, `PlayerComponents.kt`, `QuestionBoard.kt`, `Scoreboard.kt`, `SectionCard.kt` → `app.ui.components`
- Move `Palette.kt` → `app.ui.theme`
- Update package declarations in all 10 files
- Add cross-package imports wherever same-package references existed

## Capabilities

### New Capabilities
<!-- none — pure refactor, no behavior changes -->

### Modified Capabilities
<!-- none — no spec-level requirement changes -->

## Impact

- `composeApp/src/desktopMain/kotlin/app/ui/` — all 10 files restructured
- Any file outside `app.ui` that imports from it (e.g., `App.kt`, `main.kt`, display window) must update imports
- No API or behavior changes
