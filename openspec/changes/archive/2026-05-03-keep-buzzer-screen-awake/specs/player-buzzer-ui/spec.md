## ADDED Requirements

### Requirement: Buzzer screen requests a screen wake lock on activation
When the buzz section becomes active (after successful join), the player buzzer UI SHALL invoke the wake lock capability to keep the device screen on for the duration of the game session.

#### Scenario: Wake lock is requested as part of transitioning to the buzz view
- **WHEN** the join flow completes successfully and the buzz section is shown
- **THEN** a screen wake lock request is initiated before or immediately after the buzz section becomes visible
