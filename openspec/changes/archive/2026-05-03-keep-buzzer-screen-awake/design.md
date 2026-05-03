## Context

The player buzzer page is a single HTML file generated in `PageRoute.kt` and served by a Ktor embedded server. Players open it in a mobile browser (Chrome/Safari on Android/iOS) to buzz in during a quiz round. The screen-sleep timeout on phones (typically 30–60 s) causes the display to go dark while players wait, forcing an unlock before they can react.

No native mobile app exists; the solution must be entirely within the served HTML/JS.

## Goals / Non-Goals

**Goals:**
- Keep the phone screen on while the buzz section is visible
- Release the wake lock when the player leaves the page or the tab goes to background
- Reacquire the wake lock automatically if the tab returns to the foreground (e.g., after a phone call)
- Degrade gracefully on browsers that do not support the Wake Lock API

**Non-Goals:**
- Keeping the screen awake on the join/lobby section (only needed once in-game)
- Native mobile app changes
- Controlling screen brightness or timeout settings at the OS level

## Decisions

### Use the Screen Wake Lock API

The [Screen Wake Lock API](https://developer.mozilla.org/en-US/docs/Web/API/Screen_Wake_Lock_API) (`navigator.wakeLock.request('screen')`) is the standard browser mechanism. It is supported in Chrome 84+, Edge 84+, and Safari 16.4+ (covering the vast majority of players).

**Alternatives considered:**
- Playing a silent looping video — works cross-browser but wastes bandwidth and battery; dismissed
- `NoSleep.js` — a third-party library using the same API plus the video hack as fallback; adds an external dependency for trivial gain; dismissed
- Polling the server on a short interval — keeps the JS event loop busy but does not prevent the screen lock; dismissed

### Reacquire on `visibilitychange`

The wake lock is automatically released by the browser when the page is hidden (call, notification shade, tab switch). Adding a `visibilitychange` listener that re-requests the lock when the page becomes visible again ensures continuous coverage.

### Graceful fallback — log only, no user-visible indicator

Browsers that lack the API simply won't have the lock. Showing a warning adds UI noise for a minority of users; the game remains fully playable without it. A `console.warn` is sufficient.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Safari < 16.4 does not support Wake Lock API | Graceful fallback via `try/catch` and capability check; game still works |
| Some browser privacy modes may block the API | Same fallback path |
| Wake lock holds battery longer | Intentional — players are actively using the device during a game round |

## Migration Plan

1. Add wake lock JS to `renderBuzzerPage()` in `PageRoute.kt`
2. Deploy the updated server — no migration needed; existing sessions are unaffected (page refresh picks up the new code)
3. Rollback: revert the JS block in `PageRoute.kt`
