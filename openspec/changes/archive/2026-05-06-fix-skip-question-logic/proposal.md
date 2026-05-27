## Why

The skip question logic has edge cases where questions don't auto-skip when they should. When all active players have either voted to skip or given wrong answers, the question should automatically skip, but currently doesn't. Additionally, players who voted to skip can still buzz, which is incorrect since they've already indicated they want to skip the question.

## What Changes

- Modify the auto-skip threshold check to consider players who already failed (in `failedBuzzPlayerIds`) as having "voted" to skip, since they can no longer answer correctly
- Prevent players who voted to skip from buzzing during the current question
- Handle edge case: when a player answers wrong and all remaining eligible players have already voted to skip, auto-skip should trigger
- Handle edge case: when a player votes to skip and all other eligible players have already failed, auto-skip should trigger immediately

## Capabilities

### New Capabilities
<!-- No new capabilities -->

### Modified Capabilities
- `player-skip-vote`: Auto-skip trigger condition now considers failed players as having effectively voted to skip; skip voters are prevented from buzzing

## Impact

- `core/state/` - Game state machine logic for skip vote checking and buzz prevention
- `core/src/main/kotlin/engine/GameEngine.kt` - Skip vote processing and buzz event handling
- `core/src/main/kotlin/model/game.kt` - Potentially `skipVotePlayerIds` semantics
- `core/src/test/kotlin/com/sick/engine/GameEnginePlayerSkipTest.kt` - Additional test cases for edge cases
