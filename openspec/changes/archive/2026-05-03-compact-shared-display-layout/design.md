## Context

The shared display (`SharedDisplayScreen`) has a header section that shows pack name, round info, and game phase. Below it sits a `BoardOverview` card containing `QuestionBoard`, which renders each theme as a `Card` with a `FlowRow` of question buttons. The combined header height plus player cards at the bottom leaves only a small fraction of the screen for the board — typically showing 2-3 themes before requiring scroll, even though the window may have space for 6–10.

The component tree is:
- `SharedDisplayScreen` (Column)
  - Header Row (pack name + round + phase)
  - Box(weight=1) → `BoardOverview` → `QuestionBoard` (Column of theme Cards)
  - `PlayerCards` (bottom bar)

## Goals / Non-Goals

**Goals:**
- Make all themes visible simultaneously in the board view by distributing available height evenly across theme rows
- Collapse the header to a single compact line showing only "Round X / Y" (remove pack name and phase label)
- Render all question buttons for a theme in a single `Row` (no wrapping)

**Non-Goals:**
- Changing the board layout when a question is active (`CurrentQuestionPanel`, `AnswerPanel`, etc.)
- Modifying the host window — only the shared display is affected
- Changing player card appearance or behavior
- Supporting scroll when themes overflow (all themes must fit; if they don't, they shrink)

## Decisions

### 1. Even height distribution via `weight(1f)` on each theme row

Each theme row inside `QuestionBoard` gets `Modifier.weight(1f)` inside a `Column(Modifier.fillMaxSize())`. This makes Compose divide available height equally among themes automatically, with no manual calculation.

**Alternative considered**: Fixed row height. Rejected because the number of themes varies per round, so a fixed height would either waste space or clip content.

### 2. Single `Row` instead of `FlowRow` for questions

Replace `FlowRow` with `Row` and add `horizontalArrangement = Arrangement.spacedBy(...)` with no wrap. Question buttons use `weight(1f)` within the row so they fill the available width evenly regardless of count.

**Alternative considered**: Keep `FlowRow` but restrict max lines to 1. Rejected — `FlowRow` doesn't have a reliable single-line mode; extra items would silently disappear.

### 3. Header reduced to one `Text` line

Replace the multi-line header Column (pack name + round line + phase) with a single compact `Text` reading "Round X / Y" (or "Lobby" when no round is active). This frees significant vertical space.

**Alternative considered**: Keep pack name, just remove phase. Rejected per user requirement — pack name provides little value on the shared display once a game is running.

### 4. `QuestionBoard` receives a `fillHeight: Boolean` parameter

Rather than making `QuestionBoard` always fill height (which would break the host window board), add a `fillHeight: Boolean = false` parameter. When `true`, the outer `Column` uses `Modifier.fillMaxSize()` and each theme item uses `weight(1f)`.

**Alternative considered**: A separate `SharedDisplayQuestionBoard` composable. Rejected as unnecessary duplication for a one-parameter difference.

## Risks / Trade-offs

- **Many themes, small buttons**: With 8+ themes the question buttons become very small. → Acceptable; this is a read-only display, buttons aren't clickable on the shared screen.
- **Long theme names**: In a fixed-height row the name might clip. → Mitigate with `maxLines = 1` + `overflow = TextOverflow.Ellipsis`.
- **Host window regression**: The host window also uses `QuestionBoard`; the new `fillHeight` parameter defaults to `false` so existing call sites are unaffected.
