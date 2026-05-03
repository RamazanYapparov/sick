## Context

`VideoPlayer.kt` (composeApp/src/desktopMain/kotlin/app/ui/media/VideoPlayer.kt) wraps a JavaFX `MediaPlayer` inside a `JFXPanel` and embeds it into Compose via `SwingPanel`. It is rendered from `SharedDisplay.kt:CurrentQuestionPanel` whenever the active question contains a `LocalVideo` / `RemoteVideo` content item.

Today's flow on a video question:

1. `remember { JFXPanel() }` allocates one `JFXPanel` per Composable instance.
2. `DisposableEffect(uri) { … }` schedules `JfxPlatform.runLater { Media(uri); MediaPlayer(media); Scene(StackPane(MediaView(player))); jfxPanel.scene = … ; player.play() }`.
3. On disposal, another `JfxPlatform.runLater { player.dispose() }` is scheduled.

When the host moves from video question A to video question B, Compose unmounts panel A and mounts panel B. Because both the dispose of A and the setup of B are submitted to the single JavaFX application thread via `runLater`, ordering is fragile. Empirically, the second `JFXPanel` ends up with no rendered scene — visible to the user as a white box. Errors in `Media(uri)` are swallowed by `runCatching { … }.getOrElse { return@runLater }`, so the white panel is silent.

The bug is reproducible: any round whose second-or-later question is a video shows the symptom.

## Goals / Non-Goals

**Goals:**

- A video question always plays its video when shown, regardless of how many video questions came before it in the session.
- Failures in `Media(uri)` are surfaced (logged + on-screen) instead of presenting a silent white panel.
- No behavioral regression for audio playback, image playback, pause/resume on buzz-in, or stop on correct answer.

**Non-Goals:**

- Replacing JavaFX as the playback backend (e.g., switching to VLCJ or ExoPlayer-style libs).
- Adding new video features (seeking, volume controls, picture-in-picture, etc.).
- Changing the host (compact) window's "▶ Video" placeholder behavior.
- Pre-buffering or caching across questions.

## Decisions

### Decision 1: Drive playback off the panel's own initialization signal, not just the URI

**Choice:** Move the `Media`/`MediaPlayer`/`Scene` setup so it runs *after* the `JFXPanel` is attached to the Swing hierarchy and after any prior player has been disposed. Key the `DisposableEffect` on `uri` and ensure disposal `await`s on the JavaFX thread before returning, so the next composition's setup cannot start until the previous player is gone.

**Alternatives considered:**

- *Reuse a single shared `JFXPanel` across all video questions.* Rejected: requires hoisting playback state above `CurrentQuestionPanel`, complicating the call sites and tying JavaFX lifetime to the session rather than the question.
- *Switch to VLCJ or another player.* Rejected as out of scope; would also require packaging native libraries per platform.

**Rationale:** The defect is a JavaFX-thread ordering race. Serializing dispose-before-setup on that thread eliminates the race without architectural change. Compose already gives us a clean "this panel is being torn down" hook via `onDispose`; we just need to make it actually finish before the next setup runs.

### Decision 2: Surface `Media(uri)` failure visibly

**Choice:** Replace `runCatching { Media(uri) }.getOrElse { return@runLater }` with an explicit catch that records an error state. Render the error message inside the video Box (red text) so the host sees what went wrong, and `println` it to logs as today.

**Alternatives considered:**

- *Throw and let Compose's error boundary handle it.* Rejected: there is no error boundary set up; a thrown exception inside `runLater` would only log to stderr.

**Rationale:** Today's silent-failure behavior is what made this bug invisible to automated checks and is the single most likely thing to mask future regressions.

### Decision 3: Keep one `JFXPanel` per `VideoPlayer` Composable

**Choice:** Continue allocating `JFXPanel` per Composable via `remember`. Do not pool or share.

**Rationale:** A pool would couple panel lifetime to the application rather than to the question, requiring new state plumbing. The race is on the player/scene attached to the panel, not on the panel itself; fixing the dispose ordering is sufficient.

## Risks / Trade-offs

- **Risk:** Forcing dispose-before-setup synchronization on the JavaFX thread could briefly delay the second video's start. → **Mitigation:** The wait is bounded to the prior player's `dispose()` call, which is non-blocking work; perceived delay should be tens of ms at most. Acceptable for the host-facing video flow.
- **Risk:** A platform-specific JavaFX bug (e.g., on macOS Sonoma) could still leave the second panel blank even with correct ordering. → **Mitigation:** Decision 2 ensures any such failure now produces a logged, visible error rather than a silent white screen, making it diagnosable.
- **Risk:** If the user navigates away mid-load (e.g., reveals answer before media starts), the deferred setup might still attach a scene to a panel about to be disposed. → **Mitigation:** Check `playerRef[0] == null` before completing setup; if disposal already cleared it, skip attaching the scene.

## Migration Plan

- No data migration. No public API changes.
- Roll out by replacing the body of `VideoPlayer.kt`. Manually verify on the desktop app by playing a pack with two consecutive video questions.
- Rollback is `git revert` of the single composable file.

## Open Questions

- None blocking. If a JavaFX-platform issue surfaces during manual verification, capture the on-screen error message and revisit Decision 1.
