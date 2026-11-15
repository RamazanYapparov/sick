# AGENTS.md

Kotlin desktop host for SIQ quiz packs (SIGame format). The host runs a Compose desktop app with a host window and the Shared Display. Players join and buzz from phones through an embedded HTTP server.

Domain terms (Shared Display, Buzz, Buzz Window, Reaction Time, Late Buzz, Host Pick, …) are defined in `CONTEXT.md`. Use them in code and discussion. Decisions with non-obvious reasons live in `docs/adr/`. Read them before changing the behaviour they cover.

## Build & test

```bash
./gradlew :core:test
./gradlew :server:test
./gradlew :siq:test
./gradlew :composeApp:desktopTest
./gradlew :composeApp:run
./gradlew :composeApp:packagePortableExe   # Windows only: one self-contained sick.exe (see README)
```

**From WSL:** every module needs a JDK 21 toolchain, which is installed only on Windows, and Gradle on `/mnt/c` fails with I/O errors. Run the Windows wrapper instead: `cmd.exe /c "gradlew.bat :core:test --console=plain"`. Test reports land in `<module>/build/test-results/`.

## Modules

`composeApp` → `core` ← `siq` → `siq:xml`. `server` → `core`.

- **`siq:xml`**: Java POJOs with Jackson XML annotations for the raw SIQ format.
- **`siq`**: `SiqPack.open` is its only entry point. It unzips a pack into a temporary folder (zip-slip guarded), maps the XML to domain models (`mapper/`), finds every media file in `Images`/`Audio`/`Video`, and keeps what the simple mode plays (`sanitizeForSimpleMode`). A question it cannot play (unknown type, media file missing or outside its folder, malformed URL, Point Answer without an image) is left out and logged. The returned `OpenedPack` owns the folder: local media paths stay valid until `close()`, and whatever is still on disk at exit is deleted by one shutdown hook.
- **`core`**: domain model (`model/`), events (`event/Events.kt`), phases (`state/States.kt`, a sealed `GamePhase`), and `engine/GameEngine.kt`.
- **`server`**: Ktor CIO server. It serves the phone page (`PageRoute`, inline HTML/JS) and routes `/join`, `/buzz`, `/skip` straight into `GameEngine.process`.
- **`composeApp`**: `DesktopSessionController` owns the `OpenedPack`, the engine, server, and the session's `QuestionPlayback`, and replaces the last three when a pack is loaded or the game reset; loading a pack closes the previous one. `QuestionPlayback` owns question timing: the question and answer timers, the reveal and answer delays, and the media queue (Media Hold, Media Replay, pause on Buzz). It publishes a `PlaybackView`: the content on screen, set once per queue, and the current media slot. Players report the end of their media with the slot's generation and index. `DesktopUiState` is the immutable snapshot the windows render: `HostView` and `HostSession` for the host window, `SharedDisplayView` for the Shared Display. The two views are built by `audienceViews` (`state/AudienceViews.kt`) from the engine state and the `PlaybackView`, without touching the disk. Rules about what each audience sees (which panel, timer, player role and Chooser, when the answer is marked, which controls and Host Pick candidates) live there, not in Composables. Both windows render their `Panel` through `PanelContent`; sizes come from `LocalPanelSizes` (`PanelSizes.Host` / `PanelSizes.SharedDisplay`).

## Engine rules

- `GameEngine.process(event)` is the only way to mutate game state. It validates the event against the current phase, applies it, computes the next phase, and notifies listeners. All of this happens under one lock. Errors are `Either<GameError, GameState>` (arrow).
- `GameState` is immutable and updated with `copy()`. Per-question bookkeeping (failed buzzes, skip votes, buzzes) is reset explicitly in the handlers that end or start a question.
- Listeners receive an `EngineTransition` (event, state and phase before and after). They run synchronously under the engine lock, on the caller's thread: the EDT for host actions and timers, a Ktor worker for phone requests. Listeners must not call `process` re-entrantly. `DesktopSessionController.bindEngine` always queues its work with `EventQueue.invokeLater`, even when it is already on the EDT: UI work never runs under the lock (a waiting Buzz would get a longer Reaction Time), and `QuestionPlayback` sees transitions in engine order.
- Time enters the engine only through its injected monotonic `clock`. Tests pass a fake clock, so timing behaviour stays deterministic.

## Tests

Engine behaviour is tested through `GameEngine.process` sequences (`core/src/test/.../engine/`, fixtures in `test/Fixtures.kt`). Pack loading is tested through `SiqPack.open` on `.siq` files the test zips from a `content.xml` string and media entries (`siq/src/test/.../SiqPackTest.kt`). Question timing and the media queue are tested through `QuestionPlayback` with a real engine and timers on `kotlinx-coroutines-test` virtual time (`composeApp/src/desktopTest/.../session/`). What each audience sees is tested through `audienceViews` on a real engine and a real `QuestionPlayback` whose timers never run (`composeApp/src/desktopTest/.../state/AudienceViewsTest.kt`); no Compose needed. Routes are tested with `ktor-server-test-host`.
