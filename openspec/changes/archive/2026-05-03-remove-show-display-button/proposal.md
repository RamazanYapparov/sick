## Why

The "Show Display" button in the host window is redundant — the shared display window is always open and visible during a session. Having a button to show something that is always present adds visual noise and could confuse hosts into thinking the display is optional or togglable.

## What Changes

- Remove the "Show Display" button from the Session section of the host window UI.

## Capabilities

### New Capabilities

*(none)*

### Modified Capabilities

*(none — this is a pure UI cleanup with no behavioral or requirement changes)*

## Impact

- `composeApp/src/desktopMain/kotlin/app/ui/window/HostWindow.kt` — remove the `Button(onClick = controller::showDisplayWindow)` element and its `Text("Show Display")` content from the `Row` in the Session card.
