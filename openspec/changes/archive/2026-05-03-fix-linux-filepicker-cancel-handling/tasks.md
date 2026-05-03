## 1. Confirm classpath

- [x] 1.1 Verify `arrow.core.Either` is reachable from `composeApp/src/desktopMain` (try a one-line import); if not, add `implementation(libs.arrow.core)` to `composeApp/build.gradle.kts`

## 2. Introduce typed pick result

- [x] 2.1 In `SiqFilePicker.kt`, declare a private sealed interface `PickError` with `data object Cancelled` and `data object Unavailable` variants
- [x] 2.2 Add `import arrow.core.Either` and any companion imports needed (`left`, `right`)

## 3. Rewrite Linux per-tool helpers

- [x] 3.1 Change `tryKdialog()` signature to return `Either<PickError, Path>`
  - `IOException` (and any other spawn exception) → `Left(Unavailable)`
  - Exit code `0` + non-blank stdout → `Right(Path.of(output))`
  - Exit code non-zero, or exit `0` with blank stdout → `Left(Cancelled)`
- [x] 3.2 Apply the same shape to `tryZenity()`

## 4. Rewrite `pickLinux` dispatch

- [x] 4.1 Replace `tryKdialog() ?: tryZenity() ?: pickSwing()` with a loop that:
  - Returns `Right` immediately on success
  - Returns `null` immediately on `Left(Cancelled)`
  - Continues to the next tool on `Left(Unavailable)`
- [x] 4.2 Fall back to `pickSwing()` only when both kdialog and zenity report `Unavailable`

## 5. Verify

- [x] 5.1 `./gradlew :composeApp:build` — zero compilation errors
- [x] 5.2 Manual test on KDE: cancel kdialog → verify no zenity/Swing dialog appears, app receives `null`
- [x] 5.3 Manual test on KDE with `kdialog` temporarily removed from PATH (e.g. `PATH=/usr/local/bin ./gradlew :composeApp:run`): confirm zenity opens
- [x] 5.4 Manual test cancelling zenity in that scenario: confirm Swing fallback does NOT appear
