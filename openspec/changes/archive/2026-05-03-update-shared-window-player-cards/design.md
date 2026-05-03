## Context

`SharedDisplayScreen` is the audience-facing window rendered in a separate desktop window. It currently renders a `Scoreboard` widget in the top-right corner alongside pack/round info. The state machine already tracks `answeringPlayerId`, `activePlayerId`, and (in the core engine) `skipVotePlayerIds`, but the last field is not surfaced in `DesktopUiState`.

## Goals / Non-Goals

**Goals:**
- Add a bottom player-card bar to `SharedDisplayScreen` with per-player color state.
- Expose `skipVotePlayerIds` in `DesktopUiState` so the shared display can render skip state.
- Remove the existing `Scoreboard` widget from `SharedDisplayScreen`.

**Non-Goals:**
- Changing the `HostWindow` layout or its player management controls.
- Adding animations or transitions between player states.
- Sorting or ranking players in the card bar (display order tracks `state.players` list order).

## Decisions

### 1. Surface `skipVotePlayerIds` in `DesktopUiState`

Add `skipVotePlayerIds: Set<UUID>` to `DesktopUiState` (default `emptySet()`). Populate it inside `withEngineSnapshot` from `state.skipVotePlayerIds`.

**Why not pass the full `GameState` to the composable?** `DesktopUiState` is the single derived view-model crossing the session/UI boundary; adding one field keeps that contract clean and avoids exposing engine internals to the composable layer.

### 2. `PlayerCards` as a new composable in `app.ui.components`

A `PlayerCards(players, activePlayerId, answeringPlayerId, skipVotePlayerIds, compact)` composable renders a `LazyRow` (or `Row` with `horizontalScroll`) of fixed-width cards. Card background color is determined by priority: answering > skipping > default. Choosing player name is rendered in bold accent color.

**Why `LazyRow` over a fixed `Row`?** The number of players is small (typically ≤ 8) but `LazyRow` handles overflow gracefully without wrapping.

### 3. Layout restructure in `SharedDisplayScreen`

Replace the current top `Row(pack info | Scoreboard)` with just the pack/round info text column (no Scoreboard). Add a `Spacer(Modifier.weight(1f))` after the main content block so the player cards are pinned to the bottom of the outer `Column`.

```
Column(fillMaxSize) {
    Row { pack name, round, phase }   ← existing header (Scoreboard removed)
    <main content block>              ← existing when/else (with Modifier.weight(1f))
    PlayerCards(...)                  ← NEW, pinned to bottom
}
```

**Why weight on main content rather than a nested layout?** The outer `Column` already uses `fillMaxSize`; giving the content block `weight(1f)` is the idiomatic Compose approach for "fill remaining space, push the bar to the bottom."

### 4. Delete `Scoreboard.kt`

The component is used only in `SharedDisplay.kt`. After replacing it with `PlayerCards`, the file is dead code and is deleted. The `HostWindow` does not reference it.

## Risks / Trade-offs

- **Many players overflow the card bar** → `LazyRow` scrolls horizontally; cards on a TV-style display may be partially hidden. Mitigation: keep card min-width proportional to available width if player count > threshold (can be a follow-up).
- **`skipVotePlayerIds` is only meaningful during `ShowingQuestion`** → The gray state will appear briefly and clear when the question phase ends; this is correct behavior, no special handling needed.

## Migration Plan

No data migration required. The `skipVotePlayerIds` field on `DesktopUiState` gets a default of `emptySet()` so existing call sites (tests, previews) are unaffected without changes.
