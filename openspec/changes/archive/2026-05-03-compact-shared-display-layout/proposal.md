## Why

The shared display screen wastes vertical space on a large header (pack name + phase info), leaving very little room for the question board — typically only 2-3 themes are visible at once. Players and the audience lose context of the full round at a glance.

## What Changes

- Remove pack name and phase label from the shared display header; keep only "Round X/Y" info in a compact single line
- Replace the scrollable `Column` of theme cards in `BoardOverview` with a layout that distributes all themes evenly across the full available height
- Replace `FlowRow` (wrapping) question buttons inside each theme with a single `Row` so all questions appear on one line per theme

## Capabilities

### New Capabilities

- `shared-display-compact-board`: Full-screen question board on the shared display — all themes visible simultaneously with questions in a single row per theme, and a minimal header showing only round progress

### Modified Capabilities

- `shared-display-player-cards`: Header area above the board is reduced; player cards remain at the bottom but get slightly more vertical space

## Impact

- `composeApp/src/desktopMain/kotlin/app/ui/window/SharedDisplay.kt` — header section and `BoardOverview` composable
- `composeApp/src/desktopMain/kotlin/app/ui/components/QuestionBoard.kt` — layout of theme rows and question buttons
