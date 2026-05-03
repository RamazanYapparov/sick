## Context

The host window's Session card contains a row of three buttons: "Load Pack", "Create Game", and "Show Display". The display window is opened automatically when a session starts and remains open for the duration of the session, so the "Show Display" button is never necessary.

## Goals / Non-Goals

**Goals:**
- Remove the "Show Display" button from the host window UI.

**Non-Goals:**
- Changing display window lifecycle (it stays always-open).
- Removing the underlying `showDisplayWindow()` method on the controller (may be used elsewhere or kept for future use).

## Decisions

**Remove only the Button composable, not the controller method.**
The `showDisplayWindow()` function in `DesktopSessionController` is a legitimate entry point. Removing only the UI element is the minimal safe change. If the method becomes dead code after removal, it can be cleaned up in a separate pass.

## Risks / Trade-offs

- No meaningful risks. The button has no side effects beyond what the display window's own lifecycle already handles.
