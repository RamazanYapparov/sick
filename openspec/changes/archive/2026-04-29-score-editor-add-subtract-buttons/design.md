## Context

The host screen displays a player row with a collapsible score editor. Currently the editor has a single text input (accepts any integer, including negatives) and one "Apply" button. The host must type a negative number to subtract score — non-obvious and error-prone. The change is confined to the Compose UI layer in `composeApp/`; the core score service already supports both positive and negative deltas.

## Goals / Non-Goals

**Goals:**
- Restrict the score delta input to positive integers only
- Replace "Apply" with two buttons: "+" (add) and "−" (subtract)
- "+" submits `+value`, "−" submits `−value`
- Collapse the editor after either button is pressed (same as current "Apply" behavior)

**Non-Goals:**
- Changes to `core/service/` score logic (already handles signed deltas)
- Changes to the state machine or game model
- Input validation beyond positive-integer enforcement (e.g., max value caps)

## Decisions

**Two separate button callbacks, single input state**
Both "+" and "−" read the same `inputValue` state and pass `+value` or `-value` to the existing score update call. No new state needed.
- Alternative considered: separate `+` / `−` input fields — rejected as more complex for no benefit.

**Positive-integer enforcement at input level**
Use `KeyboardType.Number` and strip non-digit characters from the `onValueChange` handler. Leading zeros normalized to empty or "0".
- Alternative considered: validate only on button press (show error) — rejected; silent filtering is less disruptive for a simple game host UI.

**Button enable/disable**
Both "+" and "−" buttons disabled when input is empty or "0", preventing zero-delta submissions.

## Risks / Trade-offs

- [Input is string-based] → Parse to Int before calling score service; handle empty/invalid string by disabling buttons rather than crashing.
- [Behavior change for hosts] → The old negative-number workflow stops working; hosts must use "−" button instead. Acceptable since the new UX is strictly more intuitive.
