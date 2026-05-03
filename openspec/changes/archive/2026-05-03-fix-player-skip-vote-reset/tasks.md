## 1. Fix SKIP button in PageRoute.kt

- [x] 1.1 Remove `skipBtn.disabled = true` at the start of `doSkip()` so the button is never disabled on click
- [x] 1.2 Remove `skipBtn.disabled = false` from the failure and catch branches (no longer needed)
- [x] 1.3 Remove the now-unused `const skipBtn` local variable if no other references remain

## 2. Verify

- [x] 2.1 Open the buzzer page, join as a player, vote to skip — confirm "Skipped!" appears and the SKIP button stays enabled
- [x] 2.2 Vote to skip a second time on the same question — confirm server returns 400 and "Too late!" is shown, button still enabled
- [x] 2.3 Start a new question and vote to skip — confirm the vote is accepted (200) on the new question
