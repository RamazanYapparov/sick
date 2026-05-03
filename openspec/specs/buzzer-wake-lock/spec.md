## ADDED Requirements

### Requirement: Screen wake lock is acquired when the buzz section becomes active
When a player successfully joins the game and the buzz section is displayed, the page SHALL request a screen wake lock to prevent the device display from turning off.

#### Scenario: Wake lock acquired on join
- **WHEN** a player submits a valid name and the server responds with success
- **THEN** the buzz section is shown AND a screen wake lock is requested via the Wake Lock API

#### Scenario: No error shown when Wake Lock API is unsupported
- **WHEN** the player's browser does not support `navigator.wakeLock`
- **THEN** the buzz section is displayed normally and no error or warning is shown to the user

### Requirement: Wake lock is reacquired after the page returns to the foreground
If the browser releases the wake lock because the page was hidden (e.g., the player switched apps), the page SHALL reacquire the lock when the page becomes visible again.

#### Scenario: Reacquisition after visibility change
- **WHEN** the page was hidden (document visibility state was "hidden") and then becomes visible again
- **THEN** a new screen wake lock is requested if the buzz section is active

### Requirement: Wake lock is released on page unload
The page SHALL release any held wake lock when the player navigates away or closes the tab, allowing the device to resume its normal sleep behavior.

#### Scenario: Wake lock released on navigation
- **WHEN** the player closes the tab or navigates away from the buzzer page
- **THEN** the wake lock sentinel is released (either explicitly or via browser garbage collection)
