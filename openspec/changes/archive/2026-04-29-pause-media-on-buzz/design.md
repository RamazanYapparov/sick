## Context

Media playback (audio/video) in the question display is handled by `AudioPlayer` and `VideoPlayer` composables. They accept a `stopSignal: Int` (incrementing int) to stop playback, and an `onFinished` callback. Currently they have no pause/resume capability.

Phase transitions are driven by `DesktopSessionController.process()`, which already tracks `previousPhase` and `engine.phase` after each event. Media state is published via `DesktopUiState` (`mediaActive`, `mediaStopSignal`).

When a player buzzes: `ShowingQuestion` → `PlayerAnswering`.
When host rejects: `PlayerAnswering` → `ShowingQuestion`.
When host accepts: `PlayerAnswering` → `ShowingAnswer`.

## Goals / Non-Goals

**Goals:**
- Pause media when phase enters `PlayerAnswering`
- Resume media when phase returns to `ShowingQuestion` from `PlayerAnswering`
- Stop media (via existing `stopSignal`) when phase enters `ShowingAnswer` from `PlayerAnswering`
- Preserve existing behavior: if media was not playing when player buzzed, nothing changes

**Non-Goals:**
- Pause media for timer pause/resume (unrelated control)
- Persist playback position across question resets
- Change the timer/media-delay coordination in `TimerOrchestrator`

## Decisions

### Decision 1: `paused: Boolean` parameter over a second signal int

Add a `paused: Boolean` param to `AudioPlayer` and `VideoPlayer`. When it flips to `true`, call `player.pause()`; when it flips to `false`, call `player.play()` (only if player hasn't stopped/finished).

**Alternative considered**: second `pauseSignal: Int` / `resumeSignal: Int` pair (like `stopSignal`). Rejected — two incrementing ints for a binary state is needlessly complex. A boolean is declarative and maps directly to what the composable needs to know.

### Decision 2: Drive pause state from `DesktopSessionController.process()`

After each successful event, inspect `previousPhase` → `engine.phase` transition and set a `private var mediaPaused: Boolean` field. Publish via `DesktopUiState.mediaPaused`.

**Alternative considered**: derive `mediaPaused` from `phase` alone in `publishState()` (i.e., `mediaPaused = phase == PlayerAnswering`). Rejected — this would pause media even when no media was playing (e.g., text-only questions). The controller should only set `mediaPaused = true` when `mediaActive` is true at the time of the buzz.

### Decision 3: Stop via existing `stopSignal`, not a new mechanism

On `HostAccepted` (→ `ShowingAnswer`), increment `mediaStopSignal` just like `skipMedia()` does. Clear `mediaPaused` at the same time. No new stop path needed.

### Decision 4: No changes to `TimerOrchestrator`

If media was paused mid-play (timer hadn't started yet), `isMediaPending` remains true. When the question resumes (wrong answer → media plays again), media will eventually call `onFinished`, which triggers `timerOrchestrator.onMediaFinished()` normally. The orchestrator doesn't need to know about pause/resume.

## Risks / Trade-offs

- **JavaFX `player.pause()` from Compose thread** → Must dispatch via `JfxPlatform.runLater {}`, same as existing stop calls. Risk is low — pattern already established.
- **Media already finished when player buzzes** → `player.pause()` on an ended player is a no-op in JavaFX. Safe.
- **Player buzzes before media finishes on display window (non-compact mode only)** → compact mode shows text placeholders, not real players — no issue there. The `paused` param is only wired in the non-compact display path anyway.
- **Multiple buzz/reject cycles** → Each `HostRejected` returns to `ShowingQuestion`, which sets `mediaPaused = false` and calls `player.play()`. If media already finished during the answering pause, `play()` would restart from the beginning. This is acceptable — media is already done, `onFinished` was called; the player state prevents a meaningful replay.

## Open Questions

- Should media resume from the exact paused position, or is JavaFX's built-in pause/play sufficient? (JavaFX `MediaPlayer.pause()` preserves position by default — this is fine.)
