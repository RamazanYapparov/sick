## 1. Core Service

- [x] 1.1 Add `fun GameState.lowestScoreCandidates(): List<Player>` in `core/service/PlayerOps.kt` — returns players whose score equals `players.minOfOrNull { it.score }`, empty list when no players
- [x] 1.2 Add unit tests for `lowestScoreCandidates` in `core/src/test/` — distinct scores, tied scores, all-zero first-round case

## 2. UI State

- [x] 2.1 Add `lowestScoreCandidates: List<Player>` field to `DesktopUiState` (default `emptyList()`) in `composeApp/.../state/UiState.kt`
- [x] 2.2 Populate `lowestScoreCandidates` in `withEngineSnapshot()` by calling `state.lowestScoreCandidates()`

## 3. Auto-Selection

- [x] 3.1 In `DesktopSessionController`, after each engine transition to `ChoosingPlayer`, check if `uiState.lowestScoreCandidates.size == 1` and if so call `selectActivePlayer(candidate.id)` immediately

## 4. Host UI

- [x] 4.1 In `PhaseControls.kt` `ChoosingPlayer` branch, change `PlayerChipRow` `players` argument from `state.players` to `state.lowestScoreCandidates`
- [x] 4.2 Update label text to reflect filtered selection (e.g. "Choose who controls the board — lowest score players shown")

## 5. Verification

- [x] 5.1 Run `./gradlew :core:test` — all tests pass
- [x] 5.2 Run `./gradlew :composeApp:run` — manually verify: unique lowest-score player auto-advances; tied players show only tied chip row; first round (all zeros) shows all players
