## 1. QuestionBoard UI

- [x] 1.1 Add `var showCompleted by remember { mutableStateOf(false) }` local state in `QuestionBoard`
- [x] 1.2 Compute `val completedThemes` and `val activeThemes` by splitting `themes` on `all { it.played }`
- [x] 1.3 Render toggle button ("Show completed" / "Hide completed") above theme list when `completedThemes.isNotEmpty()`
- [x] 1.4 Render only `activeThemes` when `showCompleted = false`, render all themes when `showCompleted = true`

## 2. Verification

- [ ] 2.1 Manual test: play all questions in one theme → card disappears, toggle button appears
- [ ] 2.2 Manual test: click "Show completed" → hidden theme reappears, button says "Hide completed"
- [ ] 2.3 Manual test: click "Hide completed" → theme hides again
- [ ] 2.4 Manual test: themes with unplayed questions always visible regardless of toggle
