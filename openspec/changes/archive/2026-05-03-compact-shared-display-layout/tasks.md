## 1. QuestionBoard: single-row questions and fill-height support

- [x] 1.1 Add `fillHeight: Boolean = false` parameter to `QuestionBoard` composable in `QuestionBoard.kt`
- [x] 1.2 When `fillHeight = true`, apply `Modifier.fillMaxSize()` to the outer `Column` and `Modifier.weight(1f)` to each theme item so themes distribute evenly
- [x] 1.3 Replace `FlowRow` with `Row` for question buttons; apply `Modifier.weight(1f)` to each button so they share row width evenly
- [x] 1.4 Add `maxLines = 1` and `overflow = TextOverflow.Ellipsis` to theme name `Text` to prevent wrapping

## 2. SharedDisplay: compact header

- [x] 2.1 Replace the multi-line header `Column` in `SharedDisplayScreen` with a single compact `Text` line showing "Round X / Y" when a round is active, or "Lobby" otherwise
- [x] 2.2 Pass `fillHeight = true` to the `QuestionBoard` call inside `BoardOverview` so the board uses all available vertical space on the shared display
