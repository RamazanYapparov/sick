# AGENTS.md

Kotlin desktop host for SIQ quiz packs (SIGame format). The host runs a Compose desktop app with a host window and a shared display. Players join and buzz from phones through an embedded HTTP server.

Domain terms (Buzz, Buzz Window, Reaction Time, Late Buzz, Host Pick, …) are defined in `CONTEXT.md`. Use them in code and discussion. Decisions with non-obvious reasons live in `docs/adr/`. Read them before changing the behaviour they cover.

## Build & test

```bash
./gradlew :core:test
./gradlew :server:test
./gradlew :siq:test
./gradlew :composeApp:desktopTest
./gradlew :composeApp:run
```

**From WSL:** every module needs a JDK 21 toolchain, which is installed only on Windows, and Gradle on `/mnt/c` fails with I/O errors. Run the Windows wrapper instead: `cmd.exe /c "gradlew.bat :core:test --console=plain"`. Test reports land in `<module>/build/test-results/`.

## Modules

`composeApp` → `core` ← `siq` → `siq:xml`. `server` → `core`.

- **`siq:xml`**: Java POJOs with Jackson XML annotations for the raw SIQ format.
- **`siq`**: unzips a pack (`SiqExtractor`, zip-slip guarded), parses it, and maps XML to domain models (`mapper/`).
- **`core`**: domain model (`model/`), events (`event/Events.kt`), phases (`state/States.kt`, a sealed `GamePhase`), and `engine/GameEngine.kt`.
- **`server`**: Ktor CIO server. It serves the phone page (`PageRoute`, inline HTML/JS) and routes `/join`, `/buzz`, `/skip` straight into `GameEngine.process`.
- **`composeApp`**: `DesktopSessionController` owns the engine, server, and timers. `TimerOrchestrator` drives question, answer, reveal and media timing from phase changes. `DesktopUiState` is the immutable snapshot that every window renders.

## Engine rules

- `GameEngine.process(event)` is the only way to mutate game state. It validates the event against the current phase, applies it, computes the next phase, and notifies listeners. All of this happens under one lock. Errors are `Either<GameError, GameState>` (arrow).
- `GameState` is immutable and updated with `copy()`. Per-question bookkeeping (failed buzzes, skip votes, buzzes) is reset explicitly in the handlers that end or start a question.
- Listeners run synchronously under the engine lock, on the caller's thread: the EDT for host actions and timers, a Ktor worker for phone requests. Listeners must not call `process` re-entrantly. Hop to the EDT before touching controller fields or Compose state (`DesktopSessionController.bindEngine`).
- Time enters the engine only through its injected monotonic `clock`. Tests pass a fake clock, so timing behaviour stays deterministic.

## Tests

Engine behaviour is tested through `GameEngine.process` sequences (`core/src/test/.../engine/`, fixtures in `test/Fixtures.kt`). Routes are tested with `ktor-server-test-host`.
