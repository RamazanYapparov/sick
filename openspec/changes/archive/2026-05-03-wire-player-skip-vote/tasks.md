## 1. Server route

- [x] 1.1 Create `server/src/main/kotlin/com/sick/server/routes/SkipRoute.kt` with `Application.installSkipRoute(engine: GameEngine, buzzAllowed: () -> Boolean)` mirroring `BuzzRoute.kt`
- [x] 1.2 Inside the route, accept `playerId` form parameter, parse as UUID, call `engine.process(PlayerSkipped(playerId))`, and map left/right to 400/200
- [x] 1.3 Return 503 when `buzzAllowed()` is false (timer paused), matching `/buzz` behavior
- [x] 1.4 Wire `installSkipRoute` into `GameServer.start()` next to `installBuzzRoute`

## 2. Player UI

- [x] 2.1 In `PageRoute.kt` `renderBuzzerPage()`, add a SKIP `<button id="skip" onclick="doSkip()">SKIP</button>` inside `#buzz-section`
- [x] 2.2 Add CSS for `#skip` reusing the shared button properties (border, padding, weight) but with a distinct background color so it's visually different from BUZZ
- [x] 2.3 Add a `doSkip()` JS function that POSTs `playerId` to `/skip` and updates `#status` ("Skipped!" on 200, "Too late!" on 4xx, "Connection lost." on network error)
- [x] 2.4 On 200 leave the SKIP button disabled; on 4xx re-enable it

## 3. Tests

- [x] 3.1 Create `server/src/test/kotlin/com/sick/server/SkipRouteTest.kt` mirroring `BuzzRouteTest`
- [x] 3.2 Test: `POST /skip` returns 200 and records the vote in `skipVotePlayerIds` when called in `ShowingQuestion` for an eligible player
- [x] 3.3 Test: `POST /skip` returns 400 when `playerId` is missing
- [x] 3.4 Test: `POST /skip` returns 400 when `playerId` is not a valid UUID
- [x] 3.5 Test: `POST /skip` returns 400 when the engine rejects the event (e.g., wrong phase)
- [x] 3.6 Test: `POST /skip` returns 503 when `buzzAllowed` is false

## 4. Verification

- [x] 4.1 Run `./gradlew :server:test` and confirm all server tests pass
- [x] 4.2 Run `./gradlew :composeApp:run`, join from a browser, and confirm the SKIP button appears, posts successfully during a question, and shows the expected status text
