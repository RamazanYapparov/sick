## Why

When a round contains multiple video questions, only the first video plays correctly. Every subsequent video question shows a blank white panel where the video should be, breaking the host's ability to play video-based rounds. The cause is the JavaFX `MediaPlayer` lifecycle in `VideoPlayer.kt`: the previous player's disposal interleaves with the next player's startup on the JavaFX application thread, leaving the new `JFXPanel` without a rendered scene.

## What Changes

- Stabilize `VideoPlayer` so each question's video starts cleanly, regardless of how many video questions preceded it within the same session.
- Ensure the previous `MediaPlayer` is fully disposed before a new `Media`/`MediaPlayer`/`Scene` is wired into the same `JFXPanel`, or use a fresh `JFXPanel` per video so the JavaFX scene attachment cannot race with disposal.
- Surface visible failure (instead of a silent white panel) when `Media(uri)` cannot be constructed, so future regressions are detectable.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `media-pause-on-answer`: extend with requirements covering reliable playback when multiple video questions are played sequentially within a single session.

## Impact

- `composeApp/src/desktopMain/kotlin/app/ui/media/VideoPlayer.kt` — JavaFX init / disposal sequencing.
- The shared display window (`SharedDisplayScreen` → `CurrentQuestionPanel`) is the consumer; no API change expected, but behavior under repeated question playback changes.
- No changes to `core/`, `siq/`, or domain models; no schema or persistence impact.
