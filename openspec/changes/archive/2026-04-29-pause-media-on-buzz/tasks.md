## 1. Add pause/resume to media player composables

- [x] 1.1 Add `paused: Boolean = false` parameter to `AudioPlayer` composable; add `LaunchedEffect(paused)` that calls `player.pause()` via `JfxPlatform.runLater` when true, and `player.play()` when false (only if player is not stopped/finished)
- [x] 1.2 Add `paused: Boolean = false` parameter to `VideoPlayer` composable with the same `LaunchedEffect(paused)` logic

## 2. Add mediaPaused state to UiState and controller

- [x] 2.1 Add `mediaPaused: Boolean` field (default `false`) to `DesktopUiState` and its `initial()` companion
- [x] 2.2 Add `private var mediaPaused: Boolean = false` to `DesktopSessionController`
- [x] 2.3 In `DesktopSessionController.process()`, after a successful transition inspect `previousPhase` → `engine.phase`: set `mediaPaused = true` when entering `PlayerAnswering` and `mediaActive` is true; set `mediaPaused = false` when returning to `ShowingQuestion` from `PlayerAnswering`; set `mediaPaused = false` and increment `mediaStopSignal` when entering `ShowingAnswer` from `PlayerAnswering`
- [x] 2.4 Include `mediaPaused = mediaPaused` in `publishState()` copy block

## 3. Wire pause signal into display

- [x] 3.1 Pass `paused = state.mediaPaused` to `AudioPlayer` calls in `CurrentQuestionPanel` (non-compact branches only)
- [x] 3.2 Pass `paused = state.mediaPaused` to `VideoPlayer` calls in `CurrentQuestionPanel` (non-compact branches only)
