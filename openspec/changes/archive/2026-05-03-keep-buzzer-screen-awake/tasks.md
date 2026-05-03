## 1. Wake Lock Implementation

- [x] 1.1 In `PageRoute.kt`, add a `wakeLockSentinel` variable and a `requestWakeLock()` helper function in the page's `<script>` block that calls `navigator.wakeLock.request('screen')` inside a `try/catch`, storing the returned sentinel and logging a warning if the API is unsupported
- [x] 1.2 Call `requestWakeLock()` inside `doJoin()`'s success path, after switching from the join section to the buzz section
- [x] 1.3 Add a `document.addEventListener('visibilitychange', ...)` handler that calls `requestWakeLock()` when `document.visibilityState === 'visible'` and the buzz section is active

## 2. Verification

- [ ] 2.1 Open the buzzer page on a real phone (or Chrome DevTools device emulation), join a game, and confirm the screen does not dim or lock during an idle period
- [ ] 2.2 Confirm that switching apps and returning to the tab reacquires the lock (no error thrown, screen stays on)
- [ ] 2.3 Confirm graceful fallback: test in a browser with Wake Lock API disabled (e.g., Firefox without flag) — page should work normally with no visible error
