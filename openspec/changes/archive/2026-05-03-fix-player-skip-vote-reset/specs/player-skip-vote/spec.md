## MODIFIED Requirements

### Requirement: Player UI exposes a SKIP control
The browser buzzer page SHALL render a SKIP button alongside the BUZZ button after a player has joined. The SKIP button SHALL POST the joined player's ID to `/skip` and reflect the response in the page status area. The SKIP button SHALL remain enabled at all times; the server is responsible for rejecting invalid votes.

#### Scenario: Player taps SKIP and the vote is recorded
- **WHEN** a joined player clicks the SKIP button and the server responds `200 OK`
- **THEN** the page shows a "Skipped!" status and the SKIP button remains enabled

#### Scenario: Player taps SKIP and the vote is rejected
- **WHEN** a joined player clicks the SKIP button and the server responds `400`
- **THEN** the page shows a "Too late!" status and the SKIP button remains enabled

#### Scenario: SKIP button hidden before joining
- **WHEN** the page first loads and the player has not yet joined
- **THEN** the SKIP button is not visible (it is part of the same buzz section that is hidden until join succeeds)
