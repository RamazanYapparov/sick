## 1. Update PageRoute.kt buzzer layout

- [x] 1.1 Reorder `#buzz-section` HTML: move `<button id="skip">` above `<div id="player-strip">` and `<button id="buzz">`
- [x] 1.2 Change `#buzz` CSS `flex` value from `1` to `2`, keeping `#skip` at `flex: 1`, to achieve a 1/3 vs 2/3 proportional split

## 2. Verify

- [x] 2.1 Start the server and open the player page in a browser; confirm SKIP occupies the top third and BUZZ the bottom two-thirds
