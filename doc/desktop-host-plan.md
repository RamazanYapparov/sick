# Desktop Host Plan

## Goal

Build the desktop host side as a two-window Compose desktop application:

- `Shared display window`: projector / TV view for all players.
- `Host control window`: operator-only controls, plus a compact live preview of the shared display.

The desktop app remains the authority for game state. Players still interact through the phone buzzer page served by the embedded server.

## Current State Analysis

### What already exists

- `core` already has a phase-driven game engine:
  - `Lobby`
  - `ChoosingPlayer`
  - `ChoosingQuestion`
  - `ShowingQuestion`
  - `PlayerAnswering`
  - `RoundEnd`
  - `GameOver`
- The engine already supports:
  - player management
  - active-player selection
  - question selection
  - buzz-in
  - host accept / reject
  - automatic score add/subtract using question price
  - round transitions
- `server` already exposes a minimal player buzzer page and forwards `/buzz` into `GameEngine`.
- `composeApp` is still a placeholder and does not yet consume the engine state.

### Gaps between current logic and requested desktop UX

- There is no explicit `answer reveal` state.
  - Right now `HostAccepted` and `TimerExpired` immediately clear `currentQuestion`.
  - Your requirement needs the answer to remain visible on the shared display after:
    - correct answer
    - timeout
    - all players failed
- There is no explicit event for `all players failed`.
  - The engine tracks `failedBuzzPlayerIds`, but it never transitions into a final reveal / resolution state when everyone is locked out.
- There is no host action for:
  - loading a pack
  - creating/resetting a game session from the desktop UI
  - manual score correction
  - skipping a question
  - choosing an answering player manually
- `composeApp` does not currently depend on `siq`, so pack loading is not wired into the desktop app yet.
- The current engine constructor takes an initial `Package`, which is awkward for a desktop workflow where the host can load a pack later.

## Confirmed Product Decisions

These are now fixed for v1:

- Question content appears on the shared display immediately after question selection.
- v1 supports regular questions only.
  - `Stake`, `NoRisk`, `Secret`, and `Final` round-specific behavior are deferred.
- After timeout, skip, or all-failed resolution, the previous chooser keeps control and chooses the next question.

## Product Assumptions For This Plan

These assumptions let implementation start before final rule decisions are confirmed:

- The host is authoritative for all judging and score changes.
- The host window should embed a compact, read-only reuse of the shared display composable rather than maintain a second rendering path.
- The embedded HTTP server stays in-process inside the desktop app.

## Open Questions

No blocking product questions remain for the regular-question desktop flow.

Deferred rules:

- special SIQ question types
- final round behavior
- richer player device state beyond buzzing

## Target Desktop Architecture

## UI Structure

- `HostWindow`
  - top-level app shell
  - game setup controls
  - operational controls for the current phase
  - compact shared-display preview
  - diagnostics / errors
- `DisplayWindow`
  - clean presentation layout for projector / second monitor
  - scoreboard
  - active player
  - round / board state
  - question content
  - answer reveal when rules allow

## Shared UI Model

Introduce a desktop-facing view model layer that converts engine state into renderable screen state:

- `DesktopSessionState`
  - current pack metadata
  - engine phase
  - selected / current question
  - active player
  - answering player
  - timer state
  - scoreboard rows
  - board visibility state
  - answer visibility state
  - host-only action availability
- `DisplayViewState`
  - only information safe for the public display
- `HostViewState`
  - `DisplayViewState`
  - host-only controls, errors, and hidden metadata

This prevents the host window from accidentally leaking hidden answer state into the public window.

## State Ownership

Introduce a desktop session controller in `composeApp`:

- wraps `GameEngine`
- subscribes to engine listener callbacks
- owns timer lifecycle
- translates UI intents into engine events
- holds transient UI state:
  - selected file
  - pending score adjustment
  - pending player selection
  - dialogs
- exposes `StateFlow` or Compose `mutableStateOf` state for both windows

## Core Changes Needed

The existing engine is close, but it needs more explicit phases/events for host-driven desktop play.

### Recommended new phases

- `RevealingAnswer`
  - entered after correct answer, timeout, skip, or all-failed
  - keeps question and answer visible until host proceeds
- optionally `PackNotLoaded`
  - if we want the desktop app to start before a pack exists

### Recommended new events

- `LoadPack(pack)`
- `ResetGame`
- `RevealAnswer`
  - only if reveal is a separate action
- `ProceedAfterReveal`
- `ChooseAnsweringPlayer(playerId)`
  - if host must be able to override buzz order manually
- `SkipQuestion`
- `AdjustPlayerScore(playerId, delta)`
  - or a richer `SetPlayerScore` / `ApplyScoreAdjustment`

### Recommended state additions

- `revealedAnswer: Answer?` or answer visibility derived from phase
- `lastResolution: QuestionResolution?`
  - `Correct(playerId)`
  - `Timeout`
  - `AllFailed`
  - `Skipped`
- `questionChooserId`
  - required because chooser persists through timeout / skip / all-failed rules

### Recommended rule changes

- `HostAccepted` should not immediately clear the question.
  - It should resolve scoring, mark resolution, and transition to `RevealingAnswer`.
