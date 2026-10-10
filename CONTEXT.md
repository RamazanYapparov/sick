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
