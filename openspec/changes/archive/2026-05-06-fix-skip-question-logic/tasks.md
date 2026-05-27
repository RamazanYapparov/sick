## 1. Modify Auto-Skip Logic

- [x] 1.1 Update `PlayerSkipped` handling in `GameEngine.kt` to redefine the auto-skip threshold: skip triggers when all players are in either `skipVotePlayerIds` or `failedBuzzPlayerIds` (modify line 123-124)
- [x] 1.2 Update the `nextPhase` logic for `HostRejected` at line 222 to also consider `skipVotePlayerIds` when checking if all players have failed, ensuring consistency with the new auto-skip behavior

## 2. Prevent Skip Voters from Buzzing

- [x] 2.1 Add validation in `PlayerBuzzed` handling (line 114-117) to reject players who are in `skipVotePlayerIds` with `GameError.InvalidEvent`

## 3. Add Tests for Edge Cases

- [x] 3.1 Add test: Player A skips → Player B buzzes and answers wrong → verify auto-skip triggers and phase transitions to `ShowingAnswer`
- [x] 3.2 Add test: Player A answers wrong → Player B votes to skip → verify auto-skip triggers and phase transitions to `ShowingAnswer`
- [x] 3.3 Add test: Player votes to skip → verify player cannot buzz afterwards (engine returns `GameError.InvalidEvent`)
- [x] 3.4 Add test: Multiple players, all either skip or fail → verify auto-skip triggers correctly

## 4. Verify Existing Tests Pass

- [x] 4.1 Run existing `GameEnginePlayerSkipTest.kt` tests to ensure no regressions
- [x] 4.2 Run all core module tests with `./gradlew :core:test` to verify no regressions
