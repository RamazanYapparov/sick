## ADDED Requirements

### Requirement: Sequential video questions each play their video
When a session shows multiple video questions in sequence, every video question SHALL render and start playing its video. The first video playing successfully MUST NOT prevent or break playback of any subsequent video question.

#### Scenario: Second video question plays after first finishes
- **WHEN** the host has shown a video question whose video played to completion
- **WHEN** the host advances to a later question that also has a video
- **THEN** the new video player SHALL load the new media and begin playback
- **AND** the rendered video area SHALL display video frames (not a blank white panel)

#### Scenario: Second video question plays after first is interrupted
- **WHEN** the host has shown a video question and the question ended (correct answer accepted, skipped, or revealed) while the video was still playing or paused
- **WHEN** the host advances to a later question that also has a video
- **THEN** the new video player SHALL load the new media and begin playback
- **AND** the rendered video area SHALL display video frames (not a blank white panel)

#### Scenario: Many video questions in one session
- **WHEN** the host plays N consecutive video questions in the same session (N ≥ 3)
- **THEN** each of the N video questions SHALL successfully render and play its video on the shared display

### Requirement: Video load failures are visible
When the video player cannot construct media for a given URI, the failure SHALL be made visible to the host instead of producing a silent blank panel.

#### Scenario: Unreadable local video file
- **WHEN** a video question references a local file that cannot be opened by the JavaFX media stack
- **THEN** the video area SHALL display an error message identifying that the video could not be loaded
- **AND** the underlying error SHALL be logged

#### Scenario: Unreachable remote video URL
- **WHEN** a video question references a remote URL that the JavaFX media stack rejects
- **THEN** the video area SHALL display an error message identifying that the video could not be loaded
- **AND** the underlying error SHALL be logged
