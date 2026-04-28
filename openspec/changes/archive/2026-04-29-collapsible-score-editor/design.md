## Context

`PlayerEditorRow` in `PlayerComponents.kt` always renders the score delta `OutlinedTextField` and "Apply" button, meaning all score editors are expanded at all times in the host screen. This creates visual clutter during normal gameplay.

## Goals / Non-Goals

**Goals:**
- Each player row collapses to name + score + "Edit" button by default
- Clicking "Edit" expands that row's score-change inputs
- Clicking "Apply" submits the delta and collapses the row
- State is local to the composable — no external state machine involvement

**Non-Goals:**
- Persisting expanded/collapsed state across sessions
- Animating the expand/collapse transition
- Changing score calculation or validation logic

## Decisions

**Local `remember { mutableStateOf(false) }` for expanded flag**
Each `PlayerEditorRow` owns its own `expanded` boolean. Alternatives considered:
- Parent-managed set of expanded IDs: adds complexity with no benefit — only one row needs to be edited at a time anyway, but enforcing single-expand is not required here.
- Shared ViewModel state: overkill for pure UI toggle with no persistence need.

**Auto-collapse on Apply, not on value change**
Collapse happens when the user explicitly commits via "Apply". Collapsing on any field change would be disruptive.

## Risks / Trade-offs

- If the host clicks "Edit" on multiple rows simultaneously, all can be expanded at once — acceptable; no requirement to enforce single-expand.
- Resetting `scoreDelta` on collapse: the parent holds `scoreDelta` per player. We rely on the existing `onScoreChange` callback being called with empty string after apply; if not, stale text may reappear on next expand. → Mitigation: clear delta in `onAdjustScore` handler or reset inside the composable on collapse.
