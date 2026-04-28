## 1. Core Engine

- [x] 1.1 Add `data object SkipRound : GameEvent` to `core/src/main/kotlin/event/Events.kt`
- [x] 1.2 Allow `SkipRound` in `ChoosingQuestion` and `ChoosingPlayer` phases in `validateEventForPhase`
- [x] 1.3 Handle `SkipRound` in `applyEvent`: mark all current-round question IDs as played, clear `currentQuestion`/`answeringPlayerId`/timer state
- [x] 1.4 Handle `SkipRound` in `nextPhase`: return `GamePhase.RoundEnd`

## 2. Tests

- [x] 2.1 Add tests in `GameEngineRoundFlowTest` verifying `SkipRound` from `ChoosingQuestion` reaches `RoundEnd` with all questions marked played
- [x] 2.2 Add test verifying `SkipRound` from `ChoosingPlayer` also reaches `RoundEnd`
- [x] 2.3 Add test verifying `NextRound` after `SkipRound` advances to next round (or `GameOver` on last round)

## 3. UI — Controller

- [x] 3.1 Add `fun skipRound()` to `DesktopSessionController` that calls `process(SkipRound)`

## 4. UI — Host Window

- [x] 4.1 Add "Skip Round" button to the `ChoosingQuestion` branch in `PhaseControls.kt`, calling `controller::skipRound`