- `TimerExpired` should not immediately clear the question.
  - It should transition to `RevealingAnswer`.
- `HostRejected` should detect whether any eligible players remain.
  - If yes: return to `ShowingQuestion`
  - If no: transition to `RevealingAnswer` with `AllFailed`
- `SkipQuestion` should transition to `RevealingAnswer` with `Skipped` and preserve the previous chooser.
- Question cleanup should happen only after `ProceedAfterReveal`.
- `ProceedAfterReveal` should return to `ChoosingQuestion`, not `ChoosingPlayer`, when resolution was timeout / skip / all-failed.

## Compose Implementation Plan

## Phase 1: App Shell And Session Wiring

- Replace placeholder `App()` with a desktop session root.
- Create two Compose windows from the same application process:
  - `DisplayWindow`
  - `HostWindow`
- Start and stop `GameServer` together with desktop app lifecycle.
- Introduce a desktop session controller that owns the engine and exposes observable UI state.

## Phase 2: Pack Loading And Lobby

- Add `siq` dependency to `composeApp`.
- Implement desktop file picker flow for loading `.siq` packages.
- Parse package into `core.model.Package`.
- Support session creation/reset from the host window.
- Build lobby UI:
  - pack info
  - player list
  - start game button
  - server URL display for players

## Phase 3: Shared Display Surface

- Create reusable `SharedDisplayScreen(viewState)` composable.
- Public display contents:
  - round name
  - board with played/unplayed questions
  - current question content
  - scoreboard
  - active player
  - answering player indicator
  - timer
  - answer reveal when permitted
- Reuse the same composable in miniature inside the host window.

## Phase 4: Host Controls By Phase

- `Lobby`
  - load pack
  - create/reset game
  - start game
- `ChoosingPlayer`
  - choose active player
- `ChoosingQuestion`
  - choose question from board
- `ShowingQuestion`
  - start/stop timer if needed
  - skip question
  - manually select answering player if host override is supported
- `PlayerAnswering`
  - mark correct
  - mark wrong
  - apply full/half/double points if special rules need it
- `RevealingAnswer`
  - show why answer is being revealed
  - proceed to next chooser/board state
- any phase
  - manual score adjustments

Controls should be driven by phase-derived action availability, not ad hoc button enabling.

## Phase 5: Engine Extensions

- Extend `core` events, phases, and tests for the reveal flow.
- Add tests for:
  - correct answer -> reveal -> continue
  - timeout -> reveal -> continue
  - all players fail -> reveal -> continue
  - skip question
  - manual score adjustment
  - chooser-selection rules after unresolved questions

## Phase 6: Answer Safety And Display Rules

- Centralize answer visibility rules in one mapper.
- Ensure the host preview can optionally show more metadata without affecting public display content.
- Avoid reading hidden answer content in public composables except in `RevealingAnswer`.

## Phase 7: UX Polish

- Make the display window presentation-first:
  - large typography
  - high-contrast scoreboard
  - readable timer
  - stable layout for question text and media
- Make the host window information-dense:
  - compact preview
  - clear current phase
  - one main action group per phase
  - keyboard shortcuts for common controls

## Suggested File/Module Changes

### `composeApp`

- add desktop session/controller package
- add window-specific composables
- add reusable shared display composables
- add file loading integration
- add host action panels

Possible structure:

- `composeApp/src/desktopMain/kotlin/app/Main.kt`
- `composeApp/src/desktopMain/kotlin/session/DesktopSessionController.kt`
- `composeApp/src/desktopMain/kotlin/session/DesktopSessionState.kt`
- `composeApp/src/desktopMain/kotlin/ui/display/SharedDisplayScreen.kt`
- `composeApp/src/desktopMain/kotlin/ui/host/HostWindowScreen.kt`
- `composeApp/src/desktopMain/kotlin/ui/components/Scoreboard.kt`
- `composeApp/src/desktopMain/kotlin/ui/components/QuestionContent.kt`

### `core`

- extend `Events.kt`
- extend `States.kt`
- extend `GameState`
- update `GameEngine`
- add tests for new flow

### `server`

- likely minimal changes for v1
- later improvement:
  - expose state to phones
  - disable buzz button when buzzing is not allowed
  - push updates via WebSocket

## Delivery Order

1. Extend the engine to support reveal/resolution states cleanly.
2. Add session controller and two-window desktop shell.
3. Implement pack loading and lobby flow.
4. Implement shared display screen and host preview reuse.
5. Implement phase-specific host controls.
6. Add score adjustment and skip flows.
7. Polish visuals and keyboard shortcuts.

## Risks

- If special SIQ question types are included in v1, engine complexity rises quickly.
- If answer reveal is not modeled explicitly in `core`, the UI will accumulate fragile host-only flags.
- Pack loading and session reset are awkward until `GameEngine` can be recreated or reinitialized cleanly.
- The current server page is static HTML and does not yet reflect live game state.

## Recommendation

Implement v1 around standard question flow first, but make the core state machine answer-reveal aware from the start. That is the main architectural change needed to support both windows correctly without leaking hidden information or forcing UI-only game rules.
