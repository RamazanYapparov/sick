## Why

During a game the host may need to abandon the current round early — all interesting questions are played, time is short, or the round is unbalanced. Currently the only path to the next round is to play every remaining question to completion, which is disruptive and slow.

## What Changes

- New `SkipRound` event accepted in `ChoosingQuestion` (and `ChoosingPlayer`) phases
- Event marks all remaining round questions as played and transitions to `RoundEnd`
- New `skipRound()` method on `DesktopSessionController`
- "Skip Round" button visible in host UI during `ChoosingQuestion` phase

## Capabilities

### New Capabilities

- `skip-round`: Host can skip remaining questions in the current round and jump to the round-end screen, from which normal "Next Round" flow continues

### Modified Capabilities

<!-- No existing spec-level behavior changes -->

## Impact

- `core/event/Events.kt` — add `SkipRound` data object
- `core/engine/GameEngine.kt` — allow `SkipRound` in `ChoosingQuestion`/`ChoosingPlayer`, handle in `applyEvent` and `nextPhase`
- `composeApp/.../DesktopSessionController.kt` — add `skipRound()` method
- `composeApp/.../PhaseControls.kt` — add "Skip Round" button to `ChoosingQuestion` case
