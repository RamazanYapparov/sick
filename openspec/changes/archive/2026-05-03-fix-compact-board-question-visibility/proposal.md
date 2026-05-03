## Why

The compact shared display board — introduced in the previous layout update — has a visual bug: when `fillHeight=true` and many themes are shown, the fixed internal padding (12 dp top+bottom) and the spacer (8 dp) between the theme name and question buttons consume more vertical space than each theme row receives, causing the question price buttons to overflow and disappear. Users see theme names only.

## What Changes

- Remove the fixed `Spacer` between theme name and question row inside theme cards when `fillHeight=true`; replace with a minimal gap via `verticalArrangement` or no gap at all
- Reduce inner card padding from 12 dp to 4 dp when `fillHeight=true` so rows stay within their allocated height
- Apply `Modifier.fillMaxHeight()` to the inner `Column` inside each Card so the question row anchors to the bottom and doesn't clip

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `shared-display-compact-board`: Internal theme card layout adapts padding and spacing to match the available weighted height, ensuring question buttons are always visible regardless of theme count

## Impact

- `composeApp/src/desktopMain/kotlin/app/ui/components/QuestionBoard.kt` — inner Card `Column` padding and `Spacer` handling
