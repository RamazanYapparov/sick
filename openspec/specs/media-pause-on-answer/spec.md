## ADDED Requirements

### Requirement: Media pauses when player buzzes in
When a player buzzes in during media playback, the media SHALL pause immediately. The playback position SHALL be preserved.

#### Scenario: Player buzzes while audio is playing
- **WHEN** the question phase is `ShowingQuestion` and audio is actively playing
- **WHEN** a player buzzes in (phase transitions to `PlayerAnswering`)
- **THEN** the audio player SHALL pause immediately

#### Scenario: Player buzzes while video is playing
- **WHEN** the question phase is `ShowingQuestion` and video is actively playing
- **WHEN** a player buzzes in (phase transitions to `PlayerAnswering`)
- **THEN** the video player SHALL pause immediately

#### Scenario: Player buzzes with no media
- **WHEN** the question has no audio or video content
- **WHEN** a player buzzes in
- **THEN** no media state changes occur

### Requirement: Media resumes when wrong answer is given
When the host rejects a player's answer, returning the question to active play, media SHALL resume from the paused position.

#### Scenario: Wrong answer resumes audio
- **WHEN** audio was paused because a player buzzed in
- **WHEN** the host marks the answer as wrong (`HostRejected`, phase returns to `ShowingQuestion`)
- **THEN** the audio player SHALL resume playback from the paused position

#### Scenario: Wrong answer resumes video
- **WHEN** video was paused because a player buzzed in
- **WHEN** the host marks the answer as wrong (`HostRejected`, phase returns to `ShowingQuestion`)
- **THEN** the video player SHALL resume playback from the paused position

### Requirement: Media stops when correct answer is given
When the host accepts a player's answer, media SHALL stop completely.

#### Scenario: Correct answer stops audio
- **WHEN** audio was paused because a player buzzed in
- **WHEN** the host marks the answer as correct (`HostAccepted`, phase transitions to `ShowingAnswer`)
- **THEN** the audio player SHALL stop

#### Scenario: Correct answer stops video
- **WHEN** video was paused because a player buzzed in
- **WHEN** the host marks the answer as correct (`HostAccepted`, phase transitions to `ShowingAnswer`)
- **THEN** the video player SHALL stop
