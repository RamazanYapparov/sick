## Why

The question board clutters as a round progresses — fully-answered themes stay visible and take up space, making it harder to see what's still in play. Hiding completed themes keeps the board focused on what matters.

## What Changes

- Fully-played themes (all questions answered) are hidden from the question board by default.
- A "Show completed" toggle button appears when at least one theme is hidden, allowing the host to reveal hidden themes on demand.
- Clicking the toggle shows/hides completed themes inline in the same list (no separate section).

## Capabilities

### New Capabilities
- `hide-played-themes`: Hides fully-played themes from the question board with a toggle to reveal them.

### Modified Capabilities

## Impact

- `QuestionBoard` composable — add filtering logic and toggle button.
- `BoardThemeState` already has enough data (`questions[].played`) to compute "fully played" without engine changes.
- No state machine or engine changes needed.
- No changes to `DesktopUiState` or `UiState.kt` — computed purely in the UI.
