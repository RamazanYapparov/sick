## Why

The BUZZ button is the primary action and should dominate the screen to reduce tap errors during fast-paced play. Giving it 2/3 of the viewport makes it easier to hit under pressure, while SKIP moves to the top 1/3 as a secondary action.

## What Changes

- The SKIP button moves from the bottom half to the top 1/3 of the viewport
- The BUZZ button moves from the top half to the bottom 2/3 of the viewport
- The `#buzz-section` flex order is reversed (SKIP first in DOM, BUZZ second)
- Button flex weights are updated to reflect the new 1/3 vs 2/3 proportions

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `player-buzzer-ui`: BUZZ button now occupies bottom 2/3 of viewport; SKIP button now occupies top 1/3

## Impact

- `server/src/main/kotlin/com/sick/server/routes/PageRoute.kt` — inline CSS and HTML structure for `#buzz-section`, `#buzz`, and `#skip`
- `openspec/specs/player-buzzer-ui/spec.md` — requirements for button proportions and positions need updating
