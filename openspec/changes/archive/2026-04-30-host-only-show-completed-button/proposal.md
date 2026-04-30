## Why

The "Show completed" toggle button currently appears on both the host screen and the shared display screen, but only the host should control board visibility. Showing a UI control button on the audience display is confusing and breaks the clean separation between operator controls and the public-facing view.

## What Changes

- The "Show completed" / "Hide completed" button is removed from the shared display screen.
- The `showCompleted` toggle state is lifted out of `QuestionBoard` and shared between both screens so the host's toggle decision is reflected on the shared display.
- When the host enables "Show completed", completed themes become visible on the shared display without any button appearing there.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `hide-played-themes`: The toggle button now appears only in the host screen. The show/hide state is shared — when the host toggles it on, completed themes become visible on the shared display screen as well.

## Impact

- `composeApp/…/components/QuestionBoard.kt` — add `showCompleted: Boolean` and optional `onShowCompletedToggle: (() -> Unit)?` parameters; render button only when callback is non-null.
- `composeApp/…/components/PhaseControls.kt` — hoist `showCompleted` state; pass toggle callback to `QuestionBoard`.
- `composeApp/…/window/SharedDisplay.kt` — pass `showCompleted` state to `QuestionBoard`; no toggle callback (button hidden).
- `composeApp/…/kotlin/main.kt` or `App.kt` — hoist `showCompleted` state at a level shared by both windows so state flows to both.
