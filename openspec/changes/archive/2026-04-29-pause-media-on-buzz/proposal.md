## Why

When a player buzzes in during media playback (audio or video), the media continues playing in the background, creating noise that disrupts the answering moment and makes it hard to hear the player. Media should pause when a player is answering and resume or stop based on the outcome.

## What Changes

- Media (audio/video) pauses immediately when a player buzzes in (`ShowingQuestion` → `PlayerAnswering` transition)
- Media resumes from the paused position when the host rejects the answer and the question returns to active play (`PlayerAnswering` → `ShowingQuestion` transition)
- Media stops completely when the host accepts the answer (`PlayerAnswering` → `ShowingAnswer` transition)

## Capabilities

### New Capabilities

- `media-pause-on-answer`: Pause media on player buzz, resume on wrong answer, stop on correct answer

### Modified Capabilities

<!-- No existing spec-level requirements are changing -->

## Impact

- `composeApp/src/desktopMain/kotlin/app/ui/AudioPlayer.kt` — needs pause/resume control in addition to existing stop signal
- `composeApp/src/desktopMain/kotlin/app/ui/VideoPlayer.kt` — same as AudioPlayer
- `composeApp/src/desktopMain/kotlin/app/ui/SharedDisplay.kt` — passes new pause signal to media players
- `composeApp/src/desktopMain/kotlin/app/session/DesktopSessionController.kt` — drives pause/resume/stop based on phase transitions
- `composeApp/src/desktopMain/kotlin/app/session/TimerOrchestrator.kt` — may need coordination to not conflict with existing media-delay-timer logic
