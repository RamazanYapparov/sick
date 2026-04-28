## Why

In SIQ rules, the player with the lowest score earns the right to pick the next question. Currently the host must manually select from all players, requiring knowledge of scores and introducing error. Automating this matches game rules and removes friction.

## What Changes

- Host can now choose first-picker only from players tied for lowest score
- If exactly one player has the lowest score, they are auto-selected — no host action needed
- Host choice UI filters the player list to lowest-score candidates only
- The `ChoosePlayerToStart` state receives the filtered candidate list

## Capabilities

### New Capabilities

- `lowest-score-player-selection`: Logic and UI for determining round-start player based on minimum score, with auto-selection when the minimum is unique

### Modified Capabilities

<!-- none -->

## Impact

- `core/state/` — `ChoosePlayerToStart` state and its entry logic
- `core/service/` — new or updated function to compute lowest-score candidates
- `composeApp/` — host UI for picking the starting player filtered to candidates; auto-advance when single candidate
