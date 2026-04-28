## Why

Score-editing controls are always visible in the host screen for every player, cluttering the UI during normal gameplay. Controls should be hidden by default and only expand when the host explicitly needs to adjust a score.

## What Changes

- `PlayerEditorRow` defaults to collapsed state: shows player name, current score, and an "Edit" button
- Clicking "Edit" expands the score delta input + "Apply" button
- After "Apply" is clicked the row collapses back to default
- No change to scoring logic — only UI visibility is affected

## Capabilities

### New Capabilities
- `collapsible-score-editor`: Score editing controls in host screen toggle open/closed per-player; collapsed by default, expanded on demand, auto-collapse after apply

### Modified Capabilities

## Impact

- `composeApp/src/desktopMain/kotlin/app/ui/PlayerComponents.kt` — `PlayerEditorRow` composable
- Local expanded/collapsed state added per player row (no state machine changes)
