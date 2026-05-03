## Why

The Linux file picker fallback chain in `SiqFilePicker.kt` (`tryKdialog() ?: tryZenity() ?: pickSwing()`) uses `null` as the only failure signal. But `null` is overloaded — both "the tool is not installed / failed to spawn" and "the user cancelled the dialog" return `null`. The result: when a KDE user cancels the `kdialog` picker, `zenity` pops up next, and if the user cancels again, `JFileChooser` appears. The user has to dismiss the same intent three times.

The fallback should only fire when the previous tool is genuinely **unavailable**, not when the user **cancelled**.

## What Changes

- Introduce a typed result for each Linux picker attempt that distinguishes three outcomes: picked / cancelled / unavailable.
- Convert `tryKdialog()` and `tryZenity()` to return `Either<PickError, Path>` (using `arrow.core.Either`, already in the `composeApp` module dependencies), where `PickError` is a sealed type with `Cancelled` and `Unavailable` variants.
- Rewrite `pickLinux()` to fall through to the next picker only on `Unavailable`; on `Cancelled` it returns `null` immediately.
- Detect `Unavailable` from `IOException` at process spawn (`kdialog`/`zenity` not on `PATH`); detect `Cancelled` from a clean exit with no output (kdialog exits 1 on cancel; zenity exits 1 on cancel).

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `native-file-picker`: Linux fallback semantics change — fallthrough is now driven by tool availability, not by null result.

## Impact

- `composeApp/src/desktopMain/kotlin/app/session/SiqFilePicker.kt` — change return types of `tryKdialog`/`tryZenity` and rewrite `pickLinux`.
- No new dependencies (`arrow.core` already on `composeApp`'s classpath via `core`'s `api` configuration, or added directly if needed).
- No caller changes — `pickSiqFile(): Path?` signature is preserved.
