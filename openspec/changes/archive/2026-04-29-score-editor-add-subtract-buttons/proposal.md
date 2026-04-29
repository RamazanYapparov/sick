## Why

The score editor currently accepts negative numbers to subtract points, which is error-prone and unintuitive for hosts. Replacing the single "Apply" button with explicit "+" and "−" buttons removes ambiguity and makes score adjustments faster.

## What Changes

- Score delta input field restricted to positive integers only (no negative values accepted)
- "Apply" button removed from the score editor row
- "+" button added — applies the entered value as a positive score delta
- "−" button added — applies the entered value as a negative score delta

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `collapsible-score-editor`: Replace "Apply" button with "+" and "−" buttons; input restricted to positive numbers only

## Impact

- `composeApp/` — UI component for the player row score editor needs button layout change and input validation
- `openspec/specs/collapsible-score-editor/spec.md` — Requirements updated to reflect new button scheme and input constraint
