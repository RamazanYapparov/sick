## 1. Update PlayerEditorRow component

- [x] 1.1 Change `onAdjustScore` parameter signature from `() -> Unit` to `(Int) -> Unit` in `PlayerComponents.kt`
- [x] 1.2 Update `onValueChange` handler to strip all non-digit characters (keep digits only, no minus sign)
- [x] 1.3 Remove the "Apply" button and the "Use negative values to subtract." hint text
- [x] 1.4 Add "+" button: disabled when input is blank or "0", on click calls `onAdjustScore(value)` and collapses
- [x] 1.5 Add "−" button: disabled when input is blank or "0", on click calls `onAdjustScore(-value)` and collapses

## 2. Update call site in HostWindow

- [x] 2.1 Update `onAdjustScore` lambda in `HostWindow.kt` to accept signed `Int` and pass it directly to `controller.adjustScore(player.id, it)` (remove inline `toIntOrNull` parsing)
