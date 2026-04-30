## Context

The `QuestionBoard` composable renders all themes for the current round. As questions are answered, themes fill up with played (greyed-out) buttons. By the time half a round is done, the board is cluttered with dead cards that serve no purpose until the host wants to review them.

All data needed to determine "fully played" (`BoardThemeState.questions[].played`) is already in the UI state. No engine or state machine changes are needed.

## Goals / Non-Goals

**Goals:**
- Hide fully-played themes by default in `QuestionBoard`.
- Show a toggle button ("Show completed" / "Hide completed") when ≥1 theme is fully played.
- Toggle is local UI state — no persistence, resets when board re-renders from scratch.

**Non-Goals:**
- Not changing engine state or `DesktopUiState`.
- Not animating hide/reveal transitions.
- Not persisting toggle state across rounds.

## Decisions

### Local `remember` state for toggle

Toggle lives in `QuestionBoard` as `var showCompleted by remember { mutableStateOf(false) }`.

**Why not in `DesktopUiState`:** This is purely a display preference with no effect on game logic. Keeping it local avoids polluting the state model.

### "Fully played" computed inline

`val isFullyPlayed = theme.questions.all { it.played }` computed per theme inside the composable.

**Why not in `BoardThemeState`:** Would require adding a derived field to the data class. The computation is trivial and belongs at the display layer.

### Button placement

Toggle button rendered above the theme list, only when at least one theme is fully played. Keeps the board clean when all themes are still active.

## Risks / Trade-offs

- [Toggle resets on recomposition caused by state change] → Acceptable: a full recomposition means the game state changed (question answered), at which point resetting "show completed" back to hidden is correct behavior. `remember` without a key is intentional.
