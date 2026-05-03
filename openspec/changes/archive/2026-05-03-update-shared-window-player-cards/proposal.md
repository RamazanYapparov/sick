## Why

The shared display window currently shows a compact scoreboard widget in the top-right corner, which is hard to read from a distance during gameplay. Players need a more prominent, state-aware status bar at the bottom of the screen that communicates who is answering, who has voted to skip, and who is choosing the next question.

## What Changes

- **NEW** `PlayerCards` composable: a horizontal row of cards at the bottom of the shared display, one per player, each showing the player's name and score with color-coded state.
- **MODIFIED** `SharedDisplayScreen`: restructured to place `PlayerCards` at the bottom; the top-level layout weight changes so existing content still fills the remaining space.
- **REMOVED** `Scoreboard` component and its import from `SharedDisplay.kt`; the `Scoreboard.kt` file is deleted entirely.
- **MODIFIED** `DesktopUiState`: `skipVotePlayerIds: Set<UUID>` field added so the shared display can reflect skip-vote state.
- **MODIFIED** `UiState.withEngineSnapshot`: populates the new `skipVotePlayerIds` field from `engine.state`.

### Card color rules
| State | Color |
|---|---|
| Player is `answeringPlayerId` | Green (`#5CCD8F` family) |
| Player ID is in `skipVotePlayerIds` | Gray (`#555555` family) |
| Player ID is `activePlayerId` (choosing) | Default surface, name shown in bold / accent |
| All other players | Default dark surface |

## Capabilities

### New Capabilities
- `shared-display-player-cards`: Bottom player-card bar on the shared display with live state coloring (answering → green, skipping → gray, choosing → bold/accent, default → neutral).

### Modified Capabilities
- (none)

## Impact

- `composeApp/src/desktopMain/kotlin/app/ui/window/SharedDisplay.kt` — layout restructure, Scoreboard removed, PlayerCards added.
- `composeApp/src/desktopMain/kotlin/app/ui/components/Scoreboard.kt` — deleted.
- `composeApp/src/desktopMain/kotlin/app/state/UiState.kt` — new `skipVotePlayerIds` field.
- No changes to `core`, `siq`, or server modules.
