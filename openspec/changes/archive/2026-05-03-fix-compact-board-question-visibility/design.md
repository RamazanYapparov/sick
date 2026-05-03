## Context

`QuestionBoard` renders each theme as a `Card` containing a `Column` with:
1. `Text` (theme name)
2. `Spacer(8.dp)`
3. `Row` of question buttons

When `fillHeight=true` the outer `Column` is `fillMaxSize()` and each card gets `weight(1f)`. However the inner `Column` always uses `padding(12.dp)` (24 dp vertical total) and the fixed spacer adds 8 dp more. With many themes (6–10) the weighted height per card can easily be smaller than 88 dp (24 dp padding + ~20 dp name + 8 dp spacer + ~36 dp button), causing the question row to overflow the card and become invisible.

## Goals / Non-Goals

**Goals:**
- Make question price buttons always visible in the compact board regardless of theme count
- Reduce card-internal vertical waste when `fillHeight=true`

**Non-Goals:**
- Changing the default (non-fill-height) appearance of `QuestionBoard` used in the host window
- Changing font sizes of theme names or button labels
- Adding scroll when content genuinely cannot fit

## Decisions

### 1. Conditional compact padding inside theme cards

When `fillHeight=true`, apply `padding(horizontal = 8.dp, vertical = 4.dp)` (8 dp vertical total) instead of the default `padding(12.dp)` (24 dp vertical). This alone saves 16 dp per card.

**Alternative considered**: Always use small padding. Rejected — the host window board looks better with the current 12 dp padding; changing it unconditionally would regress the host layout.

### 2. Remove `Spacer` when `fillHeight=true`; use `Arrangement.SpaceBetween` on inner `Column`

Replace the `Spacer(8.dp)` between theme name and question row with `verticalArrangement = Arrangement.SpaceBetween` on the inner `Column` when `fillHeight=true`. This distributes whatever space is available instead of burning a fixed 8 dp.

The inner `Column` must also use `Modifier.fillMaxSize()` when `fillHeight=true` so `SpaceBetween` has space to distribute.

When `fillHeight=false`, keep the existing `Spacer(8.dp)` and no fill modifier.

**Alternative considered**: `Arrangement.spacedBy(4.dp)` instead of SpaceBetween. Rejected — SpaceBetween anchors buttons to the bottom of the card which is more legible at very small row heights.

## Risks / Trade-offs

- **Very small rows**: With 10+ themes the row height may be under 40 dp — enough for a button but the theme name will be single-line ellipsized (already handled). → Acceptable; this is a read-only display.
- **Non-fill layout unaffected**: All changes are gated on `fillHeight`, so host window remains identical.
