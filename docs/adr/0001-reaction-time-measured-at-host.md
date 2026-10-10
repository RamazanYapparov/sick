# Reaction Time is measured when the Buzz reaches the host

Reaction Time is the time between the Buzz Window opening and the host engine receiving the Buzz, read from a monotonic clock owned by the engine, under the same lock that decides who becomes the Answering Player. The Answering Player is still the first Buzz to arrive, so shown times always match who won. They include phone-to-host network latency, which on Wi‑Fi can exceed the millisecond differences between players.

## Considered Options

- **Phone-side click time with clock sync**: fairer, but it only stays consistent if the winner is also chosen by click time. That needs a collection delay after the first arriving Buzz and a new engine phase before the Answering Player is known. Deferred, not rejected.
- **Show phone-side time, pick the winner by arrival**: rejected, because the screen could show the winner with a slower time than a Late Buzz.
