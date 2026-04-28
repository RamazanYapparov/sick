## 1. Modify PlayerEditorRow

- [x] 1.1 Add `var expanded by remember { mutableStateOf(false) }` inside `PlayerEditorRow`
- [x] 1.2 Render collapsed state: player name + score text and "Edit" button when `!expanded`
- [x] 1.3 Render expanded state: score delta input + "Apply" button when `expanded`
- [x] 1.4 Set `expanded = false` inside the "Apply" click handler after calling `onAdjustScore`

## 2. Verify

- [x] 2.1 Run app and confirm player rows show collapsed state by default
- [x] 2.2 Click "Edit" — confirm inputs appear and "Edit" button hides
- [x] 2.3 Enter delta and click "Apply" — confirm score updates and row collapses
- [x] 2.4 Confirm multiple rows can be independently expanded simultaneously
