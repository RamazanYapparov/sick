## Context

`DesktopUiState.mediaPaused` already exists and is consumed by `VideoPlayer` and `AudioPlayer` to pause/resume playback. It is currently set only by the engine listener in `bindEngine()` (buzz-triggered). The `pauseTimer()` and `resumeTimer()` controller methods only dispatch engine events and do not touch `mediaPaused`.

Buzz-triggered pause is safe from conflict: `buzzAllowed = { !engine.state.isTimerPaused }` prevents players from buzzing while the timer is paused, so host-timer pause and buzz-triggered pause cannot overlap.

## Goals / Non-Goals

**Goals:**
- Clicking Pause during `ShowingQuestion` pauses media alongside the timer
- Clicking Resume restores media from the paused position
- No new UI controls, no new state fields

**Non-Goals:**
- A media-only pause without timer pause
- Pause behavior during any phase other than `ShowingQuestion`

## Decisions

### Reuse `mediaPaused` rather than adding a separate flag

`mediaPaused` is already wired to both players and published on every `publishState()` call. Adding a second flag would require merging two boolean sources at render time. Since buzz and timer-pause are mutually exclusive, a single flag is sufficient.

**Alternative considered**: `mediaTimerPaused: Boolean` field alongside `mediaBuzzPaused`. Rejected — unnecessary complexity given the mutual-exclusion guarantee.

### Set `mediaPaused` directly in `pauseTimer()` / `resumeTimer()` before calling `process()`

`process()` ends with `publishState()`, which reads the `mediaPaused` field. Setting it immediately before the call ensures the next `publishState()` snapshot reflects the correct pause state with no extra publish needed.

**Alternative considered**: Call `publishState()` again after setting the flag. Rejected — double publish is redundant.

## Risks / Trade-offs

- [Timer event rejected by engine] If `process(PauseTimer)` returns a Left error, `mediaPaused` was already mutated. Mitigation: roll back `mediaPaused` in the error branch (or accept the benign inconsistency — the button remains visible and a second click would correct state).

## Open Questions

(none)
