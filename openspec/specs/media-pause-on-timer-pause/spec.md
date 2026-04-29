## Requirements

### Requirement: Media pauses when host pauses the timer
When the host clicks Pause during an active question, media playback SHALL pause immediately. The playback position SHALL be preserved.

#### Scenario: Host pauses while audio is playing
- **WHEN** the question phase is `ShowingQuestion` and audio is actively playing
- **WHEN** the host clicks Pause (timer transitions to paused)
- **THEN** the audio player SHALL pause immediately

#### Scenario: Host pauses while video is playing
- **WHEN** the question phase is `ShowingQuestion` and video is actively playing
- **WHEN** the host clicks Pause (timer transitions to paused)
- **THEN** the video player SHALL pause immediately

#### Scenario: Host pauses with no active media
- **WHEN** the question has no audio or video content
- **WHEN** the host clicks Pause
- **THEN** no media state changes occur

### Requirement: Media resumes when host resumes the timer
When the host clicks Resume, media playback SHALL resume from the paused position.

#### Scenario: Host resumes with audio paused
- **WHEN** audio was paused because the host paused the timer
- **WHEN** the host clicks Resume (timer resumes)
- **THEN** the audio player SHALL resume playback from the paused position

#### Scenario: Host resumes with video paused
- **WHEN** video was paused because the host paused the timer
- **WHEN** the host clicks Resume (timer resumes)
- **THEN** the video player SHALL resume playback from the paused position
