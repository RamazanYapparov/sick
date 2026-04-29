## Why

The host Pause button currently freezes the timer but leaves audio/video playing, creating a disconnect where media continues while the game is paused. Pausing media together with the timer gives the host full control during interruptions.

## What Changes

- Clicking the Pause button during `ShowingQuestion` pauses both the timer and active media playback
- Clicking Resume restores both the timer and media playback from the paused position
- This is additive — existing buzz-triggered media pause behavior is unchanged

## Capabilities

### New Capabilities

- `media-pause-on-timer-pause`: Media pauses/resumes in sync with the host timer pause/resume action during `ShowingQuestion`

### Modified Capabilities

(none)

## Impact

- `DesktopSessionController`: `pauseTimer()` and `resumeTimer()` must set `mediaPaused` in addition to processing the timer event
- `DesktopUiState.mediaPaused`: already exists; shared with buzz-triggered pause logic — no schema change needed
- `VideoPlayer` / `AudioPlayer`: already respect the `paused` flag — no media player changes needed
