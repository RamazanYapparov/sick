# SICK

Desktop host for SIQ quiz games: the host runs the game, players buzz in from their phones, and a shared screen shows the board and scores to the room.

## Language

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

**Point Answer**:
An answer that is a spot on the question image (SIQ `answerType="point"`), with an accepted radius (`answerDeviation`). The host sees the spot marked on the image while the question plays; the shared display marks it only when the answer is shown. Players answer aloud and the host judges.
_Avoid_: Coordinates answer, map answer

**Media Replay**:
The host restarting the question's audio or video from the beginning after its first playback has ended. The question timer keeps running: if it expires during a replay, the replay stops and the question ends. A Buzz pauses a replay the same way it pauses the first playback.
_Avoid_: Repeat, replay sound

### Judging

**Score Multiplier**:
The scale (x0.5, x1 or x2) the host applies to the question price when accepting an answer. x0.5 rounds up, so an odd price never drops its last point. Only the host's controls show the multiplier; the shared display and phones see just the resulting score. A wrong answer always costs the full price.
_Avoid_: Bonus, coefficient
