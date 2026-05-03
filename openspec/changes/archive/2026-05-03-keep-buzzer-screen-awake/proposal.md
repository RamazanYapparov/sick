## Why

Players hold their phones while waiting to buzz in, but phone screens go dark after a short idle timeout, requiring an unlock before buzzing. The Screen Wake Lock API lets the buzzer page keep the display on so players are always ready to react.

## What Changes

- After a player joins and the buzz section is shown, the page requests a screen wake lock to prevent the phone from sleeping
- The wake lock is released when the player navigates away or closes the page
- A graceful fallback is included for browsers that do not support the Wake Lock API (older Android WebView, Safari < 16.4)

## Capabilities

### New Capabilities
- `buzzer-wake-lock`: Keeps the phone screen awake while the buzzer page is active, using the Screen Wake Lock API, with automatic reacquisition after page visibility changes and graceful degradation when unsupported

### Modified Capabilities
- `player-buzzer-ui`: The buzzer screen now requests a wake lock on entry (no requirement-level behavior change, but the spec gains a wake lock lifecycle requirement)

## Impact

- `server/src/main/kotlin/com/sick/server/routes/PageRoute.kt` — JavaScript in `renderBuzzerPage()` gains wake lock logic
- No new dependencies; the Web Wake Lock API is browser-native
- No server-side or Kotlin changes required
