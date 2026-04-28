## Context

In the `ChoosingPlayer` phase, the host currently sees all players in a chip row and manually picks who controls the board. The `GameEngine` accepts any `SelectActivePlayer` event without restriction. `GameState.players` carries each player's current `score: Int`. The UI derives state via `withEngineSnapshot()` into `DesktopUiState`, and `DesktopSessionController` dispatches events.

## Goals / Non-Goals

**Goals:**
- Restrict the host's player picker to candidates with the minimum score
- Auto-select and advance when exactly one candidate exists
- Keep the change local to service + UI layers; engine validation unchanged

**Non-Goals:**
- Enforcing candidate restriction inside the engine (engine still accepts any `SelectActivePlayer`)
- Tracking or displaying who started each round historically
- Changing auto-selection logic for the first round (all scores are 0 → all tied → host still picks)

## Decisions

**1. Candidates computed as a pure service function in `core/`**

Add `fun GameState.lowestScoreCandidates(): List<Player>` in `core/service/`. Returns all players whose score equals `players.minOf { it.score }`. Returns empty list when there are no players.

Alternatives: compute inline in UI — rejected because it belongs in the domain layer and needs to be testable.

**2. Expose candidates in `DesktopUiState`**

Add `lowestScoreCandidates: List<Player>` to `DesktopUiState`, populated in `withEngineSnapshot()`. The `ChoosingPlayer` UI branch reads this field instead of `players`.

Alternatives: derive in Composable — rejected because it couples business logic to UI.

**3. Auto-selection in `DesktopSessionController`, not in the engine**

When the engine transitions to `ChoosingPlayer`, the controller checks `state.lowestScoreCandidates`. If exactly one candidate, it immediately fires `selectActivePlayer(candidate.id)`. The engine remains a pure event processor.

Alternatives: engine fires a synthetic event — rejected because the engine should not self-drive; it processes external events only.

**4. First round behavior (all scores == 0)**

All players are tied at 0, so all are candidates. The host picks as before. No special-casing needed.

## Risks / Trade-offs

- **Stale state between phase change and auto-select** → Mitigated: state machine is synchronous; the controller reads `lowestScoreCandidates` from the just-updated snapshot before returning control to the UI.
- **Score ties are common early in the game** → Host still picks among tied players; no worse than current behavior.
- **Auto-select bypasses host input** → Intentional per spec; only fires when uniquely determined by rules.
