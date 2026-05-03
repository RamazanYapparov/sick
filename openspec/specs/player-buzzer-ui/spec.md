## ADDED Requirements

### Requirement: Buzzer screen requests a screen wake lock on activation
When the buzz section becomes active (after successful join), the player buzzer UI SHALL invoke the wake lock capability to keep the device screen on for the duration of the game session.

#### Scenario: Wake lock is requested as part of transitioning to the buzz view
- **WHEN** the join flow completes successfully and the buzz section is shown
- **THEN** a screen wake lock request is initiated before or immediately after the buzz section becomes visible

## MODIFIED Requirements

### Requirement: BUZZ and SKIP buttons occupy proportional sections of the viewport height
The SKIP button SHALL fill the top 1/3 of the viewport and the BUZZ button SHALL fill the bottom 2/3, each spanning the full viewport width.

#### Scenario: SKIP button fills top third
- **WHEN** the buzzer screen is active
- **THEN** the SKIP button is positioned at the top of the screen and has a height of approximately 33% of the viewport height and spans full width

#### Scenario: BUZZ button fills bottom two-thirds
- **WHEN** the buzzer screen is active
- **THEN** the BUZZ button is positioned below the SKIP button and player strip, and has a height of approximately 67% of the viewport height and spans full width
