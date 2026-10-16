# SICK

Desktop host for SIQ quiz games: the host runs the game, players buzz in from their phones, and the Shared Display shows the board and scores to the room.

## Language

### Room

**Shared Display**:
The window on the screen the whole room sees: the board, the question with its media, the timer and the scores. It shows an answer only once the answer is shown, and it is the only window that plays sound.
_Avoid_: Display, shared screen, projector

### Buzzing

**Buzz**:
A player's press of the buzzer button on their phone, claiming the right to answer the current question.
_Avoid_: Click, press, tap

**Buzz Window**:
The interval during which buzzes for the current question are accepted. It opens when the question is revealed, and reopens after a wrong answer leaves other players eligible or after the game resumes from a pause.
_Avoid_: First opportunity, buzz phase

**Reaction Time**:
Time from the opening of the current Buzz Window to the moment a Buzz reaches the host.
_Avoid_: Click time, buzz time, latency

**Answering Player**:
The player who currently has the right to answer: the first eligible Buzz in the Buzz Window, or a Host Pick.
_Avoid_: Buzzer, winner

**Late Buzz**:
A Buzz from an eligible player that arrives after the Answering Player has been determined in the same Buzz Window. It is recorded with its Reaction Time but grants no right to answer.
_Avoid_: Too slow, rejected buzz, queued buzz

**Host Pick**:
The host manually choosing the Answering Player instead of waiting for a Buzz. It counts as that player's Buzz at the moment of the pick, so it has a Reaction Time.
_Avoid_: Manual buzz

### Questions

**Chooser**:
The player who picks the next question on the board. At the start of each round the host names the Chooser from the players with the lowest score; afterwards the role passes to the player whose answer the host accepts. The Chooser keeps the role while the question plays and may buzz like anyone else.
_Avoid_: Active player, board controller

**Point Answer**:
An answer that is a spot on the question image (SIQ `answerType="point"`), with an accepted radius (`answerDeviation`). The host sees the spot marked on the image while the question plays; the Shared Display marks it only when the answer is shown. Players answer aloud and the host judges.
_Avoid_: Coordinates answer, map answer

**Media Replay**:
The host restarting the question's audio and video from the first one after the Media Hold has ended. The question timer keeps running: if it expires during a replay, the replay stops and the question ends. A Buzz pauses a replay the same way it pauses the first playback.
_Avoid_: Repeat, replay sound

**Media Hold**:
The stretch while the question's audio and video play for the first time, one after another in pack order, during which the question timer does not run. A missing or failing media passes the turn to the next one. It ends after the first playback of the last media, or when the host skips it.
_Avoid_: Media pending, waiting for media

### Judging

**Score Multiplier**:
The scale (x0.5, x1 or x2) the host applies to the question price when accepting an answer. x0.5 rounds up, so an odd price never drops its last point. Only the host's controls show the multiplier; the Shared Display and phones see just the resulting score. A wrong answer always costs the full price.
_Avoid_: Bonus, coefficient
