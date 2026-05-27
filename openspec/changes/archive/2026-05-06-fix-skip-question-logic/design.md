## Context

The current skip vote system in `GameEngine.kt` tracks `skipVotePlayerIds` and `failedBuzzPlayerIds` separately. The auto-skip check at line 124 only considers `skipVotePlayerIds` against eligible players (those not in `failedBuzzPlayerIds`). This creates edge cases:

1. Player A skips → Player B buzzes and answers wrong → Question doesn't auto-skip (Player A already voted to skip, Player B is now failed, so all eligible players are "done")
2. Player A answers wrong → Player B votes to skip → Question doesn't auto-skip (Player A is failed, Player B voted to skip, threshold should be met)
3. Player votes to skip, then can still buzz on the same question (should be prevented)

The fix requires modifying the auto-skip condition and adding buzz prevention for skip voters.

## Goals / Non-Goals

**Goals:**
- Auto-skip triggers when all players are either in `skipVotePlayerIds` or `failedBuzzPlayerIds`
- Players who voted to skip cannot buzz for the remainder of the current question
- All three edge cases from the proposal are handled correctly

**Non-Goals:**
- Changing the UI for skip voting (already exists)
- Modifying how `failedBuzzPlayerIds` works for answer rejection flow
- Changing the host's ability to manually skip

## Decisions

### Decision 1: Redefine "eligible players" for auto-skip threshold

**Choice**: Consider a player "accounted for" if they are in EITHER `skipVotePlayerIds` OR `failedBuzzPlayerIds`. Auto-skip when all players are accounted for.

**Rationale**: A player who failed cannot answer correctly anymore, so they effectively want to move on. A player who voted to skip explicitly wants to move on. When all players are in one of these states, the question should skip.

**Alternative considered**: Only count `skipVotePlayerIds` and require failed players to also vote to skip. Rejected because it's poor UX - failed players have already shown they can't answer, forcing them to additionally vote to skip is redundant.

### Decision 2: Prevent skip voters from buzzing

**Choice**: Add a check in `PlayerBuzzed` handling (line 114-117) to also reject players in `skipVotePlayerIds`.

**Rationale**: If a player voted to skip, they've indicated they don't want to answer this question. Allowing them to buzz contradicts their skip vote.

**Alternative considered**: Remove player from `skipVotePlayerIds` if they buzz. Rejected because it creates confusing UX - skip vote would be silently removed.

## Risks / Trade-offs

- [Edge case with HostRejected] → The `HostRejected` handler at line 222 checks if all players are in `failedBuzzPlayerIds` to transition to `ShowingAnswer`. This logic should also consider `skipVotePlayerIds` for consistency. However, this is a separate concern and can be addressed in a follow-up if needed.
- [Player buzzing after skip vote] → The buzz prevention adds a new validation check. This is a breaking change for any client that allows buzzing after skipping, but the server will now reject it with `InvalidEvent`.
