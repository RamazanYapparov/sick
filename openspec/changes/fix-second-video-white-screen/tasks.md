## 1. Reproduce and instrument

- [ ] 1.1 Identify or assemble a small SIQ pack with at least two consecutive video questions (or temporarily duplicate a video question) for manual verification.
- [ ] 1.2 Confirm the bug locally with `./gradlew :composeApp:run`: first video plays, second shows white panel.

## 2. Fix `VideoPlayer` lifecycle

- [x] 2.1 In `composeApp/src/desktopMain/kotlin/app/ui/media/VideoPlayer.kt`, change the `DisposableEffect(uri)` so the player setup waits for any prior `MediaPlayer` to finish disposal on the JavaFX thread before constructing a new `Media`/`MediaPlayer`/`Scene`.
- [x] 2.2 Inside `onDispose`, dispose the `MediaPlayer` synchronously on the JavaFX thread (e.g., via a latch or `runAndWait`-equivalent) so the next composition's setup observes a clean slate.
- [x] 2.3 Guard the deferred setup: if `playerRef[0]` was cleared by disposal before the scene attaches, skip attaching the scene rather than leaving a dangling player.

## 3. Make load failures visible

- [x] 3.1 Replace the silent `runCatching { Media(uri) }.getOrElse { return@runLater }` with an explicit catch that records an error message in Compose state and logs it.
- [x] 3.2 Render the error message inside the video `Box` (red text, similar to existing `Image not found` / `Image unavailable` cases in `SharedDisplay.kt`) when the error state is set.

## 4. Manual verification

- [ ] 4.1 Run `./gradlew :composeApp:run`, load the multi-video pack, and verify the second and a third video question both display and play their video on the shared display.
- [ ] 4.2 Verify pause-on-buzz, resume-on-wrong-answer, and stop-on-correct-answer still behave as defined in the existing `media-pause-on-answer` spec for the second video.
- [ ] 4.3 Verify that pointing a question at a non-existent video file shows a red error message (not a white panel).

## 5. Wrap up

- [x] 5.1 `./gradlew build` to confirm no compile/test regressions.
- [ ] 5.2 Archive the change with `openspec archive fix-second-video-white-screen` once merged.
