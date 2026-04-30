## Context

The question board has a "Show completed" toggle that hides fully-played themes by default. The toggle button currently renders inside `QuestionBoard`, which is called from two places:
- `PhaseControls.kt` — host screen, `ChoosingQuestion` phase (interactive)
- `SharedDisplay.kt` → `BoardOverview` — audience/shared display (read-only)

Both callers receive the same `QuestionBoard` composable and therefore both render the toggle button. The `showCompleted` state is currently local to `QuestionBoard` via `remember`, so the two screens have independent, unrelated toggle states.

The shared display (`SharedDisplayApp`) already receives `DesktopUiState` from the controller. The host window receives `DesktopSessionController` which exposes `uiState`. Both windows are composed in `main.kt` from the same controller instance.

## Goals / Non-Goals

**Goals:**
- Remove the "Show completed" button from the shared display screen entirely.
- Make the shared display reflect the host's toggle state (when host shows completed, audience sees completed themes too).
- Reset the toggle when the host moves away from `ChoosingQuestion` (consistent with the existing reset behavior).

**Non-Goals:**
- Changing the button's visual design or label.
- Persisting toggle state across sessions.
- Adding toggle controls anywhere other than the host screen.

## Decisions

### Decision: Lift `showCompleted` into `DesktopUiState` / `DesktopSessionController`

`DesktopUiState` is already passed to both `SharedDisplayApp` (directly) and `HostWindowContent` (via the controller). Adding `showCompleted: Boolean` to `DesktopUiState` and `toggleShowCompleted()` to `DesktopSessionController` propagates the state to both views with no new parameter threading.

**Alternative considered — hoist in `main.kt`**: Requires threading `showCompleted` and the toggle callback through `HostApp → HostWindowContent → PhaseControls → QuestionBoard` and `SharedDisplayApp → SharedDisplayScreen → BoardOverview → QuestionBoard`, changing every function signature. Higher ceremony for the same outcome.

**Alternative considered — `CompositionLocal`**: Avoids threading but introduces implicit dependency; harder to trace data flow. Overkill for one boolean.

**Chosen**: `DesktopUiState` approach — zero new call-site parameters on intermediate composables, consistent with existing pattern.

### Decision: `QuestionBoard` accepts `showCompleted` and optional toggle callback

Replace internal `var showCompleted by remember { mutableStateOf(false) }` with:
- `showCompleted: Boolean` — determines which themes are visible
- `onShowCompletedToggle: (() -> Unit)?` — when non-null, renders the button; when null, no button

Host caller passes `state.showCompleted` + `controller::toggleShowCompleted`.  
Shared display caller passes `state.showCompleted` + `null`.

### Decision: Controller resets `showCompleted` when a question is selected

The existing spec requires the toggle to reset when a new question is answered. Currently this happens implicitly because `QuestionBoard` unmounts (phase changes away from `ChoosingQuestion`). With lifted state, the controller's `selectQuestion` method must set `showCompleted = false` explicitly to preserve this behaviour.

## Risks / Trade-offs

- `showCompleted` is UI preference state, not game state. Placing it in `DesktopUiState` is a mild SRP violation. Acceptable because the alternative (threading through 4+ layers) is worse and this is a trivial boolean.
- The reset on question selection must be maintained manually; a future refactor of `selectQuestion` must remember to reset it. Low risk given the small codebase.

## Open Questions

None.
