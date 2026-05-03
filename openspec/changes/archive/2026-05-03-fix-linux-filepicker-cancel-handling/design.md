## Context

The recent `fix-filepicker-dialog` change introduced a Linux dispatch chain:

```kotlin
private fun pickLinux(): Path? = tryKdialog() ?: tryZenity() ?: pickSwing()
```

Each `try*` function returns `Path?`, collapsing two distinct outcomes into one value:

1. The tool is **unavailable** (binary missing — `IOException` from `ProcessBuilder.start()`).
2. The tool ran but the user **cancelled** (process exits non-zero, or empty output).

The `?:` chain treats both as "try the next thing", which is wrong for case 2. A user who cancels `kdialog` should not be ambushed by `zenity` and then `JFileChooser`.

## Goals / Non-Goals

**Goals:**
- Linux fallback fires only when the previous tool is genuinely unavailable.
- Cancelling any picker returns `null` immediately, no further dialogs.
- Public API `pickSiqFile(): Path?` is unchanged.

**Non-Goals:**
- Changing macOS or Windows behavior.
- Caching availability across calls (single picker invocation is short-lived).
- Logging / telemetry of which tool was picked.

## Decisions

### Decision 1: Use `arrow.core.Either<PickError, Path>` for per-tool results

`arrow.core` is already used in the `core` module (`engine/GameEngine.kt`, `service/PlayerOps.kt`). The `composeApp` module depends on `core`, so `Either` is reachable; if not transitively visible we add it directly.

```kotlin
private sealed interface PickError {
    data object Cancelled : PickError
    data object Unavailable : PickError
}

private fun tryKdialog(): Either<PickError, Path>
private fun tryZenity(): Either<PickError, Path>
```

**Alternatives considered:**
- *Plain Kotlin `Result<Path?>`*: `Result` is for exceptions, and `null` would still be ambiguous between "cancelled" and "no result". Wrapping it in a custom sealed type defeats the point.
- *Sealed interface with three variants (`Picked` / `Cancelled` / `Unavailable`)*: Equally valid and arguably clearer than `Either`. We pick `Either` because the user explicitly asked for `Either` or `Result`, and `Either<Error, Value>` is already the project's idiom for fallible operations.
- *Boolean "available" probe before calling*: Race-prone and doubles process spawns.

### Decision 2: Map outcomes to `PickError` variants

| Observation                                            | Mapped to                |
| ------------------------------------------------------ | ------------------------ |
| `ProcessBuilder.start()` throws `IOException`          | `Left(Unavailable)`      |
| Process exits 0 with non-blank stdout                  | `Right(Path.of(stdout))` |
| Process exits non-zero (kdialog/zenity cancel = exit 1)| `Left(Cancelled)`        |
| Process exits 0 with blank stdout                      | `Left(Cancelled)`        |
| Any other `Exception` during spawn                     | `Left(Unavailable)`      |

The "exit 0 + blank stdout" case is a defensive bucket that probably doesn't happen in practice for these tools but is safer routed to `Cancelled` (don't escalate to the next tool over what looks like a benign empty selection).

### Decision 3: `pickLinux` shape

```kotlin
private fun pickLinux(): Path? {
    for (attempt in listOf(::tryKdialog, ::tryZenity)) {
        when (val r = attempt()) {
            is Either.Right -> return r.value
            is Either.Left -> when (r.value) {
                PickError.Cancelled -> return null
                PickError.Unavailable -> continue
            }
        }
    }
    return pickSwing()
}
```

`pickSwing()` keeps its `Path?` shape — Swing's `JFileChooser` doesn't have an "unavailable" failure mode worth modeling, so `null` cleanly means cancelled at that terminal step.

## Risks / Trade-offs

- **`PickError` is private to the file** — no API surface escapes, so this can be revisited (e.g. flipped to a sealed class with messages) without coordinating with callers.
- **kdialog/zenity exit-code conventions**: both use `1` on cancel, `0` on accept. Stable across distributions in practice. If a future version diverges, the worst case is degraded fallback, not data loss.
- **arrow dependency on `composeApp`**: if not transitively exposed by `core`, we add `implementation(libs.arrow.core)` to `composeApp/build.gradle.kts`. Tiny dependency cost.

## Migration Plan

1. Edit `SiqFilePicker.kt` to introduce `PickError` and switch `tryKdialog`/`tryZenity` to `Either<PickError, Path>`.
2. Rewrite `pickLinux()` to dispatch on the `Either` results.
3. Verify `arrow.core` is on `composeApp`'s classpath; add `implementation(libs.arrow.core)` if compilation fails.
4. Build and manually test on Linux (KDE Plasma): cancel `kdialog` → expect no second dialog.

**Rollback**: revert the single file (and the build.gradle change if needed).

## Open Questions

_(none)_
