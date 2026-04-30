## 1. State Layer

- [x] 1.1 Add `showCompleted: Boolean = false` field to `DesktopUiState`
- [x] 1.2 Add `toggleShowCompleted()` method to `DesktopSessionController` that flips the flag
- [x] 1.3 Reset `showCompleted` to `false` inside `DesktopSessionController.selectQuestion()` (preserve existing reset-on-question-select behaviour)

## 2. QuestionBoard Component

- [x] 2.1 Remove internal `var showCompleted by remember { mutableStateOf(false) }` from `QuestionBoard`
- [x] 2.2 Add `showCompleted: Boolean` parameter to `QuestionBoard`
- [x] 2.3 Add `onShowCompletedToggle: (() -> Unit)?` parameter to `QuestionBoard` (default `null`)
- [x] 2.4 Render the toggle button only when `onShowCompletedToggle != null`; wire its `onClick` to the callback

## 3. Host Screen Call Site

- [x] 3.1 Update `PhaseControls.kt` `QuestionBoard` call to pass `showCompleted = state.showCompleted` and `onShowCompletedToggle = controller::toggleShowCompleted`

## 4. Shared Display Call Site

- [x] 4.1 Update `SharedDisplay.kt` `BoardOverview` / `QuestionBoard` call to pass `showCompleted = state.showCompleted` and no toggle callback (`onShowCompletedToggle = null`)

## 5. Verification

- [x] 5.1 Run the app and confirm no toggle button appears on the shared display
- [x] 5.2 Confirm toggling "Show completed" on the host screen makes completed themes appear/disappear on the shared display simultaneously
- [x] 5.3 Confirm selecting a question resets the toggle (completed themes hidden again on both screens)
