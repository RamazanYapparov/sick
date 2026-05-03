## 1. State Layer

- [x] 1.1 Add `skipVotePlayerIds: Set<UUID> = emptySet()` field to `DesktopUiState` in `UiState.kt`
- [x] 1.2 Populate `skipVotePlayerIds` in `DesktopUiState.withEngineSnapshot` from `state.skipVotePlayerIds`

## 2. PlayerCards Component

- [x] 2.1 Create `composeApp/src/desktopMain/kotlin/app/ui/components/PlayerCards.kt` with a `PlayerCards` composable accepting `players`, `activePlayerId`, `answeringPlayerId`, `skipVotePlayerIds`, and `compact`
- [x] 2.2 Render a horizontal scrollable row of cards, one per player, each showing name and score
- [x] 2.3 Apply green background (`#1E4D2B` / `#5CCD8F` text) when the player is `answeringPlayerId`
- [x] 2.4 Apply gray background (`#555555`) when the player ID is in `skipVotePlayerIds` (and not answering)
- [x] 2.5 Render the choosing player's name bold in `Palette.AccentGold` when the player is `activePlayerId`

## 3. Update SharedDisplayScreen

- [x] 3.1 In `SharedDisplay.kt`, add `Modifier.weight(1f)` to the `when` content block so it fills remaining space
- [x] 3.2 Add `PlayerCards(state.players, state.activePlayerId, state.answeringPlayerId, state.skipVotePlayerIds, compact)` as the last item in the outer `Column`
- [x] 3.3 Remove the `Scoreboard(...)` call from the top `Row` in `SharedDisplayScreen`
- [x] 3.4 Remove the `Scoreboard` import from `SharedDisplay.kt`

## 4. Cleanup

- [x] 4.1 Delete `composeApp/src/desktopMain/kotlin/app/ui/components/Scoreboard.kt`
- [x] 4.2 Build the project (`./gradlew :composeApp:build`) and confirm no compilation errors
