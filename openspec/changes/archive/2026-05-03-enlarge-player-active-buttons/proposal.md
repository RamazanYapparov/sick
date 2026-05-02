## Why

The BUZZ and SKIP buttons on the player buzzer page are constrained inside a narrow card container, making them hard to tap quickly under pressure. Enlarging them to fill the full screen as two half-height tiles makes them much easier to hit on a phone.

## What Changes

- The `#buzz-section` in the buzzer web page switches from a centered card layout to a full-screen two-panel layout
- BUZZ fills the top half of the viewport, SKIP fills the bottom half
- The join form keeps its current centered card layout (only the active/post-join view changes)

## Capabilities

### New Capabilities

*(none)*

### Modified Capabilities

- `player-buzzer-ui`: Layout of the post-join buzzer screen changes from a card with small buttons to a full-screen split with two large tap targets

## Impact

- `server/src/main/kotlin/com/sick/server/routes/PageRoute.kt` — CSS and HTML for `#buzz-section` and the two buttons
